package com.floatai.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatai.data.ChatRepository
import com.floatai.data.CharacterRepository
import com.floatai.data.SettingsRepository
import com.floatai.data.model.ChatHistory
import com.floatai.data.model.ChatMessage
import com.floatai.data.remote.ChatStreamEvent
import com.floatai.data.remote.OpenAiClient
import com.floatai.tools.ToolRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * AI 聊天 ViewModel v1.0.5：
 *  - 使用 SSE 流式输出，UI 实时看到内容滚动（修复"卡顿"）
 *  - 支持 tool_calls：AI 可调用小龙虾/数学/时间等工具
 *  - 角色集成：每次发送带上当前激活角色的 systemPrompt + greeting
 *  - 默认欢迎语：首条消息用角色 greeting（不调用 API）
 */
class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(defaultWelcome()))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _model = MutableStateFlow(settingsRepository.apiConfig.value.model.ifBlank { "auto" })
    val model: StateFlow<String> = _model.asStateFlow()

    private val _availableModels = MutableStateFlow(settingsRepository.apiConfig.value.models)
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _histories = MutableStateFlow<List<ChatHistory>>(emptyList())
    val histories: StateFlow<List<ChatHistory>> = _histories.asStateFlow()

    private val _currentHistoryId = MutableStateFlow<String?>(null)
    val currentHistoryId: StateFlow<String?> = _currentHistoryId.asStateFlow()

    private val _currentTitle = MutableStateFlow("新对话")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    val activeCharacter = characterRepository.activeId
    val characters = characterRepository.characters

    private var streamJob: Job? = null

    fun setInput(text: String) { _input.value = text }

    fun selectModel(m: String) {
        _model.value = m
        // 立即持久化（用户切换模型即写入默认）
        settingsRepository.updateApiConfig { it.copy(model = m) }
    }

    fun updateAvailableModels(list: List<String>) {
        _availableModels.value = list
    }

    fun newChat() {
        _messages.value = listOf(defaultWelcome())
        _currentHistoryId.value = null
        _currentTitle.value = "新对话"
    }

    fun loadHistory(history: ChatHistory) {
        _messages.value = history.messages.ifEmpty { listOf(defaultWelcome()) }
        _currentHistoryId.value = history.id
        _currentTitle.value = history.title
    }

    fun deleteHistory(history: ChatHistory) {
        chatRepository.deleteHistory(history.id)
        refreshHistories()
        if (_currentHistoryId.value == history.id) newChat()
    }

    fun clearAllHistories() {
        chatRepository.clearAll()
        refreshHistories()
        newChat()
    }

    fun refreshHistories() {
        viewModelScope.launch {
            _histories.value = chatRepository.loadHistories()
        }
    }

    fun clearChat() {
        _messages.value = listOf(defaultWelcome())
        _currentHistoryId.value = null
    }

    fun onCharacterChanged(id: String) {
        characterRepository.setActive(id)
        // 切换角色后用新 greeting 重置欢迎语
        val c = characterRepository.active()
        _messages.value = listOf(
            ChatMessage(
                role = "assistant",
                content = c.greeting.ifBlank { "你好，我是 ${c.name}。有什么可以帮你的？" },
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * 发送消息：流式输出，UI 实时刷新最后一条 assistant 消息。
     */
    fun send() {
        val text = _input.value.trim()
        if (text.isBlank() || _loading.value) return
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            // 1. 准备消息列表（加上角色 systemPrompt）
            val character = characterRepository.active()
            val historyMsgs = _messages.value.filter {
                // 不发送本地欢迎语给 AI（避免重复）
                it.role != "assistant" || !it.content.startsWith("你好")
            }
            val systemMsg = ChatMessage("system", character.systemPrompt, 0)
            val userMsg = ChatMessage("user", text, System.currentTimeMillis())
            _messages.update { it + userMsg }
            _input.value = ""
            _loading.value = true

            // 2. 准备 API 参数
            val api = settingsRepository.apiConfig.value
            if (api.apiKey.isBlank() || api.baseUrl.isBlank()) {
                _messages.update { it + ChatMessage("assistant", "请先在「设置 → AI 配置」中填写 Base URL 与 API Key。", System.currentTimeMillis()) }
                _loading.value = false
                return@launch
            }

            // 3. 流式调用 + 增量渲染
            val requestMsgs = listOf(systemMsg) + historyMsgs + userMsg
            val placeholder = ChatMessage("assistant", "", System.currentTimeMillis())
            _messages.update { it + placeholder }

            val sb = StringBuilder()
            try {
                OpenAiClient.chatCompletionsStream(
                    baseUrl = api.baseUrl,
                    apiKey = api.apiKey,
                    model = _model.value.ifBlank { api.model.ifBlank { "auto" } },
                    messages = requestMsgs,
                    temperature = character.temperature,
                    tools = ToolRegistry.allDefs().map { it.toJson() }
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> {
                            sb.append(event.text)
                            val acc = sb.toString()
                            _messages.update { list ->
                                list.toMutableList().also {
                                    it[it.lastIndex] = placeholder.copy(content = acc)
                                }
                            }
                        }
                        is ChatStreamEvent.ToolCall -> {
                            // 工具调用：执行并将结果作为后续消息继续
                            val result = ToolRegistry.call(event.name, event.args)
                            // 把工具调用结果反馈给 AI
                            val toolMsg = ChatMessage("tool", "tool=${event.name}\nresult=$result", System.currentTimeMillis())
                            val req2 = requestMsgs + placeholder.copy(content = sb.toString()) + toolMsg
                            // 第二轮流式（不带 tools，避免无限循环）
                            OpenAiClient.chatCompletionsStream(
                                baseUrl = api.baseUrl,
                                apiKey = api.apiKey,
                                model = _model.value.ifBlank { api.model.ifBlank { "auto" } },
                                messages = req2,
                                temperature = character.temperature
                            ).collect { ev ->
                                when (ev) {
                                    is ChatStreamEvent.Delta -> {
                                        sb.append(ev.text)
                                        val acc = sb.toString()
                                        _messages.update { list ->
                                            list.toMutableList().also {
                                                it[it.lastIndex] = placeholder.copy(content = acc)
                                            }
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is ChatStreamEvent.Error -> {
                            sb.append("\n\n[错误] ${event.message}")
                            val acc = sb.toString()
                            _messages.update { list ->
                                list.toMutableList().also {
                                    it[it.lastIndex] = placeholder.copy(content = acc)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                _messages.update { list ->
                    list.toMutableList().also {
                        it[it.lastIndex] = placeholder.copy(content = "[网络错误] ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            } finally {
                _loading.value = false
                persistCurrent()
            }
        }
    }

    private fun persistCurrent() {
        viewModelScope.launch {
            val msgs = _messages.value
            if (msgs.isEmpty()) return@launch
            val id = _currentHistoryId.value ?: run {
                val newId = java.util.UUID.randomUUID().toString().take(8)
                _currentHistoryId.value = newId
                newId
            }
            // 自动用首条用户消息作为标题
            val firstUser = msgs.firstOrNull { it.role == "user" }?.content?.take(20)
            val title = firstUser ?: _currentTitle.value
            _currentTitle.value = title
            chatRepository.saveHistory(
                ChatHistory(
                    id = id,
                    title = title,
                    messages = msgs,
                    time = System.currentTimeMillis()
                )
            )
            refreshHistories()
        }
    }

    /**
     * 切换角色后获取对应 greeting 作为首条消息。
     */
    private fun defaultWelcome(): ChatMessage {
        val c = characterRepository.active()
        return ChatMessage(
            role = "assistant",
            content = c.greeting.ifBlank { "你好，我是 ${c.name}。有什么可以帮你的？" },
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            chatRepository: ChatRepository,
            characterRepository: CharacterRepository
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ChatViewModel(settingsRepository, chatRepository, characterRepository)
                }
            }
    }
}
