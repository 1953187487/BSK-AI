package com.bskai.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用开发依赖管理。检查并安装常用开发工具。
 * 利用内置终端在 LOCAL / SHIZUKU / ROOT 环境下安装工具。
 */
object DevTools {

    data class ToolInfo(
        val name: String,
        val command: String,
        val description: String,
        val category: String,
        val installCmds: Map<String, List<String>> // backend -> commands
    )

    // 基础开发工具
    val basicTools = listOf(
        ToolInfo("git", "git", "版本控制", "基础", mapOf(
            "local" to listOf("apt-get update && apt-get install -y git"),
            "shizuku" to listOf("apt-get update && apt-get install -y git"),
            "root" to listOf("apt-get update && apt-get install -y git")
        )),
        ToolInfo("python3", "python3", "Python 运行时", "语言", mapOf(
            "local" to listOf("apt-get update && apt-get install -y python3"),
            "shizuku" to listOf("apt-get update && apt-get install -y python3"),
            "root" to listOf("apt-get update && apt-get install -y python3")
        )),
        ToolInfo("node", "node", "Node.js 运行时", "语言", mapOf(
            "local" to listOf("apt-get update && apt-get install -y nodejs npm"),
            "shizuku" to listOf("apt-get update && apt-get install -y nodejs npm"),
            "root" to listOf("apt-get update && apt-get install -y nodejs npm")
        )),
        ToolInfo("curl", "curl", "数据传输", "网络", mapOf(
            "local" to listOf("apt-get update && apt-get install -y curl"),
            "shizuku" to listOf("apt-get update && apt-get install -y curl"),
            "root" to listOf("apt-get update && apt-get install -y curl")
        )),
        ToolInfo("wget", "wget", "文件下载", "网络", mapOf(
            "local" to listOf("apt-get update && apt-get install -y wget"),
            "shizuku" to listOf("apt-get update && apt-get install -y wget"),
            "root" to listOf("apt-get update && apt-get install -y wget")
        )),
        ToolInfo("vim", "vim", "文本编辑器", "编辑", mapOf(
            "local" to listOf("apt-get update && apt-get install -y vim"),
            "shizuku" to listOf("apt-get update && apt-get install -y vim"),
            "root" to listOf("apt-get update && apt-get install -y vim")
        )),
        ToolInfo("nano", "nano", "简易编辑器", "编辑", mapOf(
            "local" to listOf("apt-get update && apt-get install -y nano"),
            "shizuku" to listOf("apt-get update && apt-get install -y nano"),
            "root" to listOf("apt-get update && apt-get install -y nano")
        ))
    )

    // Android 开发依赖
    val androidTools = listOf(
        ToolInfo("aapt2", "aapt2", "Android 资源打包工具", "Android", mapOf(
            "local" to listOf(
                "apt-get update && apt-get install -y aapt2"
            ),
            "shizuku" to listOf(
                "apt-get update && apt-get install -y aapt2"
            ),
            "root" to listOf(
                "apt-get update && apt-get install -y aapt2"
            )
        )),
        ToolInfo("d8", "d8", "DEX 编译器", "Android", mapOf(
            "local" to listOf("apt-get update && apt-get install -y d8"),
            "shizuku" to listOf("apt-get update && apt-get install -y d8"),
            "root" to listOf("apt-get update && apt-get install -y d8")
        )),
        ToolInfo("apksigner", "apksigner", "APK 签名工具", "Android", mapOf(
            "local" to listOf("apt-get update && apt-get install -y apksigner"),
            "shizuku" to listOf("apt-get update && apt-get install -y apksigner"),
            "root" to listOf("apt-get update && apt-get install -y apksigner")
        )),
        ToolInfo("zipalign", "zipalign", "APK 对齐工具", "Android", mapOf(
            "local" to listOf("apt-get update && apt-get install -y zipalign"),
            "shizuku" to listOf("apt-get update && apt-get install -y zipalign"),
            "root" to listOf("apt-get update && apt-get install -y zipalign")
        )),
        ToolInfo("adb", "adb", "Android 调试桥", "Android", mapOf(
            "local" to listOf("apt-get update && apt-get install -y adb"),
            "shizuku" to listOf("apt-get update && apt-get install -y adb"),
            "root" to listOf("apt-get update && apt-get install -y adb")
        )),
        ToolInfo("clang", "clang", "C/C++ 编译器", "编译", mapOf(
            "local" to listOf("apt-get update && apt-get install -y clang"),
            "shizuku" to listOf("apt-get update && apt-get install -y clang"),
            "root" to listOf("apt-get update && apt-get install -y clang")
        )),
        ToolInfo("cmake", "cmake", "跨平台构建", "编译", mapOf(
            "local" to listOf("apt-get update && apt-get install -y cmake"),
            "shizuku" to listOf("apt-get update && apt-get install -y cmake"),
            "root" to listOf("apt-get update && apt-get install -y cmake")
        )),
        ToolInfo("make", "make", "构建工具", "编译", mapOf(
            "local" to listOf("apt-get update && apt-get install -y make"),
            "shizuku" to listOf("apt-get update && apt-get install -y make"),
            "root" to listOf("apt-get update && apt-get install -y make")
        )),
        ToolInfo("openjdk-17", "java", "Java 17 运行时", "语言", mapOf(
            "local" to listOf("apt-get update && apt-get install -y openjdk-17-jdk"),
            "shizuku" to listOf("apt-get update && apt-get install -y openjdk-17-jdk"),
            "root" to listOf("apt-get update && apt-get install -y openjdk-17-jdk")
        ))
    )

    val commonTools: List<ToolInfo> = basicTools + androidTools

    /**
     * 检查工具是否已安装。
     */
    suspend fun isInstalled(command: String, engine: TerminalEngine): Boolean {
        return try {
            val result = engine.execute("command -v $command")
            result.exitCode == 0 && result.stdout.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查所有工具安装状态。
     */
    suspend fun checkAll(engine: TerminalEngine): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (tool in commonTools) {
            result[tool.command] = isInstalled(tool.command, engine)
        }
        return result
    }

    /**
     * 获取安装命令（根据当前后端）。
     */
    fun getInstallCommand(tool: ToolInfo, backend: String): List<String> {
        return tool.installCmds[backend] ?: tool.installCmds["local"] ?: emptyList()
    }

    /**
     * 一键安装所有 Android 开发依赖。
     */
    suspend fun installAllAndroid(engine: TerminalEngine, onProgress: (String, Int, Int) -> Unit): Boolean {
        val backend = engine.backend.value.name.lowercase()
        val total = androidTools.size
        var success = true
        for ((index, tool) in androidTools.withIndex()) {
            onProgress(tool.name, index, total)
            val cmds = getInstallCommand(tool, backend)
            for (cmd in cmds) {
                val r = engine.execute(cmd)
                if (r.exitCode != 0) {
                    success = false
                    break
                }
            }
        }
        return success
    }
}
