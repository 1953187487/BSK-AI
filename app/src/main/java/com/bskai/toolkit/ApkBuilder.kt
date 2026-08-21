package com.bskai.toolkit

import android.content.Context
import java.io.File

/**
 * APK 构建编排：项目脚手架 + android.jar 准备 + Termux 构建脚本执行。
 */
class ApkBuilder(private val context: Context) {

    val manager = ToolchainManager(context)

    fun projectDir(projectName: String): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "projects/$projectName")

    /**
     * 准备构建产物：确保 android.jar 同时存在于公共 Download 目录（Termux 可读）。
     */
    suspend fun prepareAndroidJar(onProgress: (Float) -> Unit = {}): Boolean {
        val ok = manager.downloadAndroidJar(onProgress)
        if (ok) {
            val src = manager.androidJarFile()
            if (src.exists()) {
                val publicJar = File(MediaTransfer.publicProjectRoot().parentFile, "android-34/android.jar")
                publicJar.parentFile?.mkdirs()
                src.copyTo(publicJar, overwrite = true)
            }
        }
        return ok
    }

    /**
     * 复制项目到公共目录并启动 Termux 构建。
     * @return null 表示成功发起；否则返回错误提示
     */
    fun build(projectName: String): String? {
        val project = projectDir(projectName)
        if (!project.exists()) return "项目不存在: $projectName"
        val publicDir = MediaTransfer.copyProjectToPublic(context, projectName)
            ?: return "项目复制到公共目录失败"
        val script = File(publicDir, "build.sh")
        if (!script.exists()) return "缺少 build.sh，请重新创建项目"
        // 确保脚本可执行并保留 LF 行尾
        script.writeText(script.readText().replace("\r\n", "\n"))
        val command = "bash '$script' '$projectName' && ls -la '${publicDir.absolutePath}/dist'"
        val launched = TermuxBridge.runCommand(context, command, workDir = publicDir.absolutePath)
        return if (launched) null else "无法启动 Termux，请先安装 Termux 并允许外部应用执行命令"
    }

    fun hasTermux(): Boolean = TermuxBridge.isAvailable(context)
}
