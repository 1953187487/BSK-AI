package com.bskai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        persist(next)
    }

    fun reload() {
        _settings.value = load()
    }

    private fun load(): AppSettings {
        return AppSettings(
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            autoStartService = prefs.getBoolean(KEY_AUTO_START, false),
            ttsEnabled = prefs.getBoolean(KEY_TTS_ENABLED, true),
            ttsLanguage = prefs.getString(KEY_TTS_LANGUAGE, "zh") ?: "zh",
            ttsPitch = prefs.getFloat(KEY_TTS_PITCH, 1.0f),
            ttsSpeed = prefs.getFloat(KEY_TTS_SPEED, 1.0f),
            vibrateOnResponse = prefs.getBoolean(KEY_VIBRATE, true),
            showWaveAnimation = prefs.getBoolean(KEY_SHOW_WAVE, true),
            maxHistoryLength = prefs.getInt(KEY_MAX_HISTORY, 50),
            apiProviderUrl = prefs.getString(KEY_API_URL, "") ?: "",
            apiProviderKey = prefs.getString(KEY_API_KEY, "") ?: "",
            apiModel = prefs.getString(KEY_API_MODEL, "") ?: "",
            apiConnected = prefs.getBoolean(KEY_API_CONNECTED, false),
            themeStyle = ThemeStyle.fromKey(prefs.getString(KEY_THEME_STYLE, ThemeStyle.AURORA.key))
        )
    }

    private fun persist(s: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, s.darkTheme)
            .putBoolean(KEY_AUTO_START, s.autoStartService)
            .putBoolean(KEY_TTS_ENABLED, s.ttsEnabled)
            .putString(KEY_TTS_LANGUAGE, s.ttsLanguage)
            .putFloat(KEY_TTS_PITCH, s.ttsPitch)
            .putFloat(KEY_TTS_SPEED, s.ttsSpeed)
            .putBoolean(KEY_VIBRATE, s.vibrateOnResponse)
            .putBoolean(KEY_SHOW_WAVE, s.showWaveAnimation)
            .putInt(KEY_MAX_HISTORY, s.maxHistoryLength)
            .putString(KEY_API_URL, s.apiProviderUrl)
            .putString(KEY_API_KEY, s.apiProviderKey)
            .putString(KEY_API_MODEL, s.apiModel)
            .putBoolean(KEY_API_CONNECTED, s.apiConnected)
            .putString(KEY_THEME_STYLE, s.themeStyle.key)
            .apply()
    }

    fun hasAgreed(): Boolean = prefs.getBoolean(KEY_AGREED, false)

    fun setAgreed() {
        prefs.edit().putBoolean(KEY_AGREED, true).apply()
    }

    fun lastSeenVersion(): String? = prefs.getString(KEY_LAST_VERSION, null)

    fun setLastSeenVersion(version: String) {
        prefs.edit().putString(KEY_LAST_VERSION, version).apply()
    }

    fun selectedLanguage(): String = prefs.getString(KEY_LANGUAGE, "zh") ?: "zh"

    fun setSelectedLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
    }

    companion object {
        private const val PREFS_NAME = "aura_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_LANGUAGE = "tts_language"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_SHOW_WAVE = "show_wave"
        private const val KEY_MAX_HISTORY = "max_history"
        private const val KEY_API_URL = "api_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_MODEL = "api_model"
        private const val KEY_API_CONNECTED = "api_connected"
        private const val KEY_AGREED = "agreements_accepted"
        private const val KEY_LAST_VERSION = "last_version"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_THEME_STYLE = "theme_style"
    }
}
