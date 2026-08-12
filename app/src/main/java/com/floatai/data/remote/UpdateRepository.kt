package com.floatai.data.remote

import com.floatai.data.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseNote(
    val tag: String,
    val publishedAt: Long,
    val summary: String,
    val downloadUrl: String?
)

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

    /**
     * 语义版本比较：判断 latestTag 是否比 currentTag 新。
     *
     *  - 去除 "v" 前缀
     *  - 按 "." 分割为数字段逐段比较
     *  - 缺失的段视为 0
     *  - 非数字段（pre-release 等）按字典序
     *
     * 当 latestTag 与 currentTag 完全相等时返回 false（用户已是最新）。
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
