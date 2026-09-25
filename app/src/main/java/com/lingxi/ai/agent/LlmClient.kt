package com.lingxi.ai.agent

import android.content.Context
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

/** 对话消息。role: system / user / assistant / tool。 */
data class ChatMsg(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null
)

/** 一次工具调用请求（模型发起）。 */
data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

/** 非流式补全响应。 */
data class LlmResponse(
    val content: String,
    val toolCalls: List<ToolCall>
)

/** 流式事件。 */
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

/**
 * OpenAI 兼容协议客户端（内核层）。
 *
 * 2.1.1 重写要点：
 * - 完整实现 tool 协议：assistant 消息携带 tool_calls、tool 消息携带 tool_call_id，
 *   工具结果可以正确回传给模型（旧版会把这两类消息整段丢弃，导致多轮工具链断裂）。
 * - 流式接口 [chatStream] 同样支持 tools 声明与 tool_calls 增量解析。
 * - 错误信息携带 HTTP 状态码与响应体摘要，便于排查服务商侧问题。
 */
class LlmClient(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    // ---------- 协议序列化 ----------

    /**
     * 将消息历史序列化为 OpenAI /chat/completions 的 messages 数组。
     * 与旧版不同，assistant 的 tool_calls 与 tool 结果消息都会被保留。
     */
    internal fun serializeMessages(messages: List<ChatMsg>): JSONArray {
        val arr = JSONArray()
        messages.forEach { msg ->
            val obj = JSONObject()
            when {
                msg.role == "assistant" && msg.toolCalls.isNotEmpty() -> {
                    obj.put("role", "assistant")
                    obj.put("content", if (msg.content.isBlank()) JSONObject.NULL else msg.content)
                    val calls = JSONArray()
                    msg.toolCalls.forEach { tc ->
                        calls.put(
                            JSONObject().apply {
                                put("id", tc.id)
                                put("type", "function")
                                put(
                                    "function",
                                    JSONObject().apply {
                                        put("name", tc.name)
                                        put("arguments", tc.argumentsJson.ifBlank { "{}" })
                                    }
                                )
                            }
                        )
                    }
                    obj.put("tool_calls", calls)
                }
                msg.role == "tool" -> {
                    obj.put("role", "tool")
                    obj.put("content", msg.content)
                    obj.put("tool_call_id", msg.toolCallId ?: "")
                }
                else -> {
                    obj.put("role", msg.role)
                    obj.put("content", msg.content)
                }
            }
            arr.put(obj)
        }
        return arr
    }

    /** 将工具定义列表（name/description/parameters JSON 字符串）序列化为 tools 数组。 */
    internal fun serializeTools(tools: List<Map<String, Any>>): JSONArray {
        val arr = JSONArray()
        tools.forEach { t ->
            val tobj = JSONObject()
            tobj.put("type", "function")
            tobj.put("function", JSONObject(t))
            arr.put(tobj)
        }
        return arr
    }

    private fun buildRequestBody(
        model: String,
        messages: List<ChatMsg>,
        stream: Boolean,
        tools: List<Map<String, Any>>?
    ): JSONObject {
        val body = JSONObject()
        body.put("model", model)
        body.put("messages", serializeMessages(messages))
        body.put("stream", stream)
        if (!tools.isNullOrEmpty()) {
            body.put("tools", serializeTools(tools))
        }
        return body
    }

    private fun buildRequest(
        providerUrl: String,
        apiKey: String?,
        body: JSONObject,
        accept: String
    ): Request {
        val builder = Request.Builder()
            .url(providerUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", accept)
            .post(body.toString().toRequestBody(jsonMedia))
        if (!apiKey.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }
        return builder.build()
    }

    private fun httpError(code: Int, body: String?): RuntimeException {
        val detail = body?.take(300)?.replace('\n', ' ').orEmpty()
        return RuntimeException("HTTP $code${if (detail.isNotBlank()) " $detail" else ""}")
    }

    // ---------- 模型列表 ----------

    /** 拉取服务商模型列表（GET /models）。失败返回空列表。 */
    suspend fun listModels(url: String, apiKey: String? = null): List<ModelInfo> =
        withContext(Dispatchers.IO) {
            try {
                val builder = Request.Builder()
                    .url(url.trimEnd('/') + "/models")
                    .addHeader("Accept", "application/json")
                if (!apiKey.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $apiKey")
                }
                client.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    parseModels(body)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun parseModels(json: String): List<ModelInfo> {
        val list = mutableListOf<ModelInfo>()
        try {
            val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
            for (i in 0 until data.length()) {
                val o = data.getJSONObject(i)
                val id = o.optString("id")
                if (id.isNotEmpty()) {
                    list.add(
                        ModelInfo(
                            id = id,
                            name = o.optString("name", id),
                            description = o.optString("description", ""),
                            contextLength = o.optInt("context_length", 4096)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    // ---------- 模型下载 ----------

    /** 下载模型文件到目标路径，[onProgress] 回调 (已下载字节, 总字节)。 */
    suspend fun downloadModel(
        url: String,
        targetFile: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
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
            }
        } catch (_: Exception) {
            false
        }
    }

    // ---------- 非流式对话 ----------

    /** 非流式补全。tools 传当前可用工具定义；返回 content 与模型请求的 tool_calls。 */
    suspend fun chat(
        s: com.lingxi.ai.data.AppSettings,
        messages: List<ChatMsg>,
        tools: List<Map<String, Any>>? = null
    ): LlmResponse = chatWithModel(s.apiProviderUrl, s.apiProviderKey.ifBlank { null }, s.apiModel, messages, tools)

    /**
     * 用指定模型与端点发起一次对话（用于双模型视频生成的"脚本模型"环节，
     * 允许脚本模型与主对话模型不同）。
     */
    suspend fun chatWithModel(
        providerUrl: String,
        apiKey: String?,
        model: String,
        messages: List<ChatMsg>,
        tools: List<Map<String, Any>>? = null
    ): LlmResponse = withContext(Dispatchers.IO) {
        val body = buildRequestBody(model, messages, stream = false, tools = tools)
        val request = buildRequest(providerUrl, apiKey, body, accept = "application/json")
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (!response.isSuccessful) throw httpError(response.code, responseBody)
            if (responseBody.isNullOrBlank()) throw RuntimeException("Empty response")
            parseResponse(responseBody)
        }
    }

    private fun parseResponse(json: String): LlmResponse {
        val root = JSONObject(json)
        val choices = root.optJSONArray("choices") ?: return LlmResponse("", emptyList())
        if (choices.length() == 0) return LlmResponse("", emptyList())
        val message = choices.getJSONObject(0).optJSONObject("message")
            ?: return LlmResponse("", emptyList())
        val content = message.optString("content", "")
        val toolCallsArr = message.optJSONArray("tool_calls")
        val toolCalls = mutableListOf<ToolCall>()
        if (toolCallsArr != null) {
            for (i in 0 until toolCallsArr.length()) {
                val tc = toolCallsArr.getJSONObject(i)
                val function = tc.optJSONObject("function") ?: continue
                toolCalls.add(
                    ToolCall(
                        id = tc.optString("id"),
                        name = function.optString("name"),
                        argumentsJson = function.optString("arguments")
                    )
                )
            }
        }
        return LlmResponse(content, toolCalls)
    }

    // ---------- 流式对话 ----------

    /**
     * 流式补全。2.1.1 起支持 [tools] 声明：当模型在流中返回 tool_calls 时，
     * 以 [StreamEvent.Done] 结束并在 [LlmResponse] 语义下由调用方读取
     * 累积的 tool_calls（见 [StreamingResult]）。
     */
    fun chatStream(
        s: com.lingxi.ai.data.AppSettings,
        messages: List<ChatMsg>,
        tools: List<Map<String, Any>>? = null
    ): Flow<StreamEvent> = chatStreamWithModel(
        providerUrl = s.apiProviderUrl,
        apiKey = s.apiProviderKey.ifBlank { null },
        model = s.apiModel,
        messages = messages,
        tools = tools
    )

    /**
     * 指定模型与端点的流式补全（供视频脚本等旁路调用）。
     * 事件顺序：0..N 个 [StreamEvent.Delta]，最后恰好一个 [StreamEvent.Done] 或 [StreamEvent.Error]。
     */
    fun chatStreamWithModel(
        providerUrl: String,
        apiKey: String?,
        model: String,
        messages: List<ChatMsg>,
        tools: List<Map<String, Any>>? = null
    ): Flow<StreamEvent> = flow {
        val body = buildRequestBody(model, messages, stream = true, tools = tools)
        val request = buildRequest(providerUrl, apiKey, body, accept = "text/event-stream")

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            emit(StreamEvent.Error("连接失败：${e.message ?: "网络错误"}"))
            return@flow
        }

        if (!response.isSuccessful) {
            val errBody = try { response.body?.string() } catch (_: Exception) { null }
            response.close()
            emit(StreamEvent.Error(httpError(response.code, errBody).message ?: "HTTP ${response.code}"))
            return@flow
        }

        val source = response.body?.source()
        if (source == null) {
            response.close()
            emit(StreamEvent.Error("空响应"))
            return@flow
        }

        val sb = StringBuilder()
        try {
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
                } catch (_: Exception) {
                    // 单个分片解析失败不中断流
                }
            }
            emit(StreamEvent.Done(sb.toString()))
        } catch (e: Exception) {
            if (sb.isEmpty()) {
                emit(StreamEvent.Error("流中断：${e.message ?: "未知错误"}"))
            } else {
                // 已有部分内容，视为完成，避免丢失已渲染文本
                emit(StreamEvent.Done(sb.toString()))
            }
        } finally {
            runCatching { response.close() }
        }
    }.flowOn(Dispatchers.IO)
}
