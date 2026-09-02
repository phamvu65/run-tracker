package com.example.runtracker.data.remote

import com.example.runtracker.domain.model.ActivityWeather
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class WeatherResponse(
    val hourly: WeatherHourly? = null,
)

@Serializable
data class WeatherHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidity: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList(),
)

/**
 * Chọn giờ gần [target] nhất trong mảng `hourly` và dựng [ActivityWeather].
 * `time` là ISO local; request luôn dùng `timezone=UTC` nên diễn giải theo UTC.
 * @return null nếu không có mẫu nào hoặc mẫu gần nhất thiếu nhiệt độ.
 */
fun WeatherResponse.toActivityWeather(target: Instant): ActivityWeather? {
    val hourly = hourly ?: return null
    if (hourly.time.isEmpty()) return null

    val targetMillis = target.toEpochMilli()
    val nearest = hourly.time.indices.minByOrNull { i ->
        abs(parseUtcMillis(hourly.time[i]) - targetMillis)
    } ?: return null

    val temp = hourly.temperature.getOrNull(nearest) ?: return null
    return ActivityWeather(
        temperatureC = temp,
        apparentTemperatureC = hourly.apparentTemperature.getOrNull(nearest),
        humidityPct = hourly.humidity.getOrNull(nearest)?.roundToInt(),
        windSpeedMps = hourly.windSpeed.getOrNull(nearest),
        windDirectionDeg = hourly.windDirection.getOrNull(nearest)?.roundToInt(),
        weatherCode = hourly.weatherCode.getOrNull(nearest),
    )
}

private fun parseUtcMillis(isoLocal: String): Long =
    LocalDateTime.parse(isoLocal).toInstant(ZoneOffset.UTC).toEpochMilli()
