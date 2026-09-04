package com.bskai.data

data class AppSettings(
    val darkTheme: Boolean = true,
    val autoStartService: Boolean = false,
    val ttsEnabled: Boolean = true,
    val ttsLanguage: String = "zh",
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val vibrateOnResponse: Boolean = true,
    val showWaveAnimation: Boolean = true,
    val maxHistoryLength: Int = 50,
    val apiProviderUrl: String = "",
    val apiProviderKey: String = "",
    val apiModel: String = "",
    val apiConnected: Boolean = false
) {
    val apiConfigured: Boolean
        get() = apiProviderUrl.isNotBlank() && apiProviderKey.isNotBlank() && apiModel.isNotBlank()
}
