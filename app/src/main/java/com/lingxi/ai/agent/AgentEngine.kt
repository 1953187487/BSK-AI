package com.lingxi.ai.agent

import android.content.Context
import com.lingxi.ai.agent.slash.CompactCommand
import com.lingxi.ai.agent.slash.HistoryCommand
import com.lingxi.ai.agent.slash.SlashCommand
import com.lingxi.ai.agent.slash.SlashOutcome
import com.lingxi.ai.agent.slash.SlashRegistry
import com.lingxi.ai.agent.tools.DateTimeTool
import com.lingxi.ai.agent.tools.SystemInfoTool
import com.lingxi.ai.agent.tools.ToolRegistry
import com.lingxi.ai.agent.tools.ToolResult
import com.lingxi.ai.data.AppSettings
import com.lingxi.ai.data.ChatMode
import com.lingxi.ai.data.SettingsRepository
import com.lingxi.ai.workspace.WorkspaceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LingXi (灵犀 LingXi) conversation kernel.
 *
 * Responsibilities:
 * - owns the conversation history and the processing flag;
 * - resolves slash commands locally; a matched command never reaches the LLM;
 * - runs the tool-calling loop: tool definitions are sent with every request,
 *   requested tools are executed and their results are fed back, until the
 *   model answers with plain text or the round limit is reached;
 * - streams the reply when no tool is available, so the UI can render deltas.
 *
 * Reliability features:
 * - token-budgeted history selection ([buildRequestMessages]) that always keeps
 *   the system prompt and the newest user message, and never splits an
 *   assistant(toolCalls) message from its tool results;
 * - an early-conversation summary inserted when history does not fit;
 * - per-tool timeout and result truncation ([runToolLoop], [clampToolContent]);
 * - cooperative cancellation ([requestCancel], [isCancelled]).
 *
 * @param context application context
 * @param settings global settings repository
 */
class AgentEngine(
    private val context: Context,
    private val settings: SettingsRepository
) {

    private val llm = LlmClient(context)

    private val _conversation = MutableStateFlow<List<ChatMsg>>(emptyList())
    /** Conversation history, observed by the UI. */
    val conversation: StateFlow<List<ChatMsg>> = _conversation.asStateFlow()

    /** Tool registry, wired up by the Application. */
    var toolRegistry: ToolRegistry? = null
    var workspace: WorkspaceManager? = null
    var slashRegistry: SlashRegistry? = null

    private val _processing = MutableStateFlow(false)
    /** Whether one conversation round (tool loop or streaming reply) is in progress. */
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    val workspaceEnabled: Boolean
        get() = settings.settings.value.workspaceEnabled

    fun setWorkspaceEnabled(enabled: Boolean) {
        settings.update { it.copy(workspaceEnabled = enabled) }
    }

    private val cancelFlag = AtomicBoolean(false)

    /** Whether the current round was asked to stop. */
    val isCancelled: Boolean
        get() = cancelFlag.get()

    /**
     * Requests cancellation of the in-flight round. The engine checks the flag
     * at the start of every tool round and after every streamed delta, appends
     * the stop marker to the last assistant message and returns.
     */
    fun requestCancel() {
        cancelFlag.set(true)
    }

    /**
     * Handles one user input.
     * A matched slash command is processed locally and returns its text;
     * otherwise the tool loop or the streaming reply runs.
     *
     * @return the final assistant reply text of this round (UI may copy or log it)
     */
    suspend fun answer(userText: String): String {
        cancelFlag.set(false)
        val text = userText.trim()
        if (text.isEmpty()) return "请再说一遍"

        _processing.value = true
        try {
            // Slash commands are resolved locally and never consume an LLM round.
            val slash = if (text.startsWith("/")) resolveSlash(text) else SlashResolution(null, null, null)
            val localMessage = slash.localMessage
            val aiPrompt = slash.aiPrompt
            when {
                localMessage != null -> {
                    if (localMessage.isNotEmpty()) append(ChatMsg(ROLE_ASSISTANT, localMessage))
                    return localMessage
                }
                aiPrompt != null -> {
                    slash.note?.let { note -> if (note.isNotEmpty()) append(ChatMsg(ROLE_ASSISTANT, note)) }
                    append(ChatMsg(ROLE_USER, aiPrompt))
                }
                else -> append(ChatMsg(ROLE_USER, text))
            }

            val s = settings.settings.value
            if (!s.apiConfigured) {
                val fallback = localReply(text)
                append(ChatMsg(ROLE_ASSISTANT, fallback))
                return fallback
            }

            val tools = availableTools()
            return if (tools.isNotEmpty()) runToolLoop(tools) else runStreamingReply()
        } finally {
            _processing.value = false
        }
    }

    // ---------- Slash commands ----------

    /**
     * Result of slash resolution: a local message shown to the user, a rewritten
     * prompt to send to the model (with an optional assistant note shown first),
     * or null/null when the input is not a known command.
     */
    private data class SlashResolution(
        val localMessage: String?,
        val aiPrompt: String?,
        val note: String?
    )

    /**
     * Resolves a slash command.
     * The built-in commands (/compact, /history) are handled by the engine
     * itself, since they mutate or report on the conversation history; the UI
     * sees them through [slashSuggestions]. Everything else is delegated to the
     * registry so that LingXiApp.kt can extend the command set.
     *
     * @param raw the raw input, starting with "/"
     * @return the resolved outcome; both message and prompt are null when the
     *         input is not a known command (it is then sent to the AI as-is)
     */
    private fun resolveSlash(raw: String): SlashResolution {
        val parts = raw.trim().split(' ', limit = 2)
        val key = parts[0].lowercase().removePrefix("/")
        val arg = parts.getOrElse(1) { "" }

        val builtin: SlashCommand? = when (key) {
            "compact" -> CompactCommand { compactConversation() }
            "history" -> HistoryCommand { historySummary() }
            else -> null
        }
        val cmd = builtin ?: slashRegistry?.get(parts[0]) ?: return SlashResolution(null, null, null)
        return when (val outcome = cmd.resolve(arg)) {
            is SlashOutcome.LocalMessage -> SlashResolution(outcome.message, null, null)
            is SlashOutcome.SendToAi -> SlashResolution(null, outcome.text, outcome.note)
            is SlashOutcome.Cancel -> SlashResolution("", null, null)
        }
    }

    /** Keeps only the newest [COMPACT_KEEP] messages of the conversation history. */
    fun compactConversation(): SlashOutcome {
        val current = _conversation.value
        if (current.isEmpty()) return SlashOutcome.LocalMessage("当前没有可精简的对话。")
        val keep = COMPACT_KEEP.coerceAtMost(current.size)
        if (keep >= current.size) {
            return SlashOutcome.LocalMessage("当前对话只有 $keep 条，无需精简。")
        }
        _conversation.value = current.takeLast(keep)
        val dropped = current.size - keep
        return SlashOutcome.LocalMessage("已精简对话：保留最近 $keep 条，丢弃早期 $dropped 条。")
    }

    /** Reports the message count and the per-role distribution. */
    fun historySummary(): SlashOutcome {
        val current = _conversation.value
        if (current.isEmpty()) return SlashOutcome.LocalMessage("当前对话为空。")
        val counts = current.groupingBy { it.role }.eachCount()
        val roles = counts.entries.joinToString("、") { "${it.key}:${it.value}条" }
        return SlashOutcome.LocalMessage("当前对话共 ${current.size} 条消息。角色分布：$roles")
    }

    /** Current slash command suggestions (the UI command menu uses this). */
    fun slashSuggestions(prefix: String): List<SlashCommand> {
        val registry = slashRegistry?.all().orEmpty()
        val all = builtInSlashCommands() + registry
        val unique = all.distinctBy { it.key }
        val p = prefix.removePrefix("/").lowercase()
        return unique.filter { it.key.startsWith(p) || it.label.contains(prefix, true) }
    }

    /** Commands implemented by the engine; keep this in sync with [resolveSlash]. */
    private fun builtInSlashCommands(): List<SlashCommand> = listOf(
        CompactCommand { compactConversation() },
        HistoryCommand { historySummary() }
    )

    // ---------- Tool loop ----------

    /**
     * Tool definitions to expose to the model.
     * Rules: agentToolsEnabled enables the terminal tool; workspace file tools
     * additionally require workspaceEnabled.
     */
    private fun availableTools(): List<Map<String, Any>> {
        val s = settings.settings.value
        val registry = toolRegistry ?: return emptyList()
        if (!s.agentToolsEnabled) return emptyList()
        ensureBuiltInTools()
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
     * Main tool-calling loop.
     * Every round sends the token-budgeted history to the model; when the model
     * returns tool_calls the requested tools are executed (each guarded by a
     * timeout) and their results are fed back, until the model answers with
     * plain text, the round limit is reached, or the round is cancelled.
     */
    private suspend fun runToolLoop(tools: List<Map<String, Any>>): String {
        val s = settings.settings.value
        val registry = toolRegistry

        for (round in 1..TOOL_MAX_ROUNDS) {
            if (cancelFlag.get()) return markCancelled()

            val messages = buildRequestMessages()
            val resp = try {
                llm.chat(s, messages, tools)
            } catch (e: Exception) {
                val msg = "对话请求失败：" + (e.message ?: "未知错误")
                append(ChatMsg(ROLE_ASSISTANT, msg))
                return msg
            }

            if (resp.toolCalls.isEmpty()) {
                val final = resp.content.ifBlank { "抱歉，我没有想好怎么回答。" }
                append(ChatMsg(ROLE_ASSISTANT, final))
                return final
            }

            // Record the assistant tool-call request (tool_calls are echoed back next round).
            append(ChatMsg(ROLE_ASSISTANT, resp.content, toolCalls = resp.toolCalls))

            if (registry == null) break

            for (call in resp.toolCalls) {
                if (cancelFlag.get()) return markCancelled()
                val result = executeTool(registry, call)
                append(
                    ChatMsg(
                        role = ROLE_TOOL,
                        content = clampToolContent(result.content),
                        toolCallId = call.id,
                        toolName = call.name
                    )
                )
            }
        }

        val final = "工具调用已达到轮次上限（$TOOL_MAX_ROUNDS 轮）。请把任务拆成更小的指令分步执行。"
        append(ChatMsg(ROLE_ASSISTANT, final))
        return final
    }

    /**
     * Makes the engine's built-in tools available.
     * The canonical registration site is LingXiApp.kt; this fallback registers
     * them here (idempotently) so the tools work even before the Application
     * wiring is updated.
     */
    private fun ensureBuiltInTools() {
        val registry = toolRegistry ?: return
        for (tool in listOf(SystemInfoTool(context), DateTimeTool())) {
            if (registry.get(tool.name) == null) registry.register(tool)
        }
    }

    /**
     * Executes one tool call inside a timeout.
     * A timeout or an exception never escapes: both are reported as an error
     * result so the model can recover and the coroutine never hangs.
     */
    private suspend fun executeTool(registry: ToolRegistry, call: ToolCall): ToolResult {
        val tool = registry.get(call.name)
        if (tool == null) return ToolResult(call.name, "未知工具：${call.name}", isError = true)
        if (tool.requiresWorkspace && !workspaceEnabled) {
            return ToolResult(
                call.name,
                "工作区权限未开启：输入 /ws on 或到设置中开启后再试",
                isError = true
            )
        }

        val finished: ToolResult? = withTimeoutOrNull(TOOL_TIMEOUT_MILLIS) {
            try {
                tool.execute(call.argumentsJson)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToolResult(call.name, "执行异常：${e.message}", isError = true)
            }
        }
        return if (finished == null) {
            ToolResult(
                call.name,
                "工具执行超时（超过 ${TOOL_TIMEOUT_MILLIS / 1000} 秒），未返回结果。请改用更简单的指令或稍后重试。",
                isError = true
            )
        } else {
            finished
        }
    }

    /**
     * Truncates one tool result body to [TOOL_RESULT_MAX_CHARS] characters and
     * marks the truncation, so a single verbose tool cannot blow up the request.
     */
    private fun clampToolContent(content: String): String {
        if (content.length <= TOOL_RESULT_MAX_CHARS) return content
        return content.take(TOOL_RESULT_MAX_CHARS) +
            " ...(已截断，原始 ${content.length} 字符)"
    }

    // ---------- Streaming reply ----------

    /** No-tool mode: streams the reply, the UI observes deltas through conversation. */
    private suspend fun runStreamingReply(): String {
        val s = settings.settings.value
        append(ChatMsg(ROLE_ASSISTANT, ""))
        if (cancelFlag.get()) return markCancelled()

        val sb = StringBuilder()
        // takeWhile stops pulling from the network as soon as cancellation is requested.
        val events = llm.chatStream(s, buildRequestMessages()).takeWhile { !cancelFlag.get() }
        return try {
            events.collect { ev ->
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
            if (cancelFlag.get()) markCancelled()
            else sb.toString().ifEmpty { "抱歉，我没有想好怎么回答。" }
        } catch (_: CancellationException) {
            markCancelled()
        } catch (e: Exception) {
            if (sb.isEmpty()) {
                val fallback = "请求失败：${e.message ?: "未知错误"}"
                updateLastAssistant(fallback)
                fallback
            } else sb.toString()
        }
    }

    /**
     * Finishes a cancelled round.
     * A still-empty streaming placeholder is filled with the stop marker so the
     * UI does not grow an extra bubble; in every other case a new assistant
     * message carries the marker.
     */
    private fun markCancelled(): String {
        val last = _conversation.value.lastOrNull()
        if (last != null && last.role == ROLE_ASSISTANT && last.content.isEmpty()) {
            updateLastAssistant(STOP_MARKER)
        } else {
            append(ChatMsg(ROLE_ASSISTANT, STOP_MARKER))
        }
        return STOP_MARKER
    }

    // ---------- Request construction ----------

    /**
     * System prompt + token-budgeted recent history.
     *
     * Selection rules:
     * - rough token estimate: characters / 3, system prompt included;
     * - the total budget is min([MAX_CONTEXT_TOKENS], [HARD_CONTEXT_TOKENS_CAP]);
     * - the newest [AppSettings.maxHistoryLength] messages form the window,
     *   then the oldest blocks outside the budget are dropped;
     * - the system message and the newest user message are always kept;
     * - an assistant(toolCalls) message is always kept together with its tool
     *   results, since splitting them breaks the OpenAI tool protocol;
     * - when messages are dropped, a compact early-conversation summary is
     *   inserted as an extra system message.
     */
    private fun buildRequestMessages(): List<ChatMsg> {
        val s = settings.settings.value
        val systemPrompt = buildSystemPrompt(s)
        val system = ChatMsg(ROLE_SYSTEM, systemPrompt)
        val history = _conversation.value
        if (history.isEmpty()) return listOf(system)

        val blocks = historyBlocks(history)
        val windowStart = (history.size - s.maxHistoryLength.coerceAtLeast(1)).coerceAtLeast(0)
        val windowBlocks = blocks.filter { it.first() >= windowStart }
        val keep = selectWithinBudget(history, windowBlocks, remainingBudget(systemPrompt))
        val keepSet = keep.toHashSet()

        val dropped: List<ChatMsg> = if (keep.isEmpty()) history
        else history.filterIndexed { index, _ -> index !in keepSet }
        val summary = buildEarlySummary(dropped)

        val out = mutableListOf(system)
        if (summary.isNotEmpty()) out.add(ChatMsg(ROLE_SYSTEM, "早期对话摘要：$summary"))
        out.addAll(keep.map { history[it] })

        // Protocol sanitization: a tool result without its assistant(toolCalls)
        // parent, or an assistant(toolCalls) with no tool results behind it, would
        // be rejected by the OpenAI tool protocol.
        val validIds = out
            .filter { it.role == ROLE_ASSISTANT }
            .flatMap { it.toolCalls.map { tc -> tc.id } }
            .toSet()
        out.removeAll { msg ->
            msg.role == ROLE_TOOL && (msg.toolCallId == null || msg.toolCallId !in validIds)
        }
        val orphanCalls = out.indexOfLast { it.role == ROLE_ASSISTANT && it.toolCalls.isNotEmpty() }
        if (orphanCalls >= 0 && out.subList(orphanCalls + 1, out.size).none { it.role == ROLE_TOOL }) {
            out[orphanCalls] = out[orphanCalls].copy(toolCalls = emptyList())
        }

        return out
    }

    /** Builds the Chinese system prompt for the current settings. */
    private fun buildSystemPrompt(s: AppSettings): String = buildString {
        append("你是灵犀 LingXi，一个运行在 Android 设备上的智能 AI 助手。你友好、专业、乐于助人。")
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

    /**
     * Groups the history into atomic blocks so an assistant(toolCalls) message
     * is never split from its tool results.
     */
    private fun historyBlocks(history: List<ChatMsg>): List<List<Int>> {
        val blocks = mutableListOf<List<Int>>()
        var i = 0
        while (i < history.size) {
            if (history[i].role == ROLE_ASSISTANT && history[i].toolCalls.isNotEmpty()) {
                val block = mutableListOf(i)
                var j = i + 1
                while (j < history.size && history[j].role == ROLE_TOOL) {
                    block.add(j)
                    j++
                }
                blocks.add(block)
                i = j
            } else {
                blocks.add(listOf(i))
                i++
            }
        }
        return blocks
    }

    /**
     * Picks the newest blocks that fit the token budget, then force-includes
     * the newest user message so the model always has something to answer.
     */
    private fun selectWithinBudget(history: List<ChatMsg>, blocks: List<List<Int>>, budget: Int): List<Int> {
        if (blocks.isEmpty()) return emptyList()

        val keep = mutableListOf<Int>()
        var left = budget.coerceAtLeast(0)
        for (block in blocks.asReversed()) {
            if (left <= 0) break
            val cost = block.sumOf { estimateTokens(history[it]) }
            if (cost <= left) {
                left -= cost
                keep.addAll(block)
            }
        }

        // A round must always reach the model with the newest user message.
        val lastUser = history.indexOfLast { it.role == ROLE_USER }
        if (lastUser >= 0 && lastUser !in keep) keep.add(lastUser)
        return keep.sorted()
    }

    /**
     * Builds the early-conversation summary from the dropped messages:
     * up to [SUMMARY_MAX_ITEMS] items, each truncated to [SUMMARY_ITEM_CHARS]
     * characters, joined with a Chinese semicolon. No LLM is involved.
     */
    private fun buildEarlySummary(dropped: List<ChatMsg>): String {
        if (dropped.isEmpty()) return ""
        val parts = dropped
            .filter { it.role != ROLE_TOOL }
            .map { it.content.replace('\n', ' ').trim().take(SUMMARY_ITEM_CHARS) }
            .filter { it.isNotEmpty() }
            .take(SUMMARY_MAX_ITEMS)
        return parts.joinToString("；")
    }

    /** Rough token estimate used for the context budget: characters / 3. */
    private fun estimateTokens(text: String): Int = (text.length / 3 + 1).coerceAtLeast(1)

    /**
     * Rough token estimate for one message: content plus any tool-call arguments,
     * since both are serialized into the request body.
     */
    private fun estimateTokens(msg: ChatMsg): Int =
        estimateTokens(msg.content) + msg.toolCalls.sumOf { estimateTokens(it.argumentsJson) }

    /** Token budget left for the history, after the system prompt is counted. */
    private fun remainingBudget(systemPrompt: String): Int =
        MAX_CONTEXT_TOKENS.coerceAtMost(HARD_CONTEXT_TOKENS_CAP) - estimateTokens(systemPrompt)

    /** A recent-messages view for external callers such as the video pipeline. */
    fun recentMessages(max: Int): List<ChatMsg> =
        buildRequestMessages().filter { it.role != ROLE_SYSTEM }.takeLast(max.coerceAtLeast(1))

    // ---------- State operations ----------

    /** Appends one assistant message (produced by a tool or a slash command). */
    fun notifyAssistant(text: String) {
        append(ChatMsg(ROLE_ASSISTANT, text))
    }

    /** Emits one video artifact message, rendered as a card by the UI. */
    fun emitVideo(artifact: com.lingxi.ai.data.VideoArtifact) {
        append(
            ChatMsg(
                role = "video",
                content = "已生成视频：${artifact.videoModel}（脚本：${artifact.scriptModel}）",
                toolName = artifact.videoPath
            )
        )
    }

    /** Clears the conversation history. */
    fun clearConversation() {
        _conversation.value = emptyList()
    }

    /** One non-streaming generation with the main chat model; not stored in the history. */
    suspend fun generateText(prompt: String): String {
        val s = settings.settings.value
        if (!s.apiConfigured) throw IllegalStateException("未配置 AI 服务")
        val resp = llm.chat(s, listOf(ChatMsg(ROLE_USER, prompt)))
        return resp.content.ifEmpty { throw IllegalStateException("返回内容为空") }
    }

    // ---------- Internals ----------

    private fun append(msg: ChatMsg) {
        val max = settings.settings.value.maxHistoryLength
        _conversation.value = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
    }

    /**
     * Replaces the content of the newest assistant message.
     *
     * @return true when an assistant message was found and updated
     */
    private fun updateLastAssistant(content: String): Boolean {
        val list = _conversation.value.toMutableList()
        for (i in list.indices.reversed()) {
            if (list[i].role == ROLE_ASSISTANT) {
                list[i] = list[i].copy(content = content)
                _conversation.value = list
                return true
            }
        }
        return false
    }

    private fun localReply(text: String): String {
        val s = settings.settings.value
        val tail = if (s.apiConfigured) "" else " 提示：当前未配置 AI 服务，请到设置中填入 API。"
        val base = when {
            text.contains("你好") || text.contains("您好") || text.contains("hi", true) ||
                text.contains("hello", true) || text.contains("嗨") ->
                "你好呀，我是灵犀 LingXi。"
            text.contains("你是谁") || text.contains("介绍你自己") ->
                "我是灵犀 LingXi，运行在 Android 设备上的 AI 助手。"
            text.contains("你能做什么") || text.contains("你会什么") || text.contains("有什么功能") ->
                "在设置中配置 AI 服务后，我可以陪你自由对话，回答各种问题。"
            text.contains("谢谢") -> "不客气，随时找我。"
            else -> "抱歉，我还没有完全听明白。在设置里配置 AI 服务后我能更好地回答您。"
        }
        return base + tail
    }

    companion object {
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_TOOL = "tool"

        /** Stop marker appended to the last assistant message on cancellation. */
        internal const val STOP_MARKER = "\n\n[已停止]"

        /** Soft context budget, in rough tokens. */
        private const val MAX_CONTEXT_TOKENS = 8000

        /** Hard cap for the context budget, in rough tokens. */
        private const val HARD_CONTEXT_TOKENS_CAP = 8000

        /** Messages kept by /compact. */
        private const val COMPACT_KEEP = 6

        /** Per-tool execution timeout, in milliseconds. */
        private const val TOOL_TIMEOUT_MILLIS = 60_000L

        /** Max characters of one tool result fed back to the model. */
        private const val TOOL_RESULT_MAX_CHARS = 6000

        /** Max tool-calling rounds per user turn. */
        private const val TOOL_MAX_ROUNDS = 8

        /** Max items in the early-conversation summary. */
        private const val SUMMARY_MAX_ITEMS = 8

        /** Max characters per summary item. */
        private const val SUMMARY_ITEM_CHARS = 120
    }
}
