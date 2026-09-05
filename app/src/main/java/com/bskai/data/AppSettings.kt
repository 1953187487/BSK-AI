package com.bskai.data

enum class ThemeStyle(val key: String, val label: String, val description: String) {
    AURORA("aurora", "极光", "蓝紫渐变，呼吸感背景"),
    NEON("neon", "霓虹", "赛博粉青，深底亮字"),
    GLASS("glass", "玻璃", "磨砂半透，圆角厚边"),
    LIQUID("liquid", "液态玻璃", "iOS 27 风格，液态玻璃质感");

    companion object {
        fun fromKey(k: String?): ThemeStyle = entries.firstOrNull { it.key == k } ?: AURORA
    }
}

enum class ChatMode(val key: String, val label: String, val description: String) {
    THINK("think", "思考模式", "AI 逐步推理分析"),
    DEV("dev", "应用开发模式", "AI 辅助开发 Android 应用");

    companion object {
        fun fromKey(k: String?): ChatMode = entries.firstOrNull { it.key == k } ?: THINK
    }
}

data class LocalModelEntry(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val source: String,
    val category: String = "通用",
    val downloadedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val autoStartService: Boolean = false,
    val vibrateOnResponse: Boolean = true,
    val maxHistoryLength: Int = 50,
    val apiProviderUrl: String = "",
    val apiProviderKey: String = "",
    val apiModel: String = "",
    val apiConnected: Boolean = false,
    val themeStyle: ThemeStyle = ThemeStyle.AURORA,
    val customModelList: List<String> = emptyList(),
    val agentToolsEnabled: Boolean = false,
    val localModels: List<LocalModelEntry> = emptyList(),
    val modelSource: String = "api",
    val thinkingLevel: Int = 1,
    val workspaceEnabled: Boolean = false,
    val selectedLanguage: String = "zh",
    val chatMode: ChatMode = ChatMode.THINK,
    val devDependenciesDownloaded: Boolean = false,
    val lastFeedbackDismissTime: Long = 0,
    val feedbackDismissedThisSession: Boolean = false
) {
    val apiConfigured: Boolean
        get() = apiProviderUrl.isNotBlank() && apiProviderKey.isNotBlank() && apiModel.isNotBlank()
}

val DefaultModelPresets: List<String> = listOf(
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-3.5-turbo",
    "deepseek-chat",
    "qwen-turbo",
    "qwen-plus",
    "gemini-1.5-flash",
    "claude-3-5-sonnet"
)

val DefaultApiUrlPresets: List<String> = listOf(
    "https://api.openai.com/v1",
    "https://api.deepseek.com/v1",
    "https://dashscope.aliyuncs.com/compatible-mode/v1",
    "https://generativelanguage.googleapis.com/v1beta/openai"
)
