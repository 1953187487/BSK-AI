package com.bskai.ui.viewmodel

import android.app.Application
import com.bskai.AuraApp
import com.bskai.agent.AgentEngine
import com.bskai.files.FileController
import com.bskai.intent.IntentRegistry
import com.bskai.media.AudioController
import com.bskai.voice.PermissionManager
import com.bskai.voice.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : androidx.lifecycle.AndroidViewModel(app) {

    private val auraApp = app as AuraApp
    private val voiceEngine = auraApp.voiceEngine
    private val intentRegistry = auraApp.intentRegistry
    private val settingsStore = auraApp.settingsStore
    private val fileController = FileController(app)
    private val audioController = AudioController(app)

    private val _scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _conversationHistory = MutableStateFlow<List<String>>(emptyList())
    val conversationHistory: StateFlow<List<String>> = _conversationHistory

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse

    private val _permissionManager = PermissionManager(app)
    val permissionManager: PermissionManager = _permissionManager

    private val _agent = AgentEngine(app, voiceEngine, intentRegistry, fileController, audioController)

    init {
        voiceEngine.onRecognized = { text ->
            if (text.isNotEmpty()) {
                _scope.launch { processCommand(text) }
            }
        }
        voiceEngine.onError = { error ->
            _isListening.value = false
            _currentResponse.value = "错误: $error"
        }
        voiceEngine.onSpeaking = { speaking ->
            _isSpeaking.value = speaking
        }
        _agent.onResult = { result ->
            _currentResponse.value = result
        }
    }

    fun startListening() {
        _isListening.value = true
        voiceEngine.startListening()
    }

    fun stopListening() {
        _isListening.value = false
        voiceEngine.stopListening()
    }

    private suspend fun processCommand(text: String) {
        _conversationHistory.value = _conversationHistory.value + text
        if (_conversationHistory.value.size > 50) {
            _conversationHistory.value = _conversationHistory.value.drop(_conversationHistory.value.size - 50)
        }
        val response = _agent.processVoiceInput(text)
        _currentResponse.value = response
    }

    fun clearHistory() {
        _conversationHistory.value = emptyList()
        _currentResponse.value = ""
    }

    fun checkPermissions(): Boolean = _permissionManager.hasAllPermissions()

    fun requestPermissions() {
        _permissionManager.requestMultiplePermissions(
            _permissionManager.requiredPermissions.map { it.permission }
        )
    }

    fun shutdown() {
        voiceEngine.shutdown()
        audioController.release()
    }

    class Factory(private val app: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MainViewModel(app) as T
    }
}
