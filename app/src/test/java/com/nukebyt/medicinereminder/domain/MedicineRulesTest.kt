package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DaysMask
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MedicineRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 6) // Friday
    private val eight = LocalTime.of(8, 0)

    private fun now(h: Int, m: Int = 0, date: LocalDate = today): ZonedDateTime = date.atTime(h, m).atZone(zone)

    private fun med(
        times: List<LocalTime> = listOf(eight),
        start: LocalDate = today,
        end: LocalDate? = null,
        mask: Int = DaysMask.ALL,
        paused: Boolean = false,
    ) = Medicine(
        id = 1, name = "Metformin", dosageAmount = "500", dosageUnit = "mg", form = MedForm.TABLET,
        mealRelation = MealRelation.AFTER_MEAL, colorIndex = 0, startDate = start, endDate = end,
        daysOfWeekMask = mask, isPaused = paused, times = times,
    )

    // ---- validation ----

    private fun validate(
        name: String = "Metformin",
        times: List<LocalTime> = listOf(eight),
        mask: Int = DaysMask.ALL,
        start: LocalDate = today,
        end: LocalDate? = null,
    ) = MedicineValidator.validate(name, times, mask, start, end)

    @Test fun validForm_hasNoErrors() = assertTrue(validate().isEmpty())

    @Test fun nameIsRequired_blankCounts() {
        assertEquals(setOf(MedicineFormError.NAME_REQUIRED), validate(name = "   "))
        assertEquals(setOf(MedicineFormError.NAME_REQUIRED), validate(name = ""))
    }

    @Test fun atLeastOneTime() = assertEquals(setOf(MedicineFormError.TIME_REQUIRED), validate(times = emptyList()))

    @Test fun duplicateTimesRejected() =
        assertEquals(setOf(MedicineFormError.DUPLICATE_TIME), validate(times = listOf(eight, eight)))

    @Test fun endBeforeStartRejected_sameDayAllowed() {
        assertEquals(setOf(MedicineFormError.END_BEFORE_START), validate(end = today.minusDays(1)))
        assertTrue(validate(end = today).isEmpty())
    }

    @Test fun atLeastOneDay() = assertEquals(setOf(MedicineFormError.NO_DAYS_SELECTED), validate(mask = 0))

    @Test fun severalErrorsReportedTogether() {
        val errors = validate(name = "", times = emptyList(), mask = 0)
        assertEquals(setOf(MedicineFormError.NAME_REQUIRED, MedicineFormError.TIME_REQUIRED, MedicineFormError.NO_DAYS_SELECTED), errors)
    }

    // ---- next reminder ----

    @Test fun nextReminder_laterToday() {
        assertEquals(LocalDateTime.of(2026, 11, 6, 21, 0), NextDose.nextReminder(med(times = listOf(eight, LocalTime.of(21, 0))), now(9))?.toLocalDateTime())
    }

    @Test fun nextReminder_tomorrowWhenTodaysTimesPassed() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 8, 0), NextDose.nextReminder(med(), now(9))?.toLocalDateTime())
    }

    @Test fun nextReminder_respectsDaysOfWeek() {
        val monday = LocalDate.of(2026, 11, 9)
        val onlyMonday = med(mask = DaysMask.bit(DayOfWeek.MONDAY))
        assertEquals(monday.atTime(8, 0), NextDose.nextReminder(onlyMonday, now(9))?.toLocalDateTime())
    }

    @Test fun nextReminder_waitsForFutureStartDate() {
        val start = today.plusDays(10)
        assertEquals(start.atTime(8, 0), NextDose.nextReminder(med(start = start), now(9))?.toLocalDateTime())
    }

    @Test fun nextReminder_nullWhenPausedFinishedOrNoTimes() {
        assertNull(NextDose.nextReminder(med(paused = true), now(9)))
        assertNull(NextDose.nextReminder(med(end = today.minusDays(1), start = today.minusDays(5)), now(9)))
        assertNull(NextDose.nextReminder(med(times = emptyList()), now(9)))
    }

    @Test fun nextReminder_lastDoseOnEndDateThenNone() {
        val m = med(end = today)
        assertEquals(today.atTime(8, 0), NextDose.nextReminder(m, now(7))?.toLocalDateTime())
        assertNull(NextDose.nextReminder(m, now(9)))
    }
}
