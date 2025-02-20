package com.example.music // Adjust to your actual package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.music.ui.theme.MusicTheme // Adjust to your package

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the status bar transparent and let content draw behind it
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val exoPlayer = ExoPlayer.Builder(this).build()
        setContent {
            MusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black // Black background
                ) {
                    MusicPlayerScreen(exoPlayer)
                }
            }
        }
    }
}

@Composable
fun MusicPlayerScreen(exoPlayer: ExoPlayer, viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory(exoPlayer))) {
    val songs = viewModel.songs.collectAsState().value
    val isPlaying = viewModel.isPlaying.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .safeDrawingPadding() // Add padding to respect system bars
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.togglePlayPause() }) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(onClick = { viewModel.stopPlayback() }) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Black) // Black card background
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular cover image
            AsyncImage(
                model = "https://cms.samespace.com/assets/${song.cover}",
                contentDescription = "Album cover for ${song.name}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Song name and artist in a column with space between
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White // White for song name
                )
                Spacer(modifier = Modifier.height(4.dp)) // Space between song name and artist
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall, // Smaller font
                    color = Color.Gray // Grey for artist name
                )
            }
        }
    }
}

class MusicViewModelFactory(private val exoPlayer: ExoPlayer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(exoPlayer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}