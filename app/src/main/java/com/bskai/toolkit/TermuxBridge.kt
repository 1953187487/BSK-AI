package com.bskai.toolkit

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * 通过 Termux RunCommandService 在 Termux 环境内执行命令。
 * 需要在 Termux 中允许外部应用（BSK AI）执行命令：
 *   Termux -> 长按图标/终端输入: termux-setup-storage
 *   并在 Termux 应用设置中允许 "Run command from app"。
 */
object TermuxBridge {

    const val TERMUX_PACKAGE = "com.termux"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    fun isAvailable(context: Context): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        }.getOrDefault(false)

    /**
     * 在 Termux 中后台执行一条命令。
     * @return 是否成功发起
     */
    fun runCommand(
        context: Context,
        command: String,
        workDir: String? = null,
        onResult: ((String) -> Unit)? = null
    ): Boolean {
        if (!isAvailable(context)) return false
        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setPackage(TERMUX_PACKAGE)
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir ?: "/data/data/com.termux/files/home")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.onFailure { onResult?.invoke("发起失败: ${it.message}") }.getOrDefault(false)
    }

    /**
     * 生成可直接在 Termux 中执行的构建脚本并写入项目目录。
     */
    fun writeBuildScript(script: File, content: String): Boolean =
        runCatching {
            script.parentFile?.mkdirs()
            script.writeText(content)
            true
        }.getOrDefault(false)

    /**
     * 从 BSK AI 外部存储目录构造 Termux 可访问路径。
     */
    fun termuxAccessiblePath(context: Context, file: File): String =
        file.absolutePath.replaceFirst(
            context.getExternalFilesDir(null)?.absolutePath ?: "/data/local/tmp",
            "/data/data/$TERMUX_PACKAGE/files/home"
        )
}
