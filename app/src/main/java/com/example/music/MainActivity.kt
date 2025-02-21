package com.example.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.media3.exoplayer.ExoPlayer
import com.example.music.ui.MusicPlayerScreen
import com.example.music.ui.theme.MusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val exoPlayer = ExoPlayer.Builder(this).build()
        val repository = MusicRepository(Network.api, exoPlayer)
        setContent {
            MusicTheme {
                MusicPlayerScreen(repository)
            }
        }
    }
}