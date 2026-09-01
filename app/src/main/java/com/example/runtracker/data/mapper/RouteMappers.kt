package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.RouteEntity
import com.example.runtracker.data.local.entity.RouteWaypointEntity
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.RouteWaypoint

fun Route.toRouteEntity(): RouteEntity = RouteEntity(
    id = id,
    userId = userId,
    name = name,
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    polyline = PolylineCodec.encode(polyline),
    isPublic = isPublic,
    createdAt = createdAt,
)

fun Route.toWaypointEntities(): List<RouteWaypointEntity> = waypoints.map { wp ->
    RouteWaypointEntity(
        routeId = id,
        orderIndex = wp.orderIndex,
        latitude = wp.location.latitude,
        longitude = wp.location.longitude,
        instruction = wp.instruction,
    )
}

fun RouteEntity.toDomain(waypoints: List<RouteWaypointEntity>): Route = Route(
    id = id,
    userId = userId,
    name = name,
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    polyline = PolylineCodec.decode(polyline),
    isPublic = isPublic,
    createdAt = createdAt,
    waypoints = waypoints.sortedBy { it.orderIndex }.map {
        RouteWaypoint(it.orderIndex, GeoPoint(it.latitude, it.longitude), it.instruction)
    },
)
