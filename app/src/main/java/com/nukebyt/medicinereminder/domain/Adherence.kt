package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DaySummary
import java.time.LocalDate

data class AdherenceStats(
    val taken: Int,
    val late: Int,
    val skipped: Int,
    val missed: Int,
) {
    val countable: Int get() = taken + skipped + missed

    /** 0..100, or null when there is nothing to measure yet. */
    val percent: Int? get() = if (countable == 0) null else (taken * 100) / countable
}

object Adherence {

    fun stats(days: Collection<DaySummary>): AdherenceStats =
        AdherenceStats(
            taken = days.sumOf { it.taken },
            late = days.sumOf { it.takenLate },
            skipped = days.sumOf { it.skipped },
            missed = days.sumOf { it.missed },
        )

    /** Stats for the [days] days ending on [end] (inclusive). */
    fun statsForLast(days: Int, end: LocalDate, summaries: Map<LocalDate, DaySummary>): AdherenceStats =
        stats((0 until days).mapNotNull { summaries[end.minusDays(it.toLong())] })

    /**
     * Number of consecutive fully-taken days ending today. Days without doses are neutral. Today does not
     * break the streak while doses are still pending, but a missed or skipped dose does.
     */
    fun currentStreak(today: LocalDate, summaries: Map<LocalDate, DaySummary>): Int {
        val earliest = summaries.keys.minOrNull() ?: return 0
        var streak = 0
        var day = today
        while (!day.isBefore(earliest)) {
            val s = summaries[day]
            when {
                s == null || s.total == 0 -> Unit
                s.isFullyTaken -> streak++
                day == today && s.missed == 0 && s.skipped == 0 -> Unit
                else -> return streak
            }
            day = day.minusDays(1)
        }
        return streak
    }
}
