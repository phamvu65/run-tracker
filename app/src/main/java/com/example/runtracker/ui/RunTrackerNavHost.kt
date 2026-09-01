package com.example.runtracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.runtracker.ui.detail.ARG_ACTIVITY_ID
import com.example.runtracker.ui.detail.ActivityDetailScreen
import com.example.runtracker.ui.fitness.FitnessScreen
import com.example.runtracker.ui.hrsensor.HrSensorScreen
import com.example.runtracker.ui.profile.ProfileScreen
import com.example.runtracker.ui.routes.ARG_ROUTE_ID
import com.example.runtracker.ui.routes.RouteBuilderScreen
import com.example.runtracker.ui.routes.RouteDetailScreen
import com.example.runtracker.ui.routes.RouteListScreen
import com.example.runtracker.ui.segments.ARG_SEGMENT_ID
import com.example.runtracker.ui.segments.SegmentCreateScreen
import com.example.runtracker.ui.segments.SegmentDetailScreen
import com.example.runtracker.ui.segments.SegmentListScreen
import com.example.runtracker.ui.tracking.TrackingScreen
import com.example.runtracker.ui.zones.ZoneSettingsScreen

private object Routes {
    const val TRACKING = "tracking"
    const val PROFILE = "profile"
    const val FITNESS = "fitness"
    const val ZONES = "zones"
    const val HR_SENSOR = "hr_sensor"
    const val SEGMENTS = "segments"
    const val SEGMENT_DETAIL = "segment/{$ARG_SEGMENT_ID}"
    const val SEGMENT_CREATE = "segment_create/{$ARG_ACTIVITY_ID}"
    fun segmentCreate(activityId: String) = "segment_create/$activityId"
    const val ROUTES = "routes"
    const val ROUTE_BUILDER = "route_builder"
    const val ROUTE_DETAIL = "route/{$ARG_ROUTE_ID}"
    const val DETAIL = "detail/{$ARG_ACTIVITY_ID}"
    fun detail(activityId: String) = "detail/$activityId"
    fun segment(segmentId: String) = "segment/$segmentId"
    fun route(routeId: String) = "route/$routeId"
}

private enum class TopLevelDest(val route: String, val label: String, val icon: ImageVector) {
    TRACKING(Routes.TRACKING, "Ghi", Icons.Filled.PlayArrow),
    FITNESS(Routes.FITNESS, "Fitness", Icons.Filled.Favorite),
    SEGMENTS(Routes.SEGMENTS, "Segments", Icons.Filled.Star),
    ROUTES(Routes.ROUTES, "Routes", Icons.Filled.LocationOn),
    PROFILE(Routes.PROFILE, "Hồ sơ", Icons.Filled.Person),
}

@Composable
fun RunTrackerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDest = currentRoute?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (TopLevelDest.entries.any { it.route == currentDest }) {
                NavigationBar {
                    TopLevelDest.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentDest == dest.route,
                            onClick = {
                                if (currentDest != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TRACKING,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TRACKING) {
                TrackingScreen(
                    onActivityClick = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBack = null,
                    onOpenZones = { navController.navigate(Routes.ZONES) },
                    onOpenRoutes = { navController.navigate(Routes.ROUTES) },
                    onOpenHrSensor = { navController.navigate(Routes.HR_SENSOR) },
                )
            }
            composable(Routes.HR_SENSOR) {
                HrSensorScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FITNESS) {
                FitnessScreen(onBack = null)
            }
            composable(Routes.ZONES) {
                ZoneSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SEGMENTS) {
                SegmentListScreen(
                    onBack = null,
                    onSegmentClick = { navController.navigate(Routes.segment(it)) },
                )
            }
            composable(
                route = Routes.SEGMENT_DETAIL,
                arguments = listOf(navArgument(ARG_SEGMENT_ID) { type = NavType.StringType }),
            ) {
                SegmentDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ROUTES) {
                RouteListScreen(
                    onBack = null,
                    onRouteClick = { navController.navigate(Routes.route(it)) },
                    onCreateRoute = { navController.navigate(Routes.ROUTE_BUILDER) },
                )
            }
            composable(Routes.ROUTE_BUILDER) {
                RouteBuilderScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { routeId ->
                        navController.popBackStack()
                        navController.navigate(Routes.route(routeId))
                    },
                )
            }
            composable(
                route = Routes.ROUTE_DETAIL,
                arguments = listOf(navArgument(ARG_ROUTE_ID) { type = NavType.StringType }),
            ) {
                RouteDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(ARG_ACTIVITY_ID) { type = NavType.StringType }),
            ) { entry ->
                val activityId = entry.arguments?.getString(ARG_ACTIVITY_ID)
                ActivityDetailScreen(
                    onBack = { navController.popBackStack() },
                    onCreateSegment = {
                        activityId?.let { navController.navigate(Routes.segmentCreate(it)) }
                    },
                )
            }
            composable(
                route = Routes.SEGMENT_CREATE,
                arguments = listOf(navArgument(ARG_ACTIVITY_ID) { type = NavType.StringType }),
            ) {
                SegmentCreateScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { segmentId ->
                        navController.popBackStack()
                        navController.navigate(Routes.segment(segmentId))
                    },
                )
            }
        }
    }
}
