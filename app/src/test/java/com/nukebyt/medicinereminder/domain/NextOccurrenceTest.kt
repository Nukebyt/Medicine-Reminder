package com.nukebyt.medicinereminder.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextOccurrenceTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val slot = LocalTime.of(8, 0)

    private fun now(h: Int, m: Int = 0, s: Int = 0, date: LocalDate = LocalDate.of(2026, 11, 6)) =
        ZonedDateTime.of(date, LocalTime.of(h, m, s), zone)

    @Test fun beforeTheSlot_isToday() {
        assertEquals(now(8), NextOccurrence.compute(slot, now(7, 59, 59)))
    }

    @Test fun afterTheSlot_isTomorrow() {
        assertEquals(now(8, date = LocalDate.of(2026, 11, 7)), NextOccurrence.compute(slot, now(8, 0, 1)))
    }

    @Test fun exactlyAtTheSlot_isTomorrow() {
        assertEquals(now(8, date = LocalDate.of(2026, 11, 7)), NextOccurrence.compute(slot, now(8)))
    }

    @Test fun midnightWrap_lateNightForEarlyMorningSlot() {
        val next = NextOccurrence.compute(LocalTime.of(0, 30), now(23, 59))
        assertEquals(LocalDateTime.of(2026, 11, 7, 0, 30), next.toLocalDateTime())
    }

    @Test fun midnightSlot_justAfterMidnight() {
        val next = NextOccurrence.compute(LocalTime.MIDNIGHT, now(0, 0, 1))
        assertEquals(LocalDateTime.of(2026, 11, 7, 0, 0), next.toLocalDateTime())
    }

    @Test fun monthAndYearRollover() {
        val dec31 = ZonedDateTime.of(2026, 12, 31, 23, 0, 0, 0, zone)
        assertEquals(LocalDateTime.of(2027, 1, 1, 8, 0), NextOccurrence.compute(slot, dec31).toLocalDateTime())
    }

    @Test fun followsTheZoneOfNow_afterATimeZoneChange() {
        val ny = ZoneId.of("America/New_York")
        val nowNy = ZonedDateTime.of(2026, 11, 6, 7, 0, 0, 0, ny)
        val next = NextOccurrence.compute(slot, nowNy)
        assertEquals(ny, next.zone)
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 0), next.toLocalDateTime())
    }

    @Test fun springForwardGap_slotInsideTheGapStillFiresOnce() {
        val ny = ZoneId.of("America/New_York")
        // 2026-03-08 02:30 does not exist in New York (clocks jump 02:00 -> 03:00).
        val before = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ny)
        val next = NextOccurrence.compute(LocalTime.of(2, 30), before)
        assertEquals(LocalDate.of(2026, 3, 8), next.toLocalDate())
        assertEquals(3, next.hour) // shifted forward by the gap
    }

    @Test fun resultIsAlwaysStrictlyInTheFuture() {
        for (minute in 0 until 24 * 60 step 7) {
            val n = now(minute / 60, minute % 60)
            val next = NextOccurrence.compute(slot, n)
            assert(next.toInstant() > n.toInstant()) { "next $next not after $n" }
        }
    }
}
