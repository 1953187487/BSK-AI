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
        val localModelsJson = prefs.getString(KEY_LOCAL_MODELS, "[]") ?: "[]"
        val localModels = parseLocalModels(localModelsJson)
        val rolesJson = prefs.getString(KEY_ROLES, "[]") ?: "[]"
        val roles = parseRoles(rolesJson)
        val modesJson = prefs.getString(KEY_MODES, "[]") ?: "[]"
        val modes = parseModes(modesJson)
        return AppSettings(
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            autoStartService = prefs.getBoolean(KEY_AUTO_START, false),
            ttsEnabled = prefs.getBoolean(KEY_TTS_ENABLED, false),
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
            themeStyle = ThemeStyle.fromKey(prefs.getString(KEY_THEME_STYLE, ThemeStyle.AURORA.key)),
            customModelList = prefs.getString(KEY_CUSTOM_MODELS, "")
                ?.split('\n')
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            agentToolsEnabled = prefs.getBoolean(KEY_AGENT_TOOLS_ENABLED, false),
            localModels = localModels,
            modelSource = prefs.getString(KEY_MODEL_SOURCE, "api") ?: "api",
            currentRoleId = prefs.getString(KEY_CURRENT_ROLE, "default") ?: "default",
            currentModeId = prefs.getString(KEY_CURRENT_MODE, "chat") ?: "chat",
            thinkingLevel = prefs.getInt(KEY_THINKING_LEVEL, 0),
            roles = roles,
            modes = modes
        )
    }

    private fun parseLocalModels(json: String): List<LocalModelEntry> {
        if (json == "[]") return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LocalModelEntry(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    path = o.optString("path"),
                    sizeBytes = o.optLong("sizeBytes"),
                    source = o.optString("source"),
                    category = o.optString("category", "通用"),
                    downloadedAt = o.optLong("downloadedAt")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRoles(json: String): List<ChatRole> {
        if (json == "[]") return DefaultRoles
        return try {
            val arr = org.json.JSONArray(json)
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChatRole(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    avatar = o.optString("avatar"),
                    systemPrompt = o.optString("systemPrompt"),
                    isAiGenerated = o.optBoolean("isAiGenerated"),
                    createdAt = o.optLong("createdAt")
                )
            }
            if (list.isEmpty()) DefaultRoles else list
        } catch (_: Exception) {
            DefaultRoles
        }
    }

    private fun parseModes(json: String): List<ChatMode> {
        if (json == "[]") return DefaultModes
        return try {
            val arr = org.json.JSONArray(json)
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChatMode(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    icon = o.optString("icon"),
                    description = o.optString("description"),
                    systemPrompt = o.optString("systemPrompt"),
                    thinkingLevel = o.optInt("thinkingLevel"),
                    isBuiltIn = o.optBoolean("isBuiltIn", true)
                )
            }
            if (list.isEmpty()) DefaultModes else list
        } catch (_: Exception) {
            DefaultModes
        }
    }

    private fun persist(s: AppSettings) {
        val localModelsJson = org.json.JSONArray().apply {
            s.localModels.forEach { m ->
                put(org.json.JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("path", m.path)
                    put("sizeBytes", m.sizeBytes)
                    put("source", m.source)
                    put("category", m.category)
                    put("downloadedAt", m.downloadedAt)
                })
            }
        }.toString()
        val rolesJson = org.json.JSONArray().apply {
            s.roles.forEach { r ->
                put(org.json.JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("avatar", r.avatar)
                    put("systemPrompt", r.systemPrompt)
                    put("isAiGenerated", r.isAiGenerated)
                    put("createdAt", r.createdAt)
                })
            }
        }.toString()
        val modesJson = org.json.JSONArray().apply {
            s.modes.forEach { m ->
                put(org.json.JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("icon", m.icon)
                    put("description", m.description)
                    put("systemPrompt", m.systemPrompt)
                    put("thinkingLevel", m.thinkingLevel)
                    put("isBuiltIn", m.isBuiltIn)
                })
            }
        }.toString()
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
            .putString(KEY_CUSTOM_MODELS, s.customModelList.joinToString("\n"))
            .putBoolean(KEY_AGENT_TOOLS_ENABLED, s.agentToolsEnabled)
            .putString(KEY_LOCAL_MODELS, localModelsJson)
            .putString(KEY_MODEL_SOURCE, s.modelSource)
            .putString(KEY_CURRENT_ROLE, s.currentRoleId)
            .putString(KEY_CURRENT_MODE, s.currentModeId)
            .putInt(KEY_THINKING_LEVEL, s.thinkingLevel)
            .putString(KEY_ROLES, rolesJson)
            .putString(KEY_MODES, modesJson)
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

    fun agreementVersion(): String? = prefs.getString(KEY_AGREEMENT_VERSION, null)

    fun setAgreementVersion(version: String) {
        prefs.edit().putString(KEY_AGREEMENT_VERSION, version).apply()
    }

    fun sessionAgreementAck(version: String): Boolean =
        prefs.getString(KEY_AGREEMENT_SESSION, null) == version

    fun markSessionAgreement(version: String) {
        prefs.edit().putString(KEY_AGREEMENT_SESSION, version).apply()
    }

    fun clearSessionAgreement() {
        prefs.edit().remove(KEY_AGREEMENT_SESSION).apply()
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
        private const val KEY_CUSTOM_MODELS = "custom_models"
        private const val KEY_AGREEMENT_VERSION = "agreement_version"
        private const val KEY_AGREEMENT_SESSION = "agreement_session"
        private const val KEY_AGENT_TOOLS_ENABLED = "agent_tools_enabled"
        private const val KEY_LOCAL_MODELS = "local_models"
        private const val KEY_MODEL_SOURCE = "model_source"
        private const val KEY_CURRENT_ROLE = "current_role"
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_THINKING_LEVEL = "thinking_level"
        private const val KEY_ROLES = "roles"
        private const val KEY_MODES = "modes"
    }
}
