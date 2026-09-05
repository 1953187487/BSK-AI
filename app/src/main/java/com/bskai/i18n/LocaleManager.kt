package com.bskai.i18n

import android.content.Context
import android.content.res.Configuration
import com.bskai.data.SettingsRepository
import java.util.Locale

object LocaleManager {

    fun apply(context: Context, settings: SettingsRepository): Context {
        val code = resolveCode(context, settings)
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun resolveCode(context: Context, settings: SettingsRepository): String {
        val saved = settings.selectedLanguage()
        if (saved.isNotBlank() && saved != "system") return saved
        val sys = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val tag = sys.toLanguageTag().lowercase()
        return when {
            tag.startsWith("zh") -> "zh"
            tag.startsWith("en") -> "en"
            tag.startsWith("ja") -> "ja"
            tag.startsWith("ko") -> "ko"
            tag.startsWith("es") -> "es"
            tag.startsWith("fr") -> "fr"
            tag.startsWith("de") -> "de"
            else -> "en"
        }
    }
}
