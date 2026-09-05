package com.bskai.agent

import android.content.Context
import com.bskai.agent.slash.SlashRegistry
import com.bskai.agent.tools.ToolRegistry
import com.bskai.data.SettingsRepository
import com.bskai.workspace.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentEngine(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val settings: SettingsRepository
) {

    private val llm = LlmClient(context)

    private val _conversation = MutableStateFlow<List<ChatMsg>>(emptyList())
    val conversation: StateFlow<List<ChatMsg>> = _conversation.asStateFlow()

    var toolRegistry: ToolRegistry? = null
    var workspace: WorkspaceManager? = null
    var slashRegistry: SlashRegistry? = null

    val workspaceEnabled: Boolean
        get() = settings.settings.value.workspaceEnabled

    fun setWorkspaceEnabled(enabled: Boolean) {
        settings.update { it.copy(workspaceEnabled = enabled) }
    }

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    /**
     * 用户发言入口。先入栈 user 消息，再启动一次 assistant 回复。
     * 若已启用工具注册且配置了工具，走工具循环；否则走流式文本回复。
     */
    suspend fun answer(userText: String): String {
        val text = userText.trim()
        if (text.isEmpty()) return "请再说一遍"

        _processing.value = true
        try {
            val s = settings.settings.value
            append(ChatMsg("user", text))

            if (!s.apiConfigured) {
                val fallback = localReply(text)
                append(ChatMsg("assistant", fallback))
                return fallback
            }

            val registry = toolRegistry
            if (registry != null && registry.all().isNotEmpty()) {
                return runToolLoop(registry)
            }

            return runStreamingReply()
        } finally {
            _processing.value = false
        }
    }

    /**
     * 工具循环：发送 chat 请求，若有 tool_calls 则逐个执行并回填结果，
     * 最多循环 maxToolRounds 轮，直到模型不再发起工具调用。
     */
    private suspend fun runToolLoop(registry: ToolRegistry): String {
        val s = settings.settings.value
        val toolsJson = registry.toolsJsonForLlm()
        val maxRounds = 4
        var finalContent = ""

        for (round in 1..maxRounds) {
            val messages = buildRequestMessages()
            val resp = try {
                llm.chat(s, messages, toolsJson)
            } catch (e: Exception) {
                val msg = "工具调用失败：${e.message ?: "未知错误"}"
                append(ChatMsg("assistant", msg))
                return msg
            }

            if (resp.toolCalls.isEmpty()) {
                finalContent = resp.content.ifEmpty { "抱歉，我没有想好怎么回答。" }
                append(ChatMsg("assistant", finalContent))
                return finalContent
            }

            append(ChatMsg("assistant", resp.content, toolCalls = resp.toolCalls))

            for (call in resp.toolCalls) {
                val tool = registry.get(call.name)
                val result = if (tool != null) {
                    try {
                        tool.execute(call.argumentsJson)
                    } catch (e: Exception) {
                        com.bskai.agent.tools.ToolResult(call.name, "执行异常：${e.message}", true)
                    }
                } else {
                    com.bskai.agent.tools.ToolResult(call.name, "未知工具：${call.name}", true)
                }
                append(ChatMsg("tool", result.content, toolCallId = call.id, toolName = call.name))
            }
        }

        finalContent = "已达到工具调用轮次上限，请尝试更具体的指令。"
        append(ChatMsg("assistant", finalContent))
        return finalContent
    }

    private suspend fun runStreamingReply(): String {
        val s = settings.settings.value
        val placeholderIndex = appendAndReturnIndex(ChatMsg("assistant", ""))
        val sb = StringBuilder()

        return try {
            val history = recentMessages(s.maxHistoryLength)
            llm.chatStream(s, history).collect { ev ->
                when (ev) {
                    is LlmClient.StreamEvent.Delta -> {
                        sb.append(ev.text)
                        updateLastAssistant(sb.toString())
                    }
                    is LlmClient.StreamEvent.Done -> {
                        val final = ev.fullContent.ifEmpty { sb.toString() }
                        val clean = final.ifEmpty { "抱歉，我没有想好怎么回答。" }
                        updateLastAssistant(clean)
                    }
                    is LlmClient.StreamEvent.Error -> {
                        if (sb.isEmpty()) {
                            val fallback = localReply("")
                            updateLastAssistant(fallback)
                        }
                    }
                    else -> Unit
                }
            }
            sb.toString().ifEmpty { "抱歉，我没有想好怎么回答。" }
        } catch (_: Exception) {
            if (sb.isEmpty()) {
                val fallback = localReply("")
                updateLastAssistant(fallback)
                fallback
            } else sb.toString()
        }
    }

    private fun buildRequestMessages(): List<ChatMsg> {
        val s = settings.settings.value
        val systemPrompt = "你是一个智能AI助手AURA，运行在Android设备上。你友好、专业、乐于助人。" +
                when {
                    s.chatMode == com.bskai.data.ChatMode.DEV ->
                        "\n当前处于应用开发模式。你可以使用 run_shell、read_file、write_file、list_files 等工具来帮助用户开发 Android 应用。"
                    s.thinkingLevel == 1 -> "\n请简要分析问题并给出答案。"
                    s.thinkingLevel == 2 -> "\n请逐步推理，考虑多种可能性。"
                    s.thinkingLevel >= 3 -> "\n请深入分析，考虑所有角度，给出详细推理过程。"
                    else -> ""
                }
        val system = ChatMsg("system", systemPrompt)
        val history = _conversation.value.takeLast(s.maxHistoryLength.coerceAtLeast(10))
        return listOf(system) + history
    }

    fun notifyAssistant(text: String) {
        append(ChatMsg("assistant", text))
    }

    fun clearConversation() {
        _conversation.value = emptyList()
    }

    /**
     * 单次文本生成（非流式），用于 AI 生成等场景。
     */
    suspend fun generateText(prompt: String): String {
        val s = settings.settings.value
        if (!s.apiConfigured) throw IllegalStateException("未配置 AI 服务")
        val messages = listOf(ChatMsg("user", prompt))
        return try {
            val resp = llm.chat(s, messages)
            resp.content.ifEmpty { throw IllegalStateException("返回内容为空") }
        } catch (e: Exception) {
            throw e
        }
    }

    fun recentMessages(max: Int): List<ChatMsg> = buildRequestMessages()

    private fun append(msg: ChatMsg) {
        val max = settings.settings.value.maxHistoryLength
        _conversation.value = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
    }

    private fun appendAndReturnIndex(msg: ChatMsg): Int {
        val max = settings.settings.value.maxHistoryLength
        val list = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
        _conversation.value = list
        return list.size - 1
    }

    private fun updateLastAssistant(content: String) {
        val list = _conversation.value.toMutableList()
        for (i in list.indices.reversed()) {
            if (list[i].role == "assistant") {
                list[i] = list[i].copy(content = content)
                _conversation.value = list
                return
            }
        }
    }

    private fun localReply(text: String): String {
        val s = settings.settings.value
        val tail = if (s.apiConfigured) "" else "\n\n提示：当前未配置 AI 服务，请到设置中填入 API。"

        val base = when {
            text.contains("你好") || text.contains("您好") || text.contains("hi") ||
                text.contains("hello") || text.contains("嗨") ->
                "你好呀，我是 AURA。"
            text.contains("你是谁") || text.contains("介绍你自己") ->
                "我是 AURA，运行在 Android 设备上的 AI 助手。"
            text.contains("你能做什么") || text.contains("你会什么") || text.contains("有什么功能") ->
                "在设置中配置 AI 服务后，我可以陪你自由对话，回答各种问题。"
            text.contains("谢谢") -> "不客气，随时找我。"
            else -> "抱歉，我还没有完全听明白。在设置里配置 AI 服务后我能更好地回答您。"
        }
        return base + tail
    }
}
