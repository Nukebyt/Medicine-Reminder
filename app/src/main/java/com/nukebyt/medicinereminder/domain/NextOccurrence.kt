package com.nukebyt.medicinereminder.domain

import java.time.LocalTime
import java.time.ZonedDateTime

object NextOccurrence {
    /**
     * The next trigger time for a daily [slot], strictly after [now].
     * If [now] is exactly the slot, the next occurrence is tomorrow: the alarm for today is
     * the one that is firing right now.
     * Uses the zone of [now], so a time-zone change followed by a reschedule follows the local clock.
     */
    fun compute(slot: LocalTime, now: ZonedDateTime): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), slot, now.zone)
        return if (today.toInstant() > now.toInstant()) {
            today
        } else {
            ZonedDateTime.of(now.toLocalDate().plusDays(1), slot, now.zone)
        }
    }
}
