package com.example.runtracker.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Thử thách nhóm quy mô nhỏ (Phase 3). Local-first: người tạo tự động tham gia, tiến độ được
 * tính lại từ activity của người dùng trong khoảng `[startDate, endDate]`. Cấu trúc leaderboard
 * nhiều người đã sẵn cho khi có backend.
 */
data class Challenge(
    val id: String,
    val name: String,
    val goalType: ChallengeGoalType,
    /** Đơn vị gốc theo [goalType]: mét (DISTANCE/ELEVATION), số buổi (ACTIVITIES), giây (DURATION). */
    val goalValue: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdByUserId: String,
) {
    fun statusOn(today: LocalDate): ChallengeStatus = when {
        today.isBefore(startDate) -> ChallengeStatus.UPCOMING
        today.isAfter(endDate) -> ChallengeStatus.ENDED
        else -> ChallengeStatus.ACTIVE
    }
}

enum class ChallengeGoalType(val raw: String) {
    TOTAL_DISTANCE("TOTAL_DISTANCE"),
    TOTAL_ACTIVITIES("TOTAL_ACTIVITIES"),
    TOTAL_ELEVATION("TOTAL_ELEVATION"),
    TOTAL_DURATION("TOTAL_DURATION");

    companion object {
        fun fromRaw(raw: String): ChallengeGoalType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: TOTAL_DISTANCE
    }
}

enum class ChallengeStatus { UPCOMING, ACTIVE, ENDED }

data class ChallengeParticipant(
    val challengeId: String,
    val userId: String,
    /** Tiến độ hiện tại, cùng đơn vị gốc với [Challenge.goalValue]. */
    val currentProgress: Double,
    val joinedAt: Instant,
)

/** Ghép challenge với tiến độ của người dùng hiện tại — tiện cho danh sách/chi tiết. */
data class ChallengeStanding(
    val challenge: Challenge,
    val myProgress: Double,
) {
    val fraction: Float
        get() = if (challenge.goalValue > 0) {
            (myProgress / challenge.goalValue).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }

    val isComplete: Boolean get() = challenge.goalValue > 0 && myProgress >= challenge.goalValue
}
