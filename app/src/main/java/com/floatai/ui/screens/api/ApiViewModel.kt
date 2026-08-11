package com.floatai.ui.screens.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatai.data.SettingsRepository
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
    val message: String = "填写服务商信息后点击「获取模型」"
)

class ApiViewModel(
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
        _uiState.update { it.copy(model = value) }
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
        }
    }

    fun saveConfig() {
        val state = _uiState.value
        settingsRepository.updateApiConfig {
            ApiConfig(
                baseUrl = state.baseUrl.trim(),
                apiKey = state.apiKey.trim(),
                model = state.model.trim().ifBlank { "auto" }
            )
        }
        _uiState.update { it.copy(message = "配置已保存") }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(model = model) }
        settingsRepository.updateApiConfig { it.copy(model = model) }
    }

    private fun syncFromConfig() {
        val config = settingsRepository.apiConfig.value
        _uiState.value = ApiUiState(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model.ifBlank { "auto" }
        )
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ApiViewModel(settingsRepository) }
            }
    }
}
