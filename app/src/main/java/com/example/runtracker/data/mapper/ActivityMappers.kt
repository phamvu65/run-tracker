package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.ActivityEntity
import com.example.runtracker.data.local.entity.ActivityLapEntity
import com.example.runtracker.data.local.entity.HeartRateSampleEntity
import com.example.runtracker.data.local.entity.RoutePointEntity
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.RoutePoint
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

// ---- Activity ----

fun ActivityEntity.toDomain(): Activity = Activity(
    id = id,
    userId = userId,
    type = ActivityType.fromRaw(type),
    startTime = Instant.ofEpochMilli(startTime),
    endTime = Instant.ofEpochMilli(endTime),
    distanceMeters = distanceMeters,
    duration = durationSeconds.seconds,
    movingTime = movingTimeSeconds.seconds,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgSpeedKmh = avgSpeedKmh,
    elevationGainMeters = elevationGainMeters,
    elevationLossMeters = elevationLossMeters,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    calories = calories,
    avgCadence = avgCadence,
    perceivedExertion = perceivedExertion,
    weatherTempC = weatherTempC,
    gpxRawPath = gpxRawPath,
)

/**
 * @param updatedAt thời điểm ghi (mọi thay đổi local đặt lại mốc này).
 * @param isSynced luôn false khi ghi từ client cho tới khi sync thành công.
 */
fun Activity.toEntity(updatedAt: Long, isSynced: Boolean = false): ActivityEntity = ActivityEntity(
    id = id,
    userId = userId,
    type = type.raw,
    startTime = startTime.toEpochMilli(),
    endTime = endTime.toEpochMilli(),
    distanceMeters = distanceMeters,
    durationSeconds = duration.inWholeSeconds,
    movingTimeSeconds = movingTime.inWholeSeconds,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgSpeedKmh = avgSpeedKmh,
    elevationGainMeters = elevationGainMeters,
    elevationLossMeters = elevationLossMeters,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    calories = calories,
    avgCadence = avgCadence,
    weatherTempC = weatherTempC,
    perceivedExertion = perceivedExertion,
    gpxRawPath = gpxRawPath,
    isSynced = isSynced,
    updatedAt = updatedAt,
)

// ---- RoutePoint ----

fun RoutePointEntity.toDomain(): RoutePoint = RoutePoint(
    id = id,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    speedMps = speedMps,
    accuracyMeters = accuracyMeters,
    timestamp = Instant.ofEpochMilli(timestamp),
)

fun RoutePoint.toEntity(activityId: String): RoutePointEntity = RoutePointEntity(
    id = id,
    activityId = activityId,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    speedMps = speedMps,
    accuracyMeters = accuracyMeters,
    timestamp = timestamp.toEpochMilli(),
)

// ---- HeartRateSample ----

fun HeartRateSampleEntity.toDomain(): HeartRateSample = HeartRateSample(
    bpm = bpm,
    timestamp = Instant.ofEpochMilli(timestamp),
)

fun HeartRateSample.toEntity(activityId: String): HeartRateSampleEntity = HeartRateSampleEntity(
    activityId = activityId,
    bpm = bpm,
    timestamp = timestamp.toEpochMilli(),
)

// ---- ActivityLap ----

fun ActivityLapEntity.toDomain(): ActivityLap = ActivityLap(
    lapIndex = lapIndex,
    distanceMeters = distanceMeters,
    duration = durationSeconds.seconds,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgHeartRate = avgHeartRate,
)

fun ActivityLap.toEntity(activityId: String): ActivityLapEntity = ActivityLapEntity(
    activityId = activityId,
    lapIndex = lapIndex,
    distanceMeters = distanceMeters,
    durationSeconds = duration.inWholeSeconds,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgHeartRate = avgHeartRate,
)
