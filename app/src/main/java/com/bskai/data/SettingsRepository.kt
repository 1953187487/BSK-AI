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
        return AppSettings(
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            autoStartService = prefs.getBoolean(KEY_AUTO_START, false),
            vibrateOnResponse = prefs.getBoolean(KEY_VIBRATE, true),
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
            thinkingLevel = prefs.getInt(KEY_THINKING_LEVEL, 1),
            workspaceEnabled = prefs.getBoolean(KEY_WORKSPACE_ENABLED, false),
            selectedLanguage = prefs.getString(KEY_LANGUAGE, "zh") ?: "zh",
            chatMode = ChatMode.fromKey(prefs.getString(KEY_CHAT_MODE, ChatMode.THINK.key)),
            devDependenciesDownloaded = prefs.getBoolean(KEY_DEV_DEPS_DOWNLOADED, false),
            lastFeedbackDismissTime = prefs.getLong(KEY_LAST_FEEDBACK_DISMISS, 0),
            feedbackDismissedThisSession = prefs.getBoolean(KEY_FEEDBACK_SESSION, false)
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
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, s.darkTheme)
            .putBoolean(KEY_AUTO_START, s.autoStartService)
            .putBoolean(KEY_VIBRATE, s.vibrateOnResponse)
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
            .putInt(KEY_THINKING_LEVEL, s.thinkingLevel)
            .putBoolean(KEY_WORKSPACE_ENABLED, s.workspaceEnabled)
            .putString(KEY_CHAT_MODE, s.chatMode.key)
            .putBoolean(KEY_DEV_DEPS_DOWNLOADED, s.devDependenciesDownloaded)
            .putLong(KEY_LAST_FEEDBACK_DISMISS, s.lastFeedbackDismissTime)
            .putBoolean(KEY_FEEDBACK_SESSION, s.feedbackDismissedThisSession)
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
        private const val KEY_VIBRATE = "vibrate"
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
        private const val KEY_THINKING_LEVEL = "thinking_level"
        private const val KEY_WORKSPACE_ENABLED = "workspace_enabled"
        private const val KEY_CHAT_MODE = "chat_mode"
        private const val KEY_DEV_DEPS_DOWNLOADED = "dev_deps_downloaded"
        private const val KEY_LAST_FEEDBACK_DISMISS = "last_feedback_dismiss"
        private const val KEY_FEEDBACK_SESSION = "feedback_session"
    }
}
