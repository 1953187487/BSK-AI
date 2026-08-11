package com.floatai.atk

import com.floatai.data.model.ApiConfig
import com.floatai.data.model.ChatMessage
import com.floatai.data.remote.OpenAiClient

/**
 * AI 诊断器：把构建日志喂给当前 AI 配置，让它输出"问题原因 + 修复建议"。
 */
object AtkAiDiagnose {

    /**
     * @return AI 回答的纯文本；失败时返回错误描述。
     */
    suspend fun diagnose(
        config: ApiConfig,
        logs: String,
        lang: String = "zh"
    ): String {
        val prompt = buildPrompt(logs, lang)
        return when (val r = OpenAiClient.chatCompletions(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model.ifBlank { "auto" },
            messages = listOf(
                ChatMessage("system", "You are a senior Android build engineer."),
                ChatMessage("user", prompt)
            )
        )) {
            is OpenAiClient.ChatResult.Success -> r.content
            is OpenAiClient.ChatResult.Error -> "AI 诊断失败：${r.message}"
        }
    }

    private fun buildPrompt(logs: String, lang: String): String {
        // 截取尾部相关错误日志，避免 token 爆炸
        val tail = logs.lineSequence().takeLast(200).joinToString("\n")
        return if (lang == "zh") {
            """以下是一段 Android Gradle 构建日志的尾部，请诊断错误原因并给出具体修复步骤（直接输出命令或代码片段）：

```
$tail
```
""".trimIndent()
        } else {
            """The following is the tail of an Android Gradle build log. Diagnose the failure and provide concrete fixes (commands or code snippets):

```
$tail
```
""".trimIndent()
        }
    }
}
