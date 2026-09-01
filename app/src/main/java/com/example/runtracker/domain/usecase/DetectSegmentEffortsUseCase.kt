package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.SegmentEffort
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.segment.SegmentMatcher
import javax.inject.Inject

/**
 * Sau khi một activity được chốt số liệu, thử khớp trace với mọi segment và tạo effort cho
 * những segment khớp mà chưa có effort từ activity này.
 */
class DetectSegmentEffortsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val segmentRepository: SegmentRepository,
) {
    suspend operator fun invoke(activityId: String) {
        val activity = activityRepository.getActivity(activityId) ?: return
        val route = activityRepository.getRoutePoints(activityId)
        if (route.size < 2) return

        val alreadyMatched = segmentRepository.getEffortsForActivity(activityId)
            .map { it.segmentId }
            .toSet()

        for (segment in segmentRepository.getAllSegments()) {
            if (segment.id in alreadyMatched) continue
            val match = SegmentMatcher.match(
                route = route,
                segmentStart = segment.start,
                segmentEnd = segment.end,
                segmentDistanceMeters = segment.distanceMeters,
            ) ?: continue

            segmentRepository.addEffort(
                SegmentEffort(
                    id = 0,
                    segmentId = segment.id,
                    activityId = activityId,
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
