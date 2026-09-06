package com.bskai.music

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long = 0,
    val uri: String,
    val artworkUri: String? = null,
    val isStreaming: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }

enum class PlayerState { IDLE, BUFFERING, READY, ENDED }

class MusicEngine(context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private var updateJob: kotlinx.coroutines.Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionUpdates()
                else stopPositionUpdates()
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playerState.value = when (state) {
                    Player.STATE_BUFFERING -> PlayerState.BUFFERING
                    Player.STATE_READY -> PlayerState.READY
                    Player.STATE_ENDED -> PlayerState.ENDED
                    else -> PlayerState.IDLE
                }
                if (state == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0)
                }
                if (state == Player.STATE_ENDED) {
                    when (_repeatMode.value) {
                        RepeatMode.ONE -> exoPlayer.seekTo(0)
                        RepeatMode.ALL -> {
                            if (_currentIndex.value < _queue.value.size - 1) {
                                skipToNext()
                            } else {
                                exoPlayer.seekTo(0)
                            }
                        }
                        RepeatMode.OFF -> {
                            if (_currentIndex.value < _queue.value.size - 1) {
                                skipToNext()
                            }
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                _currentIndex.value = index
                _currentTrack.value = _queue.value.getOrNull(index)
            }
        })
    }

    fun playTrack(track: Track) {
        val currentQueue = _queue.value
        val index = currentQueue.indexOfFirst { it.id == track.id }
        if (index >= 0) {
            _currentIndex.value = index
            exoPlayer.seekTo(index, 0)
        } else {
            val newQueue = currentQueue + track
            _queue.value = newQueue
            exoPlayer.setMediaItems(newQueue.map { toMediaItem(it) })
            _currentIndex.value = newQueue.size - 1
            exoPlayer.seekTo(newQueue.size - 1, 0)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _queue.value = tracks
        exoPlayer.setMediaItems(tracks.map { toMediaItem(it) })
        exoPlayer.prepare()
        exoPlayer.seekTo(startIndex, 0)
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun skipToNext() {
        val nextIndex = exoPlayer.nextMediaItemIndex
        if (nextIndex != androidx.media3.common.C.INDEX_UNSET) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
        } else {
            val prevIndex = exoPlayer.previousMediaItemIndex
            if (prevIndex != androidx.media3.common.C.INDEX_UNSET) {
                exoPlayer.seekToPreviousMediaItem()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceIn(0, exoPlayer.duration)
        exoPlayer.seekTo(newPos)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleShuffle() {
        val newValue = !_shuffle.value
        _shuffle.value = newValue
        exoPlayer.shuffleModeEnabled = newValue
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun getCurrentProgress(): Float {
        val dur = _duration.value
        return if (dur > 0) _position.value.toFloat() / dur else 0f
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        val scope = CoroutineScope(Dispatchers.Main)
        updateJob = scope.launch {
            while (true) {
                _position.value = exoPlayer.currentPosition.coerceAtLeast(0)
                _duration.value = exoPlayer.duration.coerceAtLeast(0)
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun toMediaItem(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    fun release() {
        stopPositionUpdates()
        exoPlayer.release()
    }
}
