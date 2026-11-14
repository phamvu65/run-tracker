package com.example.runtracker.tracking

import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.GpsTrackFilter
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GpsBaselineEvaluationTest {
    private val start = Instant.parse("2026-11-14T00:00:00Z")

    private fun point(index: Int) = RoutePoint(
        latitude = 10.0 + index * 0.00008,
        longitude = 106.0,
        altitude = 0.0,
        speedMps = null,
        accuracyMeters = 5f,
        timestamp = start.plusSeconds(index * 3L),
    )

    @Test
    fun nominalReplay() = runTest {
        val replay = List(1200, ::point).toMutableList()
        replay[100] = replay[100].copy(accuracyMeters = 40f)
        replay[200] = replay[200].copy(latitude = 10.25)
        replay[300] = replay[299]
        val writer = ReplayWriter(250)
        val job = launch { writer.flow.collect { writer.append(it) } }
        runCurrent()
        for (fix in replay) {
            writer.offer(fix)
            advanceTimeBy(3_000)
        }
        assertEquals(1200, writer.received)
        assertEquals(0, writer.rejected)
        assertEquals(1200, writer.reads)
        assertEquals(1197, writer.saved.size)
        assertEquals(replay.filterIndexed { index, _ -> index !in setOf(100, 200, 300) }, writer.saved)
        println("nominal received=1200 rejected=0 filtered=3 reads=1200 writes=1197 saved=1197")
        job.cancel()
    }

    @Test
    fun burstWithWriteInFlight() = runTest {
        val writer = ReplayWriter(250)
        val job = launch { writer.flow.collect { writer.append(it) } }
        runCurrent()
        writer.offer(point(0))
        runCurrent()
        for (index in 1..100) writer.offer(point(index))
        advanceTimeBy(101 * 250L)
        runCurrent()
        assertEquals(101, writer.received)
        assertEquals(101, writer.rejected + writer.saved.size)
        assertTrue(writer.rejected > 0)
        println("burst received=101 rejected=${writer.rejected} saved=${writer.saved.size}")
        job.cancel()
    }

    @Test
    fun stopWhileWriting() = runTest {
        val writer = ReplayWriter(250)
        val job = launch { writer.flow.collect { writer.append(it) } }
        runCurrent()
        writer.offer(point(0))
        advanceTimeBy(3_000)
        writer.offer(point(1))
        runCurrent()
        for (index in 2..4) writer.offer(point(index))
        assertEquals(2, writer.reads)
        assertEquals(0, writer.rejected)
        job.cancel()
        val finalized = writer.saved.toList()
        advanceTimeBy(3_000)
        assertEquals(listOf(point(0)), finalized)
        assertEquals(finalized, writer.saved)
        println("stop received=5 enqueued=5 started=2 finalized=1 missing=4")
    }

    private class ReplayWriter(private val writeDelayMs: Long) {
        private lateinit var producer: ProducerScope<RoutePoint>
        var received = 0
            private set
        var rejected = 0
            private set
        var reads = 0
            private set
        val saved = mutableListOf<RoutePoint>()
        val flow = callbackFlow {
            producer = this
            awaitClose()
        }

        fun offer(point: RoutePoint) {
            received++
            if (producer.trySend(point).isFailure) rejected++
        }

        suspend fun append(point: RoutePoint) {
            reads++
            val clean = GpsTrackFilter.sanitize(saved.lastOrNull(), listOf(point))
            if (clean.isNotEmpty()) {
                delay(writeDelayMs)
                saved.addAll(clean)
            }
        }
    }
}
