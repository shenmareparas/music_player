package com.example.music

import retrofit2.http.GET
import retrofit2.http.Headers

interface ApiService {
//    @Headers("Authorization: Bearer YOUR_API_KEY") // Replace if needed
    @GET("items/songs")
    suspend fun getSongs(): SongResponse
}