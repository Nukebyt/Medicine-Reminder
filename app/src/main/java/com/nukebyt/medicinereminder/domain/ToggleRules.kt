package com.nukebyt.medicinereminder.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class ToggleDecision {
    /** Future days are read-only. */
    BLOCKED_FUTURE,

    /** Logging a dose on a past day asks "Mark as taken for Wed 4 Nov?" first. */
    NEEDS_CONFIRMATION,

    APPLY,
}

object ToggleRules {
    /**
     * Marking Done on today is immediate (also before the scheduled time: the reminder will then skip it).
     * Un-marking is always immediate. Marking on a past day needs confirmation. Future days cannot be toggled.
     */
    fun decide(date: LocalDate, today: LocalDate, markTaken: Boolean): ToggleDecision = when {
        date.isAfter(today) -> ToggleDecision.BLOCKED_FUTURE
        date.isBefore(today) && markTaken -> ToggleDecision.NEEDS_CONFIRMATION
        else -> ToggleDecision.APPLY
    }
}

fun weekStartOf(date: LocalDate, first: DayOfWeek = DayOfWeek.MONDAY): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(first))
