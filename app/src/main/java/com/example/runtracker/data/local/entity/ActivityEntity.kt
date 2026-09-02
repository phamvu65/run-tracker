package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một buổi tập (run/ride/walk). Tổng hợp số liệu đã tính từ RoutePoint + HeartRateSample.
 * Cột `weather*` cho weather overlay (Phase 3), `perceivedExertion` là fallback cho TRIMP khi thiếu HR.
 */
@Entity(
    tableName = "activities",
    indices = [Index("userId"), Index("startTime")]
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,              // RUNNING, CYCLING, WALKING...
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val movingTimeSeconds: Long,   // loại trừ thời gian dừng (đèn đỏ, nghỉ)
    val avgPaceSecPerKm: Double,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Int?,
    val avgCadence: Int?,
    val weatherTempC: Double?,          // Phase 3 — weather overlay (Open-Meteo)
    val weatherApparentTempC: Double? = null,
    val weatherHumidityPct: Int? = null,
    val weatherWindMps: Double? = null,
    val weatherWindDirDeg: Int? = null,
    val weatherCode: Int? = null,
    val perceivedExertion: Int?,    // RPE 1-10, fallback TRIMP khi không có HR
    val gpxRawPath: String?,        // file gốc nếu cần export/backup
    val isSynced: Boolean = false,
    val updatedAt: Long
)
