package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.RouteDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toRouteEntity
import com.example.runtracker.data.mapper.toWaypointEntities
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.repository.RouteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val dao: RouteDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RouteRepository {

    override fun observeRoutes(userId: String): Flow<List<Route>> =
        dao.observeRoutes(userId).map { routes ->
            // Danh sách: chưa cần waypoint, để rỗng cho nhẹ.
            routes.map { it.toDomain(emptyList()) }
        }

    override suspend fun getRoute(routeId: String): Route? = withContext(io) {
        val route = dao.getRoute(routeId) ?: return@withContext null
        route.toDomain(dao.getWaypoints(routeId))
    }

    override suspend fun saveRoute(route: Route) = withContext(io) {
        dao.saveRouteWithWaypoints(route.toRouteEntity(), route.toWaypointEntities())
    }

    override suspend fun deleteRoute(routeId: String) = dao.deleteRoute(routeId)
}
