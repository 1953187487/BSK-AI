package com.floatai.data.remote

import com.floatai.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 流式输出块。
 */
sealed class ChatStreamEvent {
    /** 模型增量内容片段。 */
    data class Delta(val text: String) : ChatStreamEvent()
    /** 模型调用工具请求（OpenAI tool_calls）。 */
    data class ToolCall(val id: String, val name: String, val args: JSONObject) : ChatStreamEvent()
    /** 流式输出结束（成功）。 */
    data class Done(val finishReason: String) : ChatStreamEvent()
    /** 出错。 */
    data class Error(val message: String) : ChatStreamEvent()
}

sealed class ChatResult {
    data class Success(val content: String) : ChatResult()
    data class Error(val message: String) : ChatResult()
}

/**
 * OpenAI 兼容 API 客户端 v1.0.4：
 *  - 流式 SSE 输出（chatCompletionsStream）：增量内容 + 工具调用解析
 *  - 一次性返回（chatCompletions）：保留兼容
 *  - fetchModels：拉取可用模型列表
 */
object OpenAiClient {

    // ---- 流式 chat completions（SSE） ----
    fun chatCompletionsStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.7f,
        tools: List<JSONObject> = emptyList()
    ): Flow<ChatStreamEvent> = flow {
        try {
            val chatUrl = if (baseUrl.endsWith("/")) baseUrl + "chat/completions" else baseUrl + "/chat/completions"
            val conn = URL(chatUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 120_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.doOutput = true

            val body = JSONObject()
            body.put("model", if (model.isBlank() || model == "auto") "gpt-4o-mini" else model)
            body.put("temperature", temperature.toDouble())
            body.put("stream", true)
            body.put("max_tokens", 4096)

            val msgs = JSONArray()
            messages.takeLast(30).forEach { m ->
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            body.put("messages", msgs)

            if (tools.isNotEmpty()) {
                body.put("tools", JSONArray(tools.toString()))
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code >= 400) {
                val errBody = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() } ?: ""
                emit(ChatStreamEvent.Error("HTTP $code: ${errBody.take(200)}"))
                conn.disconnect()
                return@flow
            }

            // 解析 SSE：每行以 "data: " 开头；空行分隔事件；"[DONE]" 表示结束
            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val toolCallsAccumulator = mutableMapOf<Int, MutableMap<String, String>>() // index -> {id, name, args}

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: break
                if (l.isBlank()) continue
                if (!l.startsWith("data:")) continue
                val data = l.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(ChatStreamEvent.Done("stop"))
                    break
                }
                try {
                    val obj = JSONObject(data)
                    val choice = obj.optJSONArray("choices")?.optJSONObject(0) ?: continue
                    val delta = choice.optJSONObject("delta") ?: JSONObject()
                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) emit(ChatStreamEvent.Delta(content))

                    // 工具调用增量解析
                    val tcs = delta.optJSONArray("tool_calls")
                    if (tcs != null) {
                        for (i in 0 until tcs.length()) {
                            val tc = tcs.getJSONObject(i)
                            val idx = tc.optInt("index", i)
                            val acc = toolCallsAccumulator.getOrPut(idx) { mutableMapOf() }
                            if (tc.has("id")) acc["id"] = tc.optString("id")
                            if (tc.has("type")) acc["type"] = tc.optString("type")
                            val fn = tc.optJSONObject("function")
                            if (fn != null) {
                                if (fn.has("name")) acc["name"] = fn.optString("name")
                                val argsDelta = fn.optString("arguments", "")
                                if (argsDelta.isNotEmpty()) {
                                    acc["arguments"] = (acc["arguments"] ?: "") + argsDelta
                                }
                            }
                            // 若这一帧结束（arguments 已经完整 JSON），emit ToolCall
                            val fullArgs = acc["arguments"]
                            if (fullArgs != null && fullArgs.trim().startsWith("{") && fullArgs.trim().endsWith("}")) {
                                val name = acc["name"] ?: ""
                                val id = acc["id"] ?: "call_$idx"
                                val argsObj = runCatching { JSONObject(fullArgs) }.getOrElse { JSONObject() }
                                emit(ChatStreamEvent.ToolCall(id, name, argsObj))
                                acc["__emitted"] = "1"
                            }
                        }
                    }

                    val finishReason = choice.optString("finish_reason", "")
                    if (finishReason == "stop" || finishReason == "tool_calls" || finishReason == "length") {
                        // 仍要等 [DONE] 后再 emit Done
                    }
                } catch (e: Exception) {
                    // 单行解析失败继续
                }
            }
            conn.disconnect()
            emit(ChatStreamEvent.Done("stop"))
        } catch (e: Exception) {
            emit(ChatStreamEvent.Error(e.message ?: e.javaClass.simpleName))
        }
    }.flowOn(Dispatchers.IO)

    // ---- 一次性 chat（兼容保留） ----
    suspend fun chatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): ChatResult = withContext(Dispatchers.IO) {
        try {
            val chatUrl = if (baseUrl.endsWith("/")) baseUrl + "chat/completions" else baseUrl + "/chat/completions"
            val conn = URL(chatUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true

            val body = JSONObject()
            body.put("model", if (model.isBlank() || model == "auto") "gpt-4o-mini" else model)
            body.put("max_tokens", 2048)

            val msgs = JSONArray()
            messages.takeLast(20).forEach { m ->
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            body.put("messages", msgs)

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val sb = StringBuilder()
            stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    var line = br.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = br.readLine()
                    }
                }
            }
            conn.disconnect()

            if (code == 200) {
                val resp = JSONObject(sb.toString())
                val content = resp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                ChatResult.Success(content.trim())
            } else {
                ChatResult.Error("请求失败 (HTTP $code)：${sb.take(200)}")
            }
        } catch (e: Exception) {
            ChatResult.Error("网络错误：${e.message}")
        }
    }

    // ---- 拉取模型列表 ----
    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val modelsUrl = if (baseUrl.endsWith("/")) baseUrl + "models" else baseUrl + "/models"
            val conn = URL(modelsUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn.responseCode != 200) return@withContext emptyList()

            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    sb.append(line)
                    line = br.readLine()
                }
            }
            conn.disconnect()

            val data = JSONObject(sb.toString()).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val id = data.getJSONObject(i).optString("id")
                    if (id.isNotEmpty()) add(id)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
