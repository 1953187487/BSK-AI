package com.bskai.agent.tools

import org.json.JSONObject
import java.io.File

object ShellTool : Tool {
    override val name = "shell"
    override val description = "在设备上执行 shell 命令（需要 Termux 环境或 root），用于编译、构建、检查系统状态。"
    override val requiresPermission = true
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject()
            .put("command", JSONObject().put("type", "string").put("description", "要执行的 shell 命令"))
            .put("cwd", JSONObject().put("type", "string").put("description", "工作目录，默认工作区")))
        .put("required", JSONArrayOf("command"))

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val command = args.optString("command")
        val cwd = ctx.resolveWorkspace(args.optString("cwd"))
        return runCatching {
        val procDir = File(cwd)
        val proc = ProcessBuilder("sh", "-c", command)
            .apply { if (procDir.exists()) directory(procDir) }
            .redirectErrorStream(true)
            .start()
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            val truncated = output.take(6000)
            ToolResult(true, "exit=$code\n$truncated")
        }.getOrElse { ToolResult(false, "命令执行失败: ${it.message}") }
    }
}

object SystemInfoTool : Tool {
    override val name = "get_system_info"
    override val description = "获取设备系统信息（架构、内存、存储、Android 版本），用于判断构建环境。"
    override val requiresPermission = false
    override val parameters: JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject())
        .put("required", JSONArrayOf())

    override suspend fun execute(args: JSONObject, ctx: ToolContext): ToolResult {
        val rb = Runtime.getRuntime()
        val info = buildString {
            appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.contentToString()}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("JVM 内存: 已用 ${(rb.totalMemory() - rb.freeMemory()) / 1048576}MB / 最大 ${rb.maxMemory() / 1048576}MB")
        }
        return ToolResult(true, info.trimEnd())
    }
}
