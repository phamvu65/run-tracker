package com.example.runtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.runtracker.R

/** Bundled full Latin/Vietnamese fonts; no provider or network dependency at runtime. */
val AthleticDisplay = FontFamily(
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extra_bold, FontWeight.ExtraBold),
)
val AthleticBody = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Performance numbers stay upright and tabular; units/labels remain quieter Inter. */
object AthleticType {
    val distance = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.ExtraBold,
        fontSize = 112.sp, lineHeight = 120.sp, letterSpacing = (-1).sp, fontFeatureSettings = "tnum, lnum")
    val metric = TextStyle(fontFamily = AthleticDisplay, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = 0.sp, fontFeatureSettings = "tnum, lnum")
    val compactMetric = metric.copy(fontSize = 28.sp, lineHeight = 34.sp)
    val unit = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.8.sp)
}

val Typography = Typography(
    displayLarge = AthleticType.distance.copy(fontSize = 64.sp, lineHeight = 72.sp),
    displayMedium = AthleticType.metric.copy(fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, lineHeight = 60.sp),
    displaySmall = AthleticType.metric,
    headlineLarge = AthleticType.metric.copy(fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = AthleticType.metric.copy(fontSize = 32.sp, lineHeight = 40.sp),
    headlineSmall = AthleticType.compactMetric,
    titleLarge = AthleticType.compactMetric.copy(
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.2.sp),
    titleMedium = AthleticType.compactMetric.copy(
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.2.sp),
    titleSmall = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = AthleticBody, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = AthleticType.unit.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = AthleticType.unit.copy(fontSize = 10.sp, lineHeight = 15.sp, letterSpacing = 0.6.sp),
)
