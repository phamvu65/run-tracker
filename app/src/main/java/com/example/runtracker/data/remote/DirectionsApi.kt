package com.example.runtracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?,
        @Query("mode") mode: String,
        @Query("key") key: String,
    ): DirectionsResponse

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}
