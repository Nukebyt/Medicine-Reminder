package com.nukebyt.medicinereminder.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ToggleRulesTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test fun today_markAndUnmarkApplyImmediately() {
        assertEquals(ToggleDecision.APPLY, ToggleRules.decide(today, today, markTaken = true))
        assertEquals(ToggleDecision.APPLY, ToggleRules.decide(today, today, markTaken = false))
    }

    @Test fun future_isReadOnly() {
        assertEquals(ToggleDecision.BLOCKED_FUTURE, ToggleRules.decide(today.plusDays(1), today, true))
        assertEquals(ToggleDecision.BLOCKED_FUTURE, ToggleRules.decide(today.plusDays(1), today, false))
    }

    @Test fun past_markingNeedsConfirmation_unmarkingDoesNot() {
        assertEquals(ToggleDecision.NEEDS_CONFIRMATION, ToggleRules.decide(today.minusDays(2), today, true))
        assertEquals(ToggleDecision.APPLY, ToggleRules.decide(today.minusDays(2), today, false))
    }

    @Test fun weekStartIsMonday() {
        assertEquals(LocalDate.of(2026, 11, 2), weekStartOf(today)) // Friday -> Monday
        assertEquals(LocalDate.of(2026, 11, 2), weekStartOf(LocalDate.of(2026, 11, 2)))
        assertEquals(LocalDate.of(2026, 11, 2), weekStartOf(LocalDate.of(2026, 11, 8))) // Sunday
        assertEquals(LocalDate.of(2026, 11, 9), weekStartOf(LocalDate.of(2026, 11, 9)))
    }
}
