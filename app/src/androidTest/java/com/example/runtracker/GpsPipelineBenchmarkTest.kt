package com.example.runtracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.local.RunTrackerDatabase
import com.example.runtracker.data.local.dao.ActivityDao
import com.example.runtracker.data.local.entity.RoutePointEntity
import com.example.runtracker.data.repository.ActivityRepositoryImpl
import com.example.runtracker.di.ActivityRepositoryModule
import com.example.runtracker.di.DatabaseProviderModule
import com.example.runtracker.di.ElevationRepositoryModule
import com.example.runtracker.di.LocationBindModule
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ElevationRepository
import com.example.runtracker.domain.repository.RouteRepository
import com.example.runtracker.domain.usecase.FinalizeActivityUseCase
import com.example.runtracker.tracking.BeaconController
import com.example.runtracker.tracking.LocationClient
import com.example.runtracker.tracking.LocationEvent
import com.example.runtracker.tracking.LocationTrackingService
import com.example.runtracker.tracking.TrackingSession
import com.example.runtracker.tracking.TrackingStateStore
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.ui.tracking.TrackingViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(
    LocationBindModule::class,
    ActivityRepositoryModule::class,
    DatabaseProviderModule::class,
    ElevationRepositoryModule::class,
)
@RunWith(AndroidJUnit4::class)
class GpsPipelineBenchmarkTest {
    @get:Rule val hilt = HiltAndroidRule(this)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val databaseName = "gps-benchmark-${UUID.randomUUID()}.db"
    @BindValue @JvmField val database: RunTrackerDatabase =
        Room.databaseBuilder(context, RunTrackerDatabase::class.java, databaseName).build()
    private val measurements = List(POINT_COUNT) { Measurement() }
    private val byTimestamp = ConcurrentHashMap<Long, Measurement>()
    private val inserts = AtomicInteger()
    private val lastPointReads = AtomicInteger()
    private val traceEmissions = AtomicInteger()
    private val traceRows = AtomicInteger()
    private val sourceStarted = CompletableDeferred<ProducerScope<LocationEvent>>()
    private val dao = database.activityDao()
    private val delegate = ActivityRepositoryImpl(object : ActivityDao by dao {
        override suspend fun getLastRoutePoint(activityId: String): RoutePointEntity? {
            lastPointReads.incrementAndGet()
            return dao.getLastRoutePoint(activityId)
        }

        override suspend fun insertRoutePoints(points: List<RoutePointEntity>) {
            inserts.incrementAndGet()
            val start = now()
            dao.insertRoutePoints(points)
            val end = now()
            for (point in points) {
                byTimestamp.getValue(point.timestamp).apply {
                    insertNs = end - start
                    storedAtNs = end
                }
            }
        }
    }, Dispatchers.IO)

    @BindValue @JvmField val repository: ActivityRepository = object : ActivityRepository by delegate {
        override fun observeRoutePoints(activityId: String) =
            delegate.observeRoutePoints(activityId).onEach {
                traceEmissions.incrementAndGet()
                traceRows.addAndGet(it.size)
            }

        override suspend fun appendRoutePoints(activityId: String, points: List<RoutePoint>): List<RoutePoint> {
            val start = now()
            return delegate.appendRoutePoints(activityId, points).also {
                val elapsed = now() - start
                for (point in points) byTimestamp.getValue(point.timestamp.toEpochMilli()).appendNs = elapsed
            }
        }
    }
    @BindValue @JvmField val locationClient: LocationClient = object : LocationClient {
        override fun locationUpdates(intervalMillis: Long, stopInput: Job) = callbackFlow {
            check(intervalMillis == INTERVAL_MS)
            val handle = stopInput.invokeOnCompletion { close() }
            sourceStarted.complete(this)
            awaitClose { handle.dispose() }
        }
    }
    @BindValue @JvmField val elevationRepository: ElevationRepository = object : ElevationRepository {
        override suspend fun elevationsFor(points: List<GeoPoint>) = Result.success(points.map { 0.0 })
    }

    @Inject lateinit var session: TrackingSession
    @Inject lateinit var routeRepository: RouteRepository
    @Inject lateinit var stateStore: TrackingStateStore
    @Inject lateinit var finalizeActivity: FinalizeActivityUseCase
    @Inject lateinit var beaconController: BeaconController
    private val viewModels = ViewModelStore()

    @Before fun setUp() {
        hilt.inject()
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        keyEvent("WAKEUP")
    }

    @After fun cleanUp() {
        keyEvent("WAKEUP")
        context.stopService(Intent(context, LocationTrackingService::class.java))
        instrumentation.runOnMainSync { viewModels.clear() }
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun screenOnWithLiveTrace() = measure(observeTrace = true)

    @Test fun screenOffWithoutLiveTrace() = measure(observeTrace = false)

    private fun measure(observeTrace: Boolean): Unit = runBlocking {
        withTimeout(180_000) {
            val viewModel = withContext(Dispatchers.Main) {
                if (observeTrace) newTrackingViewModel() else null
            }
            val observer = viewModel?.let { model ->
                launch(Dispatchers.Main) {
                    model.liveTrace.collect { trace ->
                        val observedAt = now()
                        // An emission contains the whole trace. Credit each point only on first appearance.
                        trace.forEachIndexed { index, _ ->
                            measurements[index].apply {
                                if (observedAtNs == 0L) observedAtNs = observedAt
                            }
                        }
                    }
                }
            }
            try {
                LocationTrackingService.start(context)
                val producer = sourceStarted.await()
                val id = requireNotNull(session.state.value.activityId)
                if (!observeTrace) keyEvent("SLEEP")
                val power = context.getSystemService(PowerManager::class.java)
                withTimeout(10_000) {
                    while (power.isInteractive != observeTrace) delay(50)
                }

                val start = SystemClock.elapsedRealtime()
                val base = System.currentTimeMillis()
                for ((index, sample) in measurements.withIndex()) {
                    val age = SystemClock.elapsedRealtime() - start
                    delay((index * INTERVAL_MS - age).coerceAtLeast(0))
                    val fix = Location("benchmark").apply {
                        latitude = 10.0 + index * 0.00008
                        longitude = 106.0
                        accuracy = 5f
                        time = base + index * INTERVAL_MS
                        elapsedRealtimeNanos = now()
                    }
                    byTimestamp[fix.time] = sample
                    sample.receivedAtNs = now()
                    assertTrue(producer.trySend(LocationEvent.Fix(fix)).isSuccess)
                }

                // Measure Stop on a settled normal run. The separate integration test covers queued writes.
                session.state.first { it.pointCount == POINT_COUNT }
                if (viewModel != null) viewModel.liveTrace.first { it.size == POINT_COUNT }
                assertEquals(observeTrace, power.isInteractive)
                val stopAt = now()
                LocationTrackingService.stop(context)
                session.justFinishedActivityId.first { it == id }
                val stopNs = now() - stopAt
                session.state.first { it.status == TrackingStatus.IDLE }
                val stored = dao.getRoutePoints(id)
                assertEquals(List(POINT_COUNT) { base + it * INTERVAL_MS }, stored.map { it.timestamp })
                assertEquals(POINT_COUNT, inserts.get())
                assertEquals(POINT_COUNT, lastPointReads.get())
                val mode = if (observeTrace) "screen_on" else "screen_off"
                report(mode, stopNs, observeTrace)
            } finally {
                observer?.cancelAndJoin()
            }
        }
    }

    private fun newTrackingViewModel(): TrackingViewModel {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                modelClass.cast(TrackingViewModel(
                    session, repository, routeRepository, stateStore, finalizeActivity, beaconController,
                ))!!
        }
        return ViewModelProvider(viewModels, factory)[TrackingViewModel::class.java]
    }

    private fun report(mode: String, stopNs: Long, observeTrace: Boolean) {
        val samples = measurements.drop(WARMUP_POINTS)
        fun metric(name: String, values: List<Long>): String {
            assertTrue(values.all { it > 0 })
            val sorted = values.sorted()
            fun percentile(p: Double) = sorted[ceil(p * sorted.size).toInt() - 1] / 1_000_000.0
            return "${name}_p50_ms=${percentile(0.50)} ${name}_p95_ms=${percentile(0.95)}"
        }
        val metrics = mutableListOf(
            metric("insert", samples.map { it.insertNs }),
            metric("append", samples.map { it.appendNs }),
            metric("receive_to_store", samples.map { it.storedAtNs - it.receivedAtNs }),
        )
        if (observeTrace) metrics += metric("receive_to_trace", samples.map { it.observedAtNs - it.receivedAtNs })
        Log.i("GpsBenchmark", "mode=$mode points=$POINT_COUNT measured=${samples.size} " +
            "inserts=${inserts.get()} last_point_reads=${lastPointReads.get()} " +
            "trace_emissions=${traceEmissions.get()} trace_rows=${traceRows.get()} " +
            "stop_ms=${stopNs / 1_000_000.0} ${metrics.joinToString(" ")}")
    }

    private fun keyEvent(key: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_$key"),
        ).use { it.readBytes() }
    }

    private class Measurement {
        @Volatile var receivedAtNs = 0L
        @Volatile var storedAtNs = 0L
        @Volatile var insertNs = 0L
        @Volatile var appendNs = 0L
        @Volatile var observedAtNs = 0L
    }

    private companion object {
        const val INTERVAL_MS = 3_000L
        const val POINT_COUNT = 24
        const val WARMUP_POINTS = 4
        fun now() = SystemClock.elapsedRealtimeNanos()
    }
}
