package com.example.runtracker.domain.tracking

import kotlin.math.pow

/**
 * Đổi áp suất khí quyển (hPa) sang độ cao (m) bằng công thức khí áp chuẩn (ISA, dưới 11km).
 * Áp suất mực nước biển thực tế đổi theo thời tiết nên phải hiệu chỉnh theo 1 điểm mốc
 * (áp suất + độ cao đã biết — lấy từ GPS altitude ở điểm đầu buổi tập) thay vì dùng hằng số
 * 1013.25 hPa cố định. Thuần JVM.
 */
object BarometerAltitude {

    private const val EXPONENT = 1.0 / 5.255

    /** Áp suất mực nước biển tương đương, suy từ 1 điểm mốc (hPa, m). */
    fun seaLevelPressure(referencePressureHpa: Float, referenceAltitudeMeters: Double): Double =
        referencePressureHpa / (1 - referenceAltitudeMeters / 44_330.0).pow(5.255)

    /** Độ cao (m) ứng với áp suất hiện tại, theo mốc [seaLevelPressureHpa] đã hiệu chỉnh. */
    fun altitudeFor(pressureHpa: Float, seaLevelPressureHpa: Double): Double =
        44_330.0 * (1 - (pressureHpa / seaLevelPressureHpa).pow(EXPONENT))
}
