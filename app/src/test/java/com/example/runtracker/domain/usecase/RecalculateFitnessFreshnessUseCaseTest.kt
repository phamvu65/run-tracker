package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.DailyTrainingLoad
import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import com.example.runtracker.domain.repository.TrainingLoadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecalculateFitnessFreshnessUseCaseTest {

    private val d0: LocalDate = LocalDate.parse("2026-09-01")

    @Test
    fun `fills rest days with zero trimp and rolls ewma forward`() = runTest {
        val repo = FakeTrainingLoadRepository().apply {
            dailyLoads[d0] = load(d0, 100.0)
            dailyLoads[d0.plusDays(2)] = load(d0.plusDays(2), 50.0) // d1 = ngày nghỉ
        }

        RecalculateFitnessFreshnessUseCase(repo).invoke(upTo = d0.plusDays(2))

        val dates = repo.snapshots.keys.sorted()
        assertEquals(listOf(d0, d0.plusDays(1), d0.plusDays(2)), dates)

        // d0: CTL từ 0 với TRIMP 100
        assertEquals(100.0 / 42.0, repo.snapshots[d0]!!.ctl, 1e-9)
        // d1: ngày nghỉ -> CTL suy giảm
        val ctlD0 = repo.snapshots[d0]!!.ctl
        assertEquals(ctlD0 + (0.0 - ctlD0) / 42.0, repo.snapshots[d0.plusDays(1)]!!.ctl, 1e-9)
        // d2: TRIMP 50 tiếp nối từ d1
        val ctlD1 = repo.snapshots[d0.plusDays(1)]!!.ctl
        assertEquals(ctlD1 + (50.0 - ctlD1) / 42.0, repo.snapshots[d0.plusDays(2)]!!.ctl, 1e-9)
    }

    @Test
    fun `does nothing when there is no training load`() = runTest {
        val repo = FakeTrainingLoadRepository()
        RecalculateFitnessFreshnessUseCase(repo).invoke(upTo = d0)
        assertEquals(emptyMap<LocalDate, FitnessFreshnessSnapshot>(), repo.snapshots)
    }

    private fun load(date: LocalDate, trimp: Double) =
        DailyTrainingLoad(LOCAL_USER_ID, date, trimp, 1, 3600, 0)

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
