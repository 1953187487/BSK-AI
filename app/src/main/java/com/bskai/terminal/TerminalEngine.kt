package com.bskai.terminal

import com.bskai.permission.DhizukuBridge
import com.bskai.permission.ShizukuBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Built-in terminal engine supporting four execution backends:
 * 1. LOCAL: commands under the app's own permissions (restricted)
 * 2. SHIZUKU: privileged execution via Shizuku binder IPC (no root needed)
 * 3. DHIZUKU: privileged execution via Dhizuku DeviceOwner sharing (no root needed)
 * 4. ROOT: execution via /system/xbin/su or /system/bin/su (root device required)
 *
 * Defaults to LOCAL. SHIZUKU/DHIZUKU/ROOT are enabled after user authorization.
 */
class TerminalEngine(
    private val shizuku: ShizukuBridge?,
    private val dhizuku: DhizukuBridge? = null
) {

    /** Execution backend selector. */
    enum class Backend { LOCAL, SHIZUKU, DHIZUKU, ROOT }

    private val _backend = MutableStateFlow(Backend.LOCAL)
    /** Currently selected backend. */
    val backend: StateFlow<Backend> = _backend.asStateFlow()

    /**
     * Sets the desired execution backend.
     *
     * @param b the backend to switch to
     */
    fun setBackend(b: Backend) { _backend.value = b }

    /**
     * Resolves the effective backend, downgrading to LOCAL when Shizuku is
     * requested but not granted.
     *
     * @return the backend that will actually be used
     */
    fun resolveBackend(): Backend {
        if (_backend.value == Backend.SHIZUKU && shizuku?.isGranted() != true) return Backend.LOCAL
        if (_backend.value == Backend.DHIZUKU && dhizuku?.isGranted() != true) return Backend.LOCAL
        return _backend.value
    }

    /**
     * Executes a command on the effective backend and returns the result.
     *
     * @param command shell command to run
     * @param workingDir optional working directory
     * @return [ExecutionResult] with stdout/stderr/exitCode/duration/backend
     */
    suspend fun execute(command: String, workingDir: String? = null): ExecutionResult =
        withContext(Dispatchers.IO) {
            val effective = resolveBackend()
            val startedAt = System.currentTimeMillis()
            val result = try {
                when (effective) {
                    Backend.LOCAL -> runLocal(command, workingDir)
                    Backend.SHIZUKU -> runShizuku(command, workingDir)
                    Backend.DHIZUKU -> runDhizuku(command, workingDir)
                    Backend.ROOT -> runRoot(command, workingDir)
                }
            } catch (e: Exception) {
                ExecutionResult(
                    stdout = "",
                    stderr = "执行失败：${e.javaClass.simpleName}: ${e.message}",
                    exitCode = -1,
                    durationMs = System.currentTimeMillis() - startedAt,
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

    private fun runDhizuku(command: String, workingDir: String?): ExecutionResult {
        if (dhizuku == null || !dhizuku.isGranted()) {
            return ExecutionResult("", "Dhizuku 未授权", -1, 0L, Backend.DHIZUKU)
        }
        return try {
            val process = dhizuku.newProcess(
                arrayOf("/system/bin/sh", "-c", command),
                workingDir
            )
            readResult(process, Backend.DHIZUKU)
        } catch (e: Exception) {
            ExecutionResult("", "Dhizuku 执行失败：${e.message}", -1, 0L, Backend.DHIZUKU)
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

    /**
     * Reads stdout/stderr from a [proc] and waits up to 30 seconds for it to finish.
     */
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
                exitCode = -1,
                durationMs = 0L,
                backend = backend
            )
        }
        outThread.join(500)
        errThread.join(500)
        return ExecutionResult(
            stdout = stdout.toString().trimEnd(),
            stderr = stderr.toString().trimEnd(),
            exitCode = proc.exitValue(),
            durationMs = 0L,
            backend = backend
        )
    }

    /** Locates an executable `su` binary among the common root locations. */
    private fun findSu(): File? {
        listOf("/system/xbin/su", "/system/bin/su", "/su/bin/su", "/magisk/.core/bin/su")
            .forEach { val f = File(it); if (f.exists() && f.canExecute()) return f }
        return null
    }

    /** Releases engine resources (nothing to release currently). */
    fun shutdown() { /* nothing to release */ }

    /**
     * The outcome of a terminal execution.
     *
     * @property stdout captured standard output
     * @property stderr captured standard error
     * @property exitCode process exit code (-1 on timeout / failure)
     * @property durationMs wall-clock duration in milliseconds
     * @property backend the backend that actually ran the command
     */
    data class ExecutionResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val durationMs: Long,
        val backend: Backend
    )
}
