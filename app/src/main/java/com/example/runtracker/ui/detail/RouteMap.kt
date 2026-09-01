package com.example.runtracker.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.ui.common.PathMap
import com.google.android.gms.maps.model.LatLng

@Composable
fun RouteMap(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val latLngs = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    PathMap(latLngs = latLngs, modifier = modifier)
}
