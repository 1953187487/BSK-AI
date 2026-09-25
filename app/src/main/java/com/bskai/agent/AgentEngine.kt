package com.bskai.agent

import android.content.Context
import com.bskai.agent.slash.SlashOutcome
import com.bskai.agent.slash.SlashRegistry
import com.bskai.agent.tools.ToolRegistry
import com.bskai.data.ChatMode
import com.bskai.data.SettingsRepository
import com.bskai.workspace.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AURA 对话内核（2.1.1 重写）。
 *
 * 职责：
 * - 维护对话历史与处理状态；
 * - 斜杠命令（/ws /model /clear /help /video）本地解析，命中后不进入 LLM；
 * - 工具调用循环：把工具定义随请求发给模型，执行模型请求的工具并把结果回填，
 *   循环直至模型给出最终答复（旧版两处断裂导致工具链完全不可用，本版已修复）；
 * - 无工具可用时走流式输出，UI 可观察增量渲染。
 *
 * @param context 应用上下文
 * @param settings 全局设置仓库
 */
class AgentEngine(
    context: Context,
    private val settings: SettingsRepository
) {

    private val llm = LlmClient(context)

    private val _conversation = MutableStateFlow<List<ChatMsg>>(emptyList())
    /** 对话历史，UI 观察。 */
    val conversation: StateFlow<List<ChatMsg>> = _conversation.asStateFlow()

    /** 工具注册表；由 Application 装配。 */
    var toolRegistry: ToolRegistry? = null
    var workspace: WorkspaceManager? = null
    var slashRegistry: SlashRegistry? = null

    val workspaceEnabled: Boolean
        get() = settings.settings.value.workspaceEnabled

    fun setWorkspaceEnabled(enabled: Boolean) {
        settings.update { it.copy(workspaceEnabled = enabled) }
    }

    private val _processing = MutableStateFlow(false)
    /** 是否正在处理一轮对话（工具循环或流式输出），UI 观察以禁用输入。 */
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    /**
     * 处理一条用户输入。
     * 命中斜杠命令时本地处理并返回结果文本；否则进入工具循环或流式回答。
     *
     * @return 本轮最终的助手答复文本（UI 可用于复制/日志）
     */
    suspend fun answer(userText: String): String {
        val text = userText.trim()
        if (text.isEmpty()) return "请再说一遍"

        _processing.value = true
        try {
            // 斜杠命令：本地解析，不消耗 LLM 轮次
            if (text.startsWith("/")) {
                val resolved = resolveSlash(text)
                if (resolved != null) {
                    append(ChatMsg("assistant", resolved))
                    return resolved
                }
            }

            val s = settings.settings.value
            append(ChatMsg("user", text))

            if (!s.apiConfigured) {
                val fallback = localReply(text)
                append(ChatMsg("assistant", fallback))
                return fallback
            }

            val tools = availableTools()
            if (tools.isNotEmpty()) {
                return runToolLoop(tools)
            }

            return runStreamingReply()
        } finally {
            _processing.value = false
        }
    }

    // ---------- 斜杠命令 ----------

    /**
     * 解析斜杠命令。
     *
     * @param raw 以 "/" 开头的原始输入
     * @return 命令处理结果文本；未命中任何命令时返回 null（继续走 LLM）
     */
    private fun resolveSlash(raw: String): String? {
        val registry = slashRegistry ?: return null
        val parts = raw.trim().split(' ', limit = 2)
        val cmd = registry.get(parts[0]) ?: return null
        return when (val outcome = cmd.resolve(parts.getOrElse(1) { "" })) {
            is SlashOutcome.LocalMessage -> outcome.message
            is SlashOutcome.SendToAi -> {
                if (outcome.note != null) append(ChatMsg("assistant", outcome.note))
                null
            }
            SlashOutcome.Cancel -> ""
        }
    }

    /** 当前输入可用的斜杠命令建议（UI 唤起菜单用）。 */
    fun slashSuggestions(prefix: String): List<com.bskai.agent.slash.SlashCommand> =
        slashRegistry?.suggestions(prefix) ?: emptyList()

    // ---------- 工具循环 ----------

    /**
     * 当前应向模型暴露的工具定义。
     * 规则：agentToolsEnabled 开启时暴露终端工具；工作区文件工具额外要求 workspaceEnabled。
     */
    private fun availableTools(): List<Map<String, Any>> {
        val s = settings.settings.value
        val registry = toolRegistry ?: return emptyList()
        if (!s.agentToolsEnabled) return emptyList()
        return registry.all()
            .filter { !it.requiresWorkspace || workspaceEnabled }
            .map { tool ->
                mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to org.json.JSONObject(tool.parametersSchema)
                )
            }
    }

    /**
     * 工具调用主循环。
     * 每轮把完整历史（含工具结果）发给模型；模型返回 tool_calls 时执行并回填，
     * 直至模型给出纯文本答复或达到轮次上限。
     */
    private suspend fun runToolLoop(tools: List<Map<String, Any>>): String {
        val s = settings.settings.value
        val registry = toolRegistry
        val maxRounds = 8

        for (round in 1..maxRounds) {
            val messages = buildRequestMessages()
            val resp = try {
                llm.chat(s, messages, tools)
            } catch (e: Exception) {
                val msg = "对话请求失败：" + (e.message ?: "未知错误")
                append(ChatMsg("assistant", msg))
                return msg
            }

            if (resp.toolCalls.isEmpty()) {
                val final = resp.content.ifBlank { "抱歉，我没有想好怎么回答。" }
                append(ChatMsg("assistant", final))
                return final
            }

            // 记录 assistant 的工具调用请求（带 tool_calls，供下一轮协议回传）
            append(ChatMsg("assistant", resp.content, toolCalls = resp.toolCalls))

            if (registry == null) break

            for (call in resp.toolCalls) {
                val tool = registry.get(call.name)
                val result = when {
                    tool == null -> com.bskai.agent.tools.ToolResult(
                        call.name, "未知工具：${call.name}", isError = true
                    )
                    tool.requiresWorkspace && !workspaceEnabled -> com.bskai.agent.tools.ToolResult(
                        call.name, "工作区权限未开启：输入 /ws on 或到设置中开启后再试", isError = true
                    )
                    else -> try {
                        tool.execute(call.argumentsJson)
                    } catch (e: Exception) {
                        com.bskai.agent.tools.ToolResult(call.name, "执行异常：${e.message}", true)
                    }
                }
                append(ChatMsg("tool", result.content, toolCallId = call.id, toolName = call.name))
            }
        }

        val final = "工具调用已达到轮次上限（$maxRounds 轮）。请把任务拆成更小的指令分步执行。"
        append(ChatMsg("assistant", final))
        return final
    }

    // ---------- 流式回答 ----------

    /** 无工具模式：流式输出，UI 通过 conversation 观察增量。 */
    private suspend fun runStreamingReply(): String {
        val s = settings.settings.value
        append(ChatMsg("assistant", ""))
        val sb = StringBuilder()

        return try {
            llm.chatStream(s, buildRequestMessages()).collect { ev ->
                when (ev) {
                    is StreamEvent.Delta -> {
                        sb.append(ev.text)
                        updateLastAssistant(sb.toString())
                    }
                    is StreamEvent.Done -> {
                        val final = ev.fullContent.ifEmpty { sb.toString() }
                        updateLastAssistant(final.ifEmpty { "抱歉，我没有想好怎么回答。" })
                    }
                    is StreamEvent.Error -> {
                        if (sb.isEmpty()) {
                            val fallback = "请求失败：${ev.message}\n请检查网络与 API 配置。"
                            updateLastAssistant(fallback)
                        }
                    }
                }
            }
            sb.toString().ifEmpty { "抱歉，我没有想好怎么回答。" }
        } catch (e: Exception) {
            if (sb.isEmpty()) {
                val fallback = "请求失败：${e.message ?: "未知错误"}"
                updateLastAssistant(fallback)
                fallback
            } else sb.toString()
        }
    }

    // ---------- 消息构造 ----------

    /** 系统提示词 + 近期历史。 */
    private fun buildRequestMessages(): List<ChatMsg> {
        val s = settings.settings.value
        val systemPrompt = buildString {
            append("你是一个智能AI助手AURA，运行在Android设备上。你友好、专业、乐于助人。")
            when {
                s.chatMode == ChatMode.DEV ->
                    append(" 当前处于应用开发模式。你可以使用 run_shell、read_file、write_file、list_files 等工具来帮助用户开发 Android 应用。")
                s.thinkingLevel == 1 -> append(" 请简要分析问题并给出答案。")
                s.thinkingLevel == 2 -> append(" 请逐步推理，考虑多种可能性。")
                s.thinkingLevel >= 3 -> append(" 请深入分析，考虑所有角度，给出详细推理过程。")
            }
            if (s.agentToolsEnabled) {
                append(" 需要操作设备或文件时优先调用可用工具，而不是让用户手动操作。")
            }
        }
        val system = ChatMsg("system", systemPrompt)
        val history = _conversation.value.takeLast(s.maxHistoryLength.coerceAtLeast(10))
        return listOf(system) + history
    }

    /** 供外部（如视频旁路）复用的近期消息视图。 */
    fun recentMessages(max: Int): List<ChatMsg> = buildRequestMessages()

    // ---------- 状态操作 ----------

    /** 直接追加一条助手消息（工具/命令产生的本地提示）。 */
    fun notifyAssistant(text: String) {
        append(ChatMsg("assistant", text))
    }

    /** 输出一个视频产物消息（双模型视频生成结果），由 UI 渲染为卡片。 */
    fun emitVideo(artifact: com.bskai.data.VideoArtifact) {
        append(
            ChatMsg(
                role = "video",
                content = "已生成视频：${artifact.videoModel}（脚本：${artifact.scriptModel}）",
                toolName = artifact.videoPath
            )
        )
    }

    /** 清空对话历史。 */
    fun clearConversation() {
        _conversation.value = emptyList()
    }

    /** 用主对话模型做一次非流式生成（不进入对话历史）。 */
    suspend fun generateText(prompt: String): String {
        val s = settings.settings.value
        if (!s.apiConfigured) throw IllegalStateException("未配置 AI 服务")
        val resp = llm.chat(s, listOf(ChatMsg("user", prompt)))
        return resp.content.ifEmpty { throw IllegalStateException("返回内容为空") }
    }

    // ---------- 内部 ----------

    private fun append(msg: ChatMsg) {
        val max = settings.settings.value.maxHistoryLength
        _conversation.value = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
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
        val tail = if (s.apiConfigured) "" else " 提示：当前未配置 AI 服务，请到设置中填入 API。"
        val base = when {
            text.contains("你好") || text.contains("您好") || text.contains("hi", true) ||
                text.contains("hello", true) || text.contains("嗨") ->
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
