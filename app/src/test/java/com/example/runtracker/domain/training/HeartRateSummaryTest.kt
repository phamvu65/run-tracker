package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.HeartRateSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class HeartRateSummaryTest {

    private fun samples(vararg bpm: Int) = bpm.mapIndexed { i, v ->
        HeartRateSample(bpm = v, timestamp = Instant.EPOCH.plusSeconds(i.toLong()))
    }

    @Test
    fun `average is rounded and max is picked`() {
        val s = samples(120, 130, 141).summary()!!
        assertEquals(130, s.averageBpm) // (120+130+141)/3 = 130.33 -> 130
        assertEquals(141, s.maxBpm)
    }

    @Test
    fun `empty samples yield null`() {
        assertNull(emptyList<HeartRateSample>().summary())
    }
}
