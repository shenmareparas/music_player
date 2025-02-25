package com.example.music.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.music.Song
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenPlayer(
    song: Song,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    position: Long,
    duration: Long,
    allSongs: List<Song>,
    topTracks: List<Song>,
    sourceList: String
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val activeList = if (sourceList == "ForYou") allSongs else topTracks
    val initialIndex = activeList.indexOf(song).coerceAtLeast(0)
    val pagerState = rememberPagerState(pageCount = { activeList.size }, initialPage = initialIndex)
    var currentSong by remember { mutableStateOf(song) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val offsetY by animateFloatAsState(
        targetValue = if (isDragging) totalDragY.coerceAtMost(density.run { 2000.dp.toPx() }) else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "OffsetYAnimation"
    )

    fun updateCurrentSong(direction: Int): Song {
        val currentIndex = activeList.indexOf(currentSong)
        val newIndex = if (direction > 0) {
            if (currentIndex >= activeList.size - 1) 0 else currentIndex + 1
        } else {
            if (currentIndex <= 0) activeList.size - 1 else currentIndex - 1
        }
        return activeList[newIndex]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 0) {
                            totalDragY += dragAmount.y
                            isDragging = true
                        }
                        totalDragX += dragAmount.x
                    },
                    onDragEnd = {
                        isDragging = false
                        val horizontalThreshold = 100f
                        val verticalThreshold = 100f

                        when {
                            totalDragY > verticalThreshold -> {
                                onDismiss()
                            }
                            totalDragX > horizontalThreshold -> {
                                onPrevious()
                                currentSong = updateCurrentSong(-1)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).let {
                                            if (it < 0) activeList.size - 1 else it
                                        }
                                    )
                                }
                            }
                            totalDragX < -horizontalThreshold -> {
                                onNext()
                                currentSong = updateCurrentSong(1)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1).let {
                                            if (it >= activeList.size) 0 else it
                                        }
                                    )
                                }
                            }
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                )
            }
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(android.graphics.Color.parseColor(currentSong.accent)),
                        Color.Black
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
        ) {
            // Down arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "DownScale")

                IconButton(
                    onClick = {
                        onDismiss()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Carousel album cover
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(horizontal = 50.dp),
            ) { page ->
                val currentItem = activeList[page]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val pageOffset =
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val absOffset = pageOffset.absoluteValue

                            val scale = 0.8f + (1 - absOffset).coerceIn(0f, 0.2f)

                            scaleX = scale
                            scaleY = scale
                            alpha = scale
                        }
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://cms.samespace.com/assets/${currentItem.cover}",
                        contentDescription = "Album cover for ${currentItem.name}",
                        modifier = Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                val newIndex = pagerState.currentPage
                if (newIndex != initialIndex) {
                    currentSong = activeList[newIndex]
                    if (newIndex > initialIndex) {
                        onNext()
                    } else {
                        onPrevious()
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Song info
            Text(
                text = currentSong.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(39.dp))

            // Progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    color = Color.White,
                    trackColor = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val prevInteractionSource = remember { MutableInteractionSource() }
                val prevIsPressed by prevInteractionSource.collectIsPressedAsState()
                val prevScale by animateFloatAsState(
                    if (prevIsPressed) 0.9f else 1f,
                    label = "PrevScale"
                )

                IconButton(
                    onClick = {
                        onPrevious()
                        currentSong = updateCurrentSong(-1)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage - 1).let {
                                    if (it < 0) activeList.size - 1 else it
                                }
                            )
                        }
                    },
                    interactionSource = prevInteractionSource,
                    modifier = Modifier.scale(prevScale)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Previous",
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                }

                val playPauseInteractionSource = remember { MutableInteractionSource() }
                val playPauseIsPressed by playPauseInteractionSource.collectIsPressedAsState()
                val playPauseScale by animateFloatAsState(
                    if (playPauseIsPressed) 0.9f else 1f,
                    label = "PlayPauseScale"
                )

                Box(
                    modifier = Modifier
                        .scale(playPauseScale)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = playPauseInteractionSource,
                            indication = null
                        ) {
                            onTogglePlayPause()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(44.dp)
                    )
                }

                val nextInteractionSource = remember { MutableInteractionSource() }
                val nextIsPressed by nextInteractionSource.collectIsPressedAsState()
                val nextScale by animateFloatAsState(
                    if (nextIsPressed) 0.9f else 1f,
                    label = "NextScale"
                )

                IconButton(
                    onClick = {
                        onNext()
                        currentSong = updateCurrentSong(1)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1).let {
                                    if (it >= activeList.size) 0 else it
                                }
                            )
                        }
                    },
                    interactionSource = nextInteractionSource,
                    modifier = Modifier.scale(nextScale)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Next",
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}