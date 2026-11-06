package com.nukebyt.medicinereminder.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class DoseUi(
    val medicineId: Long,
    val medicineName: String,
    val dosageText: String,
    val form: MedForm,
    val mealRelation: MealRelation,
    val colorIndex: Int,
    val date: LocalDate,
    val time: LocalTime,
    val state: DoseState,
    val actionAt: Instant?,
) {
    val isDone: Boolean get() = state.isTaken
    val isPending: Boolean get() = !state.isResolved
}

/** Counts for one day. [taken] includes [takenLate]. */
data class DaySummary(
    val taken: Int = 0,
    val takenLate: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0,
    val pending: Int = 0,
) {
    val total: Int get() = taken + skipped + missed + pending

    /** Doses whose outcome is known: not still upcoming or overdue today. */
    val countable: Int get() = taken + skipped + missed

    val isFullyTaken: Boolean get() = total > 0 && taken == total
}
