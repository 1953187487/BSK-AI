package com.lingxi.ai.data

/**
 * 主题风格。key 用于持久化，label/description 用于 UI 展示（后续可 i18n 化）。
 */
enum class ThemeStyle(val key: String, val label: String, val description: String) {
    AURORA("aurora", "极光", "蓝紫渐变，呼吸感背景"),
    NEON("neon", "霓虹", "赛博粉青，深底亮字"),
    GLASS("glass", "玻璃", "磨砂半透，圆角厚边"),
    LIQUID("liquid", "液态玻璃", "iOS 风格液态玻璃质感");

    companion object {
        fun fromKey(k: String?): ThemeStyle = entries.firstOrNull { it.key == k } ?: AURORA
    }
}

/** 对话模式：思考（逐级深度）或应用开发。 */
enum class ChatMode(val key: String, val label: String, val description: String) {
    THINK("think", "思考模式", "AI 逐步推理分析"),
    DEV("dev", "应用开发模式", "AI 辅助开发 Android 应用");

    companion object {
        fun fromKey(k: String?): ChatMode = entries.firstOrNull { it.key == k } ?: THINK
    }
}

/** 已下载的本地模型条目。 */
data class LocalModelEntry(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val source: String,
    val category: String = "通用",
    val downloadedAt: Long = System.currentTimeMillis()
)

/** 双模型视频生成产物。 */
data class VideoArtifact(
    val script: String,
    val videoPath: String,
    val scriptModel: String,
    val videoModel: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** 双模型视频生成设置。 */
data class VideoGenSettings(
    val enabled: Boolean = false,
    /** 写脚本的模型：由已配置的 API 服务商或自定义 API 模型提供。 */
    val scriptModel: String = "",
    /** 生成视频的模型：本地已下载模型。 */
    val videoModel: String = ""
)

/**
 * 应用全局设置。所有字段均有默认值，[SettingsRepository] 负责与 SharedPreferences 双向同步。
 */
data class AppSettings(
    val darkTheme: Boolean = true,
    val vibrateOnResponse: Boolean = true,
    val maxHistoryLength: Int = 50,
    val apiProviderUrl: String = "",
    val apiProviderKey: String = "",
    val apiModel: String = "",
    val apiConnected: Boolean = false,
    val themeStyle: ThemeStyle = ThemeStyle.LIQUID,
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
    val feedbackDismissedThisSession: Boolean = false,
    val videoGen: VideoGenSettings = VideoGenSettings()
) {
    /** 是否已配置可用的 AI 服务。 */
    val apiConfigured: Boolean
        get() = apiProviderUrl.isNotBlank() && apiModel.isNotBlank() &&
            (apiProviderKey.isNotBlank() || modelSource == "local")
}

val DefaultModelPresets: List<String> = listOf(
    "gpt-4o-mini",
    "gpt-4o",
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
