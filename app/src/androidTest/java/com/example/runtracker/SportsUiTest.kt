package com.example.runtracker

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.settings.UnitSystem
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStatus
import com.example.runtracker.ui.profile.ProfileActivityOverview
import com.example.runtracker.ui.profile.ProfileSummary
import com.example.runtracker.ui.theme.LocalUnitSystem
import com.example.runtracker.ui.theme.RunTrackerTheme
import com.example.runtracker.ui.components.PremiumButton
import com.example.runtracker.ui.components.PremiumProgressIndicator
import com.example.runtracker.ui.tracking.RecordingDashboard
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class SportsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recordingActionsRemainReachableOnShortScreen() {
        var paused = false
        var stopped = false
        var mapShown = false
        compose.setContent {
            RunTrackerTheme(reduceMotion = true) {
                Box(Modifier.fillMaxWidth().height(560.dp)) {
                    RecordingDashboard(
                        state = TrackingState(status = TrackingStatus.TRACKING, distanceMeters = 4750.0,
                            elapsedSeconds = 1740, movingTimeSeconds = 1740, pointCount = 100),
                        sportName = "Chạy bộ", onPauseResume = { paused = true },
                        onStop = { stopped = true }, onShowMap = { mapShown = true },
                    )
                }
            }
        }
        compose.onNodeWithText("KM").assertIsDisplayed()
        saveScreenshot("recording")
        compose.onNodeWithText("Bản đồ").performClick()
        compose.onNodeWithContentDescription("Tạm dừng").performScrollTo().performClick()
        compose.onNodeWithText("Kết thúc buổi tập").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(true, mapShown)
            assertEquals(true, paused)
            assertEquals(true, stopped)
        }
    }

    @Test fun autoPausedRunCanResumeWithLargeFontAndImperialUnits() {
        var resumed = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.3f),
                LocalUnitSystem provides UnitSystem.IMPERIAL,
            ) {
                RunTrackerTheme(reduceMotion = true) {
                    RecordingDashboard(
                        state = TrackingState(status = TrackingStatus.PAUSED, autoPaused = true,
                            distanceMeters = 100_000.0, elapsedSeconds = 30_000, movingTimeSeconds = 25_000),
                        sportName = "Chạy bộ", onPauseResume = { resumed = true },
                        onStop = {}, onShowMap = {},
                    )
                }
            }
        }
        compose.onNodeWithText("MI").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("TỰ TẠM DỪNG · DI CHUYỂN ĐỂ TIẾP TỤC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Tiếp tục ghi").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, resumed) }
        saveScreenshot("paused-large-font")
    }

    @Test fun premiumOverviewRespectsSelectedDistanceUnitInLightTheme() {
        compose.setContent {
            CompositionLocalProvider(LocalUnitSystem provides UnitSystem.IMPERIAL) {
                RunTrackerTheme(darkTheme = false, reduceMotion = true) {
                    ProfileActivityOverview(ProfileSummary(activityCount = 8,
                        totalDistanceMeters = 42_500.0, totalMovingTime = 260.minutes))
                }
            }
        }
        compose.onNodeWithText("MI").assertIsDisplayed()
        compose.onNodeWithText("%.2f".format(42_500.0 / 1609.344)).assertIsDisplayed()
        compose.onNodeWithText("8").assertIsDisplayed()
        saveScreenshot("profile-summary-light")
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun reducedMotionShowsFinalProgressAndKeepsDisabledCtaDisabled() {
        compose.setContent {
            RunTrackerTheme(reduceMotion = true) {
                Column {
                    PremiumProgressIndicator(progress = { 0.6f }, modifier = Modifier.fillMaxWidth())
                    PremiumButton(onClick = { error("Disabled button must not invoke its action") }, enabled = false) {
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(0.6f, 0f..1f))).assertIsDisplayed()
        compose.onNodeWithText("Lưu thay đổi").assertIsNotEnabled()
    }
}
