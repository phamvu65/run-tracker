package com.example.runtracker.domain.model

import kotlin.time.Duration

data class ActivityLap(
    val lapIndex: Int,
    val distanceMeters: Double,
    val duration: Duration,
    val avgPaceSecPerKm: Double,
    val avgHeartRate: Int?,
)
