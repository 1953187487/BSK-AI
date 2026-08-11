package com.floatai

import android.app.Application
import android.content.Intent
import com.floatai.data.ChatRepository
import com.floatai.data.SettingsRepository

class App : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        chatRepository = ChatRepository(this)

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            // 崩溃兜底：记录日志，重启到 MainActivity，避免直接闪退
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
        }
    }
}
