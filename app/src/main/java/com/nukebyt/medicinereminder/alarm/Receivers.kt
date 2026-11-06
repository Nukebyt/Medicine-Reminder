package com.nukebyt.medicinereminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import com.nukebyt.medicinereminder.domain.TimeSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

/**
 * Receivers get their dependencies from a Hilt entry point. (@AndroidEntryPoint on a Kotlin BroadcastReceiver
 * requires calling an abstract super method, which Kotlin rejects.)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReceiverEntryPoint {
    fun scheduler(): AlarmScheduler
    fun gateway(): DoseGateway
    fun notifier(): ReminderNotifier
    fun settings(): SettingsRepository
    fun time(): TimeSource
}

internal fun Context.receiverDeps(): ReceiverEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, ReceiverEntryPoint::class.java)

/** Receivers must finish fast: goAsync + a coroutine, always finishing within the 10 s budget. */
internal fun BroadcastReceiver.goAsyncWork(name: String, block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            withTimeout(9_000) { block() }
        } catch (e: Exception) {
            AppLog.e("$name failed", e)
        } finally {
            pending.finish()
        }
    }
}

internal fun Intent.slotKey(): SlotKey? {
    if (!hasExtra(AlarmContract.EXTRA_SLOT) || !hasExtra(AlarmContract.EXTRA_DATE)) return null
    val minute = getIntExtra(AlarmContract.EXTRA_SLOT, -1)
    if (minute !in 0..1439) return null
    return SlotKey(
        date = LocalDate.ofEpochDay(getLongExtra(AlarmContract.EXTRA_DATE, 0L)),
        slot = minute.toSlotTime(),
        test = getBooleanExtra(AlarmContract.EXTRA_TEST, false),
    )
}

/**
 * A slot alarm (08:00) or a snooze alarm fired. Posts one grouped notification for the doses still pending and
 * arms the escalation alarm. The next occurrence is scheduled first so a failure later never loses tomorrow.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val deps = context.receiverDeps()
        val scheduler = deps.scheduler(); val gateway = deps.gateway(); val notifier = deps.notifier()
        val settings = deps.settings(); val time = deps.time()
        val key = intent.slotKey() ?: return
        val isSnooze = intent.action == AlarmContract.ACTION_SNOOZE_FIRE
        AppLog.d("reminder fired: ${key.date} ${key.slot} snooze=$isSnooze test=${key.test}")
        goAsyncWork("ReminderReceiver") {
            if (!isSnooze && !key.test) scheduler.scheduleSlot(key.slot)
            val pending = gateway.pending(key)
            if (pending.isEmpty()) {
                AppLog.d("nothing pending at ${key.slot}: no reminder (all taken early or removed)")
                return@goAsyncWork
            }
            val s = settings.current()
            notifier.showReminder(key, pending, s.snoozeMin)
            if (s.escalationDelayMin > 0) {
                scheduler.scheduleEscalation(key, time.now().plusMinutes(s.escalationDelayMin.toLong()).toInstant())
            }
        }
    }
}

/** The reminder was not answered in time: if anything is still pending, ring the alarm. */
class EscalationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val gateway = context.receiverDeps().gateway()
        val key = intent.slotKey() ?: return
        AppLog.d("escalation fired: ${key.date} ${key.slot}")
        goAsyncWork("EscalationReceiver") {
            if (gateway.pending(key).isEmpty()) {
                AppLog.d("escalation at ${key.slot}: nothing pending, staying quiet")
                return@goAsyncWork
            }
            AlarmService.start(context, key)
        }
    }
}

/** Taken / Snooze buttons on the reminder notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val deps = context.receiverDeps()
        val scheduler = deps.scheduler(); val gateway = deps.gateway(); val notifier = deps.notifier()
        val settings = deps.settings(); val time = deps.time()
        val key = intent.slotKey() ?: return
        val action = intent.action
        AppLog.d("notification action $action for ${key.date} ${key.slot}")
        goAsyncWork("NotificationActionReceiver") {
            when (action) {
                AlarmContract.ACTION_TAKEN -> {
                    // Marks every pending dose in the slot; the shared rule cancels escalation and the notification.
                    val count = gateway.markTaken(key)
                    AppLog.d("marked $count dose(s) taken from notification")
                    notifier.cancelReminder(key)
                }
                AlarmContract.ACTION_SNOOZE -> {
                    val minutes = settings.current().snoozeMin
                    scheduler.cancelEscalation(key)
                    scheduler.scheduleSnooze(key, time.now().plusMinutes(minutes.toLong()).toInstant())
                    notifier.cancelReminder(key)
                }
            }
        }
    }
}

/** Boot, time change, time-zone change, app update, exact-alarm permission change: re-arm everything. */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = context.receiverDeps().scheduler()
        val action = intent.action ?: return
        if (action !in HANDLED) return
        AppLog.d("reschedule trigger: $action")
        goAsyncWork("RescheduleReceiver") { scheduler.rescheduleAll(restoreEscalations = true) }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
