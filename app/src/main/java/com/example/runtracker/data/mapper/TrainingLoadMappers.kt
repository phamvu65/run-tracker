package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.DailyTrainingLoadEntity
import com.example.runtracker.data.local.entity.FitnessFreshnessSnapshotEntity
import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import java.time.LocalDate

fun DailyTrainingLoadEntity.toDomain(): DailyTrainingLoad = DailyTrainingLoad(
    userId = userId,
    date = LocalDate.parse(date),
    trimpScore = trimpScore,
    activityCount = activityCount,
    totalDurationSeconds = totalDurationSeconds,
    updatedAt = updatedAt,
)

fun DailyTrainingLoad.toEntity(): DailyTrainingLoadEntity = DailyTrainingLoadEntity(
    userId = userId,
    date = date.toString(),
    trimpScore = trimpScore,
    activityCount = activityCount,
    totalDurationSeconds = totalDurationSeconds,
    updatedAt = updatedAt,
)

fun FitnessFreshnessSnapshotEntity.toDomain(): FitnessFreshnessSnapshot = FitnessFreshnessSnapshot(
    userId = userId,
    date = LocalDate.parse(date),
    ctl = ctl,
    atl = atl,
    tsb = tsb,
    computedAt = computedAt,
)

fun FitnessFreshnessSnapshot.toEntity(): FitnessFreshnessSnapshotEntity = FitnessFreshnessSnapshotEntity(
    userId = userId,
    date = date.toString(),
    ctl = ctl,
    atl = atl,
    tsb = tsb,
    computedAt = computedAt,
)
