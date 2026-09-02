package com.example.runtracker.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.ui.common.PathMap
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val route by viewModel.route.collectAsState()

    LaunchedEffect(viewModel.deleted) {
        if (viewModel.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Xoá route")
                    }
                },
            )
        },
    ) { padding ->
        val r = route ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (r.polyline.size >= 2) {
                PathMap(points = r.polyline, modifier = Modifier.fillMaxWidth().height(260.dp))
            }

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(formatDistanceKm(r.distanceMeters), style = MaterialTheme.typography.titleLarge)

                val steps = r.waypoints.filter { !it.instruction.isNullOrBlank() }
                if (steps.isNotEmpty()) {
                    SectionHeader("Chỉ đường")
                    steps.forEach { wp ->
                        Text("${wp.orderIndex + 1}. ${wp.instruction}", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        "${r.waypoints.size} điểm mốc (không có chỉ đường chi tiết).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
