package com.example.runtracker.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Photon (Komoot) — reverse geocoding dựa trên dữ liệu OpenStreetMap, miễn phí, không cần API key.
 * KHÔNG dùng Nominatim chính thức (`nominatim.openstreetmap.org`): domain con của `openstreetmap.org`
 * bị một số ISP ở VN (FPT) đầu độc DNS về 127.0.0.1 — cùng lý do map tile đã đổi sang
 * `tile.openstreetmap.de` (xem `RunTrackerApp`/`OsmMap.kt`). `photon.komoot.io` nằm ở domain khác
 * nên không bị chặn. Không hỗ trợ `accept-language=vi` (chỉ default/de/en/fr) nên trả tên địa
 * phương mặc định (thường là tiếng Việt sẵn vì lấy từ tag OSM gốc).
 */
interface GeocodingApi {
    @GET("https://photon.komoot.io/reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): PhotonResponse
}

@Serializable
data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList(),
)

@Serializable
data class PhotonFeature(
    val properties: PhotonProperties? = null,
)

@Serializable
data class PhotonProperties(
    val name: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val county: String? = null,
    val city: String? = null,
    val state: String? = null,
)

/** Ghép "Quận/Huyện, Tỉnh/Thành phố" từ feature đầu tiên (gần nhất) — field OSM không đồng nhất giữa các nước. */
fun PhotonResponse.toLocationName(): String? {
    val props = features.firstOrNull()?.properties ?: return null
    val district = props.district ?: props.locality ?: props.county
    val region = props.city ?: props.state
    val parts = listOfNotNull(district, region).distinct()
    if (parts.isEmpty()) return null
    return parts.joinToString(", ")
}
