package com.example.runtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.runtracker.ui.detail.ARG_ACTIVITY_ID
import com.example.runtracker.ui.detail.ActivityDetailScreen
import com.example.runtracker.ui.fitness.FitnessScreen
import com.example.runtracker.ui.profile.ProfileScreen
import com.example.runtracker.ui.routes.ARG_ROUTE_ID
import com.example.runtracker.ui.routes.RouteBuilderScreen
import com.example.runtracker.ui.routes.RouteDetailScreen
import com.example.runtracker.ui.routes.RouteListScreen
import com.example.runtracker.ui.segments.ARG_SEGMENT_ID
import com.example.runtracker.ui.segments.SegmentDetailScreen
import com.example.runtracker.ui.segments.SegmentListScreen
import com.example.runtracker.ui.tracking.TrackingScreen
import com.example.runtracker.ui.zones.ZoneSettingsScreen

private object Routes {
    const val TRACKING = "tracking"
    const val PROFILE = "profile"
    const val FITNESS = "fitness"
    const val ZONES = "zones"
    const val SEGMENTS = "segments"
    const val SEGMENT_DETAIL = "segment/{$ARG_SEGMENT_ID}"
    const val ROUTES = "routes"
    const val ROUTE_BUILDER = "route_builder"
    const val ROUTE_DETAIL = "route/{$ARG_ROUTE_ID}"
    const val DETAIL = "detail/{$ARG_ACTIVITY_ID}"
    fun detail(activityId: String) = "detail/$activityId"
    fun segment(segmentId: String) = "segment/$segmentId"
    fun route(routeId: String) = "route/$routeId"
}

@Composable
fun RunTrackerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.TRACKING,
        modifier = modifier,
    ) {
        composable(Routes.TRACKING) {
            TrackingScreen(
                onActivityClick = { navController.navigate(Routes.detail(it)) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onFitnessClick = { navController.navigate(Routes.FITNESS) },
                onSegmentsClick = { navController.navigate(Routes.SEGMENTS) },
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenZones = { navController.navigate(Routes.ZONES) },
                onOpenRoutes = { navController.navigate(Routes.ROUTES) },
            )
        }
        composable(Routes.FITNESS) {
            FitnessScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ZONES) {
            ZoneSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEGMENTS) {
            SegmentListScreen(
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
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
        ) {
            ActivityDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
