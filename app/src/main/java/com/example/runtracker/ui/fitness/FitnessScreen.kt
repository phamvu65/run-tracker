package com.example.runtracker.ui.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.DailySuggestion
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.training.RaceDistance
import com.example.runtracker.ui.components.DiagramCard
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.IconStatGrid
import com.example.runtracker.ui.components.IconStatTileData
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onBack: (() -> Unit)? = null,
    onOpenPlan: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FitnessViewModel = hiltViewModel(),
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val suggestion by viewModel.dailySuggestion.collectAsState()
    val hasGoal by viewModel.hasGoal.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Fitness & Freshness") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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
        val latest = snapshots.lastOrNull()

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val currentSuggestion = suggestion
            if (currentSuggestion != null) {
                SuggestionCard(currentSuggestion, hasGoal = hasGoal, onOpenPlan = onOpenPlan)
            } else {
                androidx.compose.material3.FilledTonalButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasGoal) "Xem kế hoạch tập luyện" else "Tạo kế hoạch tập luyện")
                }
            }

            if (latest != null) {
                DiagramCard(
                    title = "Thể trạng hôm nay",
                    footerGrid = {
                        IconStatGrid(
                            listOf(
                                IconStatTileData(Icons.Filled.TrendingUp, "Fitness", latest.ctl.roundToInt().toString()),
                                IconStatTileData(Icons.Filled.TrendingDown, "Fatigue", latest.atl.roundToInt().toString()),
                                IconStatTileData(Icons.Filled.Favorite, "Form", latest.tsb.roundToInt().toString()),
                            ),
                        )
                    },
                ) {
                    Text(
                        formInterpretation(latest.tsb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DiagramCard(title = "90 ngày qua") {
                    FitnessChart(snapshots = snapshots, modifier = Modifier.fillMaxWidth())
                }
            } else {
                FlatCard {
                    EmptyState(
                        title = "Chưa có dữ liệu Fitness",
                        message = "Ghi một buổi tập (nhập RPE hoặc có nhịp tim) để bắt đầu theo dõi CTL/ATL/TSB.",
                    )
                }
            }

            if (predictions.isNotEmpty()) {
                PredictionSection(predictions)
            }
        }
    }
}

/** Thẻ "Gợi ý hôm nay" kiểu Figma Insights: icon tròn + heading + mô tả + CTA dạng link. */
@Composable
private fun SuggestionCard(suggestion: DailySuggestion, hasGoal: Boolean, onOpenPlan: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(Spacing.lg),
    ) {
        Icon(
            Icons.Filled.Lightbulb,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                suggestion.headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                suggestion.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onOpenPlan, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(
                    if (hasGoal) "Xem kế hoạch tập luyện →" else "Tạo kế hoạch tập luyện →",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PredictionSection(predictions: List<PerformancePrediction>) {
    val byLabel = predictions.associateBy { it.distanceLabel }
    FlatCard {
        SectionHeader(
            title = "Dự đoán thành tích",
            subtitle = "Riegel · buổi chạy nhanh nhất 90 ngày gần đây",
        )
        Spacer(Modifier.height(Spacing.xs))
        RaceDistance.entries.forEach { race ->
            val p = byLabel[race.label] ?: return@forEach
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                Text(race.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(
                    formatClock(p.predictedSeconds.roundToLong()),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatPace(p.predictedSeconds / (race.meters / 1000.0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
            if (race != RaceDistance.entries.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

private fun formInterpretation(tsb: Double): String = when {
    tsb > 5 -> "Form dương — cơ thể tươi, phù hợp buổi nặng hoặc thi đấu."
    tsb >= -10 -> "Form cân bằng — tải tập hợp lý."
    tsb >= -30 -> "Form âm — đang mệt tích luỹ, cân nhắc giảm tải."
    else -> "Form rất âm — nguy cơ quá tải, nên nghỉ hồi phục."
}
