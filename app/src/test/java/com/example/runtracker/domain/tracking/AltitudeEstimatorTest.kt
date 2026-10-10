package com.example.runtracker.domain.tracking

import org.junit.Assert.*
import org.junit.Test

class AltitudeEstimatorTest {
    @Test fun `missing and inaccurate GPS cannot calibrate pressure to zero`() {
        val estimator = AltitudeEstimator()
        estimator.updatePressure(1000f, 0)
        assertNull(estimator.read(null, null, 0))
        assertNull(estimator.read(100.0, 100f, 0))
        assertNull(estimator.read(100.0, null, 0))
        assertEquals(100.0, estimator.read(100.0, 5f, 0)!!.meters, 0.01)
    }

    @Test fun `stale pressure falls back to GPS or missing rather than freezing height`() {
        val estimator = AltitudeEstimator()
        estimator.updatePressure(1000f, 0)
        assertTrue(estimator.read(100.0, 5f, 0)!!.barometric)
        val fallback = estimator.read(105.0, 5f, 6000)!!
        assertFalse(fallback.barometric)
        assertEquals(105.0, fallback.meters, 0.01)
        assertNull(estimator.read(null, null, 7000))
    }

    @Test fun `zero metres is a valid measured altitude`() {
        val estimator = AltitudeEstimator()
        assertEquals(0.0, estimator.read(0.0, 5f, 0)!!.meters, 0.0)
    }

    @Test fun `returning sensor recalibrates after outage instead of restoring old baseline`() {
        val estimator = AltitudeEstimator()
        estimator.updatePressure(1000f, 0)
        estimator.read(100.0, 5f, 0)
        assertNull(estimator.read(null, null, 6000))
        estimator.updatePressure(990f, 7000)
        assertNull(estimator.read(null, null, 7000))
        assertEquals(120.0, estimator.read(120.0, 5f, 7000)!!.meters, 0.01)
    }
}
