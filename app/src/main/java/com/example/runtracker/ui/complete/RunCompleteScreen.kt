package com.example.runtracker.ui.complete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.PersonalRecord
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.StatCell
import com.example.runtracker.ui.components.StatStrip
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing

/**
 * Màn hiện ra ngay sau khi kết thúc một buổi tập: có kỷ lục thì ăn mừng, không thì một câu
 * khích lệ ngẫu nhiên — hoàn thành buổi nào cũng đáng được ghi nhận, không chỉ khi có PR.
 */
@Composable
fun RunCompleteScreen(
    onClose: () -> Unit,
    onViewDetail: (activityId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunCompleteViewModel = hiltViewModel(),
) {
    val activity = viewModel.activity
    val records = viewModel.records

    // Nội dung ẩn lúc đầu, hiện dần (fade + trượt lên, so le từng khối) ngay khi hết loading —
    // buổi tập nào cũng đáng một chút "ăn mừng" khi màn này xuất hiện.
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.loading) {
        if (!viewModel.loading) contentVisible = true
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onClose) { Text("Đóng") }
            }

            Spacer(Modifier.height(Spacing.lg))

            if (!viewModel.loading) {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
                ) {
                    if (records.isNotEmpty()) {
                        AchievementHeader(records, contentVisible)
                    } else {
                        EncouragementHeader(viewModel.quote)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            activity?.let { a ->
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(400, delayMillis = 150)) +
                        slideInVertically(tween(400, delayMillis = 150)) { it / 3 },
                ) {
                    FlatCard {
                        StatStrip(
                            listOf(
                                StatCell("Quãng đường", formatDistanceKm(a.distanceMeters)),
                                StatCell("Thời gian di chuyển", formatClock(a.movingTime.inWholeSeconds)),
                                StatCell("Nhịp độ", formatPace(a.avgPaceSecPerKm)),
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(400, delayMillis = 300)),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onViewDetail(viewModel.activityId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Xem chi tiết") }
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
                }
            }
        }
    }
}

@Composable
private fun AchievementHeader(records: List<PersonalRecord>, visible: Boolean) {
    val emojiScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "trophyScale",
    )
    Text("🏆", style = MaterialTheme.typography.displayLarge, modifier = Modifier.scale(emojiScale))
    Spacer(Modifier.height(Spacing.sm))
    Text(
        if (records.size > 1) "Kỷ lục mới!" else "Kỷ lục mới: ${records.first().label}!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.lg))
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        records.forEachIndexed { index, record ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 200 + index * 80)) +
                    slideInVertically(tween(300, delayMillis = 200 + index * 80)) { it / 2 },
            ) {
                FlatCard {
                    StravaLabel(record.label)
                    Spacer(Modifier.height(2.dp))
                    Text(record.valueText, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
private fun EncouragementHeader(quote: String) {
    val emojiScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        emojiScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Text("🏁", style = MaterialTheme.typography.displayLarge, modifier = Modifier.scale(emojiScale.value))
    Spacer(Modifier.height(Spacing.sm))
    Text(
        "Hoàn thành!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        quote,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.md),
    )
}
