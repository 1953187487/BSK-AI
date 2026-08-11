package com.floatai.atk

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 本地命令执行抽象：让 ATK 可在普通 Android 上跑（Runtime.exec）
 * 也方便后续替换为 Termux / 远程 SSH 实现。
 */
interface ShellExecutor {
    /**
     * 异步执行一条 shell 命令。
     * @return Job 句柄，可用于停止；输出会回调到 onLine。
     */
    fun exec(
        command: String,
        workdir: File? = null,
        onLine: (String) -> Unit,
        onComplete: (Int) -> Unit
    ): Job

    fun cancel(job: Job)

    data class Job(val id: Long, val process: Process)
}

/**
 * 基于 Runtime.exec 的默认实现。
 * 每条输出按行回调（stdout 灰、stderr 红），完成后回调退出码。
 */
class AndroidShellExecutor : ShellExecutor {

    private val jobs = ConcurrentLinkedQueue<Process>()
    private var seq = 0L

    override fun exec(
        command: String,
        workdir: File?,
        onLine: (String) -> Unit,
        onComplete: (Int) -> Unit
    ): ShellExecutor.Job {
        val id = ++seq
        val builder = ProcessBuilder("/system/bin/sh", "-c", command)
        if (workdir != null && workdir.exists()) builder.directory(workdir)
        builder.redirectErrorStream(false)
        val process = builder.start()
        jobs.add(process)

        Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.forEach { onLine("[stdout] $it") }
                }
            } catch (_: Exception) {
            }
            try {
                BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
                    lines.forEach { onLine("[stderr] $it") }
                }
            } catch (_: Exception) {
            }
            val code = try { process.waitFor() } catch (_: Exception) { -1 }
            jobs.remove(process)
            onComplete(code)
        }.start()
        return ShellExecutor.Job(id, process)
    }

    override fun cancel(job: ShellExecutor.Job) {
        try { job.process.destroy() } catch (_: Exception) {}
    }
}
