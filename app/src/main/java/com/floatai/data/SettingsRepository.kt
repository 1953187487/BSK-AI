package com.floatai.data

import android.content.Context
import android.content.SharedPreferences
import com.floatai.data.model.ApiConfig
import com.floatai.data.model.AppSettings
import com.floatai.ui.theme.accentNameByColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 设置仓库：SharedPreferences 读写 + StateFlow 响应式暴露。
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE)

    private val _apiConfig = MutableStateFlow(loadApiConfig())
    val apiConfig: StateFlow<ApiConfig> = _apiConfig.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateApiConfig(transform: (ApiConfig) -> ApiConfig) {
        _apiConfig.update { transform(it) }
        with(_apiConfig.value) {
            prefs.edit()
                .putString("api_url", baseUrl)
                .putString("api_key", apiKey)
                .putString("api_model", model)
                .apply()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update { transform(it) }
        with(_settings.value) {
            prefs.edit()
                .putBoolean("dark_theme", darkTheme)
                .putBoolean("dynamic_color", dynamicColor)
                .putString("accent_color", accentColor)
                .putBoolean("float_enabled", floatEnabled)
                .putBoolean("protocol_agreed", protocolAgreed)
                .apply()
        }
    }

    /** 兼容旧版本：读取旧字段名称的默认值。 */
    private fun loadApiConfig(): ApiConfig = ApiConfig(
        baseUrl = prefs.getString("api_url", "") ?: "",
        apiKey = prefs.getString("api_key", "") ?: "",
        model = prefs.getString("selected_model", prefs.getString("api_model", "auto") ?: "auto")
            ?: "auto"
    )

    private fun loadSettings(): AppSettings = AppSettings(
        darkTheme = prefs.getBoolean("dark_theme", true),
        dynamicColor = prefs.getBoolean("dynamic_color", false),
        accentColor = prefs.getString("theme_color", "")?.takeIf { it.startsWith("#") }
            ?.let { accentNameByColor(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it))) }
            ?: prefs.getString("accent_color", "紫色") ?: "紫色",
        floatEnabled = prefs.getBoolean("float_enabled", false),
        protocolAgreed = prefs.getBoolean("protocol_agreed", false)
    )
}
