package com.bskai.terminal

import com.bskai.permission.ShizukuBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 内置终端引擎。支持三种执行后端：
 * 1. LOCAL: 当前应用权限下可执行的命令（受限）
 * 2. SHIZUKU: 通过 Shizuku binder IPC 提权（无需 root）
 * 3. ROOT: 通过 /system/xbin/su 或 /system/bin/su 执行（需 root 设备）
 *
 * 默认 LOCAL。Shizuku/ROOT 在用户授权后启用。
 */
class TerminalEngine(private val shizuku: ShizukuBridge?) {

    enum class Backend { LOCAL, SHIZUKU, ROOT }

    private val _backend = MutableStateFlow(Backend.LOCAL)
    val backend: StateFlow<Backend> = _backend.asStateFlow()

    fun setBackend(b: Backend) { _backend.value = b }

    fun resolveBackend(): Backend {
        if (_backend.value == Backend.SHIZUKU && shizuku?.isGranted() != true) return Backend.LOCAL
        return _backend.value
    }

    suspend fun execute(command: String, workingDir: String? = null): ExecutionResult =
        withContext(Dispatchers.IO) {
            val effective = resolveBackend()
            val startedAt = System.currentTimeMillis()
            val result = try {
                when (effective) {
                    Backend.LOCAL -> runLocal(command, workingDir)
                    Backend.SHIZUKU -> runShizuku(command, workingDir)
                    Backend.ROOT -> runRoot(command, workingDir)
                }
            } catch (e: Exception) {
                ExecutionResult(
                    stdout = "", stderr = "执行失败：${e.javaClass.simpleName}: ${e.message}",
                    exitCode = -1, durationMs = System.currentTimeMillis() - startedAt,
                    backend = effective
                )
            }
            result.copy(durationMs = System.currentTimeMillis() - startedAt)
        }

    private fun runLocal(command: String, workingDir: String?): ExecutionResult {
        val pb = ProcessBuilder("/system/bin/sh", "-c", command)
        if (workingDir != null) pb.directory(File(workingDir))
        pb.redirectErrorStream(false)
        val proc = pb.start()
        return readResult(proc, Backend.LOCAL)
    }

    private fun runShizuku(command: String, workingDir: String?): ExecutionResult {
        if (shizuku == null || !shizuku.isGranted()) {
            return ExecutionResult("", "Shizuku 未授权", -1, 0L, Backend.SHIZUKU)
        }
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                String::class.java,
                String::class.java
            )
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val process = method.invoke(
                null,
                arrayOf("/system/bin/sh", "-c", command),
                workingDir,
                null
            ) as Process
            readResult(process, Backend.SHIZUKU)
        } catch (e: Exception) {
            ExecutionResult("", "Shizuku 执行失败：${e.message}", -1, 0L, Backend.SHIZUKU)
        }
    }

    private fun runRoot(command: String, workingDir: String?): ExecutionResult {
        val su = findSu() ?: return ExecutionResult(
            "", "未找到 su，未 root 或未授权", -1, 0L, Backend.ROOT
        )
        return try {
            val proc = ProcessBuilder(su.absolutePath, "-c", command)
                .redirectErrorStream(false)
                .also { if (workingDir != null) it.directory(File(workingDir)) }
                .start()
            readResult(proc, Backend.ROOT)
        } catch (e: Exception) {
            ExecutionResult("", "Root 执行失败：${e.message}", -1, 0L, Backend.ROOT)
        }
    }

    private fun readResult(proc: Process, backend: Backend): ExecutionResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outReader = BufferedReader(InputStreamReader(proc.inputStream))
        val errReader = BufferedReader(InputStreamReader(proc.errorStream))
        val outThread = Thread { outReader.forEachLine { stdout.appendLine(it) } }
        val errThread = Thread { errReader.forEachLine { stderr.appendLine(it) } }
        outThread.isDaemon = true
        errThread.isDaemon = true
        outThread.start()
        errThread.start()
        val finished = proc.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
            return ExecutionResult(
                stdout = stdout.toString(),
                stderr = stderr.toString() + "\n[超时，已强制终止]",
                exitCode = -1, durationMs = 0L, backend = backend
            )
        }
        outThread.join(500)
        errThread.join(500)
        return ExecutionResult(
            stdout = stdout.toString().trimEnd(),
            stderr = stderr.toString().trimEnd(),
            exitCode = proc.exitValue(),
            durationMs = 0L, backend = backend
        )
    }

    private fun findSu(): File? {
        listOf("/system/xbin/su", "/system/bin/su", "/su/bin/su", "/magisk/.core/bin/su")
            .forEach { val f = File(it); if (f.exists() && f.canExecute()) return f }
        return null
    }

    fun shutdown() { /* nothing to release */ }

    data class ExecutionResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val durationMs: Long,
        val backend: Backend
    )
}
