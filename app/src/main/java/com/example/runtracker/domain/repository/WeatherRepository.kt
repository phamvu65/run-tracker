package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.ActivityWeather
import java.time.Instant

interface WeatherRepository {

    /**
     * Thời tiết tại toạ độ + thời điểm đã cho. Thất bại (mạng, ngoài phạm vi ~92 ngày,
     * thiếu dữ liệu) trả [Result.failure].
     */
    suspend fun fetch(latitude: Double, longitude: Double, time: Instant): Result<ActivityWeather>
}
