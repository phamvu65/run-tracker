package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.model.User
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

class RecalculateFitnessFreshnessUseCaseTest {

    private val d0: LocalDate = LocalDate.parse("2026-09-01")

    @Test
    fun `fills rest days with zero trimp and rolls ewma forward`() = runTest {
        val repo = FakeTrainingLoadRepository().apply {
            dailyLoads[d0] = load(d0, 100.0)
            dailyLoads[d0.plusDays(2)] = load(d0.plusDays(2), 50.0) // d1 = ngày nghỉ
        }
        val activityRepo = FakeActivityRepository(listOf(sampleActivity(d0)))
        val userRepo = FakeUserRepository()
        val updateLoad = UpdateDailyTrainingLoadUseCase(activityRepo, userRepo, repo)

        RecalculateFitnessFreshnessUseCase(repo, activityRepo, updateLoad).invoke(
            upTo = d0.plusDays(2),
            zoneId = ZoneOffset.UTC,
        )

        val dates = repo.snapshots.keys.sorted()
        assertEquals(listOf(d0, d0.plusDays(1), d0.plusDays(2)), dates)

        // d0: CTL từ 0 với TRIMP được tính từ sampleActivity
        val expectedTrimpD0 = repo.dailyLoads[d0]!!.trimpScore
        val expectedCtlD0 = expectedTrimpD0 / 42.0
        assertEquals(expectedCtlD0, repo.snapshots[d0]!!.ctl, 1e-9)
        // d1: ngày nghỉ -> CTL suy giảm
        val ctlD0 = repo.snapshots[d0]!!.ctl
        assertEquals(ctlD0 + (0.0 - ctlD0) / 42.0, repo.snapshots[d0.plusDays(1)]!!.ctl, 1e-9)
        // d2: TRIMP d2 tiếp nối từ d1
        val ctlD1 = repo.snapshots[d0.plusDays(1)]!!.ctl
        val trimpD2 = repo.dailyLoads[d0.plusDays(2)]!!.trimpScore
        assertEquals(ctlD1 + (trimpD2 - ctlD1) / 42.0, repo.snapshots[d0.plusDays(2)]!!.ctl, 1e-9)
    }

    @Test
    fun `builds snapshots from existing activities without cached daily loads`() = runTest {
        val repo = FakeTrainingLoadRepository()
        val activities = FakeActivityRepository(listOf(sampleActivity(d0)))
        val update = UpdateDailyTrainingLoadUseCase(activities, FakeUserRepository(), repo)
        RecalculateFitnessFreshnessUseCase(repo, activities, update)(d0, zoneId = ZoneOffset.UTC)
        assertEquals(360.0 / 42, repo.snapshots.getValue(d0).ctl, 1e-9)
        assertEquals(360.0 / 7, repo.snapshots.getValue(d0).atl, 1e-9)
    }

    @Test
    fun `editing past effort recalculates through today despite existing snapshots`() = runTest {
        val repo = FakeTrainingLoadRepository()
        val activities = FakeActivityRepository(listOf(sampleActivity(d0).copy(perceivedExertion = 2)))
        val update = UpdateDailyTrainingLoadUseCase(activities, FakeUserRepository(), repo)
        val recalculate = RecalculateFitnessFreshnessUseCase(repo, activities, update)
        recalculate(d0.plusDays(2), zoneId = ZoneOffset.UTC)
        val oldCtl = repo.snapshots.getValue(d0.plusDays(2)).ctl
        activities.activities = listOf(sampleActivity(d0).copy(perceivedExertion = 6))
        recalculate(d0.plusDays(2), fromDate = d0, zoneId = ZoneOffset.UTC)
        assertEquals(oldCtl * 3, repo.snapshots.getValue(d0.plusDays(2)).ctl, 1e-9)
    }

    @Test
    fun `does nothing when there is no activity`() = runTest {
        val repo = FakeTrainingLoadRepository()
        val activityRepo = FakeActivityRepository(emptyList())
        val userRepo = FakeUserRepository()
        val updateLoad = UpdateDailyTrainingLoadUseCase(activityRepo, userRepo, repo)

        RecalculateFitnessFreshnessUseCase(repo, activityRepo, updateLoad).invoke(
            upTo = d0,
            zoneId = ZoneOffset.UTC,
        )
        assertEquals(emptyMap<LocalDate, FitnessFreshnessSnapshot>(), repo.snapshots)
    }

    private fun load(date: LocalDate, trimp: Double) =
        DailyTrainingLoad(LOCAL_USER_ID, date, trimp, 1, 3600, 0)

    private fun sampleActivity(date: LocalDate) = Activity(
        id = "a1",
        userId = LOCAL_USER_ID,
        type = ActivityType.RUNNING,
        startTime = date.atStartOfDay(ZoneOffset.UTC).toInstant(),
        endTime = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600),
        duration = 60.minutes,
        movingTime = 60.minutes,
        distanceMeters = 10000.0,
        avgPaceSecPerKm = 360.0,
        avgSpeedKmh = 10.0,
        elevationGainMeters = 0.0,
        elevationLossMeters = 0.0,
        calories = 600,
        avgHeartRate = 150,
        maxHeartRate = 170,
        avgCadence = 160,
        steps = 8000,
        perceivedExertion = 6,
        weather = null,
        gpxRawPath = null,
    )

    private class FakeUserRepository : UserRepository {
        override fun observeCurrentUser(): Flow<User?> = flowOf(null)
        override suspend fun getCurrentUser(): User? = null
        override suspend fun ensureCurrentUser(): User = throw UnsupportedOperationException()
        override suspend fun saveProfile(user: User) {}
    }

    private class FakeActivityRepository(var activities: List<Activity>) : ActivityRepository {
        override fun observeActivities(userId: String): Flow<List<Activity>> = flowOf(activities)
        override fun observeActivity(activityId: String): Flow<Activity?> = flowOf(activities.firstOrNull())
        override fun observeRoutePoints(activityId: String) = flowOf(emptyList<RoutePoint>())
        override fun observeLaps(activityId: String) = flowOf(emptyList<ActivityLap>())
        override fun observeHeartRateSamples(activityId: String) = flowOf(emptyList<HeartRateSample>())
        override suspend fun getActivity(activityId: String) = activities.firstOrNull()
        override suspend fun getRoutePoints(activityId: String) = emptyList<RoutePoint>()
        override suspend fun getHeartRateSamples(activityId: String) = emptyList<HeartRateSample>()
        override suspend fun getActivityDetail(activityId: String) = null
        override suspend fun getActivitiesBetween(userId: String, from: Instant, to: Instant) =
            activities.filter { it.startTime >= from && it.startTime < to }
        override suspend fun getUnsyncedActivities() = emptyList<Activity>()
        override suspend fun upsertActivity(activity: Activity) {}
        override suspend fun appendRoutePoints(activityId: String, points: List<RoutePoint>) = emptyList<RoutePoint>()
        override suspend fun appendHeartRateSamples(activityId: String, samples: List<HeartRateSample>) {}
        override suspend fun updateRoutePoints(activityId: String, points: List<RoutePoint>) {}
        override suspend fun replaceLaps(activityId: String, laps: List<ActivityLap>) {}
        override suspend fun deleteActivity(activityId: String) {}
    }

    private class FakeTrainingLoadRepository : TrainingLoadRepository {
        val dailyLoads = mutableMapOf<LocalDate, DailyTrainingLoad>()
        val snapshots = mutableMapOf<LocalDate, FitnessFreshnessSnapshot>()

        override suspend fun upsertDailyLoad(load: DailyTrainingLoad) { dailyLoads[load.date] = load }
        override suspend fun getDailyLoad(userId: String, date: LocalDate) = dailyLoads[date]
        override suspend fun getDailyLoadsBetween(userId: String, from: LocalDate, to: LocalDate) =
            dailyLoads.values.filter { it.date in from..to }.sortedBy { it.date }
        override suspend fun getEarliestDailyLoadDate(userId: String) = dailyLoads.keys.minOrNull()

        override suspend fun upsertSnapshot(snapshot: FitnessFreshnessSnapshot) {
            snapshots[snapshot.date] = snapshot
        }
        override suspend fun getSnapshot(userId: String, date: LocalDate) = snapshots[date]
        override suspend fun getLatestSnapshot(userId: String) =
            snapshots.values.maxByOrNull { it.date }
        override fun observeSnapshotsBetween(
            userId: String,
            from: LocalDate,
            to: LocalDate,
        ): Flow<List<FitnessFreshnessSnapshot>> = flowOf(
            snapshots.values.filter { it.date in from..to }.sortedBy { it.date },
        )
    }
}
