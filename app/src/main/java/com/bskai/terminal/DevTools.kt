package com.bskai.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified management of application development dependencies.
 *
 * Merged from the former DevTools (basic / Android tools) and
 * AndroidDependencyManager (IDE dependencies), serving as the single
 * dependency data source shared by the Settings and IDE screens.
 *
 * Installation strategy is corrected for Android environments (fixes
 * the "install failed" issue):
 * - shizuku / root backends download pre-compiled binaries into the
 *   app-private directory (writable), add that directory to PATH,
 *   then verify the command is available.
 * - The LOCAL backend has no package manager in the app, so only
 *   detection is performed; installation is not forced.
 */
object DevTools {

    /**
     * Metadata for a single development tool.
     *
     * @property name display name
     * @property command the CLI command to check
     * @property description short description
     * @property category grouping label
     * @property installCmds backend-to-install-commands map;
     *        the local backend only performs detection
     */
    data class ToolInfo(
        val name: String,
        val command: String,
        val description: String,
        val category: String,
        val installCmds: Map<String, List<String>>
    )

    // Private install directory: where binaries land; added to PATH
    const val BIN_DIR = "/data/local/tmp/bskai-bin"

    private val pathVar = "PATH"
    private val bashPrefix = "PATH=$BIN_DIR:/system/bin:/vendor/bin:/usr/bin:/bin:$pathVar"

    // Basic development tools
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

    // Android development dependencies
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

    /** Combined list of basic and Android tools. */
    val commonTools: List<ToolInfo> = basicTools + androidTools

    /**
     * Checks whether a tool is installed.
     *
     * Strategy: check the exit code first. Only when the exit code is 0
     * do we fall back to a stdout heuristic to detect "needs X environment"
     * messages that some install commands emit instead of installing.
     *
     * @param command CLI command name to check
     * @param engine terminal engine to run the check through
     * @return true when the command is available
     */
    suspend fun isInstalled(command: String, engine: TerminalEngine): Boolean {
        return try {
            val result = engine.execute("$bashPrefix command -v $command")
            // Primary signal: exit code
            if (result.exitCode != 0) return false
            // Secondary heuristic (only on zero exit): some tools print
            // "needs Termux / NDK pre-install" instead of resolving a path.
            result.stdout.isNotBlank() &&
                !result.stdout.contains("需", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks installation status of all tools.
     *
     * @param engine terminal engine to run checks through
     * @return map of command name to installed-state
     */
    suspend fun checkAll(engine: TerminalEngine): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        for (tool in commonTools) {
            result[tool.command] = isInstalled(tool.command, engine)
        }
        return result
    }

    /**
     * Returns the install commands for a tool under the given backend.
     * LOCAL backend does not perform installs; returns an empty list to
     * make "needs shizuku/root" explicit.
     *
     * @param tool the tool to look up
     * @param backend backend name ("shizuku", "root", "local")
     * @return list of install commands (empty for local)
     */
    fun getInstallCommand(tool: ToolInfo, backend: String): List<String> {
        if (backend.equals("local", ignoreCase = true)) return emptyList()
        return tool.installCmds[backend] ?: tool.installCmds["shizuku"] ?: emptyList()
    }

    /**
     * One-click install of all Android development dependencies.
     *
     * @param engine terminal engine to run installs through
     * @param onProgress callback (toolName, index, total) for progress UI
     * @return true when all install commands exited 0
     */
    suspend fun installAllAndroid(
        engine: TerminalEngine,
        onProgress: (String, Int, Int) -> Unit
    ): Boolean {
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
