package com.example.runtracker.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.ui.components.AppListCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    onBack: (() -> Unit)? = null,
    onRouteClick: (String) -> Unit,
    onCreateRoute: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouteListViewModel = hiltViewModel(),
) {
    val routes by viewModel.routes.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            com.example.runtracker.ui.components.CompactTopHeader(
                title = "Lộ trình đã lưu",
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                } else null,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoute,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Dựng route") },
            )
        },
    ) { padding ->
        if (routes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có route",
                    message = "Bấm \"Dựng route\" để vẽ một tuyến đường và dùng làm dẫn đường khi chạy.",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(routes, key = { it.id }) { route ->
                AppListCard(
                    title = route.name,
                    subtitle = formatDistanceKm(route.distanceMeters),
                    onClick = { onRouteClick(route.id) },
                )
            }
        }
    }
}
