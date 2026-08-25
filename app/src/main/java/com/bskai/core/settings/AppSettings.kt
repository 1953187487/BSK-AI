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

enum class WorkspaceMode(val key: String) {
    DEFAULT("default"),
    FILE("file"),
    SAF("saf");

    companion object {
        fun fromKey(key: String?): WorkspaceMode =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

data class AgentWorkspaceConfig(
    val mode: WorkspaceMode = WorkspaceMode.DEFAULT,
    val path: String = "",
    val treeUri: String = ""
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val dynamicColor: Boolean = false,
    val accentColor: Long = 0xFF6366F1,
    val protocolAgreed: Boolean = false,
    val language: BskLanguage = BskLanguage.ZH,
    val languageChosen: Boolean = false,
    val autoApproveTools: Boolean = false,
    val orchestrationMode: OrchestrationMode = OrchestrationMode.SEQUENTIAL,
    val maxPipelineRounds: Int = 2,
    val agentWorkspace: AgentWorkspaceConfig = AgentWorkspaceConfig(),
    // 流动式输出配置：可选择流式输出或进入式流式输出
    val streamingOutput: Boolean = true,
    val streamingInputMode: Boolean = false,
    // 逆向功能已去除，术语改为安卓分析（仅标签，不执行逆向工程）
    val androidAnalysisLabel: String = "安卓分析（功能已禁用）"
)
