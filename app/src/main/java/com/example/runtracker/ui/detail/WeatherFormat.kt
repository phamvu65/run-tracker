package com.example.runtracker.ui.detail

/** Mã thời tiết WMO -> mô tả tiếng Việt (Open-Meteo weather_code). */
fun weatherCodeDescription(code: Int?): String = when (code) {
    null -> "—"
    0 -> "Trời quang"
    1 -> "Ít mây"
    2 -> "Mây rải rác"
    3 -> "Nhiều mây"
    45, 48 -> "Sương mù"
    51, 53, 55 -> "Mưa phùn"
    56, 57 -> "Mưa phùn đóng băng"
    61 -> "Mưa nhẹ"
    63 -> "Mưa vừa"
    65 -> "Mưa to"
    66, 67 -> "Mưa đóng băng"
    71 -> "Tuyết nhẹ"
    73 -> "Tuyết vừa"
    75 -> "Tuyết dày"
    77 -> "Hạt tuyết"
    80 -> "Mưa rào nhẹ"
    81 -> "Mưa rào"
    82 -> "Mưa rào dữ dội"
    85, 86 -> "Mưa tuyết rào"
    95 -> "Dông"
    96, 99 -> "Dông kèm mưa đá"
    else -> "Mã $code"
}

/** Hướng gió (độ) -> 8 hướng la bàn tiếng Việt. */
fun windCompass(deg: Int?): String {
    if (deg == null) return ""
    val dirs = listOf("B", "ĐB", "Đ", "ĐN", "N", "TN", "T", "TB")
    val idx = (((deg % 360) + 360) % 360 + 22) / 45 % 8
    return dirs[idx]
}
