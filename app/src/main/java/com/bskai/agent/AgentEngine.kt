package com.bskai.agent

import android.content.Context
import com.bskai.agent.tools.Tool
import com.bskai.agent.tools.ToolContext
import com.bskai.agent.tools.ToolRegistry
import com.bskai.models.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * Claude Code 风格 Agent 引擎主循环：
 * 流式推理 -> 解析工具调用 -> 权限确认 -> 执行工具 -> 观察结果 -> 继续推理，直到完成。
 */
class AgentEngine(
    private val appContext: Context,
    private val provider: ProviderConfig,
    private val workspaceRoot: String = "",
    private val autoApprove: Boolean = false,
    private val permissionResolver: suspend (Tool, JSONObject) -> Boolean = { _, _ -> false }
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    suspend fun run(
        messages: MutableList<AgentMessage>,
        systemPrompt: String,
        onEvent: (AgentEvent) -> Unit
    ) {
        cancelled = false
        val ctx = ToolContext(appContext, workspaceRoot)
        var guard = 0

        while (!cancelled && guard < 12) {
            guard++
            val (content, toolCalls) = streamCompletion(messages, systemPrompt, onEvent)
            if (cancelled) break

            if (toolCalls.isEmpty()) {
                if (content.isNotBlank() && messages.lastOrNull()?.role != "assistant") {
                    messages.add(AgentMessage("assistant", content))
                }
                onEvent(AgentEvent.Done("stop"))
                return
            }

            // 组装 assistant 消息（含工具调用）
            messages.add(
                AgentMessage(
                    role = "assistant",
                    content = content,
                    toolName = toolCalls.first().name,
                    toolCallId = toolCalls.first().id,
                    toolArgs = toolCalls.first().args
                )
            )

            // 逐个执行工具
            for (call in toolCalls) {
                if (cancelled) return
                val tool = ToolRegistry.find(call.name)
                if (tool == null) {
                    messages.add(AgentMessage("tool", "未知工具: ${call.name}", toolCallId = call.id))
                    onEvent(AgentEvent.ToolCallFinished(call.id, call.name, "未知工具"))
                    continue
                }
                onEvent(AgentEvent.ToolCallStarted(call.id, call.name, call.args))

                val allowed = autoApprove || !tool.requiresPermission ||
                    permissionResolver(tool, call.args)
                if (!allowed) {
                    val msg = "用户拒绝了工具 ${tool.name} 的执行权限。请停止该操作并询问用户。"
                    messages.add(AgentMessage("tool", msg, toolCallId = call.id))
                    onEvent(AgentEvent.ToolCallFinished(call.id, call.name, "已拒绝"))
                    continue
                }

                val result = ToolRegistry.runTool(tool.name, call.args, ctx)
                messages.add(AgentMessage("tool", result.output, toolCallId = call.id))
                onEvent(AgentEvent.ToolCallFinished(call.id, tool.name, result.output))
            }
        }
        if (!cancelled) onEvent(AgentEvent.Error("达到最大工具调用轮次 (12)"))
    }

    /**
     * 单次无工具的流式补全，返回完整文本。供编排器/角色智能体使用。
     */
    suspend fun complete(
        messages: List<AgentMessage>,
        systemPrompt: String,
        onEvent: (AgentEvent) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        cancelled = false
        val (content, _) = streamCompletion(messages, systemPrompt, onEvent)
        content
    }

    /**
     * 流式请求 LLM，返回 (增量文本内容, 完整工具调用列表)。
     */
    private suspend fun streamCompletion(
        messages: List<AgentMessage>,
        systemPrompt: String,
        onEvent: (AgentEvent) -> Unit
    ): Pair<String, List<ToolCallData>> = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trim().trimEnd('/')
        val chatUrl = "$base/chat/completions"
        val body = JSONObject()
            .put("model", provider.model.ifBlank { "auto" })
            .put("temperature", 0.7)
            .put("max_tokens", 8192)
            .put("stream", true)
            .put("messages", buildMessages(messages, systemPrompt))
            .put("tools", ToolRegistry.definitions())
            .put("tool_choice", "auto")

        val request = Request.Builder()
            .url(chatUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${provider.apiKey}")
            .header("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val contentSb = StringBuilder()
        val calls = mutableMapOf<Int, ToolCallAcc>()
        var finishedReason = ""

        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()?.take(300) ?: ""
                    onEvent(AgentEvent.Error("HTTP ${resp.code}: $err"))
                    return@withContext Pair(contentSb.toString(), emptyList<ToolCallData>())
                }
                val reader = BufferedReader(resp.body!!.charStream())
                var line: String?
                while (reader.readLine().also { line = it } != null && !cancelled) {
                    val l = line ?: break
                    if (l.isBlank() || !l.startsWith("data:")) continue
                    val data = l.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val obj = runCatching { JSONObject(data) }.getOrNull() ?: continue
                    val choice = obj.optJSONArray("choices")?.optJSONObject(0) ?: continue
                    val delta = choice.optJSONObject("delta") ?: JSONObject()

                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) {
                        contentSb.append(content)
                        onEvent(AgentEvent.Delta(content))
                    }

                    val tcs = delta.optJSONArray("tool_calls")
                    if (tcs != null) {
                        for (i in 0 until tcs.length()) {
                            val tc = tcs.getJSONObject(i)
                            val idx = tc.optInt("index", i)
                            val acc = calls.getOrPut(idx) { ToolCallAcc() }
                            if (tc.has("id")) acc.id = tc.optString("id")
                            val fn = tc.optJSONObject("function")
                            if (fn != null) {
                                if (fn.has("name")) acc.name = fn.optString("name")
                                acc.args.append(fn.optString("arguments", ""))
                            }
                        }
                    }
                    val fr = choice.optString("finish_reason", "")
                    if (fr.isNotEmpty()) finishedReason = fr
                }
            }
        } catch (e: Exception) {
            if (!cancelled) onEvent(AgentEvent.Error(e.message ?: "网络错误"))
        }

        val result = calls.values
            .filter { it.name.isNotEmpty() }
            .mapNotNull { acc ->
                val args = runCatching { JSONObject(acc.args.toString()) }
                    .getOrElse { JSONObject().put("raw", acc.args.toString()) }
                ToolCallData(
                    id = acc.id.ifEmpty { "call_${System.currentTimeMillis()}" },
                    name = acc.name,
                    args = args
                )
            }
        Pair(contentSb.toString(), result)
    }

    private fun buildMessages(messages: List<AgentMessage>, systemPrompt: String): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", systemPrompt))
        messages.takeLast(40).forEach { arr.put(it.toWire()) }
        return arr
    }

    private class ToolCallAcc {
        var id: String = ""
        var name: String = ""
        val args = StringBuilder()
    }

    data class ToolCallData(
        val id: String,
        val name: String,
        val args: JSONObject
    )
}
