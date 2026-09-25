package com.lingxi.ai.data

import android.content.Context
import com.lingxi.ai.R
import org.json.JSONArray

/**
 * 语言元数据。
 *
 * 主清单（[languages]）用于「选择语言」对话框的本地化渲染；
 * [loadLanguages] 从 raw/languages.json 读取扩展列表（region 等字段），
 * 解析失败时回退到本地清单，保证永远有可用列表。
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val region: String = ""
)

object Languages {

    /** 本地内置清单，与 raw/languages.json 保持同步。 */
    val builtIn: List<Language> = listOf(
        Language("zh", "Chinese", "中文"),
        Language("en", "English", "English"),
        Language("ja", "Japanese", "日本語"),
        Language("ko", "Korean", "한국어"),
        Language("fr", "French", "Français"),
        Language("de", "German", "Deutsch"),
        Language("es", "Spanish", "Español"),
        Language("ru", "Russian", "Русский"),
        Language("pt", "Portuguese", "Português"),
        Language("it", "Italian", "Italiano"),
        Language("ar", "Arabic", "العربية"),
        Language("hi", "Hindi", "हिन्दी"),
        Language("th", "Thai", "ไทย"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
        Language("id", "Indonesian", "Bahasa Indonesia"),
        Language("tr", "Turkish", "Türkçe"),
        Language("pl", "Polish", "Polski"),
        Language("nl", "Dutch", "Nederlands"),
        Language("sv", "Swedish", "Svenska"),
        Language("da", "Danish", "Dansk"),
        Language("fi", "Finnish", "Suomi"),
        Language("el", "Greek", "Ελληνικά"),
        Language("cs", "Czech", "Čeština"),
        Language("ro", "Romanian", "Română"),
        Language("hu", "Hungarian", "Magyar"),
        Language("uk", "Ukrainian", "Українська"),
        Language("he", "Hebrew", "עברית"),
        Language("bn", "Bengali", "বাংলা"),
        Language("ta", "Tamil", "தமிழ்"),
        Language("te", "Telugu", "తెలుగు"),
        Language("mr", "Marathi", "मराठी"),
        Language("gu", "Gujarati", "ગુજરાતી"),
        Language("kn", "Kannada", "ಕನ್ನಡ"),
        Language("ml", "Malayalam", "മലയാളം"),
        Language("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        Language("ur", "Urdu", "اردو"),
        Language("fa", "Persian", "فارسی"),
        Language("sw", "Swahili", "Kiswahili")
    )

    /** 代码 → 内置语言（找不到时返回 null）。 */
    fun byCode(code: String): Language? = builtIn.firstOrNull { it.code == code }
}

fun loadLanguages(context: Context): List<Language> {
    return try {
        val json = context.resources.openRawResource(R.raw.languages)
            .bufferedReader().use { it.readText() }
        val parsed = JSONArray(json)
            .let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.getJSONObject(i)
                    val code = o.optString("code")
                    if (code.isBlank()) null
                    else Language(
                        code = code,
                        name = o.optString("name"),
                        nativeName = o.optString("nativeName"),
                        region = o.optString("region")
                    )
                }
            }
        parsed.ifEmpty { Languages.builtIn }
    } catch (_: Exception) {
        Languages.builtIn
    }
}
