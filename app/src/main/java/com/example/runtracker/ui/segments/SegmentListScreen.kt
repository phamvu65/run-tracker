package com.example.runtracker.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentListScreen(
    onBack: (() -> Unit)? = null,
    onSegmentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentListViewModel = hiltViewModel(),
) {
    val segments by viewModel.segments.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Segments") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (segments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Chưa có segment",
                    message = "Mở một buổi tập rồi bấm \"Tạo segment từ buổi này\" để so kè thời gian trên các đoạn quen thuộc.",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(segments, key = { it.id }) { segment ->
                AppListCard(
                    title = segment.name,
                    subtitle = "${formatDistanceKm(segment.distanceMeters)} · độ dốc ${segment.avgGrade.roundToInt()}%",
                    onClick = { onSegmentClick(segment.id) },
                )
            }
        }
    }
}
