package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeParticipant
import com.example.runtracker.domain.repository.ChallengeRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Tạo thử thách mới, người tạo tự động tham gia, rồi tính tiến độ ban đầu (activity đã có
 * trong khoảng ngày cũng được tính).
 */
class CreateChallengeUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val updateChallengeProgress: UpdateChallengeProgressUseCase,
) {
    /** @return id thử thách mới, hoặc null nếu tham số không hợp lệ. */
    suspend operator fun invoke(
        name: String,
        goalType: ChallengeGoalType,
        goalValue: Double,
        startDate: LocalDate,
        endDate: LocalDate,
        userId: String = LOCAL_USER_ID,
    ): String? {
        if (goalValue <= 0.0 || endDate.isBefore(startDate)) return null

        val id = UUID.randomUUID().toString()
        challengeRepository.upsertChallenge(
            Challenge(
                id = id,
                name = name.trim().ifEmpty { "Thử thách" },
                goalType = goalType,
                goalValue = goalValue,
                startDate = startDate,
                endDate = endDate,
                createdByUserId = userId,
            ),
        )
        challengeRepository.upsertParticipant(
            ChallengeParticipant(
                challengeId = id,
                userId = userId,
                currentProgress = 0.0,
                joinedAt = Instant.now(),
            ),
        )
        updateChallengeProgress(userId)
        return id
    }
}
