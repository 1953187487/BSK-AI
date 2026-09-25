package com.lingxi.ai.terminal

/**
 * Android 开发依赖管理（已并入 [DevTools] 统一管理）。
 *
 * 为保持 IDE 界面既有调用点兼容，本对象作为 [DevTools] 的轻量委托，
 * 依赖数据源唯一为 [DevTools.commonTools]。
 */
object AndroidDependencyManager {

    // 直接复用 DevTools 的数据模型与数据源
    val basicDependencies: List<DevTools.ToolInfo> get() = DevTools.basicTools
    val androidDependencies: List<DevTools.ToolInfo> get() = DevTools.androidTools
    val allDependencies: List<DevTools.ToolInfo> get() = DevTools.commonTools

    fun getInstallCommands(dep: DevTools.ToolInfo, backend: String): List<String> =
        DevTools.getInstallCommand(dep, backend)

    fun getInstallAllCommands(backend: String): List<String> =
        DevTools.androidTools.flatMap { DevTools.getInstallCommand(it, backend) }

    suspend fun isInstalled(command: String, engine: TerminalEngine): Boolean =
        DevTools.isInstalled(command, engine)

    suspend fun checkAll(engine: TerminalEngine): Map<String, Boolean> =
        DevTools.checkAll(engine)

    suspend fun installAllAndroid(
        engine: TerminalEngine,
        onProgress: (String, Int, Int) -> Unit
    ): Boolean = DevTools.installAllAndroid(engine, onProgress)
}
