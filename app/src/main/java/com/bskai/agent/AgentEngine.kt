package com.bskai.agent

import android.content.Context
import com.bskai.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class AgentEngine(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val settings: SettingsRepository
) {

    private val llm = LlmClient(context)

    private val _conversation = MutableStateFlow<List<ChatMsg>>(emptyList())
    val conversation: StateFlow<List<ChatMsg>> = _conversation.asStateFlow()

    /**
     * 用户发言入口。先入栈 user 消息，再启动一次 assistant 回复。
     * 流式 chat：先把 assistant 占位空消息放入栈，每收到一段 delta 就替换最后一条的 content。
     */
    suspend fun answer(userText: String): String {
        val text = userText.trim()
        if (text.isEmpty()) return "请再说一遍"

        val s = settings.settings.value
        append(ChatMsg("user", text))

        if (!s.apiConfigured) {
            val fallback = localReply(text)
            append(ChatMsg("assistant", fallback))
            return fallback
        }

        // 占位 assistant，content="" 会随流式更新
        val placeholderIndex = appendAndReturnIndex(ChatMsg("assistant", ""))
        val sb = StringBuilder()

        return try {
            val history = recentMessages(s.maxHistoryLength)
            llm.chatStream(s, history).collect { ev ->
                when (ev) {
                    is LlmClient.StreamEvent.Delta -> {
                        sb.append(ev.text)
                        updateLastAssistant(sb.toString())
                    }
                    is LlmClient.StreamEvent.Done -> {
                        val final = ev.fullContent.ifEmpty { sb.toString() }
                        val clean = final.ifEmpty { "抱歉，我没有想好怎么回答。" }
                        updateLastAssistant(clean)
                    }
                    is LlmClient.StreamEvent.Error -> {
                        if (sb.isEmpty()) {
                            val fallback = localReply(text)
                            updateLastAssistant(fallback)
                        }
                    }
                    else -> Unit
                }
            }
            sb.toString().ifEmpty { "抱歉，我没有想好怎么回答。" }
        } catch (_: Exception) {
            if (sb.isEmpty()) {
                val fallback = localReply(text)
                updateLastAssistant(fallback)
                fallback
            } else sb.toString()
        }
    }

    fun notifyAssistant(text: String) {
        append(ChatMsg("assistant", text))
    }

    fun recentMessages(max: Int): List<ChatMsg> {
        val system = ChatMsg(
            "system",
            "你是 AURA，一位手机语音助手。请用简体中文简洁回答，语气友好。"
        )
        val history = _conversation.value.takeLast(max.coerceAtLeast(2))
        return listOf(system) + history
    }

    private fun append(msg: ChatMsg) {
        val max = settings.settings.value.maxHistoryLength
        _conversation.value = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
    }

    private fun appendAndReturnIndex(msg: ChatMsg): Int {
        val max = settings.settings.value.maxHistoryLength
        val list = (_conversation.value + msg).takeLast(max.coerceAtLeast(10))
        _conversation.value = list
        return list.size - 1
    }

    private fun updateLastAssistant(content: String) {
        val list = _conversation.value.toMutableList()
        for (i in list.indices.reversed()) {
            if (list[i].role == "assistant") {
                list[i] = list[i].copy(content = content)
                _conversation.value = list
                return
            }
        }
    }

    private fun localReply(text: String): String {
        val s = settings.settings.value
        val tail = if (s.apiConfigured) "" else "\n\n提示：当前未配置 AI 服务，请到设置中填入 API。\n您仍可以按住说话，告诉我您要做什么。"

        val base = when {
            text.contains("你好") || text.contains("您好") || text.contains("hi") ||
                text.contains("hello") || text.contains("嗨") ->
                "你好呀，我是 AURA。按住说话就可以跟我聊天。"
            text.contains("你是谁") || text.contains("介绍你自己") ->
                "我是 AURA，本地语音助手。可以听你说话，连接 AI 服务后还能陪你聊天和回答各种问题。"
            text.contains("你能做什么") || text.contains("你会什么") || text.contains("有什么功能") ->
                "在设置中配置 AI 服务后，我可以陪你自由对话，回答问题。"
            text.contains("谢谢") -> "不客气，随时找我。"
            else -> "抱歉，我还没有完全听明白。在设置里配置 AI 服务后我能更好地回答您。"
        }
        return base + tail
    }
}
