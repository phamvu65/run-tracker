package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.tracking.RouteStats
import java.util.UUID
import javax.inject.Inject

/**
 * Tạo segment từ toàn bộ đường đi của một activity (v1 — chưa chọn đoạn con).
 * Tạo xong dò luôn effort cho chính activity nguồn.
 */
class CreateSegmentUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val segmentRepository: SegmentRepository,
    private val detectSegmentEfforts: DetectSegmentEffortsUseCase,
) {
    /** @return id segment mới, hoặc null nếu activity không đủ điểm GPS. */
    suspend operator fun invoke(activityId: String, name: String): String? {
        val route = activityRepository.getRoutePoints(activityId)
        if (route.size < 2) return null

        val points = route.map { GeoPoint(it.latitude, it.longitude) }
        val distance = RouteStats.cumulativeDistances(route).last()
        val grade = if (distance > 0) {
            (route.last().altitude - route.first().altitude) / distance * 100.0
        } else {
            0.0
        }

        val segment = Segment(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Segment" },
            start = points.first(),
            end = points.last(),
            distanceMeters = distance,
            avgGrade = grade,
            points = points,
            createdByUserId = LOCAL_USER_ID,
            isPublic = true,
        )
        segmentRepository.upsertSegment(segment)
        detectSegmentEfforts(activityId)
        return segment.id
    }
}
