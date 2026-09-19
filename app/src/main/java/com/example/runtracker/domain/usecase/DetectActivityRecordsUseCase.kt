package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRecordRepository
import com.example.runtracker.domain.repository.ActivityRepository
import javax.inject.Inject

/** Sau khi một activity được chốt số liệu: tính kỷ lục pace/quãng đường/độ cao cho buổi VỪA chốt. */
class DetectActivityRecordsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val activityRecordRepository: ActivityRecordRepository,
) {
    suspend operator fun invoke(activityId: String) {
        val activity = activityRepository.getActivity(activityId) ?: return
        activityRecordRepository.recomputeForActivity(activity.userId, activityId, activity, activity.startTime)
    }
}
