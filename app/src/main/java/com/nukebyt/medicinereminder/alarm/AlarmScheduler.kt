package com.nukebyt.medicinereminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.nukebyt.medicinereminder.MainActivity
import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import com.nukebyt.medicinereminder.domain.NextOccurrence
import com.nukebyt.medicinereminder.domain.ReminderScheduler
import com.nukebyt.medicinereminder.domain.TimeSource
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All AlarmManager work. Alarms are per time slot (08:00), not per medicine, so three medicines due at 8:00
 * give one grouped notification. Uses `setAlarmClock`, the most reliable exact alarm: it exits Doze and is not
 * rate-limited like `setExactAndAllowWhileIdle`, which would break a 5-minute escalation (ROADMAP section 2).
 *
 * Request codes: slot = minuteOfDay, escalation = 10_000 + minuteOfDay, snooze = 20_000 + minuteOfDay.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicineDao: MedicineDao,
    private val settings: SettingsRepository,
    private val time: TimeSource,
    private val notifier: ReminderNotifier,
    private val alarmState: AlarmStateHolder,
    private val gateway: Lazy<DoseGateway>,
) : ReminderScheduler {

    private val alarmManager: AlarmManager get() = context.getSystemService()!!

    /** False when the user has revoked "Alarms & reminders". We then fall back to an inexact alarm. */
    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // ---- public API (ROADMAP phase 5) ----

    /** Schedule the next occurrence of a daily [slot]. */
    fun scheduleSlot(slot: LocalTime): ZonedDateTime {
        val next = NextOccurrence.compute(slot, time.now())
        val key = SlotKey(next.toLocalDate(), slot)
        set(next.toInstant(), broadcast(ReminderReceiver::class.java, AlarmContract.ACTION_SLOT, AlarmContract.RC_SLOT + slot.minuteOfDayInt(), key))
        AppLog.d("slot $slot scheduled for $next (exact=${canScheduleExact()})")
        return next
    }

    fun scheduleEscalation(key: SlotKey, at: Instant) {
        set(at, broadcast(EscalationReceiver::class.java, AlarmContract.ACTION_ESCALATE, escalationCode(key), key))
        AppLog.d("escalation for ${key.date} ${key.slot} scheduled at $at")
    }

    fun cancelEscalation(key: SlotKey) {
        cancel(EscalationReceiver::class.java, AlarmContract.ACTION_ESCALATE, escalationCode(key))
        AppLog.d("escalation for ${key.date} ${key.slot} cancelled")
    }

    fun scheduleSnooze(key: SlotKey, at: Instant) {
        set(at, broadcast(ReminderReceiver::class.java, AlarmContract.ACTION_SNOOZE_FIRE, snoozeCode(key), key))
        AppLog.d("snooze for ${key.date} ${key.slot} scheduled at $at")
    }

    fun cancelSnooze(key: SlotKey) {
        cancel(ReminderReceiver::class.java, AlarmContract.ACTION_SNOOZE_FIRE, snoozeCode(key))
    }

    /**
     * Cancel alarms for slots that no longer exist, then schedule every distinct slot. Idempotent: calling it
     * again replaces each alarm. With [restoreEscalations] (boot, time change, app update) also re-arm an
     * escalation that was pending when the alarms were wiped.
     */
    suspend fun rescheduleAll(restoreEscalations: Boolean) {
        val slots = medicineDao.activeSlots()
        val wanted = slots.map { it.minuteOfDayInt() }.toSet()
        val previous = settings.scheduledSlots()
        for (stale in previous - wanted) {
            val slot = stale.toSlotTime()
            cancel(ReminderReceiver::class.java, AlarmContract.ACTION_SLOT, AlarmContract.RC_SLOT + stale)
            cancelAllFor(slot)
            notifier.cancelReminder(SlotKey(time.now().toLocalDate(), slot))
        }
        slots.forEach(::scheduleSlot)
        settings.setScheduledSlots(wanted)
        if (restoreEscalations) restoreEscalations(slots)
        AppLog.d("rescheduleAll: ${slots.size} slot(s), restore=$restoreEscalations, exact=${canScheduleExact()}")
    }

    override suspend fun rescheduleAll() = rescheduleAll(restoreEscalations = false)

    /**
     * A dose at [slot] changed state. Rule 1 (ROADMAP section 3): when nothing is pending any more, cancel the
     * escalation (and snooze), dismiss the notification and stop a ringing alarm. Otherwise refresh the texts.
     */
    override suspend fun onSlotUpdated(date: LocalDate, slot: LocalTime, pendingCount: Int) =
        onSlotUpdated(SlotKey(date, slot), pendingCount)

    suspend fun onSlotUpdated(key: SlotKey, pendingCount: Int) {
        if (pendingCount == 0) {
            cancelEscalation(key)
            cancelSnooze(key)
            notifier.cancelReminder(key)
        } else if (notifier.isReminderShowing(key)) {
            notifier.showReminder(key, gateway.get().pending(key), settings.current().snoozeMin, alert = false)
        }
        if (alarmState.isRinging) AlarmService.send(context, AlarmContract.ACTION_ALARM_REFRESH)
    }

    // ---- debug tools ----

    /** Fire a fake reminder for a made-up dose after [delaySeconds]. Debug builds only (see Settings). */
    fun scheduleTestReminder(delaySeconds: Long): SlotKey {
        val at = time.now().plusSeconds(delaySeconds)
        val key = SlotKey(at.toLocalDate(), at.toLocalTime().withSecond(0).withNano(0), test = true)
        set(at.toInstant(), broadcast(ReminderReceiver::class.java, AlarmContract.ACTION_SLOT, AlarmContract.RC_SLOT + AlarmContract.TEST_OFFSET + key.slot.minuteOfDayInt(), key))
        AppLog.d("TEST reminder scheduled for $at")
        return key
    }

    /** Human-readable list of what is scheduled, for the debug screen. `adb shell dumpsys alarm` shows the rest. */
    suspend fun describe(): String {
        val now = time.now()
        val slots = settings.scheduledSlots().sorted()
        if (slots.isEmpty()) return "No slots scheduled. Exact alarms: ${canScheduleExact()}"
        return buildString {
            appendLine("Exact alarms allowed: ${canScheduleExact()}")
            slots.forEach { appendLine("${it.toSlotTime()} -> next ${NextOccurrence.compute(it.toSlotTime(), now)}") }
        }.trim()
    }

    // ---- internals ----

    private suspend fun restoreEscalations(slots: List<LocalTime>) {
        val now = time.now()
        val delayMin = settings.current().escalationDelayMin
        if (delayMin <= 0) return
        for (slot in slots) {
            val key = SlotKey(now.toLocalDate(), slot)
            val scheduled = ZonedDateTime.of(key.date, slot, now.zone)
            val escalateAt = scheduled.plusMinutes(delayMin.toLong())
            val inWindow = !now.isBefore(scheduled) && now.isBefore(escalateAt)
            if (!inWindow) continue
            if (exists(EscalationReceiver::class.java, AlarmContract.ACTION_ESCALATE, escalationCode(key))) continue
            if (exists(ReminderReceiver::class.java, AlarmContract.ACTION_SNOOZE_FIRE, snoozeCode(key))) continue
            if (gateway.get().pending(key).isNotEmpty()) {
                scheduleEscalation(key, escalateAt.toInstant())
                AppLog.d("restored escalation for ${key.slot} after reboot or time change")
            }
        }
    }

    private fun cancelAllFor(slot: LocalTime) {
        val key = SlotKey(LocalDate.now(), slot)
        cancelEscalation(key)
        cancelSnooze(key)
    }

    private fun escalationCode(key: SlotKey) =
        AlarmContract.RC_ESCALATION + key.slot.minuteOfDayInt() + if (key.test) AlarmContract.TEST_OFFSET else 0

    private fun snoozeCode(key: SlotKey) =
        AlarmContract.RC_SNOOZE + key.slot.minuteOfDayInt() + if (key.test) AlarmContract.TEST_OFFSET else 0

    private fun set(at: Instant, operation: PendingIntent) {
        val triggerAt = at.toEpochMilli()
        val am = alarmManager
        if (canScheduleExact()) {
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showAppIntent()), operation)
                return
            } catch (e: SecurityException) {
                AppLog.w("setAlarmClock denied, falling back to an inexact alarm", e)
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    private fun showAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        AlarmContract.RC_OPEN_APP,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(receiver: Class<*>, action: String, requestCode: Int, key: SlotKey): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).setAction(action).apply {
                putExtra(AlarmContract.EXTRA_DATE, key.date.toEpochDay())
                putExtra(AlarmContract.EXTRA_SLOT, key.slot.minuteOfDayInt())
                putExtra(AlarmContract.EXTRA_TEST, key.test)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun existing(receiver: Class<*>, action: String, requestCode: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun exists(receiver: Class<*>, action: String, requestCode: Int) = existing(receiver, action, requestCode) != null

    private fun cancel(receiver: Class<*>, action: String, requestCode: Int) {
        existing(receiver, action, requestCode)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
