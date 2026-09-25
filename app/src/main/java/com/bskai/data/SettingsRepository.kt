package com.bskai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设置仓库：[AppSettings] 与 SharedPreferences 的双向同步层（内核持久化）。
 *
 * 2.1.1 重写要点：
 * - 持久化键统一到 [Keys] 常量组，移除随 2.0.3 下线的 autoStartService；
 * - 协议相关状态（agreed / agreementVersion / session ack / lastSeenVersion）
 *   与常规设置分离，方法化暴露；
 * - 所有 JSON 解析失败均回退默认值，保证升级路径安全。
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    /** 全局设置的响应式视图。 */
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** 原子更新设置并持久化。 */
    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        persist(next)
    }

    /** 丢弃内存态，从磁盘重载。 */
    fun reload() {
        _settings.value = load()
    }

    // ---------- 常规设置 ----------

    private fun load(): AppSettings {
        return AppSettings(
            darkTheme = prefs.getBoolean(Keys.DARK_THEME, true),
            vibrateOnResponse = prefs.getBoolean(Keys.VIBRATE, true),
            maxHistoryLength = prefs.getInt(Keys.MAX_HISTORY, 50),
            apiProviderUrl = prefs.getString(Keys.API_URL, "") ?: "",
            apiProviderKey = prefs.getString(Keys.API_KEY, "") ?: "",
            apiModel = prefs.getString(Keys.API_MODEL, "") ?: "",
            apiConnected = prefs.getBoolean(Keys.API_CONNECTED, false),
            themeStyle = ThemeStyle.fromKey(prefs.getString(Keys.THEME_STYLE, ThemeStyle.LIQUID.key)),
            customModelList = prefs.getString(Keys.CUSTOM_MODELS, "")
                ?.split('\n')
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            agentToolsEnabled = prefs.getBoolean(Keys.AGENT_TOOLS_ENABLED, false),
            localModels = parseLocalModels(prefs.getString(Keys.LOCAL_MODELS, "[]") ?: "[]"),
            modelSource = prefs.getString(Keys.MODEL_SOURCE, "api") ?: "api",
            thinkingLevel = prefs.getInt(Keys.THINKING_LEVEL, 1),
            workspaceEnabled = prefs.getBoolean(Keys.WORKSPACE_ENABLED, false),
            selectedLanguage = prefs.getString(Keys.LANGUAGE, "zh") ?: "zh",
            chatMode = ChatMode.fromKey(prefs.getString(Keys.CHAT_MODE, ChatMode.THINK.key)),
            devDependenciesDownloaded = prefs.getBoolean(Keys.DEV_DEPS_DOWNLOADED, false),
            lastFeedbackDismissTime = prefs.getLong(Keys.LAST_FEEDBACK_DISMISS, 0),
            feedbackDismissedThisSession = prefs.getBoolean(Keys.FEEDBACK_SESSION, false),
            videoGen = parseVideoGen(prefs.getString(Keys.VIDEO_GEN, null))
        )
    }

    private fun persist(s: AppSettings) {
        prefs.edit()
            .putBoolean(Keys.DARK_THEME, s.darkTheme)
            .putBoolean(Keys.VIBRATE, s.vibrateOnResponse)
            .putInt(Keys.MAX_HISTORY, s.maxHistoryLength)
            .putString(Keys.API_URL, s.apiProviderUrl)
            .putString(Keys.API_KEY, s.apiProviderKey)
            .putString(Keys.API_MODEL, s.apiModel)
            .putBoolean(Keys.API_CONNECTED, s.apiConnected)
            .putString(Keys.THEME_STYLE, s.themeStyle.key)
            .putString(Keys.CUSTOM_MODELS, s.customModelList.joinToString("\n"))
            .putBoolean(Keys.AGENT_TOOLS_ENABLED, s.agentToolsEnabled)
            .putString(Keys.LOCAL_MODELS, serializeLocalModels(s.localModels))
            .putString(Keys.MODEL_SOURCE, s.modelSource)
            .putInt(Keys.THINKING_LEVEL, s.thinkingLevel)
            .putBoolean(Keys.WORKSPACE_ENABLED, s.workspaceEnabled)
            .putString(Keys.CHAT_MODE, s.chatMode.key)
            .putBoolean(Keys.DEV_DEPS_DOWNLOADED, s.devDependenciesDownloaded)
            .putLong(Keys.LAST_FEEDBACK_DISMISS, s.lastFeedbackDismissTime)
            .putBoolean(Keys.FEEDBACK_SESSION, s.feedbackDismissedThisSession)
            .putString(Keys.VIDEO_GEN, serializeVideoGen(s.videoGen))
            .apply()
    }

    private fun parseVideoGen(json: String?): VideoGenSettings {
        if (json.isNullOrBlank()) return VideoGenSettings()
        return try {
            val o = org.json.JSONObject(json)
            VideoGenSettings(
                enabled = o.optBoolean("enabled", false),
                scriptModel = o.optString("scriptModel", ""),
                videoModel = o.optString("videoModel", "")
            )
        } catch (_: Exception) {
            VideoGenSettings()
        }
    }

    private fun serializeVideoGen(v: VideoGenSettings): String =
        org.json.JSONObject().apply {
            put("enabled", v.enabled)
            put("scriptModel", v.scriptModel)
            put("videoModel", v.videoModel)
        }.toString()

    private fun parseLocalModels(json: String): List<LocalModelEntry> {
        if (json.isBlank() || json == "[]") return emptyList()
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

    private fun serializeLocalModels(models: List<LocalModelEntry>): String =
        org.json.JSONArray().apply {
            models.forEach { m ->
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

    // ---------- 协议状态 ----------

    /** 用户是否已完成首次协议确认。 */
    fun hasAgreed(): Boolean = prefs.getBoolean(Keys.AGREED, false)

    fun setAgreed() {
        prefs.edit().putBoolean(Keys.AGREED, true).apply()
    }

    /** 上一次「已看」的版本（WhatsNew 判断用）。 */
    fun lastSeenVersion(): String? = prefs.getString(Keys.LAST_VERSION, null)

    fun setLastSeenVersion(version: String) {
        prefs.edit().putString(Keys.LAST_VERSION, version).apply()
    }

    /** 当前已签署协议的版本；与 [com.bskai.BuildConfig.APP_VERSION] 不一致时需重签。 */
    fun agreementVersion(): String? = prefs.getString(Keys.AGREEMENT_VERSION, null)

    fun setAgreementVersion(version: String) {
        prefs.edit().putString(Keys.AGREEMENT_VERSION, version).apply()
    }

    /** 本次会话（版本粒度）是否已确认协议。 */
    fun sessionAgreementAck(version: String): Boolean =
        prefs.getString(Keys.AGREEMENT_SESSION, null) == version

    fun markSessionAgreement(version: String) {
        prefs.edit().putString(Keys.AGREEMENT_SESSION, version).apply()
    }

    fun clearSessionAgreement() {
        prefs.edit().remove(Keys.AGREEMENT_SESSION).apply()
    }

    // ---------- 语言 ----------

    fun selectedLanguage(): String = prefs.getString(Keys.LANGUAGE, "zh") ?: "zh"

    fun setSelectedLanguage(code: String) {
        prefs.edit().putString(Keys.LANGUAGE, code).apply()
    }

    private object Keys {
        const val DARK_THEME = "dark_theme"
        const val VIBRATE = "vibrate"
        const val MAX_HISTORY = "max_history"
        const val API_URL = "api_url"
        const val API_KEY = "api_key"
        const val API_MODEL = "api_model"
        const val API_CONNECTED = "api_connected"
        const val AGREED = "agreements_accepted"
        const val LAST_VERSION = "last_version"
        const val LANGUAGE = "selected_language"
        const val THEME_STYLE = "theme_style"
        const val CUSTOM_MODELS = "custom_models"
        const val AGREEMENT_VERSION = "agreement_version"
        const val AGREEMENT_SESSION = "agreement_session"
        const val AGENT_TOOLS_ENABLED = "agent_tools_enabled"
        const val LOCAL_MODELS = "local_models"
        const val MODEL_SOURCE = "model_source"
        const val THINKING_LEVEL = "thinking_level"
        const val WORKSPACE_ENABLED = "workspace_enabled"
        const val CHAT_MODE = "chat_mode"
        const val DEV_DEPS_DOWNLOADED = "dev_deps_downloaded"
        const val LAST_FEEDBACK_DISMISS = "last_feedback_dismiss"
        const val FEEDBACK_SESSION = "feedback_session"
        const val VIDEO_GEN = "video_gen"
    }

    private companion object {
        const val PREFS_NAME = "aura_prefs"
    }
}
