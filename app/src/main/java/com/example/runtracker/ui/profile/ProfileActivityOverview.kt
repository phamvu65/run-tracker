package com.example.runtracker.ui.profile

import com.example.runtracker.ui.icons.AthleticIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runtracker.core.formatClock
import com.example.runtracker.ui.components.DistanceHero
import com.example.runtracker.ui.theme.RunTrackerTheme
import com.example.runtracker.ui.theme.Spacing
import com.example.runtracker.ui.theme.LocalPremiumPalette
import com.example.runtracker.ui.theme.ambientGlow
import com.example.runtracker.ui.theme.metricReveal
import com.example.runtracker.ui.theme.formatPaceUnit
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun ProfileActivityOverview(summary: ProfileSummary, modifier: Modifier = Modifier) {
    val brand = LocalPremiumPalette.current
    Column(
        modifier.fillMaxWidth().metricReveal(summary).ambientGlow(brand.glow)
            .clip(MaterialTheme.shapes.large)
            .background(brand.summaryGradient)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), MaterialTheme.shapes.large)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AthleticIcons.Running, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text("Tổng quan chạy bộ", style = MaterialTheme.typography.titleSmall, color = brand.summaryContent)
        }
        val pace = if (summary.totalDistanceMeters > 0) {
            summary.totalMovingTime.inWholeSeconds / (summary.totalDistanceMeters / 1000.0)
        } else 0.0
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DistanceHero(summary.totalDistanceMeters, Modifier.weight(1f), color = brand.summaryContent,
                unitColor = brand.summarySecondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OverviewStat("HOẠT ĐỘNG", summary.activityCount.toString())
                OverviewStat("THỜI GIAN", formatClock(summary.totalMovingTime.inWholeSeconds))
                OverviewStat("NHỊP ĐỘ TB", formatPaceUnit(pace))
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalPremiumPalette.current.summarySecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalPremiumPalette.current.summaryContent)
    }
}

@Preview(widthDp = 360)
@Composable
private fun ProfileActivityOverviewPreview() {
    RunTrackerTheme {
        ProfileActivityOverview(ProfileSummary(activityCount = 8, totalDistanceMeters = 42500.0, totalMovingTime = 260.minutes))
    }
}
