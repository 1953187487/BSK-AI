package com.bskai.core.admin

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false
)

object AnnouncementStore {

    private const val KEY = "announcements"

    fun load(context: Context): List<Announcement> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Announcement(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        content = o.optString("content"),
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                        pinned = o.optBoolean("pinned")
                    )
                )
            }
        }.sortedByDescending { it.pinned }.sortedByDescending { it.updatedAt }
    }

    fun save(context: Context, list: List<Announcement>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("content", it.content)
                    .put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt)
                    .put("pinned", it.pinned)
            )
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    fun publish(context: Context, title: String, content: String, pinned: Boolean = false) {
        val now = System.currentTimeMillis()
        val next = load(context) + Announcement(
            id = "ann_" + now,
            title = title,
            content = content,
            createdAt = now,
            updatedAt = now,
            pinned = pinned
        )
        save(context, next)
    }

    fun edit(context: Context, id: String, title: String, content: String, pinned: Boolean) {
        val next = load(context).map {
            if (it.id == id) it.copy(
                title = title,
                content = content,
                pinned = pinned,
                updatedAt = System.currentTimeMillis()
            ) else it
        }
        save(context, next)
    }

    fun delete(context: Context, id: String) {
        save(context, load(context).filterNot { it.id == id })
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("bsk_ai_admin", Context.MODE_PRIVATE)
}
