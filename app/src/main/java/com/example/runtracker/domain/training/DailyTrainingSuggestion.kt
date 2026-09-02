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
                headline = "Theo kế hoạch: ${plannedToday.type.label}",
                rationale = plannedToday.description,
            )
        }

        if (tsb == null) {
            return DailySuggestion(
                type = SessionType.EASY,
                headline = "Chưa đủ dữ liệu",
                rationale = "Ghi vài buổi tập có nhịp tim hoặc nhập RPE để nhận gợi ý theo form.",
            )
        }

        val t = tsb.roundToInt()
        val lowBase = (ctl ?: 0.0) < 20.0
        val baseNote = if (lowBase) " Nền còn thấp — tăng khối lượng từ từ, không quá 10%/tuần." else ""

        return when {
            tsb < -30 -> DailySuggestion(
                SessionType.REST,
                "Rất mệt — nên nghỉ",
                "TSB $t rất âm: nghỉ hoặc chỉ đi bộ / giãn cơ, tránh mọi buổi nặng.",
            )
            tsb < -10 -> DailySuggestion(
                SessionType.EASY,
                "Đang mệt tích luỹ — chạy nhẹ",
                "TSB $t âm: ưu tiên chạy nhẹ Z2, hoãn tempo / interval 1–2 ngày.$baseNote",
            )
            tsb <= 5 -> DailySuggestion(
                SessionType.EASY,
                "Form cân bằng",
                "TSB $t: chạy nhẹ hoặc theo lịch; có thể xen 1 buổi chất lượng trong tuần.$baseNote",
            )
            tsb <= 20 -> DailySuggestion(
                SessionType.TEMPO,
                "Form tươi — hợp buổi chất lượng",
                "TSB $t dương: tận dụng cho tempo, interval hoặc long run.$baseNote",
            )
            else -> DailySuggestion(
                SessionType.INTERVAL,
                "Rất tươi — có thể đẩy cường độ",
                "TSB $t cao (có thể do nghỉ nhiều): vào lại từ từ, tránh tăng khối lượng đột ngột.",
            )
        }
    }
}
