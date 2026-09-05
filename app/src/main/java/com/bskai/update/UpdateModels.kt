package com.bskai.update

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class RemoteRelease(
    val tagName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val sizeBytes: Long,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val body: String
) {
    fun publishedAtLabel(): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(publishedAt.substringBefore('.')) ?: return publishedAt
            val out = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            out.format(date)
        } catch (_: Exception) {
            publishedAt
        }
    }
}

data class UpdateCheckResult(
    val releases: List<RemoteRelease>,
    val latestRelease: RemoteRelease?,
    val hasUpdate: Boolean
)

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(val bytesRead: Long, val total: Long) : DownloadStatus {
        val percent: Int get() = if (total > 0) ((bytesRead.toDouble() / total) * 100).toInt() else 0
    }
    data class Done(val localPath: String, val size: Long) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

fun parseReleases(json: String): List<RemoteRelease> {
    val list = mutableListOf<RemoteRelease>()
    try {
        val arr = org.json.JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val assets = o.optJSONArray("assets")
            val apkAsset = assets?.let { arr ->
                (0 until arr.length()).firstNotNullOfOrNull { j ->
                    val a = arr.getJSONObject(j)
                    if (a.optString("name").endsWith(".apk", true)) a else null
                }
            }
            if (apkAsset == null) continue
            val tagName = o.optString("tag_name")
            val versionName = tagName.removePrefix("v")
            val parts = versionName.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val pre = parts.getOrNull(3) ?: ""
            val versionCode = major * 1000 + minor * 100 + patch * 10 +
                when {
                    pre.startsWith("beta") -> -2
                    pre.startsWith("rc") -> -1
                    else -> 0
                }
            list.add(
                RemoteRelease(
                    tagName = tagName,
                    name = o.optString("name"),
                    versionName = versionName,
                    versionCode = versionCode,
                    apkUrl = apkAsset.optString("browser_download_url"),
                    sizeBytes = apkAsset.optLong("size"),
                    publishedAt = o.optString("published_at"),
                    isPrerelease = o.optBoolean("prerelease"),
                    body = o.optString("body")
                )
            )
        }
    } catch (_: Exception) {
    }
    return list.sortedByDescending { it.versionCode }
}
