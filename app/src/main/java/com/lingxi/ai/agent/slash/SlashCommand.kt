package com.lingxi.ai.agent.slash

/**
 * Slash commands: local, extensible commands triggered by a "/" prefix.
 *
 * All user-facing strings are in Chinese and are i18n candidates:
 * they should be moved to a string-resource / i18n layer in a future pass.
 */

/**
 * The outcome of resolving a slash command.
 */
sealed interface SlashOutcome {
    /** Send text to the AI. [text] is the prompt, [note] is an optional user-facing note. */
    data class SendToAi(val text: String, val note: String? = null) : SlashOutcome

    /** Show a local message to the user without sending to the AI. */
    data class LocalMessage(val message: String) : SlashOutcome

    /** Cancel the command. */
    data object Cancel : SlashOutcome
}

/**
 * A slash command: an extensible, user-invokable local operation.
 */
interface SlashCommand {
    /** Stable identifier, e.g. "ws", "model", "clear". */
    val key: String
    /** Display name shown in the command menu. */
    val label: String
    /** Subtitle shown in the command menu. */
    val description: String
    /** Placeholder hint shown in the input field. */
    val placeholder: String

    /**
     * Resolves the command with the given argument (the text after the command key).
     *
     * @param arg the user-supplied argument
     * @return the [SlashOutcome] of the command
     */
    fun resolve(arg: String): SlashOutcome
}

/**
 * Registry of [SlashCommand]s, keyed by command key.
 */
class SlashRegistry {
    private val commands = linkedMapOf<String, SlashCommand>()

    /**
     * Registers a command, replacing any command with the same key.
     *
     * @param cmd the command to register
     */
    fun register(cmd: SlashCommand) {
        commands[cmd.key] = cmd
    }

    /**
     * Returns all registered commands in insertion order.
     */
    fun all(): List<SlashCommand> = commands.values.toList()

    /**
     * Returns commands whose key or label matches the given prefix.
     *
     * @param prefix the prefix to filter by (leading "/" is stripped)
     * @return matching commands
     */
    fun suggestions(prefix: String): List<SlashCommand> {
        val p = prefix.removePrefix("/").lowercase()
        return commands.values.filter {
            it.key.startsWith(p) || it.label.contains(prefix, true)
        }
    }

    /**
     * Returns the command registered under [key], case-insensitive.
     *
     * @param key command key (leading "/" is stripped)
     * @return the matching [SlashCommand] or null
     */
    fun get(key: String): SlashCommand? = commands[key.lowercase().removePrefix("/")]
}

/**
 * /ws on|off: toggles whether the AI may read/write workspace files.
 */
class WorkspaceToggleCommand(
    private val isEnabled: () -> Boolean,
    private val setEnabled: (Boolean) -> Unit
) : SlashCommand {
    override val key = "ws"
    override val label = "工作区"
    override val description = "本次对话允许 AI 读写工作区文件"
    override val placeholder = "/ws 开|关"

    /**
     * Resolves the toggle argument.
     *
     * @param arg "开"/"on"/"1"/"true" enables, "关"/"off"/"0"/"false" disables;
     *            any other value toggles the current state
     * @return [SlashOutcome.LocalMessage] describing the new state
     */
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

/**
 * /model <name>: switches the model used for the current conversation.
 */
class ModelPickCommand(
    private val current: () -> String,
    private val setModel: (String) -> Unit
) : SlashCommand {
    override val key = "model"
    override val label = "切换模型"
    override val description = "切换当前对话使用的模型"
    override val placeholder = "/model <名称>"

    /**
     * Resolves the model switch. An empty argument reports the current model.
     *
     * @param arg model name to switch to (empty = query current)
     * @return [SlashOutcome.LocalMessage]
     */
    override fun resolve(arg: String): SlashOutcome {
        val name = arg.trim()
        if (name.isBlank()) {
            return SlashOutcome.LocalMessage("当前模型：${current().ifBlank { "(未选择)" }}")
        }
        setModel(name)
        return SlashOutcome.LocalMessage("已切换到模型：$name")
    }
}

/**
 * /clear: clears the current conversation history.
 */
class ClearCommand(
    private val clear: () -> Unit
) : SlashCommand {
    override val key = "clear"
    override val label = "清空对话"
    override val description = "清空当前对话历史"
    override val placeholder = "/clear"

    /**
     * Clears the conversation and returns a confirmation message.
     */
    override fun resolve(arg: String): SlashOutcome {
        clear()
        return SlashOutcome.LocalMessage("已清空对话")
    }
}

/**
 * /help: lists all available slash commands.
 */
class HelpCommand(
    private val registry: SlashRegistry
) : SlashCommand {
    override val key = "help"
    override val label = "帮助"
    override val description = "查看所有斜杠命令"
    override val placeholder = "/help"

    /**
     * Returns a formatted list of all registered commands.
     */
    override fun resolve(arg: String): SlashOutcome {
        val list = registry.all().joinToString("\n") { "/${it.key}  ${it.label} — ${it.description}" }
        return SlashOutcome.LocalMessage(list.ifBlank { "暂无可用命令" })
    }
}
