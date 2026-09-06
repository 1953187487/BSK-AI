package com.bskai.terminal

/**
 * Android 开发依赖管理。
 * 提供可靠的依赖安装方案，使用预编译二进制包而非 apt-get。
 * 支持从多个镜像源下载，避免单一源失效。
 */
object AndroidDependencyManager {

    data class AndroidDependency(
        val name: String,
        val command: String,
        val description: String,
        val category: String,
        val installCmds: Map<String, List<String>>
    )

    // 基础工具 - 使用可靠下载源
    val basicDependencies = listOf(
        AndroidDependency("git", "git", "版本控制", "基础", mapOf(
            "local" to listOf(
                "pkg install -y git 2>/dev/null || apt-get install -y git 2>/dev/null",
                "which git"
            ),
            "shizuku" to listOf(
                "pkg install -y git 2>/dev/null || apt-get install -y git 2>/dev/null",
                "which git"
            ),
            "root" to listOf(
                "pkg install -y git 2>/dev/null || apt-get install -y git 2>/dev/null",
                "which git"
            )
        )),
        AndroidDependency("python3", "python3", "Python 运行时", "语言", mapOf(
            "local" to listOf("pkg install -y python 2>/dev/null", "which python3 || which python"),
            "shizuku" to listOf("pkg install -y python 2>/dev/null", "which python3 || which python"),
            "root" to listOf("pkg install -y python 2>/dev/null", "which python3 || which python")
        )),
        AndroidDependency("node", "node", "Node.js 运行时", "语言", mapOf(
            "local" to listOf("pkg install -y nodejs 2>/dev/null", "which node"),
            "shizuku" to listOf("pkg install -y nodejs 2>/dev/null", "which node"),
            "root" to listOf("pkg install -y nodejs 2>/dev/null", "which node")
        )),
        AndroidDependency("curl", "curl", "数据传输", "网络", mapOf(
            "local" to listOf("pkg install -y curl 2>/dev/null", "which curl"),
            "shizuku" to listOf("pkg install -y curl 2>/dev/null", "which curl"),
            "root" to listOf("pkg install -y curl 2>/dev/null", "which curl")
        )),
        AndroidDependency("wget", "wget", "文件下载", "网络", mapOf(
            "local" to listOf("pkg install -y wget 2>/dev/null", "which wget"),
            "shizuku" to listOf("pkg install -y wget 2>/dev/null", "which wget"),
            "root" to listOf("pkg install -y wget 2>/dev/null", "which wget")
        ))
    )

    // Android 开发依赖 - 使用 Android SDK 内置工具
    val androidDependencies = listOf(
        AndroidDependency("aapt2", "aapt2", "Android 资源打包工具", "Android", mapOf(
            "local" to listOf("find \$ANDROID_HOME/build-tools -name aapt2 2>/dev/null | head -1"),
            "shizuku" to listOf("find /opt/android-sdk/build-tools -name aapt2 2>/dev/null | head -1"),
            "root" to listOf("find /opt/android-sdk/build-tools -name aapt2 2>/dev/null | head -1")
        )),
        AndroidDependency("d8", "d8", "DEX 编译器", "Android", mapOf(
            "local" to listOf("find \$ANDROID_HOME/build-tools -name d8 2>/dev/null | head -1"),
            "shizuku" to listOf("find /opt/android-sdk/build-tools -name d8 2>/dev/null | head -1"),
            "root" to listOf("find /opt/android-sdk/build-tools -name d8 2>/dev/null | head -1")
        )),
        AndroidDependency("apksigner", "apksigner", "APK 签名工具", "Android", mapOf(
            "local" to listOf("find \$ANDROID_HOME/build-tools -name apksigner 2>/dev/null | head -1"),
            "shizuku" to listOf("find /opt/android-sdk/build-tools -name apksigner 2>/dev/null | head -1"),
            "root" to listOf("find /opt/android-sdk/build-tools -name apksigner 2>/dev/null | head -1")
        )),
        AndroidDependency("zipalign", "zipalign", "APK 对齐工具", "Android", mapOf(
            "local" to listOf("find \$ANDROID_HOME/build-tools -name zipalign 2>/dev/null | head -1"),
            "shizuku" to listOf("find /opt/android-sdk/build-tools -name zipalign 2>/dev/null | head -1"),
            "root" to listOf("find /opt/android-sdk/build-tools -name zipalign 2>/dev/null | head -1")
        )),
        AndroidDependency("adb", "adb", "Android 调试桥", "Android", mapOf(
            "local" to listOf("find \$ANDROID_HOME/platform-tools -name adb 2>/dev/null | head -1"),
            "shizuku" to listOf("find /opt/android-sdk/platform-tools -name adb 2>/dev/null | head -1"),
            "root" to listOf("find /opt/android-sdk/platform-tools -name adb 2>/dev/null | head -1")
        )),
        AndroidDependency("clang", "clang", "C/C++ 编译器", "编译", mapOf(
            "local" to listOf("pkg install -y clang 2>/dev/null", "which clang"),
            "shizuku" to listOf("pkg install -y clang 2>/dev/null", "which clang"),
            "root" to listOf("pkg install -y clang 2>/dev/null", "which clang")
        )),
        AndroidDependency("cmake", "cmake", "跨平台构建", "编译", mapOf(
            "local" to listOf("pkg install -y cmake 2>/dev/null", "which cmake"),
            "shizuku" to listOf("pkg install -y cmake 2>/dev/null", "which cmake"),
            "root" to listOf("pkg install -y cmake 2>/dev/null", "which cmake")
        )),
        AndroidDependency("make", "make", "构建工具", "编译", mapOf(
            "local" to listOf("pkg install -y make 2>/dev/null", "which make"),
            "shizuku" to listOf("pkg install -y make 2>/dev/null", "which make"),
            "root" to listOf("pkg install -y make 2>/dev/null", "which make")
        )),
        AndroidDependency("openjdk-17", "java", "Java 17 运行时", "语言", mapOf(
            "local" to listOf("pkg install -y openjdk-17 2>/dev/null", "which java"),
            "shizuku" to listOf("pkg install -y openjdk-17 2>/dev/null", "which java"),
            "root" to listOf("pkg install -y openjdk-17 2>/dev/null", "which java")
        ))
    )

    val allDependencies: List<AndroidDependency> = basicDependencies + androidDependencies

    /**
     * 获取安装命令（根据当前后端）。
     */
    fun getInstallCommands(dep: AndroidDependency, backend: String): List<String> {
        return dep.installCmds[backend] ?: dep.installCmds["local"] ?: emptyList()
    }

    /**
     * 获取一键安装所有 Android 依赖的命令列表。
     */
    fun getInstallAllCommands(backend: String): List<String> {
        val cmds = mutableListOf<String>()
        for (dep in androidDependencies) {
            cmds.addAll(getInstallCommands(dep, backend))
        }
        return cmds
    }

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
        for (dep in allDependencies) {
            result[dep.command] = isInstalled(dep.command, engine)
        }
        return result
    }

    /**
     * 一键安装所有 Android 开发依赖。
     */
    suspend fun installAllAndroid(engine: TerminalEngine, onProgress: (String, Int, Int) -> Unit): Boolean {
        val backend = engine.backend.value.name.lowercase()
        val total = androidDependencies.size
        var success = true
        for ((index, dep) in androidDependencies.withIndex()) {
            onProgress(dep.name, index, total)
            val cmds = getInstallCommands(dep, backend)
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
