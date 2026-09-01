package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.PerformancePredictionEntity
import com.example.runtracker.domain.model.PerformancePrediction

fun PerformancePredictionEntity.toDomain(): PerformancePrediction = PerformancePrediction(
    userId = userId,
    distanceLabel = distanceLabel,
    predictedSeconds = predictedSeconds,
    basedOnActivityId = basedOnActivityId,
    computedAt = computedAt,
)

fun PerformancePrediction.toEntity(): PerformancePredictionEntity = PerformancePredictionEntity(
    userId = userId,
    distanceLabel = distanceLabel,
    predictedSeconds = predictedSeconds,
    basedOnActivityId = basedOnActivityId,
    computedAt = computedAt,
)
