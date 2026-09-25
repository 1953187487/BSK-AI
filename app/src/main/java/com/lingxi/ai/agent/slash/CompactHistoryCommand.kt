package com.lingxi.ai.agent.slash

/**
 * /compact: trims the conversation history down to the last few messages.
 *
 * NOTE: user-facing strings are in Chinese and are i18n candidates.
 * The command is handled by AgentEngine itself, since it mutates the
 * conversation history; register it in LingXiApp.kt so the UI menu shows it.
 */
class CompactCommand(
    private val compact: () -> SlashOutcome
) : SlashCommand {
    override val key = "compact"
    override val label = "精简对话"
    override val description = "仅保留最近 6 条消息，丢弃早期对话以缩短上下文"
    override val placeholder = "/compact"

    /**
     * Trims the conversation and returns the confirmation message.
     */
    override fun resolve(arg: String): SlashOutcome = compact()
}

/**
 * /history: reports the current conversation length and role distribution.
 *
 * NOTE: user-facing strings are in Chinese and are i18n candidates.
 * The command is handled by AgentEngine itself; register it in LingXiApp.kt
 * so the UI menu shows it.
 */
class HistoryCommand(
    private val summary: () -> SlashOutcome
) : SlashCommand {
    override val key = "history"
    override val label = "对话统计"
    override val description = "查看当前对话条数与角色分布"
    override val placeholder = "/history"

    /**
     * Returns the statistics of the current conversation.
     */
    override fun resolve(arg: String): SlashOutcome = summary()
}
