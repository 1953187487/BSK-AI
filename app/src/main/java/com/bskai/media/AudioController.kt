package com.bskai.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AudioController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: ExoPlayer? = null

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
        player?.next()
    }

    fun playPrevious() {
        player?.previous()
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.resume()
    }

    fun togglePlay() {
        if (player?.isPlaying == true) pause() else resume()
    }

    fun createPlayer(): ExoPlayer {
        player?.release()
        player = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
        return player!!
    }

    fun playUri(uri: android.net.Uri) {
        player?.stop()
        player?.setMediaItem(MediaItem.fromUri(uri))
        player?.prepare()
        player?.play()
    }

    fun release() {
        player?.release()
        player = null
    }
}
