package com.example.runtracker.domain.tracking

import java.time.Instant
import kotlin.math.abs

/** Streaming median + reversal hysteresis. Small steps accumulate; small oscillations do not.
 * The last sample is pending until a neighbour confirms it (up to one sample of latency).
 * Missing data and long gaps start a new baseline, never a fabricated climb.
 */
class ElevationAccumulator {
    var gainMeters = 0.0
        private set
    var lossMeters = 0.0
        private set
    private val window = ArrayDeque<Double>()
    private var baseline: Double? = null
    private var direction = 0
    private var lastTime: Instant? = null
    private var lastRaw: Double? = null

    fun add(altitude: Double?, time: Instant) {
        val dt = lastTime?.let { (time.toEpochMilli() - it.toEpochMilli()) / 1000.0 }
        if (dt != null && dt <= 0) return
        if (altitude == null || !altitude.isFinite() || (dt != null && dt > 30)) {
            breakSegment()
        }
        if (altitude == null || !altitude.isFinite()) return
        // Reject isolated vertical jumps without shifting the accepted reference.
        if (dt != null && dt in 0.001..30.0 && lastRaw != null &&
            abs(altitude - lastRaw!!) > maxOf(10.0, dt * 3.0)) return
        lastTime = time
        lastRaw = altitude
        window.addLast(altitude)
        if (window.size < 3) return
        val filtered = window.sorted()[1]
        window.removeFirst()
        val anchor = baseline
        if (anchor == null) { baseline = filtered; return }
        val delta = filtered - anchor
        when {
            direction >= 0 && delta >= REVERSAL_METERS -> {
                gainMeters += delta; baseline = filtered; direction = 1
            }
            direction <= 0 && delta <= -REVERSAL_METERS -> {
                lossMeters -= delta; baseline = filtered; direction = -1
            }
            direction == 1 && delta > 0 -> { gainMeters += delta; baseline = filtered }
            direction == -1 && delta < 0 -> { lossMeters -= delta; baseline = filtered }
            direction == 1 && delta <= -REVERSAL_METERS -> {
                lossMeters -= delta; baseline = filtered; direction = -1
            }
            direction == -1 && delta >= REVERSAL_METERS -> {
                gainMeters += delta; baseline = filtered; direction = 1
            }
        }
    }

    private fun breakSegment() {
        window.clear(); baseline = null; direction = 0; lastTime = null; lastRaw = null
    }

    companion object { const val REVERSAL_METERS = 3.0 }
}
