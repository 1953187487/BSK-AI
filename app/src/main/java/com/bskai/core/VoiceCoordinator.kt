package com.bskai.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bskai.agent.AgentEngine
import com.bskai.data.SettingsRepository
import com.bskai.voice.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceCoordinator(
    private val context: Context,
    private val settings: SettingsRepository,
    private val voice: VoiceEngine,
    private val agent: AgentEngine
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    init {
        voice.onFinalResult = { handleSpeech(it) }
        voice.onError = { handleError(it) }
    }

    fun listenNow() {
        if (!_processing.value) {
            voice.startListening()
        }
    }

    fun stopListening() {
        voice.stopListening()
    }

    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.startsWith("/")) {
            handleSlash(trimmed)
            return
        }
        handleSpeech(trimmed)
    }

    private fun handleSlash(command: String) {
        val registry = agent.slashRegistry
        if (registry == null) {
            agent.notifyAssistant("斜杠命令暂不可用")
            return
        }
        val key = command.substringBefore(" ").removePrefix("/")
        val arg = command.substringAfter(" ", "").trim()
        val cmd = registry.get(key)
        if (cmd == null) {
            agent.notifyAssistant("未知命令 /$key，输入 /help 查看可用命令")
            return
        }
        when (val outcome = cmd.resolve(arg)) {
            is com.bskai.agent.slash.SlashOutcome.LocalMessage ->
                agent.notifyAssistant(outcome.message)
            is com.bskai.agent.slash.SlashOutcome.SendToAi -> {
                if (outcome.note != null) agent.notifyAssistant(outcome.note)
                handleSpeech(outcome.text)
            }
            is com.bskai.agent.slash.SlashOutcome.Cancel -> Unit
        }
    }

    private fun handleSpeech(text: String) {
        if (text.isBlank()) return
        scope.launch {
            _processing.value = true
            val reply = try {
                agent.answer(text)
            } catch (_: Exception) {
                agent.notifyAssistant("处理出错了，请稍后再试")
                "处理出错了，请稍后再试"
            }
            _processing.value = false
            respond(reply)
        }
    }

    private fun handleError(message: String) {
        scope.launch {
            if (message == "没有听清，请再说一遍") return@launch
            agent.notifyAssistant(message)
            respond(message)
        }
    }

    private fun respond(reply: String) {
        voice.speak(reply)
        if (settings.settings.value.vibrateOnResponse) {
            vibrate()
        }
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }
}
