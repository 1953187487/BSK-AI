package com.bskai

import android.app.Application
import android.content.Intent
import com.bskai.settings.AppSettings
import com.bskai.settings.SettingsStore
import com.bskai.voice.VoiceEngine
import com.bskai.intent.IntentRegistry

class AuraApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var voiceEngine: VoiceEngine
        private set
    lateinit var intentRegistry: IntentRegistry
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        voiceEngine = VoiceEngine(this)
        intentRegistry = IntentRegistry(this, voiceEngine)

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
        }
    }
}
