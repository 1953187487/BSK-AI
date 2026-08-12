package com.floatai.data.remote

import com.floatai.data.model.ChatMessage

/**
 * API 客户端简单封装 v1.0.4：
 *  - 调用 SettingsRepository 当前配置的 baseUrl/apiKey/model
 *  - 单次简单对话（用于插件/技能 AI 生成等）
 */
object ApiClient {

    /**
     * 单轮 chat 调用：传入 baseUrl/apiKey/model/prompt 返回 content。
     */
    suspend fun simpleChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        systemPrompt: String = "You are a helpful assistant."
    ): String {
        require(apiKey.isNotBlank()) { "请先在「设置」中配置 API Key" }
        require(baseUrl.isNotBlank()) { "请先在「设置」中配置 Base URL" }

        val msgs = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = prompt)
        )
        return when (val r = OpenAiClient.chatCompletions(baseUrl, apiKey, model, msgs)) {
            is ChatResult.Success -> r.content
            is ChatResult.Error -> throw RuntimeException(r.message)
        }
    }
}
