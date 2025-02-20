package com.example.music // Adjust to your actual package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel // Import base ViewModel class
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import com.example.music.ui.theme.MusicTheme // Adjust to your package

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exoPlayer = ExoPlayer.Builder(this).build()
        setContent {
            MusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Music",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${song.name} - ${song.artist}",
                style = MaterialTheme.typography.bodyLarge
            )
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