package com.bskai.models

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/**
 * 本地模型下载引擎：支持进度回调、断点续传与取消。
 */
class ModelDownloader(
    private val context: Context,
    private val store: ModelStore,
    private val onProgress: (String, Float, Long, Long) -> Unit,
    private val onFinished: (String, Boolean, String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    @Volatile
    var cancelled = false
        private set

    suspend fun download(model: LocalModel) {
        cancelled = false
        val dir = store.modelsDir(context).apply { mkdirs() }
        val target = File(dir, model.fileName)
        val temp = File(dir, model.fileName + ".part")
        val existing = if (temp.exists()) temp.length() else if (target.exists()) target.length() else 0L

        store.updateOne(context, model.catalogId) {
            it.withStatus(ModelStatus.DOWNLOADING, progress = if (target.exists()) 1f else 0f, localPath = target.absolutePath)
        }
        onProgress(model.catalogId, if (target.exists()) 1f else 0f, existing, -1)

        try {
            if (!target.exists()) {
                val request = Request.Builder()
                    .url(model.url)
                    .header("Range", "bytes=$existing-")
                    .header("User-Agent", "BSK-AI/1.0.6")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful && resp.code != 206) {
                        throw Exception("HTTP ${resp.code}")
                    }
                    val total = resp.header("Content-Range")
                        ?.substringAfter("/")
                        ?.toLongOrNull()
                        ?: (existing + (resp.body?.contentLength() ?: 0L))
                    val body = resp.body ?: throw Exception("空响应体")
                    val len = body.contentLength()
                    val raf = RandomAccessFile(temp, "rw")
                    raf.seek(existing)
                    body.byteStream().use { input ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalRead = existing
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            val coroutineContext = currentCoroutineContext()
                            if (cancelled || !coroutineContext.isActive) throw CancellationException("用户取消")
                            raf.write(buf, 0, read)
                            totalRead += read
                            if (total > 0) {
                                onProgress(model.catalogId, (totalRead.toFloat() / total).coerceIn(0f, 1f), totalRead, total)
                            }
                        }
                    }
                    raf.close()
                }
                if (!temp.renameTo(target)) {
                    // 重命名失败则拷贝
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
            }
            val size = target.length()
            store.updateOne(context, model.catalogId) {
                it.withStatus(ModelStatus.READY, progress = 1f, localPath = target.absolutePath, downloadedBytes = size)
            }
            onFinished(model.catalogId, true, target.absolutePath)
        } catch (e: CancellationException) {
            store.updateOne(context, model.catalogId) {
                it.withStatus(ModelStatus.NOT_DOWNLOADED, progress = 0f)
            }
            onFinished(model.catalogId, false, "已取消")
        } catch (e: Exception) {
            store.updateOne(context, model.catalogId) {
                it.withStatus(ModelStatus.FAILED)
            }
            onFinished(model.catalogId, false, e.message ?: "下载失败")
        }
    }

    fun cancel() {
        cancelled = true
    }
}
