package com.example.runtracker.data.beacon

import com.example.runtracker.domain.model.LiveLocationUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LoopbackLiveLocationTransportTest {

    private fun update(lat: Double, lng: Double) = LiveLocationUpdate(
        latitude = lat, longitude = lng, timestamp = Instant.EPOCH,
        elapsedSeconds = 0, distanceMeters = 0.0, paused = false,
    )

    @Test
    fun `publish updates latest and appends to trail for a follower`() = runTest {
        val transport = LoopbackLiveLocationTransport()
        transport.startBroadcast("ABC234")
        transport.publish("ABC234", update(1.0, 1.0))
        transport.publish("ABC234", update(2.0, 2.0))

        val snap = transport.observe("ABC234").first()
        assertEquals(2.0, snap.latest!!.latitude, 0.0)
        assertEquals(2, snap.trail.size)
        assertFalse(snap.ended)
    }

    @Test
    fun `endBroadcast marks snapshot ended`() = runTest {
        val transport = LoopbackLiveLocationTransport()
        transport.startBroadcast("ABC234")
        transport.publish("ABC234", update(1.0, 1.0))
        transport.endBroadcast("ABC234")

        assertTrue(transport.observe("ABC234").first().ended)
    }

    @Test
    fun `startBroadcast clears a previous session on the same code`() = runTest {
        val transport = LoopbackLiveLocationTransport()
        transport.startBroadcast("ABC234")
        transport.publish("ABC234", update(1.0, 1.0))
        transport.startBroadcast("ABC234")

        val snap = transport.observe("ABC234").first()
        assertEquals(null, snap.latest)
        assertTrue(snap.trail.isEmpty())
    }

    @Test
    fun `observing an unknown code yields an empty waiting snapshot`() = runTest {
        val snap = LoopbackLiveLocationTransport().observe("ZZZ999").first()
        assertEquals("ZZZ999", snap.code)
        assertEquals(null, snap.latest)
        assertFalse(snap.ended)
    }
}
