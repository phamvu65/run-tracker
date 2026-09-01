package com.example.runtracker.domain.model

import java.time.Instant

data class Segment(
    val id: String,
    val name: String,
    val start: GeoPoint,
    val end: GeoPoint,
    val distanceMeters: Double,
    val avgGrade: Double,
    val points: List<GeoPoint>,
    val createdByUserId: String,
    val isPublic: Boolean,
)

data class SegmentEffort(
    val id: Long,
    val segmentId: String,
    val activityId: String,
    val userId: String,
    val elapsedSeconds: Double,
    val startTime: Instant,
    val avgHeartRate: Int?,
    /** Thứ hạng trên leaderboard (1 = nhanh nhất); null nếu chưa tính. */
    val rank: Int?,
)
