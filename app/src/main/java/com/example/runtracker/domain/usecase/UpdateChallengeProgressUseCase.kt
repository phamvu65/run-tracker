package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.challenge.ChallengeProgress
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ChallengeRepository
import java.time.ZoneId
import javax.inject.Inject

/**
 * Tính lại tiến độ mọi thử thách mà người dùng đang tham gia: tổng hợp activity trong khoảng
 * ngày của thử thách theo `goalType`. Gọi sau mỗi buổi tập được chốt và trong job hằng đêm.
 */
class UpdateChallengeProgressUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val activityRepository: ActivityRepository,
) {
    suspend operator fun invoke(
        userId: String = LOCAL_USER_ID,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val participations = challengeRepository.getParticipationsForUser(userId)
        for (participant in participations) {
            val challenge = challengeRepository.getChallenge(participant.challengeId) ?: continue
            val from = challenge.startDate.atStartOfDay(zoneId).toInstant()
            val to = challenge.endDate.plusDays(1).atStartOfDay(zoneId).toInstant().minusMillis(1)

            val activities = activityRepository.getActivitiesBetween(userId, from, to)
            val progress = ChallengeProgress.compute(challenge.goalType, activities)

            if (progress != participant.currentProgress) {
                challengeRepository.upsertParticipant(participant.copy(currentProgress = progress))
            }
        }
    }
}
