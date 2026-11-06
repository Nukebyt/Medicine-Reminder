package com.nukebyt.medicinereminder.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Source of "now" so logic is testable; always reads the current default zone. */
fun interface TimeSource {
    fun now(): ZonedDateTime
}

/**
 * What the data layer needs from the alarm layer. Keeps repositories free of Android alarm code and
 * lets every entry point (Today toggle, notification action, alarm screen) share one rule set.
 */
interface ReminderScheduler {
    /** Cancel stale slot alarms and schedule every distinct slot. Call after any medicine change. */
    suspend fun rescheduleAll()

    /**
     * A dose in the slot changed state. When [pendingCount] is 0 the escalation alarm, the reminder
     * notification and any ringing alarm for that slot must be cancelled.
     */
    suspend fun onSlotUpdated(date: LocalDate, slot: LocalTime, pendingCount: Int)
}
