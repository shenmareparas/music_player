package com.example.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.music.MusicRepository
import com.example.music.MusicViewModel
import com.example.music.MusicViewModelFactory
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicPlayerScreen(
    repository: MusicRepository,
    viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory(repository))
) {
    val uiState = viewModel.uiState.collectAsState().value
    var playerState by remember { mutableStateOf(PlayerState.Mini) } // Track player state
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Handle system back press to minimize full-screen player
    BackHandler(enabled = playerState == PlayerState.FullScreen) {
        playerState = PlayerState.Mini
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
        ) {
            when {
                uiState.isLoading -> {
                    ShimmerLoadingList()
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

                    val tabs = listOf("For You", "Top Tracks")
                    val pagerState = rememberPagerState(
                        initialPage = uiState.selectedTabIndex,
                        initialPageOffsetFraction = 0f
                    ) {
                        tabs.size
                    }
                    val coroutineScope = rememberCoroutineScope()

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        // Song list
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            val songsToShow = if (page == 0) allSongs else topTracks
                            items(songsToShow) { song ->
                                SongItem(
                                    song = song,
                                    onClick = {
                                        viewModel.playSong(
                                            song,
                                            if (page == 0) "ForYou" else "TopTracks"
                                        )
                                    }
                                )
                            }
                        }
                    }

                    val transition =
                        updateTransition(targetState = playerState, label = "PlayerTransition")
                    transition.AnimatedContent { state ->
                        when (state) {
                            PlayerState.Mini -> {
                                uiState.currentSong?.let { song ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        NowPlayingView(
                                            song = song,
                                            isPlaying = uiState.isPlaying,
                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                            onClick = { playerState = PlayerState.FullScreen },
                                            onPrevious = {
                                                viewModel.playPreviousSong(
                                                    uiState.songs,
                                                    uiState.songs.filter { it.top_track }
                                                )
                                            },
                                            onNext = {
                                                viewModel.playNextSong(
                                                    uiState.songs,
                                                    uiState.songs.filter { it.top_track }
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            PlayerState.FullScreen -> {
                                uiState.currentSong?.let { song ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        FullScreenPlayer(
                                            song = song,
                                            isPlaying = uiState.isPlaying,
                                            onPrevious = {
                                                viewModel.playPreviousSong(
                                                    uiState.songs,
                                                    uiState.songs.filter { it.top_track }
                                                )
                                            },
                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                            onNext = {
                                                viewModel.playNextSong(
                                                    uiState.songs,
                                                    uiState.songs.filter { it.top_track }
                                                )
                                            },
                                            onDismiss = { playerState = PlayerState.Mini },
                                            position = uiState.songPosition,
                                            duration = uiState.songDuration
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Bottom Navigation Bar
                    CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 80.dp),
                            indicator = { },
                            divider = { }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = Color.Gray,
                                    modifier = Modifier.drawBehind {
                                        if (pagerState.currentPage == index) {
                                            val radius = with(density) { 4.dp.toPx() }
                                            val y = with(density) { size.height - 4.dp.toPx()}
                                            drawCircle(
                                                color = Color.White,
                                                radius = radius,
                                                center = Offset(
                                                    x = size.width / 2f,
                                                    y = y
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingList() {
    LazyColumn {
        items(8) {
            ShimmerSongItem()
        }
    }
}

@Composable
fun ShimmerSongItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Song name placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.typography.bodyLarge.fontSize.value.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Artist name placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f) // Slightly shorter than song name
                        .height(MaterialTheme.typography.bodySmall.fontSize.value.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = "ShimmerOffsetX"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}

enum class PlayerState {
    Mini,
    FullScreen
}

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
}