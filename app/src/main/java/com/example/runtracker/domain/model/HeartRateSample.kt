package com.example.runtracker.domain.model

import java.time.Instant

data class HeartRateSample(
    val bpm: Int,
    val timestamp: Instant,
)
