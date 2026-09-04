package com.bskai.data

import android.content.Context
import org.json.JSONObject

data class Announcement(
    val version: String,
    val title: String,
    val content: String,
    val changelog: List<String>
)

fun loadAnnouncements(context: Context): List<Announcement> {
    return try {
        val json = context.assets.open("announcements.json")
            .bufferedReader().use { it.readText() }
        parseAnnouncements(json)
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun parseAnnouncements(json: String): List<Announcement> {
    return try {
        val root = JSONObject(json)
        val arr = root.getJSONArray("announcements")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val changelog = o.optJSONArray("changelog")?.let { a ->
                (0 until a.length()).map { a.getString(it) }
            } ?: emptyList()
            Announcement(
                version = o.getString("version"),
                title = o.getString("title"),
                content = o.getString("content"),
                changelog = changelog
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
