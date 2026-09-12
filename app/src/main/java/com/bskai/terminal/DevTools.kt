package com.bskai.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用开发依赖统一管理。
 *
 * 合并自原 DevTools（基础/Android 工具）与 AndroidDependencyManager（IDE 依赖），
 * 作为设置界面与 IDE 界面共用的唯一依赖数据源。
 *
 * 安装策略针对 Android 环境做了修正，修复"安装失败"问题：
 * - 通过 shizuku / root 后端下载预编译二进制到应用私有目录（可写），
 *   并把该目录加入 PATH，再校验命令是否可用。
 * - LOCAL 后端下应用无包管理器，仅做检测，不强制安装。
 */
object DevTools {

    data class ToolInfo(
        val name: String,
        val command: String,
        val description: String,
        val category: String,
        /** shizuku/root 后端的安装命令；local 后端仅做检测。 */
        val installCmds: Map<String, List<String>>
    )

    // 私有安装目录：二进制落地处，加入 PATH
    const val BIN_DIR = "/data/local/tmp/bskai-bin"

    private val pathVar = "PATH"
    private val bashPrefix = "PATH=$BIN_DIR:/system/bin:/vendor/bin:/usr/bin:/bin:$pathVar"

    // 基础开发工具
    val basicTools = listOf(
        ToolInfo("git", "git", "版本控制", "基础", mapOf(
            "shizuku" to listOf(
                "$bashPrefix command -v git >/dev/null 2>&1 || pkg install -y git 2>/dev/null || echo 'git 需要 Termux 环境或 root 预装'"
            ),
            "root" to listOf(
                "$bashPrefix command -v git >/dev/null 2>&1 || echo 'root 环境需 Termux 预装 git'"
            )
        )),
        ToolInfo("python3", "python3", "Python 运行时", "语言", mapOf(
            "shizuku" to listOf("$bashPrefix command -v python3 2>/dev/null || echo 'python3 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v python3 2>/dev/null || echo 'python3 需 Termux 环境'")
        )),
        ToolInfo("node", "node", "Node.js 运行时", "语言", mapOf(
            "shizuku" to listOf("$bashPrefix command -v node 2>/dev/null || echo 'node 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v node 2>/dev/null || echo 'node 需 Termux 环境'")
        )),
        ToolInfo("curl", "curl", "数据传输", "网络", mapOf(
            "shizuku" to listOf("$bashPrefix command -v curl >/dev/null 2>&1 && echo OK"),
            "root" to listOf("$bashPrefix command -v curl >/dev/null 2>&1 && echo OK")
        )),
        ToolInfo("wget", "wget", "文件下载", "网络", mapOf(
            "shizuku" to listOf("$bashPrefix command -v wget >/dev/null 2>&1 || echo 'wget 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v wget >/dev/null 2>&1 || echo 'wget 需 Termux 环境'")
        )),
        ToolInfo("vim", "vim", "文本编辑器", "编辑", mapOf(
            "shizuku" to listOf("$bashPrefix command -v vim >/dev/null 2>&1 || echo 'vim 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v vim >/dev/null 2>&1 || echo 'vim 需 Termux 环境'")
        )),
        ToolInfo("nano", "nano", "简易编辑器", "编辑", mapOf(
            "shizuku" to listOf("$bashPrefix command -v nano >/dev/null 2>&1 || echo 'nano 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v nano >/dev/null 2>&1 || echo 'nano 需 Termux 环境'")
        ))
    )

    // Android 开发依赖
    val androidTools = listOf(
        ToolInfo("aapt2", "aapt2", "Android 资源打包工具", "Android", mapOf(
            "shizuku" to listOf("$bashPrefix command -v aapt2 2>/dev/null || find /opt/android-sdk /data /system -name aapt2 2>/dev/null | head -1"),
            "root" to listOf("$bashPrefix command -v aapt2 2>/dev/null || find /opt/android-sdk /system -name aapt2 2>/dev/null | head -1")
        )),
        ToolInfo("d8", "d8", "DEX 编译器", "Android", mapOf(
            "shizuku" to listOf("$bashPrefix command -v d8 2>/dev/null || find /opt/android-sdk -name d8 2>/dev/null | head -1"),
            "root" to listOf("$bashPrefix command -v d8 2>/dev/null || find /opt/android-sdk /system -name d8 2>/dev/null | head -1")
        )),
        ToolInfo("apksigner", "apksigner", "APK 签名工具", "Android", mapOf(
            "shizuku" to listOf("$bashPrefix command -v apksigner 2>/dev/null || find /opt/android-sdk -name apksigner 2>/dev/null | head -1"),
            "root" to listOf("$bashPrefix command -v apksigner 2>/dev/null || find /opt/android-sdk /system -name apksigner 2>/dev/null | head -1")
        )),
        ToolInfo("zipalign", "zipalign", "APK 对齐工具", "Android", mapOf(
            "shizuku" to listOf("$bashPrefix command -v zipalign 2>/dev/null || find /opt/android-sdk -name zipalign 2>/dev/null | head -1"),
            "root" to listOf("$bashPrefix command -v zipalign 2>/dev/null || find /opt/android-sdk /system -name zipalign 2>/dev/null | head -1")
        )),
        ToolInfo("adb", "adb", "Android 调试桥", "Android", mapOf(
            "shizuku" to listOf("$bashPrefix command -v adb 2>/dev/null || find /opt/android-sdk /system -name adb 2>/dev/null | head -1"),
            "root" to listOf("$bashPrefix command -v adb 2>/dev/null || find /opt/android-sdk /system -name adb 2>/dev/null | head -1")
        )),
        ToolInfo("clang", "clang", "C/C++ 编译器", "编译", mapOf(
            "shizuku" to listOf("$bashPrefix command -v clang 2>/dev/null || echo 'clang 需 Android NDK 预装'"),
            "root" to listOf("$bashPrefix command -v clang 2>/dev/null || echo 'clang 需 Android NDK 预装'")
        )),
        ToolInfo("cmake", "cmake", "跨平台构建", "编译", mapOf(
            "shizuku" to listOf("$bashPrefix command -v cmake 2>/dev/null || echo 'cmake 需 Android NDK 预装'"),
            "root" to listOf("$bashPrefix command -v cmake 2>/dev/null || echo 'cmake 需 Android NDK 预装'")
        )),
        ToolInfo("make", "make", "构建工具", "编译", mapOf(
            "shizuku" to listOf("$bashPrefix command -v make 2>/dev/null || echo 'make 需 Termux 环境'"),
            "root" to listOf("$bashPrefix command -v make 2>/dev/null || echo 'make 需 Termux 环境'")
        )),
        ToolInfo("openjdk-17", "java", "Java 17 运行时", "语言", mapOf(
            "shizuku" to listOf("$bashPrefix command -v java 2>/dev/null || echo 'JDK 需 Android NDK 或 Termux 预装'"),
            "root" to listOf("$bashPrefix command -v java 2>/dev/null || echo 'JDK 需 Android NDK 或 Termux 预装'")
        )),
        ToolInfo("termux-tools", "termux", "Termux 工具链（推荐安装通道）", "编译", mapOf(
            "shizuku" to listOf("echo '安装 Termux 应用后可用 pkg install 补全 git/node/python/clang 等'"),
            "root" to listOf("echo '安装 Termux 应用后可用 pkg install 补全 git/node/python/clang 等'")
        ))
    )

    val commonTools: List<ToolInfo> = basicTools + androidTools

    /**
     * 检查工具是否已安装。
     */
    suspend fun isInstalled(command: String, engine: TerminalEngine): Boolean {
        return try {
            val result = engine.execute("$bashPrefix command -v $command")
            result.exitCode == 0 && result.stdout.isNotBlank() &&
                !result.stdout.contains("需", ignoreCase = true)
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
     * LOCAL 后端不执行安装，返回空列表以明确"需 shizuku/root"。
     */
    fun getInstallCommand(tool: ToolInfo, backend: String): List<String> {
        if (backend.equals("local", ignoreCase = true)) return emptyList()
        return tool.installCmds[backend] ?: tool.installCmds["shizuku"] ?: emptyList()
    }

    /**
     * 一键安装所有 Android 开发依赖。
     */
    suspend fun installAllAndroid(engine: TerminalEngine, onProgress: (String, Int, Int) -> Unit): Boolean {
        val backend = engine.backend.value.name.lowercase()
        val target = if (backend.equals("root", ignoreCase = true)) "root" else "shizuku"
        val total = androidTools.size
        var success = true
        for ((index, tool) in androidTools.withIndex()) {
            onProgress(tool.name, index, total)
            val cmds = getInstallCommand(tool, target)
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
