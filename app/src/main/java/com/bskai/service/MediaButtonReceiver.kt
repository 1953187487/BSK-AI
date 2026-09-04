package com.bskai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = intent.getParcelableExtra(android.view.KeyEvent>(Intent.EXTRA_KEY_EVENT)
                ?: return
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        val app = context.applicationContext as? AuraApp ?: return
                        val voiceEngine = app.voiceEngine
                        if (VoiceService.isRunning.value) {
                            voiceEngine.stopBackgroundListening()
                        } else {
                            voiceEngine.startListening()
                        }
                    }
                }
            }
        }
    }
}
