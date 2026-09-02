package com.example.runtracker.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * OpenRouteService Directions API — miễn phí, chỉ cần API key (đăng ký bằng email,
 * không cần thẻ). Trả về geometry mã hoá polyline (độ chính xác 1e-5, giống Google).
 */
interface DirectionsApi {

    @POST("v2/directions/{profile}")
    suspend fun directions(
        @Path("profile") profile: String,
        @Header("Authorization") apiKey: String,
        @Body body: DirectionsRequest,
    ): DirectionsResponse

    companion object {
        const val BASE_URL = "https://api.openrouteservice.org/"
    }
}
