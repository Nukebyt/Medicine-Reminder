package com.nukebyt.medicinereminder.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/** Live status of everything a reminder needs. Re-read in onResume: the user changes these in system Settings. */
data class PermissionStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** Without these a reminder cannot arrive on time. */
    val criticalMissing: Boolean get() = !notifications || !exactAlarms

    val allGranted: Boolean get() = notifications && exactAlarms && fullScreen && batteryUnrestricted

    companion object {
        /** Used before the first read. */
        val Unknown = PermissionStatus(notifications = true, exactAlarms = true, fullScreen = true, batteryUnrestricted = true)
    }
}

object Permissions {
    fun read(context: Context): PermissionStatus {
        val nm = context.getSystemService<NotificationManager>()
        val alarms = context.getSystemService<AlarmManager>()
        val power = context.getSystemService<PowerManager>()

        val postGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return PermissionStatus(
            notifications = postGranted && (nm?.areNotificationsEnabled() ?: true),
            exactAlarms = Build.VERSION.SDK_INT < 31 || alarms?.canScheduleExactAlarms() ?: true,
            fullScreen = Build.VERSION.SDK_INT < 34 || nm?.canUseFullScreenIntent() ?: true,
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
        )
    }

    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 31) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
        } else {
            appDetailsIntent(context)
        }

    fun fullScreenIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
        } else {
            appDetailsIntent(context)
        }

    /**
     * The battery optimisation list, which needs no special permission. (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is
     * restricted on Google Play, so we deep-link instead.)
     */
    fun batteryIntent(context: Context): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    /** Start a settings page, falling back to the app's details page if this phone has no such screen. */
    fun launch(context: Context, intent: Intent) {
        val withTask = Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(withTask)
        } catch (e: Exception) {
            AppLog.w("settings screen unavailable for ${intent.action}", e)
            runCatching { context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
