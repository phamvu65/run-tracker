package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.PlannedSession
import com.example.runtracker.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyTrainingSuggestionTest {

    @Test
    fun `planned session overrides tsb logic`() {
        val planned = PlannedSession(LocalDate.EPOCH, SessionType.LONG_RUN, 18.0, "Long run 18 km")
        val s = DailyTrainingSuggestion.suggest(tsb = -40.0, ctl = 50.0, plannedToday = planned)
        assertEquals(SessionType.LONG_RUN, s.type)
        assertTrue(s.rationale.contains("18 km"))
    }

    @Test
    fun `no data when tsb missing`() {
        assertEquals(
            SessionType.EASY,
            DailyTrainingSuggestion.suggest(tsb = null, ctl = null, plannedToday = null).type,
        )
    }

    @Test
    fun `very negative tsb suggests rest`() {
        assertEquals(
            SessionType.REST,
            DailyTrainingSuggestion.suggest(tsb = -35.0, ctl = 40.0, plannedToday = null).type,
        )
    }

    @Test
    fun `mildly negative tsb suggests easy`() {
        assertEquals(
            SessionType.EASY,
            DailyTrainingSuggestion.suggest(tsb = -15.0, ctl = 40.0, plannedToday = null).type,
        )
    }

    @Test
    fun `positive tsb suggests quality`() {
        assertEquals(
            SessionType.TEMPO,
            DailyTrainingSuggestion.suggest(tsb = 12.0, ctl = 40.0, plannedToday = null).type,
        )
    }

    @Test
    fun `low ctl adds a base-building note`() {
        val s = DailyTrainingSuggestion.suggest(tsb = 0.0, ctl = 10.0, plannedToday = null)
        assertTrue(s.rationale.contains("Nền còn thấp"))
    }
}
