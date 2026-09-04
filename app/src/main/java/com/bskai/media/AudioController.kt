package com.bskai.media

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.IOException

class AudioController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null

    val isMuted: Boolean get() = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    val currentVolume: Int get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVolume: Int get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val isMediaPlaying: Boolean get() = player?.isPlaying == true

    fun setVolume(level: Int) {
        val clamped = level.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
    }

    fun toggleMute() {
        audioManager.ringerMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            AudioManager.RINGER_MODE_NORMAL
        } else {
            AudioManager.RINGER_MODE_SILENT
        }
    }

    fun playNext() {
        // Placeholder for now - would need a playlist to implement
    }

    fun playPrevious() {
        // Placeholder for now - would need a playlist to implement
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.start()
    }

    fun togglePlay() {
        if (player?.isPlaying == true) pause() else startPlaying()
    }

    fun startPlaying() {
        try {
            player?.release()
            player = MediaPlayer().apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            player = null
        }
    }

    fun stopPlaying() {
        player?.stop()
        player?.release()
        player = null
    }

    fun release() {
        player?.release()
        player = null
    }
}
