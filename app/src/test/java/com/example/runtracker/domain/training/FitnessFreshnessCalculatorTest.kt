package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.FitnessFreshnessSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FitnessFreshnessCalculatorTest {

    private val d0: LocalDate = LocalDate.parse("2026-09-01")

    @Test
    fun `first day starts from zero`() {
        val s = FitnessFreshnessCalculator.next(null, "u", d0, trimpToday = 100.0, computedAt = 0)

        assertEquals(100.0 / 42.0, s.ctl, 1e-9)
        assertEquals(100.0 / 7.0, s.atl, 1e-9)
        assertEquals(0.0, s.tsb, 1e-9)
    }

    @Test
    fun `rest day decays fitness and fatigue toward zero`() {
        val yesterday = FitnessFreshnessSnapshot("u", d0, ctl = 40.0, atl = 60.0, tsb = 0.0, computedAt = 0)

        val s = FitnessFreshnessCalculator.next(yesterday, "u", d0.plusDays(1), trimpToday = 0.0)

        assertEquals(40.0 + (0.0 - 40.0) / 42.0, s.ctl, 1e-9)
        assertEquals(60.0 + (0.0 - 60.0) / 7.0, s.atl, 1e-9)
        // TSB dùng CTL/ATL của HÔM QUA
        assertEquals(40.0 - 60.0, s.tsb, 1e-9)
    }

    @Test
    fun `atl reacts faster than ctl to a hard day`() {
        val base = FitnessFreshnessSnapshot("u", d0, ctl = 30.0, atl = 30.0, tsb = 0.0, computedAt = 0)

        val s = FitnessFreshnessCalculator.next(base, "u", d0.plusDays(1), trimpToday = 150.0)

        val ctlDelta = s.ctl - 30.0
        val atlDelta = s.atl - 30.0
        assert(atlDelta > ctlDelta * 5) { "ATL should move much faster: ctl=$ctlDelta atl=$atlDelta" }
    }
}
