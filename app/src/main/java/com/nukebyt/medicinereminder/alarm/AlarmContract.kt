package com.nukebyt.medicinereminder.alarm

import android.util.Log
import java.time.LocalDate
import java.time.LocalTime

/** One log tag for the whole reminder pipeline: `adb logcat -s MedicineReminder:*`. */
object AppLog {
    const val TAG = "MedicineReminder"
    fun d(message: String) { Log.d(TAG, message) }
    fun w(message: String, t: Throwable? = null) { Log.w(TAG, message, t) }
    fun e(message: String, t: Throwable? = null) { Log.e(TAG, message, t) }
}

object AlarmContract {
    private const val PREFIX = "com.nukebyt.medicinereminder.action."

    // Broadcast actions
    const val ACTION_SLOT = PREFIX + "SLOT"
    const val ACTION_SNOOZE_FIRE = PREFIX + "SNOOZE_FIRE"
    const val ACTION_ESCALATE = PREFIX + "ESCALATE"
    const val ACTION_TAKEN = PREFIX + "TAKEN"
    const val ACTION_SNOOZE = PREFIX + "SNOOZE"

    // AlarmService commands
    const val ACTION_ALARM_START = PREFIX + "ALARM_START"
    const val ACTION_ALARM_STOP = PREFIX + "ALARM_STOP"
    const val ACTION_ALARM_TOOK = PREFIX + "ALARM_TOOK"
    const val ACTION_ALARM_SNOOZE = PREFIX + "ALARM_SNOOZE"
    const val ACTION_ALARM_REFRESH = PREFIX + "ALARM_REFRESH"

    const val EXTRA_DATE = "date_epoch_day"
    const val EXTRA_SLOT = "slot_minute"
    const val EXTRA_TEST = "is_test"

    // PendingIntent request codes (ROADMAP section 8, phase 5). Test alarms add TEST_OFFSET so they never
    // collide with a real slot at the same minute.
    const val RC_SLOT = 0
    const val RC_ESCALATION = 10_000
    const val RC_SNOOZE = 20_000
    const val RC_ACTION_TAKEN = 40_000
    const val RC_ACTION_SNOOZE = 50_000
    const val RC_OPEN_APP = 60_000
    const val RC_ALARM_SCREEN = 60_001
    const val RC_ALARM_STOP = 60_002
    const val RC_ALARM_TOOK = 60_003
    const val TEST_OFFSET = 100_000

    // Notification channels
    const val CHANNEL_REMINDERS = "dose_reminders"
    const val CHANNEL_ALARMS = "dose_alarms"
    const val CHANNEL_MISSED = "dose_missed"

    // Notification ids
    const val NOTIF_REMINDER_BASE = 1_000
    const val NOTIF_ALARM = 2_001
    const val NOTIF_MISSED_BASE = 3_000
}

/** Minute of day 0..1439, used for request codes and notification ids. */
fun LocalTime.minuteOfDayInt(): Int = hour * 60 + minute

fun Int.toSlotTime(): LocalTime = LocalTime.of(this / 60, this % 60)

/** A slot that is ringing or has been reminded: one dose time on one date. [test] is for the debug tools. */
data class SlotKey(val date: LocalDate, val slot: LocalTime, val test: Boolean = false)
