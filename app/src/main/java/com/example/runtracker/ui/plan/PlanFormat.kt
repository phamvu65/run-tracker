package com.example.runtracker.ui.plan

import com.example.runtracker.domain.model.TrainingPhase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun TrainingPhase.label(): String = when (this) {
    TrainingPhase.BASE -> "Xây nền"
    TrainingPhase.BUILD -> "Tăng tải"
    TrainingPhase.PEAK -> "Đỉnh"
    TrainingPhase.TAPER -> "Giảm tải"
    TrainingPhase.RACE -> "Tuần thi đấu"
}

fun DayOfWeek.viShort(): String = when (this) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

fun LocalDate.dayMonth(): String = format(DAY_MONTH)
