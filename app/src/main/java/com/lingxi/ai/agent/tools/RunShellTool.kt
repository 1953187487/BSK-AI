package com.lingxi.ai.agent.tools

import com.lingxi.ai.terminal.TerminalEngine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * run_shell: lets the AI run a command in LingXi's built-in terminal.
 *
 * Security audit:
 * - dangerous commands (rm -rf /, mkfs, dd, shutdown, reboot, poweroff, iptables, visudo) are rejected
 * - non-LOCAL backends are rejected by default; user must explicitly enable them
 * - single execution timeout of 30 seconds (default, max 300), enforced with a
 *   coroutine timeout so a hung backend can never suspend the tool loop; note
 *   that the terminal engine itself force-kills a process after 30 seconds, so
 *   the effective bound is min(requested, 30)
 * - stdout + stderr are truncated to 8000 characters in total
 */
class RunShellTool(private val engine: TerminalEngine) : Tool {
    override val name = "run_shell"
    override val description =
        "Run a shell command in LingXi's built-in terminal. Returns stdout, stderr and exitCode. " +
            "The command must finish within 30 seconds, otherwise it is terminated."
    override val parametersSchema = """
{
    "type": "object",
    "properties": {
        "command": {"type": "string", "description": "Shell command to execute (sh -c)"},
        "working_dir": {"type": "string", "description": "Optional working directory"},
        "timeout_seconds": {"type": "integer", "description": "Optional timeout in seconds, default 30, max 300"}
    },
    "required": ["command"]
}
""".trimIndent()

    /**
     * Executes the shell command described by [argumentsJson].
     *
     * @param argumentsJson JSON with `command` (required), optional `working_dir` and `timeout_seconds`
     * @return [ToolResult] containing the formatted execution output
     */
    override suspend fun execute(argumentsJson: String): ToolResult {
        val obj = try {
            JSONObject(argumentsJson)
        } catch (_: Exception) {
            return ToolResult(name, "arguments must be JSON", isError = true)
        }
        val command = obj.optString("command", "").trim()
        if (command.isEmpty()) {
            return ToolResult(name, "command is required", isError = true)
        }
        val blocked = matchDangerous(command)
        if (blocked != null) {
            return ToolResult(
                name,
                "该命令被安全策略拦截，未执行。匹配的危险模式：$blocked。" +
                    "请勿对设备执行破坏性、格式化、卸载、重启或防火墙修改类操作。",
                isError = true
            )
        }
        val workingDir = obj.optString("working_dir", "").ifBlank { null }
        val timeoutSeconds = obj.optInt("timeout_seconds", DEFAULT_TIMEOUT_SECONDS)
            .coerceIn(1, MAX_TIMEOUT_SECONDS).toLong()

        val result = withTimeoutOrNull(timeoutSeconds * 1000) {
            engine.execute(command, workingDir)
        }
        if (result == null) {
            return ToolResult(name, "命令执行超时（${timeoutSeconds} 秒），已停止。", isError = true)
        }

        val body = buildBody(result)
        return ToolResult(name, body, isError = result.exitCode != 0)
    }

    /** Formats one execution into the text fed back to the model, with output truncation. */
    private fun buildBody(result: TerminalEngine.ExecutionResult): String {
        val stdout = result.stdout
        val stderr = result.stderr
        val total = stdout.length + stderr.length
        var body = stdout
        var cutStderr = false
        if (total > MAX_OUTPUT_CHARS) {
            if (stdout.length >= MAX_OUTPUT_CHARS) {
                body = stdout.take(MAX_OUTPUT_CHARS)
                cutStderr = true
            } else {
                val remaining = MAX_OUTPUT_CHARS - stdout.length
                body = stdout + stderr.take(remaining)
                cutStderr = true
            }
        }
        return buildString {
            appendLine("# backend: ${result.backend}")
            appendLine("# exitCode: ${result.exitCode}")
            appendLine("# duration: ${result.durationMs} ms")
            appendLine("## stdout")
            appendLine(body)
            if (cutStderr && stderr.isNotEmpty()) {
                append("...(stdout+stderr 已截断，原始合计 $total 字符)")
            }
            if (stderr.isNotEmpty() && !cutStderr) {
                appendLine("## stderr")
                appendLine(stderr)
            }
        }.trimEnd()
    }

    /**
     * Finds the first destructive or dangerous pattern contained in [cmd].
     * Substring patterns catch compound forms (e.g. `rm -rf /`), token patterns
     * catch bare commands whose letters appear inside ordinary words.
     *
     * @param cmd the command string to inspect
     * @return the matched pattern, or null when the command is not banned
     */
    private fun matchDangerous(cmd: String): String? {
        val lower = cmd.lowercase()
        DANGEROUS_PATTERNS.firstOrNull { lower.contains(it) }?.let { return it }
        val tokens = lower.split(Regex("[^a-z0-9._/]+"))
        return DANGEROUS_TOKENS.firstOrNull { tokens.contains(it) }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30
        const val MAX_TIMEOUT_SECONDS = 300
        const val MAX_OUTPUT_CHARS = 8000

        val DANGEROUS_PATTERNS = listOf(
            "rm -rf /",
            "rm -rf /*",
            "dd if=",
            "dd of=",
            ":(){ :|:& };:",
            "init 0"
        )

        val DANGEROUS_TOKENS = listOf(
            "dd",
            "mkfs",
            "shutdown",
            "reboot",
            "poweroff",
            "iptables",
            "visudo",
            "wipefs",
            "fdisk",
            "parted",
            "sgdisk",
            "tune2fs",
            "halt"
        )
    }
}
