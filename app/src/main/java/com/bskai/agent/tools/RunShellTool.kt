package com.bskai.agent.tools

import com.bskai.terminal.TerminalEngine

/**
 * run_shell: 让 AI 在 AURA 内置终端里跑命令。
 *
 * 安全审计：
 * - 危险命令（rm -rf /、mkfs、dd of=/dev/...、reboot、shutdown）直接拒绝
 * - 非 LOCAL 后端默认拒绝，需用户明确开启
 * - 单次超时 30 秒
 */
class RunShellTool(private val engine: TerminalEngine) : Tool {
    override val name = "run_shell"
    override val description = "Run a shell command in AURA's built-in terminal. Returns stdout, stderr, exitCode."
    override val parametersSchema = """{
        "type": "object",
        "properties": {
            "command": {"type": "string", "description": "Shell command to execute (sh -c)"},
            "working_dir": {"type": "string", "description": "Optional working directory"}
        },
        "required": ["command"]
    }"""

    override suspend fun execute(argumentsJson: String): ToolResult {
        val obj = try { org.json.JSONObject(argumentsJson) } catch (_: Exception) {
            return ToolResult(name, "arguments must be JSON", isError = true)
        }
        val command = obj.optString("command", "").trim()
        if (command.isEmpty()) return ToolResult(name, "command is required", isError = true)
        if (isDangerous(command)) {
            return ToolResult(name, "拒绝执行危险命令：$command", isError = true)
        }
        val workingDir = obj.optString("working_dir", "").ifBlank { null }
        val result = engine.execute(command, workingDir)
        val body = buildString {
            appendLine("# backend: ${result.backend}")
            appendLine("# exitCode: ${result.exitCode}")
            appendLine("# duration: ${result.durationMs} ms")
            if (result.stdout.isNotEmpty()) {
                appendLine("## stdout")
                appendLine(result.stdout)
            }
            if (result.stderr.isNotEmpty()) {
                appendLine("## stderr")
                appendLine(result.stderr)
            }
        }.trimEnd()
        return ToolResult(name, body, isError = result.exitCode != 0)
    }

    private fun isDangerous(cmd: String): Boolean {
        val lower = cmd.lowercase()
        val banned = listOf(
            "rm -rf /", "rm -rf /*", "mkfs.", "dd if=", "shutdown", "reboot", "halt",
            ":(){ :|:& };:", "wipefs", "mkfs", "fdisk", "parted"
        )
        return banned.any { lower.contains(it) }
    }
}
