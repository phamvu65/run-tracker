package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {

    fun observeRoutes(userId: String): Flow<List<Route>>
    suspend fun getRoute(routeId: String): Route?
    suspend fun saveRoute(route: Route)
    suspend fun deleteRoute(routeId: String)
}
