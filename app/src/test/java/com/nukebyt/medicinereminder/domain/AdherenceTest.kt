package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DaySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AdherenceTest {
    private val today = LocalDate.of(2026, 11, 6)

    private fun full(n: Int = 2) = DaySummary(taken = n)
    private fun missedDay() = DaySummary(taken = 1, missed = 1)

    @Test fun percentIsNullWhenNothingMeasured() {
        assertNull(Adherence.stats(listOf(DaySummary(pending = 3))).percent)
    }

    @Test fun percentExcludesPendingDoses() {
        val stats = Adherence.stats(listOf(DaySummary(taken = 3, missed = 1, pending = 5)))
        assertEquals(75, stats.percent)
    }

    @Test fun lateDosesCountAsTakenButAreTrackedSeparately() {
        val stats = Adherence.stats(listOf(DaySummary(taken = 4, takenLate = 1, missed = 0)))
        assertEquals(100, stats.percent)
        assertEquals(1, stats.late)
    }

    @Test fun statsForLastUsesOnlyTheWindow() {
        val map = mapOf(
            today to full(1),
            today.minusDays(1) to full(1),
            today.minusDays(10) to DaySummary(missed = 5),
        )
        assertEquals(100, Adherence.statsForLast(7, today, map).percent)
        assertEquals(2, Adherence.statsForLast(7, today, map).taken)
    }

    @Test fun streakCountsConsecutiveFullDays() {
        val map = (0..3).associate { today.minusDays(it.toLong()) to full() }
        assertEquals(4, Adherence.currentStreak(today, map))
    }

    @Test fun streakBreaksOnAMissedDay() {
        val map = mapOf(
            today to full(),
            today.minusDays(1) to full(),
            today.minusDays(2) to missedDay(),
            today.minusDays(3) to full(),
        )
        assertEquals(2, Adherence.currentStreak(today, map))
    }

    @Test fun todayWithPendingDosesDoesNotBreakTheStreak() {
        val map = mapOf(
            today to DaySummary(taken = 1, pending = 1),
            today.minusDays(1) to full(),
            today.minusDays(2) to full(),
        )
        assertEquals(2, Adherence.currentStreak(today, map))
    }

    @Test fun todayWithAMissedDoseBreaksTheStreak() {
        val map = mapOf(today to missedDay(), today.minusDays(1) to full())
        assertEquals(0, Adherence.currentStreak(today, map))
    }

    @Test fun daysWithoutDosesAreNeutral() {
        val map = mapOf(
            today to full(),
            today.minusDays(1) to DaySummary(),
            today.minusDays(2) to full(),
        )
        assertEquals(2, Adherence.currentStreak(today, map))
    }

    @Test fun emptyHistoryIsZero() {
        assertEquals(0, Adherence.currentStreak(today, emptyMap()))
    }
}
