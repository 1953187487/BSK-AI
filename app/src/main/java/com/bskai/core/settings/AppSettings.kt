package com.bskai.core.settings

enum class BskLanguage(val code: String, val display: String) {
    ZH("zh", "简体中文"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): BskLanguage =
            entries.firstOrNull { it.code == code } ?: ZH
    }
}

enum class OrchestrationMode(val key: String, val display: String) {
    SEQUENTIAL("sequential", "串行流水线"),
    PARALLEL("parallel", "并行分发");

    companion object {
        fun fromKey(key: String?): OrchestrationMode =
            entries.firstOrNull { it.key == key } ?: SEQUENTIAL
    }
}

data class AppSettings(
    val darkTheme: Boolean = true,
    val accentColor: Long = 0xFF6366F1,
    val protocolAgreed: Boolean = false,
    val language: BskLanguage = BskLanguage.ZH,
    val languageChosen: Boolean = false,
    val autoApproveTools: Boolean = false,
    val orchestrationMode: OrchestrationMode = OrchestrationMode.SEQUENTIAL,
    val maxPipelineRounds: Int = 2,
    val providerUrl: String = "",
    val apiKey: String = "",
    val selectedModel: String = ""
)
