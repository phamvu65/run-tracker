package com.example.runtracker.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.ui.common.MapGuideLineColor
import com.example.runtracker.ui.common.MapLine
import com.example.runtracker.ui.common.OsmMap
import com.example.runtracker.ui.common.startFinishMarkers
import com.example.runtracker.ui.components.SuccessCheckModal
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentCreateScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentCreateViewModel = hiltViewModel(),
) {
    val total = viewModel.totalDistanceMeters
    var range by remember(total) { mutableStateOf(0f..total) }
    var name by remember { mutableStateOf("") }
    val density = LocalDensity.current
    var panelHeight by remember { mutableStateOf(0.dp) }

    val fullLine = viewModel.routePoints
    val subLine = remember(range, viewModel.routePoints) {
        viewModel.subRange(range.start, range.endInclusive)
    }
    val outlineColor = MapGuideLineColor
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tạo segment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OsmMap(
                modifier = Modifier.fillMaxSize(),
                lines = buildList {
                    if (fullLine.size >= 2) add(MapLine(fullLine, outlineColor, widthDp = 3f))
                    if (subLine.size >= 2) {
                        add(MapLine(subLine, primaryColor, widthDp = 4.5f, showDirection = true))
                    }
                },
                markers = startFinishMarkers(subLine),
                fitToLines = true,
                controlsPadding = PaddingValues(end = 12.dp, bottom = panelHeight + 12.dp, top = 12.dp),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { panelHeight = with(density) { it.height.toDp() } },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        "Đoạn: ${formatDistanceKm(range.start.toDouble())} → " +
                            "${formatDistanceKm(range.endInclusive.toDouble())} " +
                            "(${formatDistanceKm((range.endInclusive - range.start).toDouble())})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (total > 0f) {
                        RangeSlider(
                            value = range,
                            onValueChange = { range = it },
                            valueRange = 0f..total,
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên segment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.save(name.trim(), range.start, range.endInclusive) },
                        enabled = name.isNotBlank() && !viewModel.saving &&
                            (range.endInclusive - range.start) >= 100f,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (viewModel.saving) "Đang lưu…" else "Lưu segment") }
                }
            }
        }
    }

    val savedSegmentId = viewModel.savedSegmentId
    if (savedSegmentId != null) {
        SuccessCheckModal(
            message = "Đã tạo segment!",
            onDismiss = {
                viewModel.consumeSaved()
                onCreated(savedSegmentId)
            },
        )
    }
}
