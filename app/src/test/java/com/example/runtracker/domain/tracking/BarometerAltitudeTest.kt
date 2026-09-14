package com.example.runtracker.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class BarometerAltitudeTest {

    @Test
    fun `sea level pressure at zero altitude equals reference pressure`() {
        val p0 = BarometerAltitude.seaLevelPressure(1013.25f, 0.0)
        assertEquals(1013.25, p0, 0.01)
    }

    @Test
    fun `calibrating then reading back the reference point returns the reference altitude`() {
        val p0 = BarometerAltitude.seaLevelPressure(950.0f, 550.0)
        val altitude = BarometerAltitude.altitudeFor(950.0f, p0)
        assertEquals(550.0, altitude, 0.01)
    }

    @Test
    fun `rising pressure after calibration means lower altitude`() {
        val p0 = BarometerAltitude.seaLevelPressure(1000.0f, 100.0)
        val higherPressureAltitude = BarometerAltitude.altitudeFor(1005.0f, p0)
        val lowerPressureAltitude = BarometerAltitude.altitudeFor(995.0f, p0)

        assert(higherPressureAltitude < 100.0)
        assert(lowerPressureAltitude > 100.0)
        assert(higherPressureAltitude < lowerPressureAltitude)
    }
}
