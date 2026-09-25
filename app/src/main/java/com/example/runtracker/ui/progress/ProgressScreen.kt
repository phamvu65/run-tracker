package com.example.runtracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatMinutesSeconds
import com.example.runtracker.domain.training.PeriodComparison
import com.example.runtracker.domain.training.TrendPeriod
import com.example.runtracker.ui.components.EmptyState
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.PeriodSwitch
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.formatDistanceUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val period by viewModel.period.collectAsState()
    val buckets by viewModel.buckets.collectAsState()
    val comparison by viewModel.comparison.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tiến độ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            PeriodSwitch(
                options = listOf("Tuần", "Tháng", "Năm"),
                selectedIndex = period.ordinal,
                onSelect = { viewModel.selectPeriod(TrendPeriod.entries[it]) },
            )

            FlatCard {
                SectionHeader("Quãng đường theo kỳ")
                Spacer(Modifier.height(Spacing.md))
                if (buckets.all { it.distanceMeters <= 0.0 }) {
                    EmptyState(
                        title = "Chưa có dữ liệu",
                        message = "Ghi vài buổi tập để xem biểu đồ tiến độ.",
                    )
                } else {
                    ProgressBarChart(buckets, modifier = Modifier.fillMaxWidth())
                }
            }

            comparison?.let { c ->
                FlatCard {
                    SectionHeader("So với kỳ trước")
                    Spacer(Modifier.height(Spacing.sm))
                    ComparisonRow(c)
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(comparison: PeriodComparison) {
    val distanceText = comparison.distanceDeltaPct?.let { pct ->
        val sign = if (pct >= 0) "+" else ""
        "$sign${pct.roundToInt()}% quãng đường (${formatDistanceUnit(comparison.currentDistanceMeters)} " +
            "so với ${formatDistanceUnit(comparison.previousDistanceMeters)})"
    } ?: "Kỳ trước chưa có dữ liệu để so sánh."
    Text(distanceText, style = MaterialTheme.typography.bodyMedium)

    comparison.paceDeltaSecPerKm?.let { delta ->
        Spacer(Modifier.height(Spacing.xs))
        val paceText = when {
            delta < -0.5 -> "Nhanh hơn ${formatMinutesSeconds((-delta).roundToLong())}/km"
            delta > 0.5 -> "Chậm hơn ${formatMinutesSeconds(delta.roundToLong())}/km"
            else -> "Pace tương đương kỳ trước"
        }
        Text(paceText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
