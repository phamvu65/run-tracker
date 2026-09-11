package com.example.runtracker.data.remote

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * OSRM của FOSSGIS (`routing.openstreetmap.de`) — miễn phí, KHÔNG cần API key.
 * `/route` bám đường qua một chuỗi điểm. Lời gọi dùng URL tuyệt đối; [BASE_URL] chỉ
 * để Retrofit khởi tạo.
 */
interface DirectionsApi {

    @GET
    suspend fun route(@Url url: String): OsrmRouteResponse

    companion object {
        const val BASE_URL = "https://routing.openstreetmap.de/"
    }
}
