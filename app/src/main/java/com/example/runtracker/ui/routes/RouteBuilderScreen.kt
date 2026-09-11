package com.example.runtracker.ui.routes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.hasLocationPermission
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.ui.common.MapGuideLineColor
import com.example.runtracker.ui.common.MapLine
import com.example.runtracker.ui.common.MapMarker
import com.example.runtracker.ui.common.OsmMap
import com.example.runtracker.ui.common.startFinishMarkers
import com.example.runtracker.ui.theme.Spacing

private val DEFAULT_CAMERA = GeoPoint(10.7769, 106.7009) // TP.HCM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteBuilderScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RouteBuilderViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.savedRouteId) {
        viewModel.savedRouteId?.let {
            viewModel.consumeSaved()
            onSaved(it)
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var panelHeight by remember { mutableStateOf(0.dp) }

    // Xin quyền vị trí để hiện chấm "vị trí của tôi" + nút định vị trên bản đồ.
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {}
    LaunchedEffect(Unit) {
        if (!context.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val tapped = viewModel.tappedPoints
    val planned = viewModel.planned
    val previewLine = planned?.polyline ?: tapped
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MapGuideLineColor

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Dựng route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        val drawMode = viewModel.drawMode
        Box(Modifier.fillMaxSize().padding(padding)) {
            val sketch = viewModel.lastSketch
            OsmMap(
                modifier = Modifier.fillMaxSize(),
                lines = buildList {
                    // Nét vẽ tay làm đường dẫn mờ để đối chiếu với route đã bám đường.
                    if (planned?.snappedToRoads == true && sketch.size >= 2) {
                        add(MapLine(sketch, guideColor, widthDp = 2f))
                    }
                    if (previewLine.size >= 2) {
                        add(MapLine(previewLine, lineColor, widthDp = 4.5f, showDirection = true))
                    }
                },
                markers = when {
                    // Đã có route: chỉ cần mốc xuất phát / về đích, mũi tên lo phần chiều đi.
                    previewLine.size >= 2 && (planned != null || drawMode) ->
                        startFinishMarkers(previewLine)
                    drawMode -> emptyList()
                    else -> tapped.mapIndexed { i, p -> MapMarker(p, "Điểm ${i + 1}") }
                },
                onTap = if (drawMode) null else ({ viewModel.addPoint(it) }),
                fitToLines = false,
                initialCenter = DEFAULT_CAMERA,
                initialZoom = 13.0,
                showMyLocation = true,
                drawMode = drawMode,
                onSketch = viewModel::applySketch,
                // chừa lề dưới cho panel điều khiển (đo động)
                controlsPadding = PaddingValues(end = 12.dp, bottom = panelHeight + 12.dp, top = 12.dp),
            )

            if (drawMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.sm),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        "Vẽ tay: khoanh quanh khu vực muốn chạy rồi thả tay",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }

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
                        when {
                            viewModel.loading -> "Đang bám đường…"
                            planned != null -> "${formatDistanceKm(planned.distanceMeters)}" + when {
                                planned.snappedToRoads -> " (bám đường)"
                                viewModel.lastSketch.size >= 2 -> " (theo nét vẽ tay)"
                                else -> " (đường thẳng)"
                            }
                            drawMode -> "Khoanh một vòng theo đường bạn muốn chạy rồi thả tay"
                            tapped.size >= 2 -> "${tapped.size} điểm — bấm \"Tính đường\""
                            else -> "Chạm bản đồ để thêm điểm, hoặc bật \"Vẽ tay\""
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )

                    viewModel.notice?.let { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilterChip(
                            selected = !drawMode,
                            onClick = { if (drawMode) viewModel.toggleDrawMode() },
                            label = { Text("Chạm điểm") },
                        )
                        FilterChip(
                            selected = drawMode,
                            onClick = { if (!drawMode) viewModel.toggleDrawMode() },
                            label = { Text("Vẽ tay") },
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilterChip(
                            selected = viewModel.mode == TravelMode.WALKING,
                            onClick = { viewModel.selectMode(TravelMode.WALKING) },
                            label = { Text("Đi bộ") },
                        )
                        FilterChip(
                            selected = viewModel.mode == TravelMode.CYCLING,
                            onClick = { viewModel.selectMode(TravelMode.CYCLING) },
                            label = { Text("Đạp xe") },
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = viewModel::clear, enabled = tapped.isNotEmpty()) { Text("Xoá hết") }
                        if (!drawMode) {
                            OutlinedButton(onClick = viewModel::undo, enabled = tapped.isNotEmpty()) { Text("Lùi") }
                            Button(
                                onClick = viewModel::computeRoute,
                                enabled = tapped.size >= 2 && !viewModel.loading,
                            ) { Text(if (viewModel.loading) "Đang tính…" else "Tính đường") }
                        }
                    }

                    if (drawMode && planned != null && viewModel.lastSketch.size >= 2) {
                        if (planned.snappedToRoads) {
                            OutlinedButton(
                                onClick = viewModel::useSketchAsRoute,
                                enabled = !viewModel.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Bám sai? Quay lại nét vẽ tay") }
                        } else {
                            OutlinedButton(
                                onClick = viewModel::snapSketchToRoads,
                                enabled = !viewModel.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (viewModel.loading) "Đang bám đường…" else "Thử bám đường lại") }
                        }
                    }

                    Button(
                        onClick = { showNameDialog = true },
                        enabled = planned != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Lưu route") }
                }
            }
        }
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Tên route") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Ví dụ: Vòng hồ Bán Nguyệt") },
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    viewModel.save(name.trim())
                    showNameDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Huỷ") } },
        )
    }
}
