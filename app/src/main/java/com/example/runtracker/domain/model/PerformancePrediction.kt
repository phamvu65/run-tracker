package com.example.runtracker.domain.model

data class PerformancePrediction(
    val userId: String,
    val distanceLabel: String,
    val predictedSeconds: Double,
    val basedOnActivityId: String,
    val computedAt: Long,
)
