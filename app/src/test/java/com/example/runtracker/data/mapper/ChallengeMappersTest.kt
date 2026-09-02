package com.example.runtracker.data.mapper

import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeParticipant
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ChallengeMappersTest {

    @Test
    fun `challenge round-trips through entity`() {
        val challenge = Challenge(
            id = "c1", name = "Tháng 9", goalType = ChallengeGoalType.TOTAL_DURATION,
            goalValue = 36_000.0,
            startDate = LocalDate.of(2026, 9, 1), endDate = LocalDate.of(2026, 9, 30),
            createdByUserId = "local-user",
        )
        assertEquals(challenge, challenge.toEntity().toDomain())
    }

    @Test
    fun `unknown goal type falls back to distance`() {
        val entity = Challenge(
            id = "c", name = "x", goalType = ChallengeGoalType.TOTAL_DISTANCE, goalValue = 1.0,
            startDate = LocalDate.EPOCH, endDate = LocalDate.EPOCH, createdByUserId = "u",
        ).toEntity().copy(goalType = "WEIRD")
        assertEquals(ChallengeGoalType.TOTAL_DISTANCE, entity.toDomain().goalType)
    }

    @Test
    fun `participant round-trips through entity`() {
        val participant = ChallengeParticipant("c1", "local-user", 4_200.0, Instant.ofEpochMilli(1_700_000_000_000))
        assertEquals(participant, participant.toEntity().toDomain())
    }
}
