package com.floatai.data.model

/**
 * AI 角色 v1.0.4：
 *  - name: 角色名（显示在 AI 聊天顶部）
 *  - avatar: 头像 URI（content:// 或 file://）或 null（用 initials 圆头像占位）
 *  - systemPrompt: 系统提示词，发给 AI 模型作为前缀
 *  - greeting: 角色欢迎语（首条 assistant 消息）
 *  - temperature: 模型温度
 *  - builtin: 是否为内置角色（不可删除）
 */
data class Character(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val systemPrompt: String = "",
    val greeting: String = "",
    val temperature: Float = 0.7f,
    val builtin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** 内置默认角色（不可删除，作为首次启动的默认）。 */
val DEFAULT_CHARACTER = Character(
    id = "builtin-floatai",
    name = "FloatAI 助手",
    avatar = null,
    systemPrompt = "你是 FloatAI，一个本地优先的开源 Android AI 助手。" +
        "回答简洁友好，使用中文。代码示例优先用 Kotlin。",
    greeting = "你好，我是 FloatAI 助手。有什么可以帮你的？",
    temperature = 0.7f,
    builtin = true,
    createdAt = 0
)
