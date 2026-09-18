package com.example.runtracker.ui.complete

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.core.formatPace
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.PersonalRecord
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.IconStatGrid
import com.example.runtracker.ui.components.IconStatTileData
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Màn hiện ra ngay sau khi kết thúc một buổi tập: có kỷ lục thì ăn mừng lớn (pháo hoa giấy +
 * huy hiệu cúp), không thì vẫn ăn mừng nhẹ nhàng kèm một câu khích lệ — hoàn thành buổi nào cũng
 * đáng được ghi nhận, không chỉ khi có PR.
 *
 * Bố cục: thanh đóng trên cùng, nội dung cuộn CĂN TRÊN (không dùng Arrangement.Center — đó là
 * nguyên nhân khiến các khối đè lên nhau khi tổng chiều cao nội dung đổi theo animation/dữ liệu),
 * ba nút hành động ghim ở đáy, pháo hoa vẽ đè lên trên cùng bằng lớp Box riêng.
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
    val context = LocalContext.current

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.loading) {
        if (!viewModel.loading) contentVisible = true
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().padding(Spacing.screen),
                contentAlignment = Alignment.CenterEnd,
            ) {
                TextButton(onClick = onClose) { Text("Đóng") }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(Spacing.lg))

                if (!viewModel.loading) {
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
                    ) {
                        CelebrationHeader(records, viewModel.quote, viewModel.athleteName)
                    }
                }

                activity?.let { a ->
                    Spacer(Modifier.height(Spacing.xl))
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(400, delayMillis = 150)) +
                            slideInVertically(tween(400, delayMillis = 150)) { it / 3 },
                    ) {
                        FlatCard { IconStatGrid(statTiles(a)) }
                    }
                }

                if (records.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    RecordsList(records)
                }

                Spacer(Modifier.height(Spacing.xl))
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(300, delayMillis = 300)),
            ) {
                Column(Modifier.fillMaxWidth().padding(Spacing.screen)) {
                    Button(
                        onClick = { onViewDetail(viewModel.activityId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Xem chi tiết") }
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = { activity?.let { shareAchievement(context, it) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Chia sẻ thành tích") }
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Về trang chủ") }
                }
            }
        }

        if (contentVisible) {
            ConfettiOverlay(Modifier.fillMaxSize())
        }
    }
}

private fun statTiles(a: Activity): List<IconStatTileData> = buildList {
    add(IconStatTileData(Icons.Filled.LocationOn, "Quãng đường", formatDistanceKm(a.distanceMeters)))
    add(IconStatTileData(Icons.Filled.Timer, "Thời gian", formatClock(a.movingTime.inWholeSeconds)))
    add(IconStatTileData(Icons.Filled.DirectionsRun, "Nhịp độ", formatPace(a.avgPaceSecPerKm)))
    a.calories?.takeIf { it > 0 }?.let {
        add(IconStatTileData(Icons.Filled.LocalFireDepartment, "Calo", it.toString(), unit = "kcal"))
    }
    a.steps?.takeIf { it > 0 }?.let {
        add(IconStatTileData(Icons.Filled.DirectionsWalk, "Bước chân", "%,d".format(it)))
    }
}

private fun shareAchievement(context: android.content.Context, a: Activity) {
    val text = "Vừa hoàn thành ${formatDistanceKm(a.distanceMeters)} trong " +
        "${formatClock(a.movingTime.inWholeSeconds)} (nhịp độ ${formatPace(a.avgPaceSecPerKm)}) " +
        "trên RunTracker! 🏃"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Chia sẻ thành tích"))
}

@Composable
private fun CelebrationHeader(records: List<PersonalRecord>, quote: String, athleteName: String?) {
    val isRecord = records.isNotEmpty()
    val namePart = athleteName?.let { ", $it" } ?: ""

    // QUAN TRỌNG: AnimatedVisibility bọc nội dung trong một Box nội bộ (không phải Column) —
    // nếu gọi các composable con trực tiếp không có Column bao ngoài ở đây, chúng sẽ bị Box xếp
    // chồng lên nhau tại cùng một vị trí thay vì xếp dọc tuần tự. Đây chính là nguyên nhân khiến
    // tiêu đề/câu khích lệ/icon đè lên nhau ở bản cũ.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CelebrationBadge(isRecord = isRecord)
        Spacer(Modifier.height(Spacing.md))
        Text(
            if (isRecord) "Tuyệt vời$namePart!" else "Hoàn thành$namePart!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            if (isRecord) {
                if (records.size > 1) {
                    "Bạn vừa lập ${records.size} kỷ lục cá nhân mới!"
                } else {
                    "Kỷ lục mới: ${records.first().label}!"
                }
            } else {
                quote
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
    }
}

@Composable
private fun RecordsList(records: List<PersonalRecord>) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        records.forEachIndexed { index, record ->
            // Tự quản trạng thái visible riêng (không mượn boolean của cha) — cha chỉ mount khối
            // này SAU KHI đã chuyển sang visible=true, nên delayMillis trên AnimatedVisibility(cha
            // truyền true ngay từ đầu) sẽ không có hiệu ứng gì để chạy; state nội bộ này mới thật
            // sự bắt đầu ở false rồi bật lên true, nên hiệu ứng so le mới thực sự chạy.
            var itemVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(150L + index * 80L)
                itemVisible = true
            }
            AnimatedVisibility(
                visible = itemVisible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
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

/**
 * Huy hiệu ăn mừng: vòng hào quang mờ dần ra ngoài + icon cúp (có kỷ lục) hoặc dấu tích (hoàn
 * thành thường). Khung ngoài cùng CỐ ĐỊNH kích thước — animation chỉ scale lúc vẽ, không đổi kích
 * thước layout — nên dù nảy quá 100% (overshoot của spring) cũng không đè lên nội dung bên dưới
 * (khác lỗi cũ: Text emoji cỡ displayLarge tràn khỏi khung 96dp chứa nó).
 */
@Composable
private fun CelebrationBadge(isRecord: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(isRecord) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Box(modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(128.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        )
        Box(
            Modifier
                .size(96.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        )
        Box(
            Modifier
                .size(68.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isRecord) Icons.Filled.EmojiEvents else Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}
