package com.bskai.agent

import android.content.Context
import com.bskai.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple coordinator that bridges UI input to AgentEngine.
 * Replaces the old VoiceCoordinator (which was voice-specific).
 */
class Coordinator(
    private val settings: SettingsRepository,
    private val agent: AgentEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun submit(text: String) {
        scope.launch {
            agent.answer(text)
        }
    }
}
