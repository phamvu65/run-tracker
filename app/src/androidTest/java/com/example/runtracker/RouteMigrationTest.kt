package com.example.runtracker

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.local.ALL_MIGRATIONS
import com.example.runtracker.data.local.RunTrackerDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteMigrationTest {
    @Test fun migration6To7PreservesLegacyRoutesAndPassesRoomValidation() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "route-migration-test.db"
        context.deleteDatabase(name)
        try {
            val json = instrumentation.context.assets.open(
                "com.example.runtracker.data.local.RunTrackerDatabase/6.json",
            ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                val entities = json.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                    for (j in 0 until indices.length()) {
                        old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
                    }
                }
                val setup = json.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
                old.execSQL("INSERT INTO routes(id,userId,name,distanceMeters,elevationGainMeters,polyline,isPublic,createdAt) VALUES ('legacy','u','Lake',123.0,0.0,'',0,42)")
                old.execSQL("INSERT INTO route_waypoints(routeId,orderIndex,latitude,longitude,instruction) VALUES ('legacy',0,10.0,106.0,'Finish')")
                old.version = 6
            }
            val database = Room.databaseBuilder(context, RunTrackerDatabase::class.java, name)
                .addMigrations(*ALL_MIGRATIONS).build()
            try {
                // Room opens the file, runs the actual migration, then validates the full v7 schema.
                val route = database.routeDao().getRoute("legacy")!!
                assertEquals("Lake", route.name)
                assertEquals(42L, route.createdAt)
                assertEquals(123.0, route.distanceMeters, 0.0)
                assertNull(route.travelMode)
                assertNull(route.sourcePolyline)
                assertFalse(route.snappedToRoads)
                assertFalse(route.drawnFromSketch)
                assertEquals(1, database.routeDao().getWaypoints("legacy").size)
            } finally { database.close() }
        } finally { context.deleteDatabase(name) }
    }
}
