package com.example.runtracker.domain.usecase

import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.PersonalRecord
import com.example.runtracker.domain.model.PersonalRecordKind
import com.example.runtracker.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Kiểm tra một buổi tập vừa chốt số liệu có phá/lập kỷ lục cá nhân nào không, so với các buổi
 * tập TRƯỚC ĐÓ cùng loại hoạt động ([com.example.runtracker.domain.model.ActivityType]). Chỉ so
 * 3 chỉ số toàn buổi đơn giản — không phải "best effort" theo từng mốc cự ly cụ thể (VD "5K
 * nhanh nhất trong buổi") như [RiegelPredictor]/segment leaderboard.
 *
 * Buổi tập đầu tiên của một loại hoạt động không tính kỷ lục gì (chưa có gì để so/phá).
 */
class DetectPersonalRecordsUseCase @Inject constructor(
    private val repository: ActivityRepository,
) {
    suspend operator fun invoke(activityId: String): List<PersonalRecord> {
        val current = repository.getActivity(activityId) ?: return emptyList()
        val previous = repository.observeActivities(current.userId).first()
            .filter { it.id != current.id && it.type == current.type }
        if (previous.isEmpty()) return emptyList()

        val records = mutableListOf<PersonalRecord>()

        val prevMaxDistance = previous.maxOf { it.distanceMeters }
        if (current.distanceMeters > prevMaxDistance) {
            records += PersonalRecord(
                PersonalRecordKind.LONGEST_DISTANCE,
                "Quãng đường dài nhất",
                formatDistanceKm(current.distanceMeters),
            )
        }

        if (current.distanceMeters >= MIN_DISTANCE_FOR_PACE_RECORD_M && current.avgPaceSecPerKm > 0) {
            val prevBestPace = previous
                .filter { it.distanceMeters >= MIN_DISTANCE_FOR_PACE_RECORD_M && it.avgPaceSecPerKm > 0 }
                .minOfOrNull { it.avgPaceSecPerKm }
            if (prevBestPace == null || current.avgPaceSecPerKm < prevBestPace) {
                records += PersonalRecord(
                    PersonalRecordKind.FASTEST_PACE,
                    "Pace nhanh nhất",
                    formatPace(current.avgPaceSecPerKm),
                )
            }
        }

        val prevMaxMoving = previous.maxOf { it.movingTime }
        if (current.movingTime > prevMaxMoving) {
            records += PersonalRecord(
                PersonalRecordKind.LONGEST_DURATION,
                "Thời gian tập lâu nhất",
                formatClock(current.movingTime.inWholeSeconds),
            )
        }

        return records
    }

    private companion object {
        /** Buổi quá ngắn thì pace không nói lên nhiều điều — bỏ qua kỷ lục pace. */
        const val MIN_DISTANCE_FOR_PACE_RECORD_M = 1_000.0
    }
}
