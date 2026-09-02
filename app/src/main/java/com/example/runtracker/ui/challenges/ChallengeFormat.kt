package com.example.runtracker.ui.challenges

import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ChallengeStatus
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Nhãn loại mục tiêu cho UI. */
fun ChallengeGoalType.label(): String = when (this) {
    ChallengeGoalType.TOTAL_DISTANCE -> "Quãng đường"
    ChallengeGoalType.TOTAL_ACTIVITIES -> "Số buổi"
    ChallengeGoalType.TOTAL_ELEVATION -> "Độ cao tích luỹ"
    ChallengeGoalType.TOTAL_DURATION -> "Thời gian"
}

/** Đơn vị người dùng nhập ở màn tạo (khác đơn vị gốc khi lưu). */
fun ChallengeGoalType.inputUnit(): String = when (this) {
    ChallengeGoalType.TOTAL_DISTANCE -> "km"
    ChallengeGoalType.TOTAL_ACTIVITIES -> "buổi"
    ChallengeGoalType.TOTAL_ELEVATION -> "m"
    ChallengeGoalType.TOTAL_DURATION -> "phút"
}

/** Quy đổi giá trị người dùng nhập -> đơn vị gốc lưu trong DB. */
fun ChallengeGoalType.toBaseValue(input: Double): Double = when (this) {
    ChallengeGoalType.TOTAL_DISTANCE -> input * 1_000.0
    ChallengeGoalType.TOTAL_ACTIVITIES -> input
    ChallengeGoalType.TOTAL_ELEVATION -> input
    ChallengeGoalType.TOTAL_DURATION -> input * 60.0
}

/** Hiển thị một giá trị (đơn vị gốc) thành chuỗi dễ đọc. */
fun ChallengeGoalType.formatAmount(baseValue: Double): String = when (this) {
    ChallengeGoalType.TOTAL_DISTANCE -> "%.1f km".format(baseValue / 1_000.0)
    ChallengeGoalType.TOTAL_ACTIVITIES -> "${baseValue.roundToInt()} buổi"
    ChallengeGoalType.TOTAL_ELEVATION -> "${baseValue.roundToInt()} m"
    ChallengeGoalType.TOTAL_DURATION -> formatClock(baseValue.roundToLong())
}

fun ChallengeStatus.label(): String = when (this) {
    ChallengeStatus.UPCOMING -> "Sắp bắt đầu"
    ChallengeStatus.ACTIVE -> "Đang diễn ra"
    ChallengeStatus.ENDED -> "Đã kết thúc"
}
