package com.floatai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatai.data.SettingsRepository
import com.floatai.data.model.AppSettings
import com.floatai.data.model.UpdateInfo
import com.floatai.data.remote.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val notice: UpdateInfo? = null,
    val message: String = ""
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = settingsRepository.settings
    val settings: StateFlow<AppSettings> = _settings

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    fun setDarkTheme(value: Boolean) {
        settingsRepository.updateSettings { it.copy(darkTheme = value) }
    }

    fun setDynamicColor(value: Boolean) {
        settingsRepository.updateSettings { it.copy(dynamicColor = value) }
    }

    fun setAccentColor(name: String) {
        settingsRepository.updateSettings { it.copy(accentColor = name) }
    }

    fun setFloatEnabled(value: Boolean) {
        settingsRepository.updateSettings { it.copy(floatEnabled = value) }
    }

    fun setLanguage(lang: com.floatai.data.model.AppLanguage) {
        settingsRepository.updateSettings { it.copy(language = lang) }
    }

    fun setGithubToken(token: String) {
        settingsRepository.updateSettings { it.copy(githubToken = token) }
    }

    fun checkUpdate(currentTag: String) {
        _update.update { it.copy(checking = true, message = "") }
        viewModelScope.launch {
            val info = UpdateRepository.checkLatest(currentTag)
            _update.update {
                when {
                    info.latestTag.isEmpty() -> it.copy(checking = false, message = info.changelog)
                    info.isNewer -> it.copy(checking = false, notice = info)
                    else -> it.copy(checking = false, message = "当前版本已是最新 (${info.latestTag})")
                }
            }
        }
    }

    fun dismissUpdate() {
        _update.update { it.copy(notice = null) }
    }

    fun clearMessage() {
        _update.update { it.copy(message = "") }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(settingsRepository) }
            }
    }
}
