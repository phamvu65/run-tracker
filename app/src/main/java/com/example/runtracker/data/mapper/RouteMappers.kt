package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.RouteEntity
import com.example.runtracker.data.local.entity.RouteWaypointEntity
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.RouteWaypoint
import com.example.runtracker.domain.model.TravelMode

fun Route.toRouteEntity(): RouteEntity = RouteEntity(
    id = id,
    userId = userId,
    name = name,
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    polyline = PolylineCodec.encode(polyline),
    isPublic = isPublic,
    createdAt = createdAt,
    travelMode = travelMode?.name,
    sourcePolyline = sourcePoints.takeIf { it.isNotEmpty() }?.let(PolylineCodec::encode),
    drawnFromSketch = drawnFromSketch,
    snappedToRoads = snappedToRoads,
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
    travelMode = TravelMode.entries.firstOrNull { it.name == travelMode },
    sourcePoints = sourcePolyline?.let(PolylineCodec::decode).orEmpty(),
    drawnFromSketch = drawnFromSketch,
    snappedToRoads = snappedToRoads,
    waypoints = waypoints.sortedBy { it.orderIndex }.map {
        RouteWaypoint(it.orderIndex, GeoPoint(it.latitude, it.longitude), it.instruction)
    },
)
