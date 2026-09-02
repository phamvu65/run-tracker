package com.example.runtracker.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.ui.common.PathMap

@Composable
fun RouteMap(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val geoPoints = remember(points) { points.map { GeoPoint(it.latitude, it.longitude) } }
    PathMap(points = geoPoints, modifier = modifier)
}
