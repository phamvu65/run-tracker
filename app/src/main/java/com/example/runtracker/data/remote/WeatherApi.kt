package com.example.runtracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo — dự báo + lịch sử gần (miễn phí, không cần API key). Dùng URL tuyệt đối nên
 * không phụ thuộc baseUrl của Retrofit (đang trỏ Google).
 * Docs: https://open-meteo.com/en/docs
 */
interface WeatherApi {

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("hourly") hourly: String = HOURLY_VARS,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
        @Query("timezone") timezone: String = "UTC",
    ): WeatherResponse

    companion object {
        const val HOURLY_VARS =
            "temperature_2m,relative_humidity_2m,apparent_temperature," +
                "weather_code,wind_speed_10m,wind_direction_10m"
    }
}
