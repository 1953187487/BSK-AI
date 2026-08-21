package com.bskai

import android.app.Application
import android.content.Intent
import com.bskai.core.settings.SettingsStore
import com.bskai.models.ModelStore
import com.bskai.models.ProviderStore

class BskApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var providerStore: ProviderStore
        private set
    lateinit var modelStore: ModelStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        providerStore = ProviderStore(this)
        modelStore = ModelStore(this)

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
        }
    }
}
