package com.bskai.media

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager

class AudioController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun musicMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    fun musicVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    fun setMusicVolume(level: Int) {
        val max = musicMaxVolume()
        val safe = level.coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safe, 0)
    }

    fun setMusicVolumePercent(percent: Int) {
        val max = musicMaxVolume()
        setMusicVolume(max * percent.coerceIn(0, 100) / 100)
    }

    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun isMuted(): Boolean {
        return try {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) {
            false
        }
    }

    fun toggleMute(): Boolean {
        return if (isMuted()) {
            unmute()
            false
        } else {
            mute()
            true
        }
    }

    private fun mute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            0
        )
    }

    private fun unmute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            0
        )
    }

    fun tryToggleMediaPlayPause(context: Context) {
        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(null)
            if (sessions.isEmpty()) return
            val controller: MediaController = sessions[0]
            if (controller.playbackState?.isActive == true) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } catch (_: Exception) {
        }
    }
}
