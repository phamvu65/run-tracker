package com.example.runtracker.domain.tracking

/** Chooses only fresh pressure samples, calibrated from a valid GPS altitude.
 * A source change must start a new gain/loss baseline in the caller.
 */
class AltitudeEstimator {
    data class Reading(val meters: Double, val barometric: Boolean)
    private var pressure: Float? = null
    private var pressureAtMillis: Long? = null
    private var seaLevelPressure: Double? = null

    fun updatePressure(hpa: Float, elapsedMillis: Long) {
        if (!hpa.isFinite() || hpa !in 300f..1100f) return
        pressure = hpa
        pressureAtMillis = elapsedMillis
    }

    fun read(gpsAltitude: Double?, verticalAccuracy: Float?, elapsedMillis: Long): Reading? {
        val gps = gpsAltitude?.takeIf {
            it.isFinite() && it in -500.0..9000.0 && verticalAccuracy != null &&
                verticalAccuracy.isFinite() && verticalAccuracy in 0f..20f
        }
        val age = pressureAtMillis?.let { elapsedMillis - it }
        val currentPressure = pressure?.takeIf { age != null && age in 0..5_000 }
        if (currentPressure == null) {
            // Recalibrate after a sensor outage; do not reuse an old weather reference.
            seaLevelPressure = null
            return gps?.let { Reading(it, false) }
        }
        if (seaLevelPressure == null && gps != null) {
            seaLevelPressure = BarometerAltitude.seaLevelPressure(currentPressure, gps)
        }
        return seaLevelPressure?.let {
            Reading(BarometerAltitude.altitudeFor(currentPressure, it), true)
        } ?: gps?.let { Reading(it, false) }
    }
}
