package com.floatai.atk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 调用 gradle assembleDebug/Release 构建 APK 脚手架项目。
 *
 * 设计：
 *  - 强制使用项目内 gradlew（如脚手架已生成）；若没有则尝试系统 PATH 中的 `gradle`。
 *  - 收集构建日志（stdout+stderr）回调给 UI；构建完成后扫描 build/outputs 找 APK。
 */
object AtkBuilder {

    sealed class Result {
        data class Success(val apk: File, val logs: String) : Result()
        data class Failure(val logs: String, val exitCode: Int) : Result()
    }

    private val executor: ShellExecutor = AndroidShellExecutor()

    suspend fun build(
        projectDir: File,
        task: String = "assembleDebug",
        onLine: (String) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        val gradlew = File(projectDir, "gradlew")
        val cmd = if (gradlew.exists()) "./gradlew $task --no-daemon" else "gradle $task --no-daemon"
        val logBuilder = StringBuilder()
        val done = CompletableDeferred<Int>()

        executor.exec(
            command = cmd,
            workdir = projectDir,
            onLine = { line ->
                synchronized(logBuilder) { logBuilder.appendLine(line) }
                onLine(line)
            },
            onComplete = { code -> done.complete(code) }
        )

        val exit = done.await()
        val apk = findApk(projectDir, task)
        val logs = synchronized(logBuilder) { logBuilder.toString() }
        if (apk != null) Result.Success(apk, logs)
        else Result.Failure(logs, exit)
    }

    private fun findApk(projectDir: File, task: String): File? {
        val variant = if (task.contains("Release", ignoreCase = true)) "release" else "debug"
        val out = File(projectDir, "app/build/outputs/apk/$variant")
        if (!out.exists()) return null
        return out.walkTopDown().filter { it.isFile && it.name.endsWith(".apk") }
            .maxByOrNull { it.lastModified() }
    }
}
