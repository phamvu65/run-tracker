package com.example.runtracker.data.health

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Ghi nhớ đai nhịp tim BLE đã ghép để tự kết nối lại mỗi buổi tập. */
@Singleton
class BleHeartRateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("ble_hr", Context.MODE_PRIVATE)

    fun savedAddress(): String? = prefs.getString(KEY_ADDRESS, null)
    fun savedName(): String? = prefs.getString(KEY_NAME, null)

    fun save(address: String, name: String?) = prefs.edit {
        putString(KEY_ADDRESS, address)
        putString(KEY_NAME, name)
    }

    fun clear() = prefs.edit { remove(KEY_ADDRESS); remove(KEY_NAME) }

    private companion object {
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
    }
}
