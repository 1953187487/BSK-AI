package com.floatai.capture

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 抓包会话与记录持久化（v1.0.3）：
 *  - 抓包数据写入 filesDir/captures/{sessionId}.jsonl
 *  - 每行一条 JSON 记录
 *  - 简化版：当前仅记录「用户主动添加的流量条目」（不解析 IP 包）
 *  - v1.0.4 计划接入 VpnService IP 包解析（需要 native 库或自实现 TCP 重组）
 */
data class CaptureRecord(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val sourceApp: String,
    val method: String,
    val url: String,
    val status: Int,
    val requestBody: String,
    val responseBody: String,
    val note: String = ""
) {
    fun toJsonLine(): String = JSONObject()
        .put("id", id)
        .put("sessionId", sessionId)
        .put("timestamp", timestamp)
        .put("sourceApp", sourceApp)
        .put("method", method)
        .put("url", url)
        .put("status", status)
        .put("requestBody", requestBody)
        .put("responseBody", responseBody)
        .put("note", note)
        .toString()

    companion object {
        fun fromJsonLine(line: String): CaptureRecord? = runCatching {
            val o = JSONObject(line)
            CaptureRecord(
                id = o.optString("id"),
                sessionId = o.optString("sessionId"),
                timestamp = o.optLong("timestamp"),
                sourceApp = o.optString("sourceApp"),
                method = o.optString("method"),
                url = o.optString("url"),
                status = o.optInt("status"),
                requestBody = o.optString("requestBody"),
                responseBody = o.optString("responseBody"),
                note = o.optString("note")
            )
        }.getOrNull()
    }
}

class CaptureRepository(context: Context) {

    private val dir: File = File(context.filesDir, "captures").apply { mkdirs() }

    fun listSessions(): List<File> = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun sessionFile(sessionId: String): File = File(dir, "${sessionId}.jsonl")

    fun appendRecord(record: CaptureRecord) {
        sessionFile(record.sessionId).appendText(record.toJsonLine() + "\n")
    }

    fun loadRecords(sessionId: String): List<CaptureRecord> {
        val f = sessionFile(sessionId)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull { line ->
            if (line.isBlank()) null else CaptureRecord.fromJsonLine(line)
        }
    }

    fun deleteSession(sessionId: String) {
        sessionFile(sessionId).delete()
    }

    fun newSessionId(): String = UUID.randomUUID().toString().take(8)
}
