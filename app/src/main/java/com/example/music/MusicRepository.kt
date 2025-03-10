package com.example.music

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MusicRepository(private val apiService: ApiService, private val exoPlayer: ExoPlayer) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    suspend fun fetchSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            apiService.getSongs().data
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun playSong(song: Song) {
        val mediaItem = MediaItem.fromUri(song.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun releasePlayer() {
        exoPlayer.release()
    }

    val songProgress = flow {
        while (true) {
            if (exoPlayer.isPlaying || exoPlayer.playbackState == Player.STATE_READY) {
                val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(1L)
                emit(Pair(position, duration))
            } else {
                emit(Pair(0L, 1L))
            }
            kotlinx.coroutines.delay(1000)
        }
    }.flowOn(Dispatchers.Main)
}