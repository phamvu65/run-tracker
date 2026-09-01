package com.example.runtracker.tracking

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ghi bền id buổi tập đang chạy để phát hiện buổi bị gián đoạn khi process bị OS kill.
 * Set lúc START, xoá khi buổi được chốt số liệu.
 */
@Singleton
class TrackingStateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("tracking_state", Context.MODE_PRIVATE)

    fun activeActivityId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun markActive(activityId: String) = prefs.edit { putString(KEY_ACTIVE_ID, activityId) }

    fun clear() = prefs.edit { remove(KEY_ACTIVE_ID) }

    private companion object {
        const val KEY_ACTIVE_ID = "active_activity_id"
    }
}
