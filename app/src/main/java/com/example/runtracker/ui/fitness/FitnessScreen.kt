package com.example.runtracker.ui.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.PerformancePrediction
import com.example.runtracker.domain.training.RaceDistance
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FitnessViewModel = hiltViewModel(),
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val predictions by viewModel.predictions.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Fitness & Freshness") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        val latest = snapshots.lastOrNull()
        if (latest == null && predictions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Chưa có dữ liệu. Ghi một buổi tập (nhập RPE hoặc có nhịp tim) để bắt đầu.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (latest != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metric("Fitness", latest.ctl, Modifier.weight(1f))
                    Metric("Fatigue", latest.atl, Modifier.weight(1f))
                    Metric("Form", latest.tsb, Modifier.weight(1f))
                }
                Text(formInterpretation(latest.tsb), style = MaterialTheme.typography.bodyMedium)
                FitnessChart(snapshots = snapshots, modifier = Modifier.fillMaxWidth())
            }

            if (predictions.isNotEmpty()) {
                PredictionSection(predictions)
            }
        }
    }
}

@Composable
private fun PredictionSection(predictions: List<PerformancePrediction>) {
    val byLabel = predictions.associateBy { it.distanceLabel }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Dự đoán thành tích", style = MaterialTheme.typography.titleSmall)
        Text(
            "Riegel, dựa trên buổi chạy nhanh nhất 90 ngày gần đây.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RaceDistance.entries.forEach { race ->
            val p = byLabel[race.label] ?: return@forEach
            Row(Modifier.fillMaxWidth()) {
                Text(race.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatClock(p.predictedSeconds.roundToLong()),
                    style = MaterialTheme.typography.bodyMedium,
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
            HorizontalDivider()
        }
    }
}

@Composable
private fun Metric(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value.roundToInt().toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formInterpretation(tsb: Double): String = when {
    tsb > 5 -> "Form dương — cơ thể tươi, phù hợp buổi nặng hoặc thi đấu."
    tsb >= -10 -> "Form cân bằng — tải tập hợp lý."
    tsb >= -30 -> "Form âm — đang mệt tích luỹ, cân nhắc giảm tải."
    else -> "Form rất âm — nguy cơ quá tải, nên nghỉ hồi phục."
}
