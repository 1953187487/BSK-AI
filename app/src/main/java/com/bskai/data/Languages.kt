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

val languageList = listOf(
    "zh" to "中文",
    "en" to "English",
    "ja" to "日本語",
    "ko" to "한국어",
    "fr" to "Français",
    "de" to "Deutsch",
    "es" to "Español",
    "ru" to "Русский",
    "pt" to "Português",
    "it" to "Italiano",
    "ar" to "العربية",
    "hi" to "हिन्दी",
    "th" to "ไทย",
    "vi" to "Tiếng Việt",
    "id" to "Bahasa Indonesia",
    "tr" to "Türkçe",
    "pl" to "Polski",
    "nl" to "Nederlands",
    "sv" to "Svenska",
    "da" to "Dansk",
    "fi" to "Suomi",
    "el" to "Ελληνικά",
    "cs" to "Čeština",
    "ro" to "Română",
    "hu" to "Magyar",
    "uk" to "Українська",
    "he" to "עברית",
    "bn" to "বাংলা",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "mr" to "मराठी",
    "gu" to "ગુજરાતી",
    "kn" to "ಕನ್ನಡ",
    "ml" to "മലയാളം",
    "pa" to "ਪੰਜਾਬੀ",
    "ur" to "اردو",
    "fa" to "فارسی",
    "sw" to "Kiswahili"
)
