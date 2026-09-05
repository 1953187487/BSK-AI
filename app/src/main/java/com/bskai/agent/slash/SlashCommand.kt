package com.bskai.agent.slash

/**
 * 斜杠命令：本地命令 + 可扩展。
 *
 * 输入以 "/" 开头的字符串会唤起命令菜单。
 * 选择某个命令后，命令会消费掉 "/xxx" 并可能被替换为实际发送给 AI 的 prompt。
 */
sealed interface SlashOutcome {
    data class SendToAi(val text: String, val note: String? = null) : SlashOutcome
    data class LocalMessage(val message: String) : SlashOutcome
    data object Cancel : SlashOutcome
}

interface SlashCommand {
    val key: String              // 命令标识，如 "ws", "model", "clear"
    val label: String            // 菜单中显示名称
    val description: String      // 菜单中副标题
    val placeholder: String      // 占位提示
    fun resolve(arg: String): SlashOutcome
}

class SlashRegistry {
    private val commands = linkedMapOf<String, SlashCommand>()

    fun register(cmd: SlashCommand) {
        commands[cmd.key] = cmd
    }

    fun all(): List<SlashCommand> = commands.values.toList()

    fun suggestions(prefix: String): List<SlashCommand> {
        val p = prefix.removePrefix("/").lowercase()
        return commands.values.filter {
            it.key.startsWith(p) || it.label.contains(prefix, true)
        }
    }

    fun get(key: String): SlashCommand? = commands[key.lowercase().removePrefix("/")]
}

class WorkspaceToggleCommand(
    private val isEnabled: () -> Boolean,
    private val setEnabled: (Boolean) -> Unit
) : SlashCommand {
    override val key = "ws"
    override val label = "工作区"
    override val description = "本次对话允许 AI 读写工作区文件"
    override val placeholder = "/ws 开|关"

    override fun resolve(arg: String): SlashOutcome {
        val v = arg.trim().lowercase()
        when (v) {
            "开", "on", "1", "true" -> {
                setEnabled(true)
                return SlashOutcome.LocalMessage("已开启工作区权限")
            }
            "关", "off", "0", "false" -> {
                setEnabled(false)
                return SlashOutcome.LocalMessage("已关闭工作区权限")
            }
            else -> {
                val next = !isEnabled()
                setEnabled(next)
                return SlashOutcome.LocalMessage(if (next) "已开启工作区权限" else "已关闭工作区权限")
            }
        }
    }
}

class ModelPickCommand(
    private val current: () -> String,
    private val setModel: (String) -> Unit
) : SlashCommand {
    override val key = "model"
    override val label = "切换模型"
    override val description = "切换当前对话使用的模型"
    override val placeholder = "/model <名称>"

    override fun resolve(arg: String): SlashOutcome {
        val name = arg.trim()
        if (name.isBlank()) {
            return SlashOutcome.LocalMessage("当前模型：${current().ifBlank { "(未选择)" }}")
        }
        setModel(name)
        return SlashOutcome.LocalMessage("已切换到模型：$name")
    }
}

class ClearCommand(
    private val clear: () -> Unit
) : SlashCommand {
    override val key = "clear"
    override val label = "清空对话"
    override val description = "清空当前对话历史"
    override val placeholder = "/clear"
    override fun resolve(arg: String): SlashOutcome {
        clear()
        return SlashOutcome.LocalMessage("已清空对话")
    }
}

class HelpCommand(
    private val registry: SlashRegistry
) : SlashCommand {
    override val key = "help"
    override val label = "帮助"
    override val description = "查看所有斜杠命令"
    override val placeholder = "/help"
    override fun resolve(arg: String): SlashOutcome {
        val list = registry.all().joinToString("\n") { "/${it.key}  ${it.label} — ${it.description}" }
        return SlashOutcome.LocalMessage(list.ifBlank { "暂无可用命令" })
    }
}
