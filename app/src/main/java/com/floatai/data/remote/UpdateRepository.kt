package com.floatai.data.remote

import com.floatai.data.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseNote(
    val tag: String,
    val publishedAt: Long,
    val summary: String,
    val downloadUrl: String?
)

/** APK asset 信息：直连下载用。 */
data class ApkAsset(
    val name: String,
    val url: String,
    val sizeBytes: Long
)

/** APK 下载进度。 */
sealed class ApkDownloadProgress {
    data class Started(val url: String, val total: Long) : ApkDownloadProgress()
    data class Progress(val downloaded: Long, val total: Long, val speed: Long) : ApkDownloadProgress()
    data class Verifying(val sha256: String) : ApkDownloadProgress()
    data class Completed(val file: File) : ApkDownloadProgress()
    data class Error(val message: String) : ApkDownloadProgress()
}

object UpdateRepository {
    suspend fun checkLatest(currentTag: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/1953187487/FloatAI/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) {
                return@withContext UpdateInfo("", "获取失败 (HTTP ${conn.responseCode})", false)
            }
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    sb.append(line)
                    line = br.readLine()
                }
            }
            conn.disconnect()
            val json = JSONObject(sb.toString())
            val tag = json.optString("tag_name", currentTag)
            val body = json.optString("body", "")
            val isNewer = isTagNewer(currentTag, tag)
            UpdateInfo(tag, body, isNewer)
        } catch (e: Exception) {
            UpdateInfo("", "检查失败：${e.message}", false)
        }
    }

    /** 拉取最近 N 个 release（默认 5）。 */
    suspend fun loadRecent(count: Int = 5): List<ReleaseNote> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/1953187487/FloatAI/releases?per_page=$count")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return@withContext emptyList()
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    sb.append(line)
                    line = br.readLine()
                }
            }
            conn.disconnect()
            val arr = JSONArray(sb.toString())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val body = o.optString("body", "")
                    add(
                        ReleaseNote(
                            tag = o.optString("tag_name", ""),
                            publishedAt = parseIso(o.optString("published_at", "")),
                            summary = body.take(120),
                            downloadUrl = o.optString("html_url", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 获取指定 tag 的最新 APK asset 直链。 */
    suspend fun findApkAsset(tag: String): ApkAsset? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/1953187487/FloatAI/releases/tags/$tag")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return@withContext null
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
                var line = br.readLine()
                while (line != null) { sb.append(line); line = br.readLine() }
            }
            conn.disconnect()
            val json = JSONObject(sb.toString())
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                if (name.endsWith(".apk")) {
                    return@withContext ApkAsset(
                        name = name,
                        url = a.optString("browser_download_url", ""),
                        sizeBytes = a.optLong("size", -1)
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 直接下载 APK（不通过浏览器）到 cacheDir/update.apk，返回进度流。
     * 完整度 = 流式 Progress；完成时返回 File 供 InstallPackage 调用。
     */
    fun downloadApk(target: File, url: String): Flow<ApkDownloadProgress> = flow {
        try {
            emit(ApkDownloadProgress.Started(url, -1L))
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", "FloatAI-Updater/1.0.4")
            conn.connect()
            if (conn.responseCode !in 200..299) {
                emit(ApkDownloadProgress.Error("HTTP ${conn.responseCode}"))
                return@flow
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
            emit(ApkDownloadProgress.Started(url, total))
            var downloaded = 0L
            val start = System.currentTimeMillis()
            var lastEmit = 0L
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(target).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        out.write(buf, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 250) {
                            val speed = downloaded * 1000L / (now - start).coerceAtLeast(1L)
                            emit(ApkDownloadProgress.Progress(downloaded, total, speed))
                            lastEmit = now
                        }
                    }
                }
            }
            conn.disconnect()
            val sha256 = try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                target.inputStream().use {
                    val buf = ByteArray(64 * 1024)
                    var r: Int
                    while (it.read(buf).also { r = it } > 0) md.update(buf, 0, r)
                }
                md.digest().joinToString("") { "%02x".format(it) }
            } catch (_: Exception) { "" }
            emit(ApkDownloadProgress.Verifying(sha256))
            emit(ApkDownloadProgress.Completed(target))
        } catch (e: Exception) {
            emit(ApkDownloadProgress.Error(e.message ?: e.javaClass.simpleName))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 语义版本比较：判断 latestTag 是否比 currentTag 新。
     */
    fun isTagNewer(currentTag: String, latestTag: String): Boolean {
        val cur = normalize(currentTag)
        val latest = normalize(latestTag)
        if (cur == latest) return false
        val curParts = cur.split(".")
        val latestParts = latest.split(".")
        val len = maxOf(curParts.size, latestParts.size)
        for (i in 0 until len) {
            val a = curParts.getOrNull(i)?.toIntOrNull() ?: 0
            val b = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (b > a) return true
            if (b < a) return false
        }
        return false
    }

    private fun normalize(tag: String): String =
        tag.trim().lowercase().removePrefix("v")

    private fun parseIso(s: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .parse(s)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}
