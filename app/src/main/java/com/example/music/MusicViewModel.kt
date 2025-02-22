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
    val songDuration: Long = 1L,
    val sourceList: String? = null,
    val selectedTabIndex: Int = 0 // New: Tracks the current tab
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

    fun playSong(song: Song, sourceList: String) {
        repository.playSong(song)
        val tabIndex = if (sourceList == "ForYou") 0 else 1 // Map sourceList to tab index
        _uiState.value = _uiState.value.copy(
            currentSong = song,
            sourceList = sourceList,
            selectedTabIndex = tabIndex
        )
    }

    fun togglePlayPause() {
        repository.togglePlayPause()
    }

    fun playNextSong(allSongs: List<Song>, topTracks: List<Song>) {
        val currentSong = _uiState.value.currentSong ?: return
        val sourceList = _uiState.value.sourceList ?: "ForYou"
        val activeList = if (sourceList == "ForYou") allSongs else topTracks
        val currentIndex = activeList.indexOf(currentSong)
        val nextIndex = if (currentIndex >= activeList.size - 1) 0 else currentIndex + 1
        val nextSong = activeList[nextIndex]
        playSong(nextSong, sourceList)
    }

    fun playPreviousSong(allSongs: List<Song>, topTracks: List<Song>) {
        val currentSong = _uiState.value.currentSong ?: return
        val sourceList = _uiState.value.sourceList ?: "ForYou"
        val activeList = if (sourceList == "ForYou") allSongs else topTracks
        val currentIndex = activeList.indexOf(currentSong)
        val prevIndex = if (currentIndex <= 0) activeList.size - 1 else currentIndex - 1
        val prevSong = activeList[prevIndex]
        playSong(prevSong, sourceList)
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