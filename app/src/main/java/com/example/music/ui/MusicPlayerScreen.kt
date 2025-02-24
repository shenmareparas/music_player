package com.example.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val hapticFeedback = LocalHapticFeedback.current

    // Handle system back press to close full-screen player
    BackHandler(enabled = showFullScreenPlayer) {
        showFullScreenPlayer = false
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize() // Ensure the Box fills the entire screen
    ) {
        // Main content (song list, mini player, tabs)
        Column(
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
                .padding(vertical = 16.dp)
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

                    // Tab state synced with UiState
                    var selectedTabIndex by remember { mutableIntStateOf(uiState.selectedTabIndex) }
                    val tabs = listOf("For You", "Top Tracks")

                    // Song list
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val songsToShow = if (selectedTabIndex == 0) allSongs else topTracks
                        items(songsToShow) { song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    viewModel.playSong(
                                        song,
                                        if (selectedTabIndex == 0) "ForYou" else "TopTracks"
                                    )
                                },
                                isPlaying = uiState.currentSong == song && uiState.isPlaying
                            )
                        }
                    }

                    // Mini player (Now Playing view) stretching edge-to-edge
                    uiState.currentSong?.let { song ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            NowPlayingView(
                                song = song,
                                isPlaying = uiState.isPlaying,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onClick = { showFullScreenPlayer = true }
                            )
                        }
                    }

                    // TabRow at the bottom with navigation bar padding
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 0.dp),
                        indicator = { },
                        divider = { }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                text = { Text(title) },
                                selected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Animated full-screen player sliding up/down to fill the entire screen
        AnimatedVisibility(
            visible = showFullScreenPlayer && uiState.currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight }, // Slide up from bottom
                animationSpec = tween(durationMillis = 300) // Duration in milliseconds
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight }, // Slide down to bottom
                animationSpec = tween(durationMillis = 300) // Duration in milliseconds
            ),
            modifier = Modifier.fillMaxSize() // Ensure full-screen coverage during animation
        ) {
            if (uiState.currentSong != null) {
                FullScreenPlayer(
                    song = uiState.currentSong,
                    isPlaying = uiState.isPlaying,
                    onPrevious = { viewModel.playPreviousSong(uiState.songs, uiState.songs.filter { it.top_track }) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNextSong(uiState.songs, uiState.songs.filter { it.top_track }) },
                    onDismiss = { showFullScreenPlayer = false },
                    position = uiState.songPosition,
                    duration = uiState.songDuration
                )
            }
        }
    }
}