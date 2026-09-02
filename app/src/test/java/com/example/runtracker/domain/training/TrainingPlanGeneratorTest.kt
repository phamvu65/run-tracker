package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.SessionType
import com.example.runtracker.domain.model.TrainingGoal
import com.example.runtracker.domain.model.TrainingPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TrainingPlanGeneratorTest {

    private val monday = LocalDate.of(2026, 1, 5) // thứ Hai

    @Test
    fun `12-week half plan has expected shape`() {
        val goal = TrainingGoal(RaceDistance.HALF, LocalDate.of(2026, 3, 28)) // T7 tuần thứ 12
        val plan = TrainingPlanGenerator.generate(goal, monday, currentCtl = 40.0, recentWeeklyKm = 35.0)

        assertEquals(12, plan.weeks.size)
        assertEquals(TrainingPhase.BASE, plan.weeks.first().phase)
        assertEquals(TrainingPhase.RACE, plan.weeks.last().phase)
        assertEquals(2, plan.weeks.count { it.phase == TrainingPhase.TAPER })

        // mỗi tuần 7 buổi, Mon..Sun liên tục
        plan.weeks.forEach { week ->
            assertEquals(7, week.sessions.size)
            assertEquals(DayOfWeek.MONDAY, week.startDate.dayOfWeek)
            week.sessions.forEachIndexed { i, s ->
                assertEquals(week.startDate.plusDays(i.toLong()), s.date)
            }
        }

        // đỉnh tải cao hơn tuần đầu; taper thấp hơn đỉnh
        val peak = plan.weeks.filter { it.phase == TrainingPhase.PEAK }.maxOf { it.targetDistanceKm }
        assertTrue(peak > plan.weeks.first().targetDistanceKm)
        val lastTaper = plan.weeks.last { it.phase == TrainingPhase.TAPER }
        assertTrue(lastTaper.targetDistanceKm < peak)
    }

    @Test
    fun `race day has a race session at the race distance`() {
        val raceDate = LocalDate.of(2026, 3, 28)
        val goal = TrainingGoal(RaceDistance.TEN_K, raceDate)
        val plan = TrainingPlanGenerator.generate(goal, monday, currentCtl = 0.0, recentWeeklyKm = 20.0)

        val session = plan.sessionOn(raceDate)
        assertNotNull(session)
        assertEquals(SessionType.RACE, session!!.type)
        assertEquals(10.0, session.distanceKm!!, 0.01)
    }

    @Test
    fun `short window collapses to taper plus race week`() {
        val goal = TrainingGoal(RaceDistance.FIVE_K, monday.plusWeeks(1).plusDays(6))
        val plan = TrainingPlanGenerator.generate(goal, monday, currentCtl = 30.0, recentWeeklyKm = 25.0)

        assertEquals(2, plan.weeks.size)
        assertEquals(TrainingPhase.RACE, plan.weeks.last().phase)
    }

    @Test
    fun `race beyond max weeks yields base-building block without a race week`() {
        val goal = TrainingGoal(RaceDistance.FULL, monday.plusWeeks(40))
        val plan = TrainingPlanGenerator.generate(goal, monday, currentCtl = 50.0, recentWeeklyKm = 45.0)

        assertEquals(TrainingPlanGenerator.MAX_WEEKS, plan.weeks.size)
        assertTrue(plan.weeks.none { it.phase == TrainingPhase.RACE })
        assertTrue(plan.weeks.none { it.phase == TrainingPhase.TAPER })
    }

    @Test
    fun `new runner with no history gets a conservative but non-zero plan`() {
        val goal = TrainingGoal(RaceDistance.TEN_K, monday.plusWeeks(10).plusDays(5))
        val plan = TrainingPlanGenerator.generate(goal, monday, currentCtl = 0.0, recentWeeklyKm = 0.0)

        assertTrue(plan.weeks.all { it.targetDistanceKm > 0.0 })
        val base = plan.weeks.first().targetDistanceKm
        val peak = plan.weeks.filter { it.phase == TrainingPhase.PEAK }.maxOf { it.targetDistanceKm }
        assertTrue(peak >= base)
    }
}
