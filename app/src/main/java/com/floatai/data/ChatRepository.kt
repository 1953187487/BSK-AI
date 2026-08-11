package com.floatai.data

import android.content.Context
import android.content.SharedPreferences
import com.floatai.data.model.ChatHistory
import com.floatai.data.model.ChatMessage
import org.json.JSONArray
import java.util.UUID

/**
 * 聊天历史仓库：基于 SharedPreferences 的 JSON 持久化。
 */
class ChatRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("float_ai_chats", Context.MODE_PRIVATE)

    fun loadHistories(): List<ChatHistory> {
        val raw = prefs.getString("histories", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(ChatHistory.fromJson(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveHistory(history: ChatHistory) {
        val list = loadHistories().toMutableList()
        val idx = list.indexOfFirst { it.id == history.id }
        if (idx >= 0) list[idx] = history else list.add(0, history)
        persist(list)
    }

    fun deleteHistory(id: String) {
        val list = loadHistories().filterNot { it.id == id }
        persist(list)
    }

    fun clearAll() {
        prefs.edit().remove("histories").apply()
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)

    private fun persist(list: List<ChatHistory>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("histories", arr.toString()).apply()
    }
}

/** 默认起始对话 */
fun defaultWelcome(): ChatMessage = ChatMessage(
    role = "assistant",
    content = "你好，我是 FloatAI 助手。请先到「API 配置」填写服务商地址与密钥，然后就可以开始对话了。"
)
