package com.floatai.data.remote

import com.floatai.data.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
            UpdateInfo(tag, body, tag != currentTag)
        } catch (e: Exception) {
            UpdateInfo("", "检查失败：${e.message}", false)
        }
    }
}
