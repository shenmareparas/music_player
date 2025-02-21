package com.example.music

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val apiService: ApiService, private val exoPlayer: ExoPlayer) {
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

    fun stopPlayback() {
        exoPlayer.stop()
    }

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    fun releasePlayer() {
        exoPlayer.release()
    }
}