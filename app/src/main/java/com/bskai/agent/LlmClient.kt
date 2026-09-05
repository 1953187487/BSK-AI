package com.bskai.agent

import android.content.Context
import com.bskai.data.AppSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatMsg(
    val role: String,
    val content: String,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolCalls: List<ToolCall> = emptyList()
)

data class LlmResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

class LlmClient(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 把消息列表序列化为 OpenAI 协议的 messages 数组。
     * - role=tool：输出 content + tool_call_id
     * - role=assistant 且带 toolCalls：输出 content + tool_calls 数组（完整 id/function）
     * - 其余：role + content
     */
    private fun wireMessages(messages: List<ChatMsg>): JSONArray {
        val arr = JSONArray()
        messages.forEach { m ->
            val o = JSONObject().put("role", m.role)
            when {
                m.role == "tool" -> {
                    o.put("content", m.content)
                    o.put("tool_call_id", m.toolCallId ?: "")
                }
                m.role == "assistant" && m.toolCalls.isNotEmpty() -> {
                    o.put("content", m.content)
                    val calls = JSONArray()
                    m.toolCalls.forEach { c ->
                        calls.put(
                            JSONObject().apply {
                                put("id", c.id)
                                put("type", "function")
                                put(
                                    "function", JSONObject().apply {
                                        put("name", c.name)
                                        put("arguments", c.argumentsJson.ifBlank { "{}" })
                                    }
                                )
                            }
                        )
                    }
                    o.put("tool_calls", calls)
                }
                else -> o.put("content", m.content)
            }
            arr.put(o)
        }
        return arr
    }

    /**
     * 调用 OpenAI 兼容 chat/completions。若 messages 中包含 tool 角色则会被一并序列化。
     * @param toolsJson LLM 工具描述 JSON 数组字符串，传 null/空则不带 tools
     */
    suspend fun chat(
        s: AppSettings,
        messages: List<ChatMsg>,
        toolsJson: String? = null
    ): LlmResponse = suspendCancellableCoroutine { cont ->
        val base = s.apiProviderUrl.trim().trimEnd('/')
        if (base.isEmpty()) {
            cont.resumeWithException(IllegalStateException("未配置 API 地址"))
            return@suspendCancellableCoroutine
        }
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val payload = JSONObject().apply {
            put("model", s.apiModel)
            put("messages", wireMessages(messages))
            if (!toolsJson.isNullOrBlank()) put("tools", JSONArray(toolsJson))
            put("temperature", 0.6)
            put("stream", false)
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${s.apiProviderKey}")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        if (cont.isActive) cont.resumeWithException(
                            RuntimeException("服务返回异常 HTTP ${resp.code}: ${body.take(200)}")
                        )
                        return
                    }
                    try {
                        val parsed = parseResponse(body)
                        if (cont.isActive) cont.resume(parsed)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(RuntimeException("解析服务响应失败: ${e.message}"))
                    }
                }
             }
        })
        cont.invokeOnCancellation { call.cancel() }
    }

    /**
     * 流式 chat：返回 Flow<StreamEvent>，每个 delta 触发一次 collect。
     * SSE 格式：data: {json}\n\n
     * 结束标记：data: [DONE]
     * @param toolsJson LLM 工具描述 JSON 数组字符串，传 null/空则不带 tools
     */
    fun chatStream(s: AppSettings, messages: List<ChatMsg>, toolsJson: String? = null): Flow<StreamEvent> = callbackFlow {
        val base = s.apiProviderUrl.trim().trimEnd('/')
        if (base.isEmpty()) {
            trySend(StreamEvent.Error("未配置 API 地址"))
            close()
            return@callbackFlow
        }
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val payload = JSONObject().apply {
            put("model", s.apiModel)
            put("messages", wireMessages(messages))
            if (!toolsJson.isNullOrBlank()) put("tools", JSONArray(toolsJson))
            put("temperature", 0.6)
            put("stream", true)
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${s.apiProviderKey}")
            .addHeader("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val call = client.newCall(request)
        trySend(StreamEvent.Start)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                trySend(StreamEvent.Error(e.message ?: "网络请求失败"))
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        trySend(StreamEvent.Error("HTTP ${resp.code}: ${body.take(200)}"))
                        close()
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        trySend(StreamEvent.Error("响应体为空"))
                        close()
                        return
                    }
                    try {
                        val reader = BufferedReader(source.inputStream().reader(Charsets.UTF_8))
                        val sb = StringBuilder()
                        val toolCalls = LinkedHashMap<Int, MutableList<String>>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) continue
                            if (line.startsWith(":")) continue
                            if (!line.startsWith("data:")) continue
                            val payload = line.substring(5).trim()
                            if (payload == "[DONE]") {
                                val calls = JSONArray()
                                toolCalls.forEach { (_, parts) ->
                                    calls.put(
                                        JSONObject().apply {
                                            put("id", parts[0])
                                            put("name", parts[1])
                                            put("arguments", parts[2])
                                        }
                                    )
                                }
                                trySend(StreamEvent.Done(fullContent = sb.toString(), toolCallsJson = calls.toString()))
                                break
                            }
                            try {
                                val obj = JSONObject(payload)
                                val choices = obj.optJSONArray("choices")
                                if (choices == null || choices.length() == 0) continue
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta") ?: continue
                                val piece = delta.optString("content", "")
                                if (piece.isNotEmpty()) {
                                    sb.append(piece)
                                    trySend(StreamEvent.Delta(piece))
                                }
                                val tc = delta.optJSONArray("tool_calls")
                                if (tc != null && tc.length() > 0) {
                                    for (i in 0 until tc.length()) {
                                        val objCall = tc.getJSONObject(i)
                                        val idx = objCall.optInt("index", 0)
                                        val entry = toolCalls.getOrPut(idx) { mutableListOf("", "", StringBuilder().toString()) }
                                        val id = objCall.optString("id", "")
                                        if (id.isNotEmpty() && entry[0].isEmpty()) entry[0] = id
                                        val fn = objCall.optJSONObject("function")
                                        if (fn != null) {
                                            val nm = fn.optString("name", "")
                                            if (nm.isNotEmpty() && entry[1].isEmpty()) entry[1] = nm
                                            val args = fn.optString("arguments", "")
                                            if (args.isNotEmpty()) entry[2] = entry[2] + args
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // 忽略单行解析失败，继续下一行
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        trySend(StreamEvent.Error(e.message ?: "流解析失败"))
                        close()
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }

    sealed class StreamEvent {
        object Start : StreamEvent()
        data class Delta(val text: String) : StreamEvent()
        data class Done(val fullContent: String, val toolCallsJson: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    private fun parseResponse(body: String): LlmResponse {
        val json = JSONObject(body)
        val choice = json.getJSONArray("choices").getJSONObject(0)
        val msg = choice.getJSONObject("message")
        val content = msg.optString("content", "").trim()
        val calls = mutableListOf<ToolCall>()
        val callsArr = msg.optJSONArray("tool_calls")
        if (callsArr != null) {
            for (i in 0 until callsArr.length()) {
                val c = callsArr.getJSONObject(i)
                val id = c.optString("id", "")
                val fn = c.optJSONObject("function") ?: JSONObject()
                val name = fn.optString("name", "")
                val args = fn.optString("arguments", "{}")
                if (name.isNotBlank()) calls.add(ToolCall(id, name, args))
            }
        }
        return LlmResponse(content = content, toolCalls = calls)
    }

    /** 拉取模型列表（OpenAI /v1/models 兼容 + DashScope 兼容） */
    suspend fun listModels(apiUrl: String, apiKey: String): List<String> =
        suspendCancellableCoroutine { cont ->
            val base = apiUrl.trim().trimEnd('/')
            val url = when {
                base.endsWith("/v1/models") -> base
                base.contains("/compatible-mode") -> "$base/models"
                base.endsWith("/v1") -> "$base/models"
                else -> "$base/models"
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val call = client.newCall(req)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    if (cont.isActive) cont.resume(emptyList())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            if (cont.isActive) cont.resume(emptyList())
                            return
                        }
                        val list = try {
                            parseModels(body)
                        } catch (_: Exception) { emptyList() }
                        if (cont.isActive) cont.resume(list)
                    }
                }
            })
            cont.invokeOnCancellation { call.cancel() }
        }

    private fun parseModels(body: String): List<String> {
        val arr = JSONObject(body).optJSONArray("data") ?: return emptyList()
        val names = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id", "")
            if (id.isNotBlank()) names.add(id)
        }
        return names.distinct().sorted()
    }
}
