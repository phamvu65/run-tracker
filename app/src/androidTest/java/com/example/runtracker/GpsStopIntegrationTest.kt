package com.example.runtracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.local.RunTrackerDatabase
import com.example.runtracker.data.repository.ActivityRepositoryImpl
import com.example.runtracker.di.ActivityRepositoryModule
import com.example.runtracker.di.DatabaseProviderModule
import com.example.runtracker.di.ElevationRepositoryModule
import com.example.runtracker.di.LocationBindModule
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ElevationRepository
import com.example.runtracker.domain.tracking.RunAggregator
import com.example.runtracker.tracking.LocationClient
import com.example.runtracker.tracking.LocationEvent
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingSession
import com.example.runtracker.tracking.TrackingStatus
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(
    LocationBindModule::class,
    ActivityRepositoryModule::class,
    DatabaseProviderModule::class,
    ElevationRepositoryModule::class,
)
@RunWith(AndroidJUnit4::class)
class GpsStopIntegrationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val base = System.currentTimeMillis() - 15_000
    @BindValue @JvmField val database: RunTrackerDatabase =
        Room.inMemoryDatabaseBuilder(context, RunTrackerDatabase::class.java).build()
    private val source = TestLocationClient()
    @BindValue @JvmField val locationClient: LocationClient = source
    private val secondWrite = CompletableDeferred<Unit>()
    private val releaseWrite = CompletableDeferred<Unit>()
    private val delegate = ActivityRepositoryImpl(database.activityDao(), Dispatchers.IO)
    @BindValue @JvmField val repository: ActivityRepository = object : ActivityRepository by delegate {
        override suspend fun appendRoutePoints(activityId: String, points: List<RoutePoint>): List<RoutePoint> {
            if (points.single().latitude == latitude(1)) {
                secondWrite.complete(Unit)
                releaseWrite.await()
            }
            return delegate.appendRoutePoints(activityId, points)
        }
    }
    @BindValue @JvmField val elevationRepository: ElevationRepository = object : ElevationRepository {
        override suspend fun elevationsFor(points: List<GeoPoint>) = Result.success(points.map { 0.0 })
    }
    @Inject lateinit var session: TrackingSession

    @Before fun inject() {
        hilt.inject()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @After fun cleanUp() {
        releaseWrite.complete(Unit)
        context.stopService(Intent(context, LocationTrackingService::class.java))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        database.close()
    }

    @Test fun stopWaitsForQueuedPointsAndPublishesPersistedStats(): Unit = runBlocking {
        withTimeout(60_000) {
            val (id, producer) = startRecording()
            send(producer, 1)
            secondWrite.await()
            for (index in 2..4) send(producer, index)

            LocationTrackingService.stop(context)
            source.stopped.receive()
            val beforeRelease = delegate.getRoutePoints(id)
            assertEquals(listOf(base), beforeRelease.map { it.timestamp.toEpochMilli() })
            assertNull(session.justFinishedActivityId.value)
            releaseWrite.complete(Unit)

            session.justFinishedActivityId.first { it == id }
            val points = delegate.getRoutePoints(id)
            assertEquals((0..4).map { base + it * 3_000 }, points.map { it.timestamp.toEpochMilli() })
            val activity = requireNotNull(delegate.getActivity(id))
            assertEquals(RunAggregator.fromPoints(points).distanceMeters, activity.distanceMeters, 0.001)
            assertTrue(activity.distanceMeters > 0.0)
            session.state.first { it.status == TrackingStatus.IDLE }
        }
    }

    @Test fun pauseDrainsOldPointsBeforeResumeStartsAnotherCollection(): Unit = runBlocking {
        withTimeout(60_000) {
            val (id, producer) = startRecording()
            send(producer, 1)
            secondWrite.await()
            send(producer, 2)
            LocationTrackingService.pause(context)
            source.stopped.receive()
            session.state.first { it.status == TrackingStatus.PAUSED }
            LocationTrackingService.resume(context)
            session.state.first { it.status == TrackingStatus.TRACKING }
            assertTrue(source.started.tryReceive().isFailure)
            releaseWrite.complete(Unit)
            val resumed = source.started.receive()
            assertEquals((0..2).map { base + it * 3_000 }, timestamps(id))
            send(resumed, 3)
            session.state.first { it.pointCount == 4 }
            LocationTrackingService.stop(context)
            session.justFinishedActivityId.first { it == id }
            assertEquals((0..3).map { base + it * 3_000 }, timestamps(id))
            session.state.first { it.status == TrackingStatus.IDLE }
        }
    }

    @Test fun discardWaitsForWriterBeforeDeletingActivity(): Unit = runBlocking {
        withTimeout(60_000) {
            val (id, producer) = startRecording()
            send(producer, 1)
            secondWrite.await()
            send(producer, 2)
            LocationTrackingService.discard(context)
            source.stopped.receive()
            assertEquals(listOf(base), timestamps(id))
            releaseWrite.complete(Unit)
            session.state.first { it.status == TrackingStatus.IDLE }
            assertNull(delegate.getActivity(id))
            assertTrue(timestamps(id).isEmpty())
            assertNull(session.justFinishedActivityId.value)
        }
    }

    private suspend fun startRecording(): Pair<String, ProducerScope<LocationEvent>> {
        LocationTrackingService.start(context)
        val producer = source.started.receive()
        val id = session.state.first { it.activityId != null }.activityId!!
        while (delegate.getActivity(id) == null) delay(10)
        send(producer, 0)
        session.state.first { it.pointCount == 1 }
        return id to producer
    }

    private fun send(producer: ProducerScope<LocationEvent>, index: Int) {
        val fix = Location("test").apply {
            latitude = latitude(index)
            longitude = 106.0
            accuracy = 5f
            time = base + index * 3_000
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        assertTrue(producer.trySend(LocationEvent.Fix(fix)).isSuccess)
    }

    private suspend fun timestamps(id: String) =
        delegate.getRoutePoints(id).map { it.timestamp.toEpochMilli() }

    private class TestLocationClient : LocationClient {
        val started = Channel<ProducerScope<LocationEvent>>(Channel.UNLIMITED)
        val stopped = Channel<Unit>(Channel.UNLIMITED)

        override fun locationUpdates(intervalMillis: Long, stopInput: Job) = callbackFlow {
            val stopHandle = stopInput.invokeOnCompletion { close() }
            started.send(this)
            awaitClose {
                stopHandle.dispose()
                stopped.trySend(Unit)
            }
        }
    }

    private companion object {
        fun latitude(index: Int) = 10.0 + index * 0.00008
    }
}
