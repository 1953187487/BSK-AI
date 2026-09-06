package com.bskai.agent

import android.content.Context
import com.bskai.data.SettingsRepository
import com.bskai.data.ChatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ChatMsg(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class LlmResponse(
    val content: String,
    val toolCalls: List<ToolCall>
)

sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class Done(val fullContent: String) : StreamEvent
    data class Error(val message: String) : StreamEvent
}

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val contextLength: Int = 4096
)

class LlmClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun listModels(url: String, apiKey: String? = null): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("$url/models")
                .addHeader("Accept", "application/json")
            if (!apiKey.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $apiKey")
            }
            val response = client.newCall(builder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            parseModels(body)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseModels(json: String): List<ModelInfo> {
        val list = mutableListOf<ModelInfo>()
        try {
            val root = JSONObject(json)
            val data = root.optJSONArray("data") ?: return emptyList()
            for (i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                val id = o.optString("id")
                if (id.isNotEmpty()) {
                    list.add(ModelInfo(
                        id = id,
                        name = o.optString("name", id),
                        description = o.optString("description", ""),
                        contextLength = o.optInt("context_length", 4096)
                    ))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun downloadModel(
        url: String,
        targetFile: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false
            val body = response.body ?: return@withContext false
            val total = body.contentLength()
            targetFile.parentFile?.mkdirs()
            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buf = ByteArray(8 * 1024)
                    var read: Int
                    var sum = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        sum += read
                        onProgress(sum, total)
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun chat(s: com.bskai.data.AppSettings, messages: List<ChatMsg>, tools: List<Map<String, Any>>? = null): LlmResponse {
        return withContext(Dispatchers.IO) {
            val messagesJson = JSONArray()
            messages.filter { it.toolCalls.isEmpty() && it.toolCallId == null }.forEach { msg ->
                val obj = JSONObject()
                obj.put("role", msg.role)
                obj.put("content", msg.content)
                messagesJson.put(obj)
            }

            val body = JSONObject()
            body.put("model", s.apiModel)
            body.put("messages", messagesJson)
            body.put("stream", false)

            if (tools != null && tools.isNotEmpty()) {
                val toolsArr = JSONArray()
                tools.forEach { t ->
                    val tobj = JSONObject()
                    tobj.put("type", "function")
                    tobj.put("function", JSONObject(t))
                    toolsArr.put(tobj)
                }
                body.put("tools", toolsArr)
            }

            val requestBuilder = Request.Builder()
                .url(s.apiProviderUrl + "/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))

            if (s.apiProviderKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${s.apiProviderKey}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}")
            }
            val responseBody = response.body?.string() ?: throw RuntimeException("Empty response")
            parseResponse(responseBody)
        }
    }

    private fun parseResponse(json: String): LlmResponse {
        val root = JSONObject(json)
        val choices = root.optJSONArray("choices") ?: return LlmResponse("", emptyList())
        if (choices.length() == 0) return LlmResponse("", emptyList())
        val message = choices.getJSONObject(0).optJSONObject("message") ?: return LlmResponse("", emptyList())
        val content = message.optString("content", "")
        val toolCallsArr = message.optJSONArray("tool_calls")
        val toolCalls = mutableListOf<ToolCall>()
        if (toolCallsArr != null) {
            for (i in 0 until toolCallsArr.length()) {
                val tc = toolCallsArr.getJSONObject(i)
                val id = tc.optString("id")
                val function = tc.optJSONObject("function") ?: continue
                val name = function.optString("name")
                val args = function.optString("arguments")
                toolCalls.add(ToolCall(id, name, args))
            }
        }
        return LlmResponse(content, toolCalls)
    }

    fun chatStream(s: com.bskai.data.AppSettings, messages: List<ChatMsg>): Flow<StreamEvent> = flow {
        val messagesJson = JSONArray()
        messages.filter { it.toolCalls.isEmpty() && it.toolCallId == null }.forEach { msg ->
            val obj = JSONObject()
            obj.put("role", msg.role)
            obj.put("content", msg.content)
            messagesJson.put(obj)
        }

        val body = JSONObject()
        body.put("model", s.apiModel)
        body.put("messages", messagesJson)
        body.put("stream", true)

        val requestBuilder = Request.Builder()
            .url(s.apiProviderUrl + "/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))

        if (s.apiProviderKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${s.apiProviderKey}")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            emit(StreamEvent.Error("HTTP ${response.code}"))
            return@flow
        }

        val source = response.body?.source() ?: run {
            emit(StreamEvent.Error("Empty response"))
            return@flow
        }

        val sb = StringBuilder()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            try {
                val json = JSONObject(data)
                val choices = json.optJSONArray("choices") ?: continue
                if (choices.length() == 0) continue
                val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                val content = delta.optString("content", "")
                if (content.isNotEmpty()) {
                    sb.append(content)
                    emit(StreamEvent.Delta(content))
                }
            } catch (_: Exception) {}
        }
        emit(StreamEvent.Done(sb.toString()))
    }.flowOn(Dispatchers.IO)
}
