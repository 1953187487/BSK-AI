package com.floatai.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatai.data.model.ApiConfig
import com.floatai.data.remote.OpenAiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApiUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "auto",
    val models: List<String> = emptyList(),
    val loading: Boolean = false,
    val message: String = ""
)

/**
 * 模型管理 ViewModel：原独立 API 页的全部能力，
 * 现作为 AI 聊天页的"管理模型"入口使用。
 */
class ApiConfigManager(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiUiState())
    val uiState: StateFlow<ApiUiState> = _uiState.asStateFlow()

    init {
        syncFromConfig()
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun onModelChange(value: String) {
        val normalized = value.trim().ifBlank { "auto" }
        _uiState.update { it.copy(model = normalized) }
        settingsRepository.updateApiConfig { it.copy(model = normalized) }
    }

    fun fetchModels() {
        val state = _uiState.value
        if (state.baseUrl.isBlank() || state.apiKey.isBlank()) {
            _uiState.update { it.copy(message = "请先填写 Base URL 和 API Key") }
            return
        }
        _uiState.update { it.copy(loading = true, message = "正在获取模型列表...") }
        viewModelScope.launch {
            val list = OpenAiClient.fetchModels(state.baseUrl.trim(), state.apiKey.trim())
            _uiState.update {
                it.copy(
                    loading = false,
                    models = list,
                    message = if (list.isEmpty()) "未能获取到模型，请检查地址和密钥"
                    else "获取到 ${list.size} 个模型"
                )
            }
            if (list.isNotEmpty()) {
                settingsRepository.updateApiConfig { it.copy(models = list) }
            }
        }
    }

    fun saveConfig() {
        val state = _uiState.value
        val saved = ApiConfig(
            baseUrl = state.baseUrl.trim(),
            apiKey = state.apiKey.trim(),
            model = state.model.trim().ifBlank { "auto" },
            models = state.models
        )
        settingsRepository.updateApiConfig { saved }
        _uiState.update { it.copy(message = "配置已保存") }
    }

    fun selectModel(model: String) {
        onModelChange(model)
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = "") }
    }

    private fun syncFromConfig() {
        val config = settingsRepository.apiConfig.value
        _uiState.value = ApiUiState(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model.ifBlank { "auto" },
            models = config.models
        )
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ApiConfigManager(settingsRepository) }
            }
    }
}
