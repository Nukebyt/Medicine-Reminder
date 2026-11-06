package com.nukebyt.medicinereminder.alarm

import androidx.core.net.toUri
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.nukebyt.medicinereminder.MainActivity
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.ui.util.formatTime
import com.nukebyt.medicinereminder.ui.util.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Channel setup and notification builders for reminders, the alarm service and missed doses. */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nm: NotificationManager get() = context.getSystemService()!!

    // ---- channels ----

    /**
     * Channel sound and importance cannot change after creation. If you change them during development,
     * use a new channel id or uninstall the app (ROADMAP section 4).
     */
    fun createChannels() {
        val chime = "android.resource://${context.packageName}/${R.raw.reminder_chime}".toUri()
        val reminders = NotificationChannel(
            AlarmContract.CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            setSound(chime, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
        }
        val alarms = NotificationChannel(
            AlarmContract.CHANNEL_ALARMS,
            context.getString(R.string.channel_alarms_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            setSound(null, null) // AlarmService plays the sound on the alarm stream
            enableVibration(false)
        }
        val missed = NotificationChannel(
            AlarmContract.CHANNEL_MISSED,
            context.getString(R.string.channel_missed_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_missed_desc) }
        nm.createNotificationChannels(listOf(reminders, alarms, missed))
    }

    // ---- text ----

    /** "Metformin 500 mg · after meal". The meal part is dropped for "any time". */
    fun doseLine(dose: DoseUi): String {
        val name = listOf(dose.medicineName, dose.dosageText).filter { it.isNotEmpty() }.joinToString(" ")
        if (dose.mealRelation == MealRelation.ANY_TIME) return name
        return context.getString(R.string.notif_dose_line, name, context.getString(dose.mealRelation.labelRes()).lowercase())
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        AlarmContract.RC_OPEN_APP,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun reminderId(key: SlotKey) =
        AlarmContract.NOTIF_REMINDER_BASE + key.slot.minuteOfDayInt() + if (key.test) AlarmContract.TEST_OFFSET else 0

    private fun missedId(key: SlotKey) =
        AlarmContract.NOTIF_MISSED_BASE + key.slot.minuteOfDayInt() + if (key.test) AlarmContract.TEST_OFFSET else 0

    private fun actionIntent(action: String, key: SlotKey, requestBase: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestBase + key.slot.minuteOfDayInt() + if (key.test) AlarmContract.TEST_OFFSET else 0,
        Intent(context, NotificationActionReceiver::class.java).setAction(action).apply {
            putExtra(AlarmContract.EXTRA_DATE, key.date.toEpochDay())
            putExtra(AlarmContract.EXTRA_SLOT, key.slot.minuteOfDayInt())
            putExtra(AlarmContract.EXTRA_TEST, key.test)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // ---- reminder ----

    /** The on-time reminder: one grouped notification for every dose due at the slot. */
    fun showReminder(key: SlotKey, doses: List<DoseUi>, snoozeMinutes: Int, alert: Boolean = true) {
        if (doses.isEmpty()) return
        val lines = doses.map(::doseLine)
        val style = NotificationCompat.InboxStyle().also { s -> lines.forEach(s::addLine) }
        val whenMillis = key.date.atTime(key.slot).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val notification = NotificationCompat.Builder(context, AlarmContract.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(if (lines.size == 1) lines[0] else context.getString(R.string.notif_more, lines[0], lines.size - 1))
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(whenMillis)
            .setShowWhen(true)
            .setOnlyAlertOnce(!alert)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.notif_action_taken), actionIntent(AlarmContract.ACTION_TAKEN, key, AlarmContract.RC_ACTION_TAKEN))
            .addAction(0, context.getString(R.string.notif_action_snooze, snoozeMinutes), actionIntent(AlarmContract.ACTION_SNOOZE, key, AlarmContract.RC_ACTION_SNOOZE))
            .build()
        nm.notify(reminderId(key), notification)
        AppLog.d("reminder posted for ${key.date} ${key.slot} (${doses.size} dose(s))")
    }

    fun isReminderShowing(key: SlotKey): Boolean = nm.activeNotifications.any { it.id == reminderId(key) }

    fun cancelReminder(key: SlotKey) {
        nm.cancel(reminderId(key))
    }

    // ---- missed ----

    /** Wording avoids "take it now": a late dose is a question for the doctor or pharmacist (ROADMAP section 11). */
    fun showMissed(key: SlotKey, doses: List<DoseUi>) {
        if (doses.isEmpty()) return
        val first = doses.first().medicineName
        val timeText = formatTime(key.slot)
        val text = if (doses.size == 1) {
            context.getString(R.string.notif_missed_single, first, timeText)
        } else {
            context.getString(R.string.notif_missed_multiple, first, doses.size - 1, timeText)
        }
        val notification = NotificationCompat.Builder(context, AlarmContract.CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(context.getString(R.string.notif_missed_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        nm.notify(missedId(key), notification)
        AppLog.d("missed notification posted for ${key.date} ${key.slot}")
    }

    // ---- alarm (foreground service notification) ----

    fun buildAlarmNotification(doses: List<DoseUi>, snoozeMinutes: Int): Notification {
        val lines = doses.map(::doseLine)
        val fullScreen = PendingIntent.getActivity(
            context,
            AlarmContract.RC_ALARM_SCREEN,
            AlarmActivity.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle().also { s -> lines.forEach(s::addLine) }
        return NotificationCompat.Builder(context, AlarmContract.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setContentText(lines.firstOrNull().orEmpty().let { if (lines.size > 1) context.getString(R.string.notif_more, it, lines.size - 1) else it })
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, context.getString(R.string.alarm_action_stop), serviceIntent(AlarmContract.ACTION_ALARM_STOP, AlarmContract.RC_ALARM_STOP))
            .addAction(0, context.getString(R.string.alarm_action_took), serviceIntent(AlarmContract.ACTION_ALARM_TOOK, AlarmContract.RC_ALARM_TOOK))
            .build()
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        context,
        requestCode,
        Intent(context, AlarmService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun updateAlarmNotification(doses: List<DoseUi>, snoozeMinutes: Int) {
        nm.notify(AlarmContract.NOTIF_ALARM, buildAlarmNotification(doses, snoozeMinutes))
    }

    fun cancelAlarmNotification() {
        nm.cancel(AlarmContract.NOTIF_ALARM)
    }

    /** True if the full-screen alarm screen may take over the lock screen (Android 14+ can deny this). */
    fun canUseFullScreenIntent(): Boolean =
        android.os.Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
}
