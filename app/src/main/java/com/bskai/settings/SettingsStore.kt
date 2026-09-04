package com.bskai.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

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

class SettingsStore(private val context: Context) {

    private val settings = context.dataStore

    val settings: Flow<AppSettings> = settings.data.map { prefs ->
        AppSettings(
            darkTheme = prefs[DARK_THEME] ?: true,
            autoStartService = prefs[AUTO_START] ?: false,
            wakeWordEnabled = prefs[WAKE_WORD_ENABLED] ?: false,
            wakeWord = prefs[WAKE_WORD] ?: "小欧",
            ttsEnabled = prefs[TTS_ENABLED] ?: true,
            ttsLanguage = prefs[TTS_LANGUAGE] ?: "zh",
            ttsPitch = prefs[TTS_PITCH] ?: 1.0f,
            ttsSpeed = prefs[TTS_SPEED] ?: 1.0f,
            vibrateOnResponse = prefs[VIBRATE] ?: true,
            showWaveAnimation = prefs[SHOW_WAVE] ?: true,
            maxHistoryLength = prefs[MAX_HISTORY] ?: 50,
            apiProviderUrl = prefs[API_URL] ?: "",
            apiProviderKey = prefs[API_KEY] ?: "",
            apiModel = prefs[API_MODEL] ?: ""
        )
    }

    suspend fun update(transform: suspend (AppSettings) -> AppSettings) {
        settings.edit { prefs ->
            val newSettings = transform(load())
            prefs[DARK_THEME] = newSettings.darkTheme
            prefs[AUTO_START] = newSettings.autoStartService
            prefs[WAKE_WORD_ENABLED] = newSettings.wakeWordEnabled
            prefs[WAKE_WORD] = newSettings.wakeWord
            prefs[TTS_ENABLED] = newSettings.ttsEnabled
            prefs[TTS_LANGUAGE] = newSettings.ttsLanguage
            prefs[TTS_PITCH] = newSettings.ttsPitch
            prefs[TTS_SPEED] = newSettings.ttsSpeed
            prefs[VIBRATE] = newSettings.vibrateOnResponse
            prefs[SHOW_WAVE] = newSettings.showWaveAnimation
            prefs[MAX_HISTORY] = newSettings.maxHistoryLength
            prefs[API_URL] = newSettings.apiProviderUrl
            prefs[API_KEY] = newSettings.apiProviderKey
            prefs[API_MODEL] = newSettings.apiModel
        }
    }

    private fun load(): AppSettings = settings.value

    companion object {
        private val DARK_THEME = booleanPreferencesKey("dark_theme")
        private val AUTO_START = booleanPreferencesKey("auto_start_service")
        private val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val WAKE_WORD = stringPreferencesKey("wake_word")
        private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val TTS_LANGUAGE = stringPreferencesKey("tts_language")
        private val TTS_PITCH = floatPreferencesKey("tts_pitch")
        private val TTS_SPEED = floatPreferencesKey("tts_speed")
        private val VIBRATE = booleanPreferencesKey("vibrate")
        private val SHOW_WAVE = booleanPreferencesKey("show_wave")
        private val MAX_HISTORY = intPreferencesKey("max_history")
        private val API_URL = stringPreferencesKey("api_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val API_MODEL = stringPreferencesKey("api_model")
    }
}
