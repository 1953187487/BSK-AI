package com.bskai

import android.app.Application
import android.content.Context
import com.bskai.agent.AgentEngine
import com.bskai.core.VoiceCoordinator
import com.bskai.data.SettingsRepository
import com.bskai.intent.SkillEngine
import com.bskai.voice.VoiceEngine

class AuraApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var voice: VoiceEngine
        private set
    lateinit var skills: SkillEngine
        private set
    lateinit var agent: AgentEngine
        private set
    lateinit var coordinator: VoiceCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        skills = SkillEngine(this)
        voice = VoiceEngine(this, settings)
        agent = AgentEngine(this, settings, skills)
        coordinator = VoiceCoordinator(this, settings, voice, agent)
    }

    companion object {
        fun of(context: Context): AuraApp = context.applicationContext as AuraApp
    }
}
