package com.example.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.music.MusicRepository
import com.example.music.MusicViewModel
import com.example.music.MusicViewModelFactory

@Composable
fun MusicPlayerScreen(
    repository: MusicRepository,
    viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory(repository))
) {
    val uiState = viewModel.uiState.collectAsState().value
    var showFullScreenPlayer by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.background(Color.Black)
    ) {
        if (showFullScreenPlayer && uiState.currentSong != null) {
            FullScreenPlayer(
                song = uiState.currentSong,
                allSongs = uiState.songs,
                isPlaying = uiState.isPlaying,
                onPrevious = { viewModel.playPreviousSong(uiState.songs) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNextSong(uiState.songs) },
                onDismiss = { showFullScreenPlayer = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()) // Add status bar padding
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {
                        val allSongs = uiState.songs
                        val topTracks = uiState.songs.filter { it.top_track }

                        // Tab state
                        var selectedTabIndex by remember { mutableIntStateOf(0) }
                        val tabs = listOf("For You", "Top Tracks")

                        // Song list
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            val songsToShow = if (selectedTabIndex == 0) allSongs else topTracks
                            items(songsToShow) { song ->
                                SongItem(
                                    song = song,
                                    onClick = { viewModel.playSong(song) },
                                    isPlaying = uiState.currentSong == song && uiState.isPlaying
                                )
                            }
                        }

                        // Mini player (Now Playing view)
                        uiState.currentSong?.let { song ->
                            NowPlayingView(
                                song = song,
                                allSongs = uiState.songs,
                                isPlaying = uiState.isPlaying,
                                onPrevious = { viewModel.playPreviousSong(uiState.songs) },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.playNextSong(uiState.songs) },
                                onClick = { showFullScreenPlayer = true }
                            )
                        }

                        // TabRow at the bottom
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            modifier = Modifier.padding(top = 8.dp),
                            indicator = { },
                            divider = { }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}