package com.example.music

data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val url: String,
    val cover: String,
    val top_track: Boolean,
    val accent: String
)

data class SongResponse(
    val data: List<Song>
)