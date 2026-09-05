package com.bskai.data

enum class ThemeStyle(val key: String, val label: String, val description: String) {
    AURORA("aurora", "极光", "蓝紫渐变，呼吸感背景"),
    NEON("neon", "霓虹", "赛博粉青，深底亮字"),
    GLASS("glass", "玻璃", "磨砂半透，圆角厚边"),
    VOICE("voice", "语音优先", "大按钮，单屏聚焦说话");

    companion object {
        fun fromKey(k: String?): ThemeStyle = entries.firstOrNull { it.key == k } ?: AURORA
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

data class ChatRole(
    val id: String,
    val name: String,
    val avatar: String,
    val systemPrompt: String,
    val isAiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMode(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val systemPrompt: String = "",
    val thinkingLevel: Int = 0,
    val isBuiltIn: Boolean = true
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val autoStartService: Boolean = false,
    val ttsEnabled: Boolean = false,
    val ttsLanguage: String = "zh",
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val vibrateOnResponse: Boolean = true,
    val showWaveAnimation: Boolean = true,
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
    val currentRoleId: String = "default",
    val currentModeId: String = "chat",
    val thinkingLevel: Int = 0,
    val roles: List<ChatRole> = emptyList(),
    val modes: List<ChatMode> = emptyList()
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

val DefaultModes: List<ChatMode> = listOf(
    ChatMode("chat", "聊天", "💬", "自由对话模式"),
    ChatMode("think", "思考", "🧠", "深度思考模式", thinkingLevel = 2),
    ChatMode("analyze", "分析", "🔍", "分析工作区APK应用"),
    ChatMode("dev", "开发", "🛠️", "开发模式，安全APK分析"),
    ChatMode("creative", "创意", "🎨", "创意写作模式"),
    ChatMode("code", "编程", "💻", "代码编写与调试"),
    ChatMode("translate", "翻译", "🌐", "多语言翻译模式"),
    ChatMode("tutor", "教学", "📚", "知识讲解模式")
)

val DefaultRoles: List<ChatRole> = listOf(
    ChatRole("default", "AURA", "🤖", "你是一个智能AI助手AURA，运行在Android设备上。你友好、专业、乐于助人。"),
    ChatRole("coder", "代码专家", "💻", "你是一个专业的编程专家，精通各种编程语言和技术栈。"),
    ChatRole("writer", "写作助手", "✍️", "你是一个专业的写作助手，擅长创意写作、文案撰写和内容创作。"),
    ChatRole("analyst", "数据分析师", "📊", "你是一个数据分析专家，擅长分析数据、发现趋势和提供洞察。"),
    ChatRole("teacher", "知识导师", "🎓", "你是一个耐心的教师，擅长用简单易懂的方式解释复杂概念。")
)
