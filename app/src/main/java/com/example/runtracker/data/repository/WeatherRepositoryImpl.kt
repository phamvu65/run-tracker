package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.WeatherApi
import com.example.runtracker.data.remote.toActivityWeather
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.model.ActivityWeather
import com.example.runtracker.domain.repository.WeatherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WeatherRepository {

    override suspend fun fetch(
        latitude: Double,
        longitude: Double,
        time: Instant,
    ): Result<ActivityWeather> = withContext(io) {
        runCatching {
            val date = time.atZone(ZoneOffset.UTC).toLocalDate().toString()
            val response = api.forecast(
                latitude = latitude,
                longitude = longitude,
                startDate = date,
                endDate = date,
            )
            response.toActivityWeather(time) ?: error("không có dữ liệu thời tiết cho $date")
        }
    }
}
