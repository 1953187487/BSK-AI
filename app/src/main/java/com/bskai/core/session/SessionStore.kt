package com.bskai.core.session

import android.content.Context
import com.bskai.agent.AgentMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SessionMeta(
    val id: String,
    val title: String,
    val updatedAt: Long
)

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<AgentMessage> = emptyList()
)

class SessionStore(context: Context) {

    private val dir: File = File(context.filesDir, "sessions")

    init {
        dir.mkdirs()
    }

    fun list(): List<SessionMeta> {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { f ->
            runCatching {
                val obj = JSONObject(f.readText())
                SessionMeta(
                    id = obj.optString("id", f.name.removeSuffix(".json")),
                    title = obj.optString("title", "未命名会话"),
                    updatedAt = obj.optLong("updatedAt")
                )
            }.getOrNull()
        }.sortedByDescending { it.updatedAt }
    }

    fun load(id: String): Session? {
        val f = File(dir, "$id.json")
        if (!f.exists()) return null
        return runCatching {
            val obj = JSONObject(f.readText())
            val arr = obj.optJSONArray("messages") ?: JSONArray()
            val msgs = (0 until arr.length()).map { i -> AgentMessage.fromJson(arr.getJSONObject(i)) }
            Session(
                id = obj.optString("id", id),
                title = obj.optString("title", "未命名会话"),
                createdAt = obj.optLong("createdAt"),
                updatedAt = obj.optLong("updatedAt"),
                messages = msgs
            )
        }.getOrNull()
    }

    fun save(session: Session) {
        val obj = JSONObject()
            .put("id", session.id)
            .put("title", session.title)
            .put("createdAt", session.createdAt)
            .put("updatedAt", session.updatedAt)
        val arr = JSONArray()
        session.messages.forEach { arr.put(it.toJson()) }
        obj.put("messages", arr)
        dir.mkdirs()
        File(dir, "${session.id}.json").writeText(obj.toString())
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    fun newId(): String = "${System.currentTimeMillis()}_${(1000..9999).random()}"
}
