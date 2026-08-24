package com.bskai.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("bsk_ai_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        _settings.update { transform(it) }
        val s = _settings.value
        prefs.edit()
            .putBoolean("dark_theme", s.darkTheme)
            .putBoolean("dynamic_color", s.dynamicColor)
            .putLong("accent_color", s.accentColor)
            .putBoolean("protocol_agreed", s.protocolAgreed)
            .putString("language", s.language.code)
            .putBoolean("language_chosen", s.languageChosen)
            .putBoolean("auto_approve_tools", s.autoApproveTools)
            .putString("orchestration_mode", s.orchestrationMode.key)
            .putInt("max_pipeline_rounds", s.maxPipelineRounds)
            .putString("agent_ws_mode", s.agentWorkspace.mode.key)
            .putString("agent_ws_path", s.agentWorkspace.path)
            .putString("agent_ws_uri", s.agentWorkspace.treeUri)
            .apply()
    }

    fun reload() {
        _settings.value = load()
    }

    private fun load(): AppSettings = AppSettings(
        darkTheme = prefs.getBoolean("dark_theme", true),
        dynamicColor = prefs.getBoolean("dynamic_color", false),
        accentColor = prefs.getLong("accent_color", 0xFF6366F1),
        protocolAgreed = prefs.getBoolean("protocol_agreed", false),
        language = BskLanguage.fromCode(prefs.getString("language", null)),
        languageChosen = prefs.getBoolean("language_chosen", false),
        autoApproveTools = prefs.getBoolean("auto_approve_tools", false),
        orchestrationMode = OrchestrationMode.fromKey(prefs.getString("orchestration_mode", null)),
        maxPipelineRounds = prefs.getInt("max_pipeline_rounds", 2),
        agentWorkspace = AgentWorkspaceConfig(
            mode = WorkspaceMode.fromKey(prefs.getString("agent_ws_mode", null)),
            path = prefs.getString("agent_ws_path", "") ?: "",
            treeUri = prefs.getString("agent_ws_uri", "") ?: ""
        )
    )
}
