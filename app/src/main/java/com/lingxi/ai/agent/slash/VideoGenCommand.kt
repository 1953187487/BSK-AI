package com.lingxi.ai.agent.slash

/**
 * /video <prompt>: dual-model video generation.
 * The script model (API / custom) writes the shot breakdown, the local video
 * model generates the video, and the artifact lands in the conversation flow.
 *
 * NOTE: user-facing strings are in Chinese and are i18n candidates.
 */
class VideoGenCommand(
    private val generate: (String) -> Unit
) : SlashCommand {
    override val key = "video"
    override val label = "视频生成"
    override val description = "双模型视频生成：脚本模型写分镜 + 本地模型出片"
    override val placeholder = "/video <描述>"

    /**
     * Resolves the video generation command.
     *
     * @param arg video description prompt (empty = usage message)
     * @return [SlashOutcome.LocalMessage] acknowledging the submission
     */
    override fun resolve(arg: String): SlashOutcome {
        val prompt = arg.trim()
        if (prompt.isEmpty()) {
            return SlashOutcome.LocalMessage("用法：/video <视频描述>")
        }
        generate(prompt)
        return SlashOutcome.LocalMessage("已提交视频生成任务：$prompt")
    }
}
