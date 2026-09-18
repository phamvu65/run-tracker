package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityDetail
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.Challenge
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeParticipant
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class UpdateChallengeProgressUseCaseTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    @Test
    fun `recomputes progress from activities inside the challenge window`() = runTest {
        val challenge = Challenge(
            id = "c1", name = "Tháng 9", goalType = ChallengeGoalType.TOTAL_DISTANCE,
            goalValue = 50_000.0,
            startDate = LocalDate.of(2026, 9, 1), endDate = LocalDate.of(2026, 9, 30),
            createdByUserId = "u",
        )
        val inside = run("in1", "2026-09-05T06:00:00Z", 6_000.0)
        val alsoInside = run("in2", "2026-09-12T06:00:00Z", 4_000.0)
        val outside = run("out", "2026-10-02T06:00:00Z", 9_000.0)

        val challengeRepo = FakeChallengeRepository(
            challenges = listOf(challenge),
            participants = mutableListOf(ChallengeParticipant("c1", "u", 0.0, Instant.EPOCH)),
        )
        val activityRepo = FakeActivityRepository(listOf(inside, alsoInside, outside))

        UpdateChallengeProgressUseCase(challengeRepo, activityRepo).invoke("u", zone)

        assertEquals(10_000.0, challengeRepo.participants.single().currentProgress, 0.001)
    }

    @Test
    fun `does not write when progress is unchanged`() = runTest {
        val challenge = Challenge(
            id = "c1", name = "x", goalType = ChallengeGoalType.TOTAL_ACTIVITIES, goalValue = 10.0,
            startDate = LocalDate.of(2026, 9, 1), endDate = LocalDate.of(2026, 9, 30),
            createdByUserId = "u",
        )
        val challengeRepo = FakeChallengeRepository(
            challenges = listOf(challenge),
            participants = mutableListOf(ChallengeParticipant("c1", "u", 1.0, Instant.EPOCH)),
        )
        val activityRepo = FakeActivityRepository(listOf(run("in1", "2026-09-05T06:00:00Z", 6_000.0)))

        UpdateChallengeProgressUseCase(challengeRepo, activityRepo).invoke("u", zone)

        assertEquals(0, challengeRepo.upsertCount)
    }

    private fun run(id: String, start: String, distance: Double) = Activity(
        id = id, userId = "u", type = ActivityType.RUNNING,
        startTime = Instant.parse(start), endTime = Instant.parse(start).plusSeconds(1_800),
        distanceMeters = distance,
        duration = 30.minutes, movingTime = 30.minutes,
        avgPaceSecPerKm = 0.0, avgSpeedKmh = 0.0,
        elevationGainMeters = 0.0, elevationLossMeters = 0.0,
        avgHeartRate = null, maxHeartRate = null, calories = null, steps = null, avgCadence = null,
        perceivedExertion = null, weather = null, gpxRawPath = null,
    )

    private class FakeChallengeRepository(
        private val challenges: List<Challenge>,
        val participants: MutableList<ChallengeParticipant>,
    ) : ChallengeRepository {
        var upsertCount = 0

        override suspend fun getParticipationsForUser(userId: String) =
            participants.filter { it.userId == userId }

        override suspend fun getChallenge(id: String) = challenges.firstOrNull { it.id == id }

        override suspend fun upsertParticipant(participant: ChallengeParticipant) {
            upsertCount++
            participants.removeAll { it.challengeId == participant.challengeId && it.userId == participant.userId }
            participants.add(participant)
        }

        override fun observeChallenges(): Flow<List<Challenge>> = unused()
        override fun observeChallenge(id: String): Flow<Challenge?> = unused()
        override suspend fun upsertChallenge(challenge: Challenge) = unused()
        override suspend fun deleteChallenge(id: String) = unused()
        override fun observeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> = unused()
        override fun observeParticipationsForUser(userId: String): Flow<List<ChallengeParticipant>> = unused()
        override suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipant? = unused()
        override suspend fun leaveChallenge(challengeId: String, userId: String) = unused()

        private fun unused(): Nothing = throw UnsupportedOperationException("not needed for this test")
    }

    private class FakeActivityRepository(
        private val activities: List<Activity>,
    ) : ActivityRepository {
        override suspend fun getActivitiesBetween(userId: String, from: Instant, to: Instant) =
            activities.filter { it.userId == userId && it.startTime in from..to }

        override fun observeActivities(userId: String): Flow<List<Activity>> = unused()
        override fun observeActivity(activityId: String): Flow<Activity?> = unused()
        override fun observeRoutePoints(activityId: String): Flow<List<RoutePoint>> = unused()
        override fun observeLaps(activityId: String): Flow<List<ActivityLap>> = unused()
        override fun observeHeartRateSamples(activityId: String): Flow<List<HeartRateSample>> = unused()
        override suspend fun getActivity(activityId: String): Activity? = unused()
        override suspend fun getRoutePoints(activityId: String): List<RoutePoint> = unused()
        override suspend fun getHeartRateSamples(activityId: String): List<HeartRateSample> = unused()
        override suspend fun getActivityDetail(activityId: String): ActivityDetail? = unused()
        override suspend fun getUnsyncedActivities(): List<Activity> = unused()
        override suspend fun upsertActivity(activity: Activity) = unused()
        override suspend fun appendRoutePoints(activityId: String, points: List<RoutePoint>) = unused()
        override suspend fun appendHeartRateSamples(activityId: String, samples: List<HeartRateSample>) = unused()
        override suspend fun replaceLaps(activityId: String, laps: List<ActivityLap>) = unused()
        override suspend fun deleteActivity(activityId: String) = unused()

        private fun unused(): Nothing = throw UnsupportedOperationException("not needed for this test")
    }
}
