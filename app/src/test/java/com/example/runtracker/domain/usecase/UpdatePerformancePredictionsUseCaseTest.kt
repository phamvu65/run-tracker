package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityDetail
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.PerformancePredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class UpdatePerformancePredictionsUseCaseTest {

    private val now: Instant = Instant.parse("2026-09-01T00:00:00Z")

    @Test
    fun `bases prediction on the fastest qualifying run`() = runTest {
        val fastShortRide = activity("ride", ActivityType.CYCLING, 8_000.0, 180.0, 24.minutes)
        val tooShortRun = activity("short", ActivityType.RUNNING, 2_000.0, 200.0, 6.minutes)
        val bestRun = activity("best", ActivityType.RUNNING, 5_000.0, 240.0, 20.minutes)
        val slowerRun = activity("slow", ActivityType.RUNNING, 10_000.0, 300.0, 50.minutes)

        val activityRepo = FakeActivityRepository(listOf(fastShortRide, tooShortRun, bestRun, slowerRun))
        val predictionRepo = FakePredictionRepository()

        UpdatePerformancePredictionsUseCase(activityRepo, predictionRepo).invoke(now)

        assertEquals(listOf("5K", "10K", "HALF", "FULL"), predictionRepo.saved.map { it.distanceLabel })
        assertTrue(predictionRepo.saved.all { it.basedOnActivityId == "best" })
        // 5K từ chính buổi 5K -> ~1200s
        assertEquals(1_200.0, predictionRepo.saved.first { it.distanceLabel == "5K" }.predictedSeconds, 1.0)
    }

    @Test
    fun `clears predictions when there is no qualifying run`() = runTest {
        val activityRepo = FakeActivityRepository(
            listOf(activity("ride", ActivityType.CYCLING, 20_000.0, 150.0, 50.minutes)),
        )
        val predictionRepo = FakePredictionRepository().apply {
            saved = listOf(PerformancePrediction("u", "5K", 1_200.0, "x", 0))
        }

        UpdatePerformancePredictionsUseCase(activityRepo, predictionRepo).invoke(now)

        assertEquals(emptyList<PerformancePrediction>(), predictionRepo.saved)
    }

    private fun activity(
        id: String,
        type: ActivityType,
        distanceMeters: Double,
        avgPaceSecPerKm: Double,
        movingTime: Duration,
    ) = Activity(
        id = id, userId = "u", type = type,
        startTime = now.minusSeconds(3_600), endTime = now,
        distanceMeters = distanceMeters,
        duration = movingTime, movingTime = movingTime,
        avgPaceSecPerKm = avgPaceSecPerKm, avgSpeedKmh = 0.0,
        elevationGainMeters = 0.0, elevationLossMeters = 0.0,
        avgHeartRate = null, maxHeartRate = null, calories = null, avgCadence = null,
        perceivedExertion = null, weather = null, gpxRawPath = null,
    )

    private class FakePredictionRepository : PerformancePredictionRepository {
        var saved: List<PerformancePrediction> = emptyList()
        override fun observeForUser(userId: String): Flow<List<PerformancePrediction>> =
            throw UnsupportedOperationException()
        override suspend fun get(userId: String, distanceLabel: String) =
            saved.firstOrNull { it.distanceLabel == distanceLabel }
        override suspend fun replaceAll(predictions: List<PerformancePrediction>) {
            saved = predictions
        }
    }

    private class FakeActivityRepository(
        private val activities: List<Activity>,
    ) : ActivityRepository {
        override suspend fun getActivitiesBetween(userId: String, from: Instant, to: Instant) =
            activities.filter { it.startTime in from..to }

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
        override suspend fun appendHeartRateSamples(activityId: String, samples: List<HeartRateSample>) =
            unused()
        override suspend fun replaceLaps(activityId: String, laps: List<ActivityLap>) = unused()
        override suspend fun deleteActivity(activityId: String) = unused()

        private fun unused(): Nothing = throw UnsupportedOperationException("not needed for this test")
    }
}
