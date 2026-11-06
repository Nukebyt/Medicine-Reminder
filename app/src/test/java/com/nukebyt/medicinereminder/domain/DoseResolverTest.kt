package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DaysMask
import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DoseResolverTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 6) // a Friday
    private val eight = LocalTime.of(8, 0)
    private val nine = LocalTime.of(21, 0)

    private fun at(date: LocalDate, h: Int, m: Int = 0): ZonedDateTime = date.atTime(h, m).atZone(zone)

    private fun med(
        id: Long = 1,
        name: String = "Metformin",
        times: List<LocalTime> = listOf(eight),
        start: LocalDate = today.minusDays(30),
        end: LocalDate? = null,
        mask: Int = DaysMask.ALL,
        paused: Boolean = false,
    ) = Medicine(
        id = id, name = name, dosageAmount = "500", dosageUnit = "mg", form = MedForm.TABLET,
        mealRelation = MealRelation.AFTER_MEAL, colorIndex = 0, startDate = start, endDate = end,
        daysOfWeekMask = mask, isPaused = paused, times = times,
    )

    private fun log(date: LocalDate, time: LocalTime, actionAt: ZonedDateTime, status: DoseStatus = DoseStatus.TAKEN, id: Long = 1) =
        DoseLog(id, date, time, status, actionAt.toInstant())

    private fun stateAt(now: ZonedDateTime, log: DoseLog? = null, date: LocalDate = today) =
        DoseResolver.resolve(date, now, listOf(med()), listOfNotNull(log)).single().state

    // ---- each state in the table ----

    @Test fun upcoming_whenScheduledTimeIsInTheFuture() {
        assertEquals(DoseState.UPCOMING, stateAt(at(today, 7, 59)))
    }

    @Test fun overdue_whenTodayAndPastScheduledTime() {
        assertEquals(DoseState.OVERDUE, stateAt(at(today, 8, 12)))
    }

    @Test fun overdue_exactlyAtScheduledTime() {
        assertEquals(DoseState.OVERDUE, stateAt(at(today, 8, 0)))
    }

    @Test fun missed_onAPastDayWithNoLog() {
        assertEquals(DoseState.MISSED, stateAt(at(today, 10), date = today.minusDays(1)))
    }

    @Test fun upcoming_onAFutureDay() {
        assertEquals(DoseState.UPCOMING, stateAt(at(today, 10), date = today.plusDays(1)))
    }

    @Test fun taken_whenActedWithinThirtyMinutes() {
        val l = log(today, eight, at(today, 8, 30))
        assertEquals(DoseState.TAKEN, stateAt(at(today, 9), l))
    }

    @Test fun takenLate_whenActedAfterThirtyMinutes() {
        val l = log(today, eight, at(today, 8, 31))
        assertEquals(DoseState.TAKEN_LATE, stateAt(at(today, 9), l))
    }

    @Test fun taken_whenMarkedEarlyBeforeTheScheduledTime() {
        val l = log(today, eight, at(today, 7, 0))
        assertEquals(DoseState.TAKEN, stateAt(at(today, 7, 30), l))
    }

    @Test fun skipped_whenLogIsSkipped() {
        val l = log(today, eight, at(today, 8, 5), DoseStatus.SKIPPED)
        assertEquals(DoseState.SKIPPED, stateAt(at(today, 9), l))
    }

    @Test fun lateLoggingOfAPastDay_isTakenLate() {
        val yesterday = today.minusDays(1)
        val l = log(yesterday, eight, at(today, 9))
        assertEquals(DoseState.TAKEN_LATE, stateAt(at(today, 10), l, yesterday))
    }

    // ---- scheduling rules ----

    @Test fun pausedMedicineProducesNoDoses() {
        assertTrue(DoseResolver.resolve(today, at(today, 7), listOf(med(paused = true)), emptyList()).isEmpty())
    }

    @Test fun notBeforeStartDate() {
        val m = med(start = today.plusDays(1))
        assertTrue(DoseResolver.resolve(today, at(today, 7), listOf(m), emptyList()).isEmpty())
        assertEquals(1, DoseResolver.resolve(today.plusDays(1), at(today, 7), listOf(m), emptyList()).size)
    }

    @Test fun endDateIsInclusive() {
        val m = med(end = today)
        assertEquals(1, DoseResolver.resolve(today, at(today, 7), listOf(m), emptyList()).size)
        assertTrue(DoseResolver.resolve(today.plusDays(1), at(today, 7), listOf(m), emptyList()).isEmpty())
    }

    @Test fun daysOfWeekMask_onlyChosenDays() {
        // Friday 2026-11-06. Mon/Wed only.
        val mask = DaysMask.bit(DayOfWeek.MONDAY) or DaysMask.bit(DayOfWeek.WEDNESDAY)
        val m = med(mask = mask)
        assertTrue(DoseResolver.resolve(today, at(today, 7), listOf(m), emptyList()).isEmpty())
        assertEquals(1, DoseResolver.resolve(LocalDate.of(2026, 11, 9), at(today, 7), listOf(m), emptyList()).size) // Monday
        assertEquals(1, DoseResolver.resolve(LocalDate.of(2026, 11, 11), at(today, 7), listOf(m), emptyList()).size) // Wednesday
    }

    @Test fun maskBitZeroIsMonday_bitSixIsSunday() {
        assertEquals(0b0000001, DaysMask.bit(DayOfWeek.MONDAY))
        assertEquals(0b1000000, DaysMask.bit(DayOfWeek.SUNDAY))
        assertEquals(DayOfWeek.entries, DaysMask.days(DaysMask.ALL))
        assertFalse(DaysMask.contains(DaysMask.WEEKDAYS, DayOfWeek.SATURDAY))
        assertEquals(DaysMask.WEEKEND, DaysMask.WEEKDAYS xor DaysMask.ALL)
        assertEquals(0, DaysMask.toggle(DaysMask.bit(DayOfWeek.TUESDAY), DayOfWeek.TUESDAY))
    }

    @Test fun multipleTimesAreSortedAndIndependent() {
        val m = med(times = listOf(nine, eight))
        val l = log(today, eight, at(today, 8, 1))
        val doses = DoseResolver.resolve(today, at(today, 12), listOf(m), listOf(l))
        assertEquals(listOf(eight, nine), doses.map { it.time })
        assertEquals(listOf(DoseState.TAKEN, DoseState.UPCOMING), doses.map { it.state })
    }

    @Test fun duplicateTimesCollapse() {
        val m = med(times = listOf(eight, eight))
        assertEquals(1, DoseResolver.resolve(today, at(today, 7), listOf(m), emptyList()).size)
    }

    @Test fun sameSlotDifferentMedicinesSortByName() {
        val a = med(id = 1, name = "Zinc")
        val b = med(id = 2, name = "aspirin")
        val doses = DoseResolver.resolve(today, at(today, 7), listOf(a, b), emptyList())
        assertEquals(listOf("aspirin", "Zinc"), doses.map { it.medicineName })
    }

    @Test fun logForAnotherMedicineOrTimeDoesNotLeak() {
        val other = log(today, nine, at(today, 8, 1))
        assertEquals(DoseState.OVERDUE, stateAt(at(today, 9), other))
        val otherMed = DoseLog(99, today, eight, DoseStatus.TAKEN, Instant.EPOCH)
        assertEquals(DoseState.OVERDUE, stateAt(at(today, 9), otherMed))
    }

    @Test fun finishedMedicine() {
        assertTrue(med(end = today.minusDays(1)).isFinished(today))
        assertFalse(med(end = today).isFinished(today))
        assertFalse(med(end = null).isFinished(today))
    }

    @Test fun dosageTextJoinsAmountAndUnit() {
        assertEquals("500 mg", med().dosageText)
        assertEquals("1 tablet", med().copy(dosageAmount = "1", dosageUnit = "tablet").dosageText)
        assertEquals("", med().copy(dosageAmount = " ", dosageUnit = "").dosageText)
    }

    @Test fun summaryCountsEachState() {
        val m = med(times = listOf(eight, LocalTime.of(14, 0), nine, LocalTime.of(23, 0)))
        val logs = listOf(
            log(today, eight, at(today, 8, 5)),
            log(today, LocalTime.of(14, 0), at(today, 15, 0), DoseStatus.SKIPPED),
        )
        val doses = DoseResolver.resolve(today, at(today, 22), listOf(m), logs)
        val s = DoseResolver.summarize(doses)
        assertEquals(1, s.taken)
        assertEquals(1, s.skipped)
        assertEquals(2, s.pending) // 21:00 overdue + 23:00 upcoming
        assertEquals(4, s.total)
        assertFalse(s.isFullyTaken)
    }

    @Test fun summaryRangeCoversEveryDate() {
        val dates = (0..6).map { today.minusDays(6).plusDays(it.toLong()) }
        val summaries = DoseResolver.summarizeRange(dates, at(today, 22), listOf(med()), emptyList())
        assertEquals(7, summaries.size)
        assertEquals(6, summaries.values.sumOf { it.missed }) // six past days missed
        assertEquals(1, summaries.getValue(today).pending)   // today 08:00 is overdue but not yet "missed"
    }
}
