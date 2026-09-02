package com.example.runtracker.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class WeatherResponseMappingTest {

    private fun response() = WeatherResponse(
        hourly = WeatherHourly(
            time = listOf("2026-09-02T05:00", "2026-09-02T06:00", "2026-09-02T07:00"),
            temperature = listOf(24.0, 26.0, 29.0),
            humidity = listOf(90.0, 80.4, 60.0),
            apparentTemperature = listOf(25.0, 28.0, 32.0),
            weatherCode = listOf(1, 2, 3),
            windSpeed = listOf(1.0, 2.5, 4.0),
            windDirection = listOf(100.0, 180.6, 270.0),
        ),
    )

    @Test
    fun `picks the hour nearest the target time`() {
        val target = Instant.parse("2026-09-02T06:10:00Z")
        val weather = response().toActivityWeather(target)!!

        assertEquals(26.0, weather.temperatureC, 0.001)
        assertEquals(28.0, weather.apparentTemperatureC!!, 0.001)
        assertEquals(80, weather.humidityPct)
        assertEquals(2.5, weather.windSpeedMps!!, 0.001)
        assertEquals(181, weather.windDirectionDeg)
        assertEquals(2, weather.weatherCode)
    }

    @Test
    fun `picks the next hour when closer to it`() {
        val target = Instant.parse("2026-09-02T06:40:00Z")
        assertEquals(29.0, response().toActivityWeather(target)!!.temperatureC, 0.001)
    }

    @Test
    fun `null when hourly missing`() {
        assertNull(WeatherResponse(hourly = null).toActivityWeather(Instant.EPOCH))
    }

    @Test
    fun `null when nearest sample has no temperature`() {
        val r = WeatherResponse(
            hourly = WeatherHourly(
                time = listOf("2026-09-02T06:00"),
                temperature = listOf(null),
            ),
        )
        assertNull(r.toActivityWeather(Instant.parse("2026-09-02T06:00:00Z")))
    }
}
