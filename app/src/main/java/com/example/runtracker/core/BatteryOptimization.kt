package com.example.runtracker.core

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Tiện ích xử lý việc thiết bị tự tắt foreground service khi màn hình tắt.
 * Xiaomi/Oppo/Vivo... còn có màn "tự khởi động" (autostart) riêng ngoài tối ưu hoá pin của Android.
 */
object BatteryOptimization {

    fun isIgnoringOptimizations(context: Context): Boolean {
        val pm = context.getSystemService<PowerManager>() ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Mở hộp thoại xin miễn tối ưu hoá pin cho app (cần quyền REQUEST_IGNORE_BATTERY_OPTIMIZATIONS). */
    @SuppressLint("BatteryLife")
    fun requestIgnoreOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            openAppDetailsSettings(context)
        }
    }

    /** True nếu thiết bị thuộc hãng hay kill service nền và có màn autostart riêng. */
    fun hasAggressiveOem(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m in setOf("xiaomi", "redmi", "poco", "oppo", "realme", "oneplus", "vivo", "iqoo", "huawei", "honor")
    }

    /** Thử mở màn autostart của hãng; fallback về màn thông tin app. */
    fun openOemAutoStartSettings(context: Context) {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        )
        for (component in candidates) {
            val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        openAppDetailsSettings(context)
    }

    private fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
