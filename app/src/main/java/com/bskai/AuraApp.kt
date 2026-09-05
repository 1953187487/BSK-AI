package com.bskai

import android.app.Application
import android.content.Context
import com.bskai.agent.AgentEngine
import com.bskai.core.VoiceCoordinator
import com.bskai.data.SettingsRepository
import com.bskai.i18n.LocaleManager
import com.bskai.voice.VoiceEngine

class AuraApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var voice: VoiceEngine
        private set
    lateinit var agent: AgentEngine
        private set
    lateinit var coordinator: VoiceCoordinator
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        applyLocale()
        voice = VoiceEngine(this, settings)
        agent = AgentEngine(this, settings)
        coordinator = VoiceCoordinator(this, settings, voice, agent)
    }

    fun applyLocale() {
        LocaleManager.apply(this, settings)
    }

    companion object {
        fun of(context: Context): AuraApp = context.applicationContext as AuraApp
    }
}
