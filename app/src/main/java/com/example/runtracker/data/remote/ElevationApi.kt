package com.example.runtracker.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Elevation (SRTM 90m) — tra độ cao thật theo toạ độ, miễn phí, không cần API key,
 * cùng domain đã dùng cho `WeatherApi` (không bị chặn DNS như `*.openstreetmap.org` — xem
 * `GeocodingApi`). Dùng khi máy không có barometer nên độ cao đang dựa hoàn toàn vào GPS altitude
 * thô (sai số thường ±10-30m, rất nhiễu) — xem `CorrectElevationUseCase`.
 * Docs: https://open-meteo.com/en/docs/elevation-api
 */
interface ElevationApi {
    @GET("https://api.open-meteo.com/v1/elevation")
    suspend fun elevation(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
    ): ElevationResponse
}

@Serializable
data class ElevationResponse(val elevation: List<Double> = emptyList())
