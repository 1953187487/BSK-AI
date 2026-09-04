package com.bskai.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    val darkTheme: Boolean = true,
    val autoStartService: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val wakeWord: String = "小欧",
    val ttsEnabled: Boolean = true,
    val ttsLanguage: String = "zh",
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val vibrateOnResponse: Boolean = true,
    val showWaveAnimation: Boolean = true,
    val maxHistoryLength: Int = 50,
    val apiProviderUrl: String = "",
    val apiProviderKey: String = "",
    val apiModel: String = ""
)

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        _settings.update { transform(it) }
        val s = _settings.value
        prefs.edit()
            .putBoolean("dark_theme", s.darkTheme)
            .putBoolean("auto_start", s.autoStartService)
            .putBoolean("wake_word_enabled", s.wakeWordEnabled)
            .putString("wake_word", s.wakeWord)
            .putBoolean("tts_enabled", s.ttsEnabled)
            .putString("tts_language", s.ttsLanguage)
            .putFloat("tts_pitch", s.ttsPitch)
            .putFloat("tts_speed", s.ttsSpeed)
            .putBoolean("vibrate", s.vibrateOnResponse)
            .putBoolean("show_wave", s.showWaveAnimation)
            .putInt("max_history", s.maxHistoryLength)
            .putString("api_url", s.apiProviderUrl)
            .putString("api_key", s.apiProviderKey)
            .putString("api_model", s.apiModel)
            .apply()
    }

    fun reload() {
        _settings.value = load()
    }

    private fun load(): AppSettings = AppSettings(
        darkTheme = prefs.getBoolean("dark_theme", true),
        autoStartService = prefs.getBoolean("auto_start", false),
        wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false),
        wakeWord = prefs.getString("wake_word", "小欧") ?: "小欧",
        ttsEnabled = prefs.getBoolean("tts_enabled", true),
        ttsLanguage = prefs.getString("tts_language", "zh") ?: "zh",
        ttsPitch = prefs.getFloat("tts_pitch", 1.0f),
        ttsSpeed = prefs.getFloat("tts_speed", 1.0f),
        vibrateOnResponse = prefs.getBoolean("vibrate", true),
        showWaveAnimation = prefs.getBoolean("show_wave", true),
        maxHistoryLength = prefs.getInt("max_history", 50),
        apiProviderUrl = prefs.getString("api_url", "") ?: "",
        apiProviderKey = prefs.getString("api_key", "") ?: "",
        apiModel = prefs.getString("api_model", "") ?: ""
    )
}
