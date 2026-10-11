package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.DailySuggestion
import com.example.runtracker.domain.model.PlannedSession
import com.example.runtracker.domain.model.SessionType
import kotlin.math.roundToInt

/**
 * "Hôm nay nên tập gì" — nếu có kế hoạch giải chạy thì lấy đúng buổi của hôm nay;
 * nếu không, gợi ý theo form (TSB) và mức nền (CTL). Thuần JVM.
 */
object DailyTrainingSuggestion {

    fun suggest(tsb: Double?, ctl: Double?, plannedToday: PlannedSession?): DailySuggestion {
        if (plannedToday != null) {
            return DailySuggestion(
                type = plannedToday.type,
                headline = "Hôm nay: ${plannedToday.type.label}",
                rationale = plannedToday.description,
            )
        }

        if (tsb == null) {
            return DailySuggestion(
                type = SessionType.EASY,
                headline = "Chưa đủ dữ liệu",
                rationale = "Ghi vài buổi tập và nhập mức gắng sức (RPE) để nhận gợi ý theo form.",
            )
        }

        val t = tsb.roundToInt()
        val lowBase = (ctl ?: 0.0) < 20.0
        val baseNote = if (lowBase) " Nền còn thấp — tăng khối lượng từ từ, không quá 10%/tuần." else ""

        return when {
            tsb < -30 -> DailySuggestion(
                SessionType.REST,
                "Cơ thể cần hồi phục",
                "TSB $t đang rất thấp: nên nghỉ hoặc chỉ đi bộ, giãn cơ; chưa nên tập nặng.",
            )
            tsb < -10 -> DailySuggestion(
                SessionType.EASY,
                "Đang mệt tích lũy — chạy nhẹ",
                "TSB $t đang âm: ưu tiên chạy nhẹ vùng 2, hoãn bài tempo hoặc biến tốc 1–2 ngày.$baseNote",
            )
            tsb <= 5 -> DailySuggestion(
                SessionType.EASY,
                "Thể trạng cân bằng",
                "TSB $t: chạy nhẹ hoặc theo lịch; có thể xen một buổi chất lượng trong tuần.$baseNote",
            )
            tsb <= 20 -> DailySuggestion(
                SessionType.TEMPO,
                "Thể trạng tốt — phù hợp buổi chất lượng",
                "TSB $t đang dương: có thể tập tempo, biến tốc hoặc chạy dài.$baseNote",
            )
            else -> DailySuggestion(
                SessionType.INTERVAL,
                "Thể trạng rất tốt — có thể tăng cường độ",
                "TSB $t cao (có thể do nghỉ nhiều): bắt đầu vừa sức, tránh tăng khối lượng đột ngột.",
            )
        }
    }
}
