package com.example.runtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.runtracker.data.local.entity.RouteEntity
import com.example.runtracker.data.local.entity.RouteWaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    // ---- Route ----

    @Upsert
    suspend fun upsertRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRoute(routeId: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeRoutes(userId: String): Flow<List<RouteEntity>>

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    // ---- RouteWaypoint ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<RouteWaypointEntity>)

    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getWaypoints(routeId: String): List<RouteWaypointEntity>

    @Query("DELETE FROM route_waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypoints(routeId: String)

    @Transaction
    suspend fun saveRouteWithWaypoints(route: RouteEntity, waypoints: List<RouteWaypointEntity>) {
        upsertRoute(route)
        deleteWaypoints(route.id)
        insertWaypoints(waypoints)
    }
}
