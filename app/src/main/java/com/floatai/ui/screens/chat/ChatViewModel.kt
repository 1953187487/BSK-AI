package com.floatai.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatai.data.ChatRepository
import com.floatai.data.SettingsRepository
import com.floatai.data.defaultWelcome
import com.floatai.data.model.ApiConfig
import com.floatai.data.model.ChatHistory
import com.floatai.data.model.ChatMessage
import com.floatai.data.remote.ChatResult
import com.floatai.data.remote.OpenAiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _model = MutableStateFlow("auto")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _histories = MutableStateFlow<List<ChatHistory>>(emptyList())
    val histories: StateFlow<List<ChatHistory>> = _histories.asStateFlow()

    private val _currentHistoryId = MutableStateFlow<String?>(null)
    private val _currentTitle = MutableStateFlow("新对话")

    private var apiConfig: ApiConfig = ApiConfig()

    init {
        _model.value = settingsRepository.apiConfig.value.model.ifBlank { "auto" }
        refreshHistories()
        // 默认加载一个欢迎对话
        _messages.value = listOf(defaultWelcome())
    }

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun refreshConfig() {
        apiConfig = settingsRepository.apiConfig.value
    }

    fun setModel(model: String) {
        _model.value = model
        settingsRepository.updateApiConfig { it.copy(model = model) }
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty() || _loading.value) return

        refreshConfig()
        if (apiConfig.baseUrl.isBlank() || apiConfig.apiKey.isBlank()) {
            _messages.update {
                it + ChatMessage("assistant", "请先在「API 配置」页填写服务商地址和密钥")
            }
            return
        }

        _input.value = ""
        _messages.update { it + ChatMessage("user", text) }
        _loading.value = true

        viewModelScope.launch {
            val result = OpenAiClient.chatCompletions(
                baseUrl = apiConfig.baseUrl,
                apiKey = apiConfig.apiKey,
                model = _model.value,
                messages = _messages.value
            )
            _messages.update { msgs ->
                msgs + when (result) {
                    is ChatResult.Success -> ChatMessage("assistant", result.content)
                    is ChatResult.Error -> ChatMessage("assistant", result.message)
                }
            }
            _loading.value = false
            persistCurrent()
        }
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

    fun clearChat() {
        _messages.value = listOf(defaultWelcome())
        _currentHistoryId.value = null
        _currentTitle.value = "新对话"
    }

    fun clearAllHistories() {
        chatRepository.clearAll()
        refreshHistories()
        newChat()
    }

    private fun refreshHistories() {
        _histories.value = chatRepository.loadHistories()
    }

    private fun persistCurrent() {
        val msgs = _messages.value
        if (msgs.isEmpty()) return
        val title = if (_currentTitle.value.isNotBlank() && _currentTitle.value != "新对话") {
            _currentTitle.value
        } else {
            msgs.firstOrNull { it.role == "user" }?.content?.take(20) ?: "新对话"
        }
        val history = ChatHistory(
            id = _currentHistoryId.value ?: chatRepository.newId(),
            title = title,
            messages = msgs,
            time = System.currentTimeMillis()
        )
        chatRepository.saveHistory(history)
        _currentHistoryId.value = history.id
        _currentTitle.value = title
        refreshHistories()
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatViewModel(settingsRepository, chatRepository) }
        }
    }
}
