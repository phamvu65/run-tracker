package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.domain.tracking.RouteStats
import java.util.UUID
import javax.inject.Inject

/**
 * Tạo segment từ một đoạn con của đường đi một activity, chọn theo khoảng quãng đường
 * `[fromDistanceMeters, toDistanceMeters]`. Bỏ trống -> toàn bộ route.
 * Tạo xong dò ngược mọi activity đã có.
 */
class CreateSegmentUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val segmentRepository: SegmentRepository,
    private val backfillSegmentEfforts: BackfillSegmentEffortsUseCase,
) {
    /** @return id segment mới, hoặc null nếu đoạn được chọn quá ngắn / không đủ điểm. */
    suspend operator fun invoke(
        activityId: String,
        name: String,
        fromDistanceMeters: Double? = null,
        toDistanceMeters: Double? = null,
    ): String? {
        val route = activityRepository.getRoutePoints(activityId)
        if (route.size < 2) return null

        val cumulative = RouteStats.cumulativeDistances(route)
        val total = cumulative.last()
        val from = (fromDistanceMeters ?: 0.0).coerceIn(0.0, total)
        val to = (toDistanceMeters ?: total).coerceIn(from, total)

        val sliceIndices = route.indices.filter { cumulative[it] in from..to }
        val slice = if (sliceIndices.size >= 2) {
            route.subList(sliceIndices.first(), sliceIndices.last() + 1)
        } else {
            route
        }
        if (slice.size < 2) return null

        val points = slice.map { GeoPoint(it.latitude, it.longitude) }
        val distance = GeoMath.pathDistanceMeters(points)
        if (distance < MIN_SEGMENT_METERS) return null

        val grade = if (distance > 0) {
            (slice.last().altitude - slice.first().altitude) / distance * 100.0
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
        backfillSegmentEfforts(segment.id)
        return segment.id
    }

    private companion object {
        const val MIN_SEGMENT_METERS = 100.0
    }
}
