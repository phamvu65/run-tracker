package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.SegmentEffort
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.segment.SegmentMatcher
import java.time.Instant
import javax.inject.Inject

/**
 * Sau khi tạo một segment mới, quét toàn bộ activity đã có và tạo effort cho những buổi
 * đi qua segment đó (mà chưa có effort).
 */
class BackfillSegmentEffortsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val segmentRepository: SegmentRepository,
) {
    suspend operator fun invoke(segmentId: String) {
        val segment = segmentRepository.getSegment(segmentId) ?: return
        val activities = activityRepository.getActivitiesBetween(
            userId = LOCAL_USER_ID,
            from = Instant.EPOCH,
            to = Instant.now(),
        )

        for (activity in activities) {
            val alreadyMatched = segmentRepository.getEffortsForActivity(activity.id)
                .any { it.segmentId == segmentId }
            if (alreadyMatched) continue

            val route = activityRepository.getRoutePoints(activity.id)
            if (route.size < 2) continue

            val match = SegmentMatcher.match(
                route = route,
                segmentStart = segment.start,
                segmentEnd = segment.end,
                segmentDistanceMeters = segment.distanceMeters,
                segmentPolyline = segment.points,
            ) ?: continue

            segmentRepository.addEffort(
                SegmentEffort(
                    id = 0,
                    segmentId = segmentId,
                    activityId = activity.id,
                    userId = activity.userId,
                    elapsedSeconds = match.elapsedSeconds,
                    startTime = match.startTime,
                    avgHeartRate = null,
                    rank = null,
                ),
            )
        }
    }
}
