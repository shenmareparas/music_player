package com.example.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UiState(
    val songs: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MusicViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState

    init {
        fetchSongs()
        // Collect playback state changes
        viewModelScope.launch {
            repository.isPlaying.collectLatest { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }
    }

    private fun fetchSongs() {
        viewModelScope.launch {
            val songs = repository.fetchSongs()
            _uiState.value = _uiState.value.copy(
                songs = songs,
                isLoading = false,
                error = if (songs.isEmpty()) "Failed to load songs" else null
            )
        }
    }

    fun playSong(song: Song) {
        repository.playSong(song)
        _uiState.value = _uiState.value.copy(currentSong = song)
        // No need to set isPlaying here; the listener handles it
    }

    fun togglePlayPause() {
        repository.togglePlayPause()
        // No need to update isPlaying manually; the listener will trigger
    }

    fun playNextSong(allSongs: List<Song>) {
        val currentIndex = allSongs.indexOf(_uiState.value.currentSong)
        if (currentIndex >= 0 && currentIndex < allSongs.size - 1) {
            val nextSong = allSongs[currentIndex + 1]
            playSong(nextSong)
        }
    }

    fun playPreviousSong(allSongs: List<Song>) {
        val currentIndex = allSongs.indexOf(_uiState.value.currentSong)
        if (currentIndex > 0) {
            val prevSong = allSongs[currentIndex - 1]
            playSong(prevSong)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.releasePlayer()
    }
}