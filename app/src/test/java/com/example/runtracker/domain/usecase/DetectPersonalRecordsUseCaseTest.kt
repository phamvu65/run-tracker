package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityDetail
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.PersonalRecordKind
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DetectPersonalRecordsUseCaseTest {

    private val now: Instant = Instant.parse("2026-09-01T00:00:00Z")

    @Test
    fun `no previous activity of the same type yields no records`() = runTest {
        val current = activity("cur", ActivityType.RUNNING, 5_000.0, 300.0, 25.minutes)
        val useCase = DetectPersonalRecordsUseCase(FakeActivityRepository(listOf(current)))

        val records = useCase(current.id)

        assertTrue(records.isEmpty())
    }

    @Test
    fun `longest distance and longest duration records are detected independently`() = runTest {
        val past = activity("past", ActivityType.RUNNING, 5_000.0, 300.0, 25.minutes)
        val current = activity("cur", ActivityType.RUNNING, 6_000.0, 310.0, 20.minutes)
        val useCase = DetectPersonalRecordsUseCase(FakeActivityRepository(listOf(past, current)))

        val records = useCase(current.id)

        assertEquals(listOf(PersonalRecordKind.LONGEST_DISTANCE), records.map { it.kind })
    }

    @Test
    fun `fastest pace record requires at least 1km`() = runTest {
        val past = activity("past", ActivityType.RUNNING, 5_000.0, 300.0, 25.minutes)
        // Buổi mới nhanh hơn nhưng chưa tới 1km -> không tính kỷ lục pace.
        val current = activity("cur", ActivityType.RUNNING, 500.0, 200.0, 2.minutes)
        val useCase = DetectPersonalRecordsUseCase(FakeActivityRepository(listOf(past, current)))

        val records = useCase(current.id)

        assertTrue(records.none { it.kind == PersonalRecordKind.FASTEST_PACE })
    }

    @Test
    fun `beating all three metrics reports all three records`() = runTest {
        val past = activity("past", ActivityType.RUNNING, 5_000.0, 300.0, 25.minutes)
        val current = activity("cur", ActivityType.RUNNING, 6_000.0, 280.0, 30.minutes)
        val useCase = DetectPersonalRecordsUseCase(FakeActivityRepository(listOf(past, current)))

        val records = useCase(current.id)

        assertEquals(
            setOf(
                PersonalRecordKind.LONGEST_DISTANCE,
                PersonalRecordKind.FASTEST_PACE,
                PersonalRecordKind.LONGEST_DURATION,
            ),
            records.map { it.kind }.toSet(),
        )
    }

    @Test
    fun `does not compare against a different activity type`() = runTest {
        val pastRide = activity("ride", ActivityType.CYCLING, 20_000.0, 150.0, 50.minutes)
        val current = activity("cur", ActivityType.RUNNING, 3_000.0, 300.0, 15.minutes)
        val useCase = DetectPersonalRecordsUseCase(FakeActivityRepository(listOf(pastRide, current)))

        val records = useCase(current.id)

        assertTrue(records.isEmpty())
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
        avgHeartRate = null, maxHeartRate = null, calories = null, steps = null, avgCadence = null,
        perceivedExertion = null, weather = null, gpxRawPath = null,
    )

    private class FakeActivityRepository(
        private val activities: List<Activity>,
    ) : ActivityRepository {
        override fun observeActivities(userId: String): Flow<List<Activity>> =
            flowOf(activities.filter { it.userId == userId })

        override suspend fun getActivity(activityId: String): Activity? =
            activities.firstOrNull { it.id == activityId }

        override fun observeActivity(activityId: String): Flow<Activity?> = unused()
        override fun observeRoutePoints(activityId: String): Flow<List<RoutePoint>> = unused()
        override fun observeLaps(activityId: String): Flow<List<ActivityLap>> = unused()
        override fun observeHeartRateSamples(activityId: String): Flow<List<HeartRateSample>> = unused()
        override suspend fun getRoutePoints(activityId: String): List<RoutePoint> = unused()
        override suspend fun getHeartRateSamples(activityId: String): List<HeartRateSample> = unused()
        override suspend fun getActivityDetail(activityId: String): ActivityDetail? = unused()
        override suspend fun getActivitiesBetween(userId: String, from: Instant, to: Instant) = unused()
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
