package com.bskai.data

import android.content.Context
import com.bskai.R
import org.json.JSONArray

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val region: String
)

fun loadLanguages(context: Context): List<Language> {
    return try {
        val json = context.resources.openRawResource(R.raw.languages)
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            Language(
                code = o.optString("code"),
                name = o.optString("name"),
                nativeName = o.optString("nativeName"),
                region = o.optString("region")
            )
        }
    } catch (_: Exception) {
        listOf(Language("zh", "Chinese", "中文", "中国"))
    }
}
