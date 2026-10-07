package com.example.runtracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.ui.tracking.RoutePickerDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutePickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun legacyRouteOpensRepairAndVerifiedRouteCanBeSelected() {
        var selected: String? = null
        var edited: String? = null
        val legacy = Route("old", "user", "Tuyến cũ", 1200.0, 0.0,
            emptyList(), false, 0L, emptyList())
        val verified = legacy.copy(id = "new", name = "Tuyến mới",
            snappedToRoads = true, travelMode = TravelMode.WALKING)
        compose.setContent {
            MaterialTheme {
                RoutePickerDialog(listOf(legacy, verified), null,
                    onSelect = { selected = it }, onDismiss = {}, onCreate = {},
                    onEdit = { edited = it })
            }
        }
        compose.onNodeWithText("Tuyến cũ", substring = true).performClick()
        compose.runOnIdle {
            assertEquals("old", edited)
            assertNull(selected)
        }
        compose.onNodeWithText("Tuyến mới", substring = true).performClick()
        compose.runOnIdle { assertEquals("new", selected) }
        compose.onNodeWithText("Không dẫn đường").performClick()
        compose.runOnIdle { assertNull(selected) }
    }

    @Test fun emptyListOffersRouteCreation() {
        var created = false
        compose.setContent {
            MaterialTheme {
                RoutePickerDialog(emptyList(), null, onSelect = {}, onDismiss = {},
                    onCreate = { created = true }, onEdit = {})
            }
        }
        compose.onNodeWithText("Tạo lộ trình mới").performClick()
        compose.runOnIdle { assertEquals(true, created) }
    }
}
