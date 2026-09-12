package com.bskai.agent.slash

/**
 * /video <提示词>：双模型视频生成。
 * 脚本模型（API/自定义）写分镜，本地视频模型生成视频，产物落到对话流。
 */
class VideoGenCommand(
    private val generate: (String) -> Unit
) : SlashCommand {
    override val key = "video"
    override val label = "视频生成"
    override val description = "双模型视频生成：脚本模型写分镜 + 本地模型出片"
    override val placeholder = "/video <描述>"

    override fun resolve(arg: String): SlashOutcome {
        val prompt = arg.trim()
        if (prompt.isEmpty()) {
            return SlashOutcome.LocalMessage("用法：/video <视频描述>")
        }
        generate(prompt)
        return SlashOutcome.LocalMessage("已提交视频生成任务：$prompt")
    }
}

