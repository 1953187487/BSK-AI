package com.bskai.update

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object GitHubApi {

    private const val OWNER = "1953187487"
    private const val REPO = "BSK-AI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun listReleases(): List<RemoteRelease> = suspendCancellableCoroutine { cont ->
        val request = Request.Builder()
            .url("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30")
            .addHeader("Accept", "application/vnd.github+json")
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (cont.isActive) {
                        if (resp.isSuccessful) {
                            cont.resume(parseReleases(body))
                        } else {
                            cont.resume(emptyList())
                        }
                    }
                }
            }
        })
        cont.invokeOnCancellation { call.cancel() }
    }

    /**
     * 下载 APK 到目标文件，以 Flow<DownloadStatus> 形式实时回报进度与最终结果。
     */
    fun downloadApk(url: String, target: File): Flow<DownloadStatus> = callbackFlow {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        var cancelled = false
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cancelled) trySend(DownloadStatus.Failed(e.message ?: "网络错误"))
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        trySend(DownloadStatus.Failed("HTTP ${resp.code}"))
                        close()
                        return
                    }
                    val body = resp.body
                    val total = body?.contentLength() ?: -1L
                    val source = body?.byteStream()
                    if (source == null) {
                        trySend(DownloadStatus.Failed("空响应"))
                        close()
                        return
                    }
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out ->
                        val buf = ByteArray(8 * 1024)
                        var read: Int
                        var sum = 0L
                        try {
                            while (source.read(buf).also { read = it } != -1) {
                                if (cancelled) {
                                    out.close()
                                    target.delete()
                                    return
                                }
                                out.write(buf, 0, read)
                                sum += read
                                trySend(DownloadStatus.Downloading(sum, total))
                            }
                            out.flush()
                            trySend(DownloadStatus.Done(target.absolutePath, sum))
                        } catch (e: Exception) {
                            target.delete()
                            trySend(DownloadStatus.Failed(e.message ?: "写入失败"))
                        } finally {
                            try { source.close() } catch (_: Exception) {}
                            close()
                        }
                    }
                }
            }
        })
        awaitClose {
            cancelled = true
            runCatching { call.cancel() }
        }
    }
}
