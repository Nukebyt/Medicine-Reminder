package com.nukebyt.medicinereminder.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Bit 0 = Monday ... bit 6 = Sunday. */
object DaysMask {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKEND = 0b1100000

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)
    fun days(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { contains(mask, it) }
}

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosageAmount: String,
    val dosageUnit: String,
    val form: MedForm,
    val mealRelation: MealRelation,
    val colorIndex: Int,
    val notes: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val daysOfWeekMask: Int = DaysMask.ALL,
    val isPaused: Boolean = false,
    val createdAt: Instant = Instant.EPOCH,
    val times: List<LocalTime> = emptyList(),
) {
    val dosageText: String
        get() = listOf(dosageAmount.trim(), dosageUnit.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    /** True when a dose is scheduled on [date]: in range, on a chosen weekday and not paused. */
    fun occursOn(date: LocalDate): Boolean =
        !isPaused &&
            !date.isBefore(startDate) &&
            (endDate == null || !date.isAfter(endDate)) &&
            DaysMask.contains(daysOfWeekMask, date.dayOfWeek)

    fun isFinished(today: LocalDate): Boolean = endDate != null && today.isAfter(endDate)
}

data class DoseLog(
    val medicineId: Long,
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    val actionAt: Instant,
)
