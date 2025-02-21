package com.example.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        _uiState.value = _uiState.value.copy(
            currentSong = song,
            isPlaying = repository.isPlaying()
        )
    }

    fun togglePlayPause() {
        repository.togglePlayPause()
        _uiState.value = _uiState.value.copy(isPlaying = repository.isPlaying())
    }

    fun stopPlayback() {
        repository.stopPlayback()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    override fun onCleared() {
        super.onCleared()
        repository.releasePlayer()
    }
}