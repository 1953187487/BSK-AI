package com.bskai.agent

import android.content.Context
import com.bskai.data.SettingsRepository
import com.bskai.intent.SkillEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentEngine(
    context: Context,
    private val settings: SettingsRepository,
    private val skillEngine: SkillEngine
) {

    private val llm = LlmClient(context)

    private val _conversation = MutableStateFlow<List<ChatMsg>>(emptyList())
    val conversation: StateFlow<List<ChatMsg>> = _conversation.asStateFlow()

    suspend fun answer(userText: String): String {
        val text = userText.trim()
        if (text.isEmpty()) return "请再说一遍"

        skillEngine.tryExecute(text)?.let { reply ->
            append(ChatMsg("user", text))
            append(ChatMsg("assistant", reply))
            return reply
        }

        val s = settings.settings.value
        if (s.apiConfigured) {
            append(ChatMsg("user", text))
            val history = recentMessages(s.maxHistoryLength)
            return try {
                val reply = llm.chat(s, history)
                val clean = reply.ifEmpty { "抱歉，我没有想好怎么回答。" }
                append(ChatMsg("assistant", clean))
                clean
            } catch (_: Exception) {
                val fallback = localReply(text)
                append(ChatMsg("assistant", fallback))
                fallback
            }
        }
        return localReply(text)
    }

    private fun announce(text: String) {
        append(ChatMsg("assistant", text))
    }

    fun notifyAssistant(text: String) = announce(text)

    fun recentMessages(max: Int): List<ChatMsg> {
        val system = ChatMsg(
            "system",
            "你是 AURA，一位手机语音助手。请用简体中文简洁回答，语气友好。涉及控制手机的操作请先说明会怎么做。"
        )
        val history = _conversation.value.takeLast(max.coerceAtLeast(2))
        return listOf(system) + history
    }

    private fun append(msg: ChatMsg) {
        val max = settings.settings.value.maxHistoryLength
        _conversation.value = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
    }

    private fun localReply(text: String): String {
        return when {
            text.contains("你好") || text.contains("您好") || text.contains("hi") ||
                text.contains("hello") || text.contains("嗨") ->
                "你好呀，我是 AURA。按住说话就可以跟我聊天，也可以让我调音量、报时间、打开应用或系统设置。"
            text.contains("你是谁") || text.contains("介绍你自己") ->
                "我是 AURA，一个本地语音助手。可以听你说话，帮你完成音量调节、静音、报时间、打开应用和设置等操作。连接 AI 服务后还能陪你聊天和回答各种问题。"
            text.contains("你能做什么") || text.contains("你会什么") || text.contains("有什么功能") ->
                "我可以帮你：调节音量和静音，播放或暂停音乐，报时间和日期，查询设备信息，打开常见应用和系统设置。在设置里配置 AI 服务后，还能与你自由对话。"
            text.contains("谢谢") -> "不客气，随时找我。"
            else -> "抱歉，我还没有完全听明白。可以试试让我调节音量、报时间、打开应用，或者在设置里连接 AI 服务获得更强的对话能力。"
        }
    }
}
