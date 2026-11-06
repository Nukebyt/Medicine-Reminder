package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.Medicine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class MedicineFormError { NAME_REQUIRED, TIME_REQUIRED, DUPLICATE_TIME, END_BEFORE_START, NO_DAYS_SELECTED, DURATION_INVALID }

/** Validation for the add/edit form: name required, at least one time, no duplicate times, end >= start. */
object MedicineValidator {
    fun validate(
        name: String,
        times: List<LocalTime>,
        daysMask: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
    ): Set<MedicineFormError> = buildSet {
        if (name.isBlank()) add(MedicineFormError.NAME_REQUIRED)
        if (times.isEmpty()) add(MedicineFormError.TIME_REQUIRED)
        if (times.size != times.distinct().size) add(MedicineFormError.DUPLICATE_TIME)
        if (daysMask and 0b1111111 == 0) add(MedicineFormError.NO_DAYS_SELECTED)
        if (endDate != null && endDate.isBefore(startDate)) add(MedicineFormError.END_BEFORE_START)
    }
}

object NextDose {
    /** The first moment after [now] at which [medicine] has a dose, or null if it never will again. */
    fun nextReminder(medicine: Medicine, now: ZonedDateTime): ZonedDateTime? {
        if (medicine.isPaused || medicine.times.isEmpty()) return null
        val first = maxOf(now.toLocalDate(), medicine.startDate)
        // A weekly mask repeats within seven days, so eight days is enough from the first candidate date.
        for (offset in 0L..8L) {
            val date = first.plusDays(offset)
            if (!medicine.occursOn(date)) continue
            val next = medicine.times
                .map { ZonedDateTime.of(date, it, now.zone) }
                .filter { it.toInstant() > now.toInstant() }
                .minOrNull()
            if (next != null) return next
        }
        return null
    }
}
