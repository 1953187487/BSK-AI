package com.floatai.data.model

import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("role", role)
        .put("content", content)
        .put("timestamp", timestamp)

    companion object {
        fun fromJson(obj: JSONObject): ChatMessage = ChatMessage(
            role = obj.optString("role", "user"),
            content = obj.optString("content", ""),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

data class ChatHistory(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val time: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("time", time)
        .put("messages", JSONArray().apply {
            messages.forEach { put(it.toJson()) }
        })

    companion object {
        fun fromJson(obj: JSONObject): ChatHistory {
            val arr = obj.optJSONArray("messages") ?: JSONArray()
            val messages = buildList {
                for (i in 0 until arr.length()) {
                    add(ChatMessage.fromJson(arr.getJSONObject(i)))
                }
            }
            return ChatHistory(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                title = obj.optString("title", "新对话"),
                messages = messages,
                time = obj.optLong("time", System.currentTimeMillis())
            )
        }
    }
}

data class ApiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "auto"
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = false,
    val accentColor: String = "紫色",
    val floatEnabled: Boolean = false,
    val protocolAgreed: Boolean = false
)

data class UpdateInfo(
    val latestTag: String,
    val changelog: String,
    val isNewer: Boolean
)
