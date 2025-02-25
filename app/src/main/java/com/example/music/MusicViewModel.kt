package com.example.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val songs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val songPosition: Long = 0L,
    val songDuration: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTabIndex: Int = 0, // 0 for "For You", 1 for "Top Tracks"
    val sourceList: String? = "ForYou" // "ForYou" or "TopTracks"
)

class MusicViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadSongs()
        observePlaybackState()
        observeProgress()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val songs = repository.fetchSongs()
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load songs: ${e.message}") }
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            repository.isPlaying.collectLatest { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            repository.songProgress.collectLatest { (position, duration) ->
                _uiState.update {
                    it.copy(
                        songPosition = position,
                        songDuration = duration
                    )
                }
            }
        }
    }

    fun playSong(song: Song, source: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(sourceList = source, currentSong = song) }
            repository.playSong(song)
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            repository.togglePlayPause()
        }
    }

    fun playPreviousSong(allSongs: List<Song>, topTracks: List<Song>) {
        viewModelScope.launch {
            val activeList = if (_uiState.value.sourceList == "ForYou") allSongs else topTracks
            val currentSong = _uiState.value.currentSong ?: return@launch
            val currentIndex = activeList.indexOf(currentSong)
            val newIndex = if (currentIndex <= 0) activeList.size - 1 else currentIndex - 1
            val previousSong = activeList[newIndex]
            playSong(previousSong, _uiState.value.sourceList ?: "ForYou")
        }
    }

    fun playNextSong(allSongs: List<Song>, topTracks: List<Song>) {
        viewModelScope.launch {
            val activeList = if (_uiState.value.sourceList == "ForYou") allSongs else topTracks
            val currentSong = _uiState.value.currentSong ?: return@launch
            val currentIndex = activeList.indexOf(currentSong)
            val newIndex = if (currentIndex >= activeList.size - 1) 0 else currentIndex + 1
            val nextSong = activeList[newIndex]
            playSong(nextSong, _uiState.value.sourceList ?: "ForYou")
        }
    }
}