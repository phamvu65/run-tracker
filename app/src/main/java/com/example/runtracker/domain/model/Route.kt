package com.example.runtracker.domain.model

/** Chế độ di chuyển cho Directions API. */
enum class TravelMode(val apiValue: String) {
    WALKING("walking"),
    CYCLING("bicycling"),
}

/** Một bước rẽ trong route. */
data class RouteStep(
    val instruction: String,
    val location: GeoPoint,
    val distanceMeters: Double,
)

/** Kết quả dựng route (từ Directions hoặc fallback đường thẳng). */
data class PlannedRoute(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
    val steps: List<RouteStep>,
    val snappedToRoads: Boolean,
)

/** Route đã lưu. */
data class Route(
    val id: String,
    val userId: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val polyline: List<GeoPoint>,
    val isPublic: Boolean,
    val createdAt: Long,
    val waypoints: List<RouteWaypoint>,
)

data class RouteWaypoint(
    val orderIndex: Int,
    val location: GeoPoint,
    val instruction: String?,
)
