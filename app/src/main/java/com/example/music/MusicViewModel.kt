package com.example.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    val error: String? = null,
    val songPosition: Long = 0L,
    val songDuration: Long = 1L
)

class MusicViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState

    init {
        fetchSongs()
        viewModelScope.launch {
            repository.isPlaying.collectLatest { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }
        viewModelScope.launch {
            repository.songProgress.collectLatest { (position, duration) ->
                _uiState.value = _uiState.value.copy(
                    songPosition = position,
                    songDuration = duration
                )
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
    }

    fun togglePlayPause() {
        repository.togglePlayPause()
    }

    fun playNextSong(allSongs: List<Song>) {
        val currentIndex = allSongs.indexOf(_uiState.value.currentSong)
        val nextIndex = if (currentIndex >= allSongs.size - 1) 0 else currentIndex + 1 // Cycle to first if at last
        val nextSong = allSongs[nextIndex]
        playSong(nextSong)
    }

    fun playPreviousSong(allSongs: List<Song>) {
        val currentIndex = allSongs.indexOf(_uiState.value.currentSong)
        val prevIndex = if (currentIndex <= 0) allSongs.size - 1 else currentIndex - 1 // Cycle to last if at first
        val prevSong = allSongs[prevIndex]
        playSong(prevSong)
    }

    override fun onCleared() {
        super.onCleared()
        repository.releasePlayer()
    }
}

class MusicViewModelFactory(private val repository: MusicRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}