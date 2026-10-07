package com.example.runtracker.domain.model

/**
 * Chế độ di chuyển cho routing. Dùng OSRM của FOSSGIS (`routing.openstreetmap.de`) —
 * miễn phí, không cần key. [osrmHost] là nhánh máy chủ theo hồ sơ, [osrmProfile] là
 * tên profile trong đường dẫn.
 */
enum class TravelMode(val osrmHost: String, val osrmProfile: String) {
    WALKING("routed-foot", "foot"),
    CYCLING("routed-bike", "bike"),
}

/** Một bước rẽ trong route. */
data class RouteStep(
    val instruction: String,
    val location: GeoPoint,
    val distanceMeters: Double,
)

/** Một chặng giữa 2 via point liên tiếp trong route đã bám đường qua OSRM. */
data class RouteLeg(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
)

/**
 * Kết quả dựng lộ trình. [snappedToRoads] chỉ đúng khi toàn bộ hình dạng lấy từ bộ định tuyến.
 * [legs] là các chặng đường thật giữa những điểm trung gian.
 * Thất bại trả polyline rỗng và snappedToRoads=false, không tạo đường thẳng thay thế.
 * [gapPolylines] dành cho kết quả cũ có đoạn chưa xác minh; bộ dựng hiện tại không tạo đoạn này.
 */
data class PlannedRoute(
    val polyline: List<GeoPoint>,
    val distanceMeters: Double,
    val steps: List<RouteStep>,
    val snappedToRoads: Boolean,
    val legs: List<RouteLeg> = emptyList(),
    val gapPolylines: List<List<GeoPoint>> = emptyList(),
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
    val travelMode: TravelMode? = null,
    val sourcePoints: List<GeoPoint> = emptyList(),
    val drawnFromSketch: Boolean = false,
    val snappedToRoads: Boolean = false,
)

data class RouteWaypoint(
    val orderIndex: Int,
    val location: GeoPoint,
    val instruction: String?,
)
