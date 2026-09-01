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
import com.example.runtracker.ui.tracking.TrackingScreen

private object Routes {
    const val TRACKING = "tracking"
    const val DETAIL = "detail/{$ARG_ACTIVITY_ID}"
    fun detail(activityId: String) = "detail/$activityId"
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
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(ARG_ACTIVITY_ID) { type = NavType.StringType }),
        ) {
            ActivityDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
