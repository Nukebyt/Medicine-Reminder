package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DaySummary
import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.model.Medicine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Builds the dose list for a day from medicines x times, joined with the stored user actions.
 * Pure Kotlin, no Android imports.
 */
object DoseResolver {

    /** A taken dose is "late" if the user acted more than this long after the scheduled time. */
    val LATE_GRACE: Duration = Duration.ofMinutes(30)

    fun resolve(
        date: LocalDate,
        now: ZonedDateTime,
        medicines: List<Medicine>,
        logs: List<DoseLog>,
    ): List<DoseUi> {
        val logIndex = logs.asSequence()
            .filter { it.date == date }
            .associateBy { it.medicineId to it.time }
        val doses = ArrayList<DoseUi>()
        for (medicine in medicines) {
            if (!medicine.occursOn(date)) continue
            for (time in medicine.times.distinct()) {
                val log = logIndex[medicine.id to time]
                doses += DoseUi(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    dosageText = medicine.dosageText,
                    form = medicine.form,
                    mealRelation = medicine.mealRelation,
                    colorIndex = medicine.colorIndex,
                    date = date,
                    time = time,
                    state = stateOf(date, time, log, now),
                    actionAt = log?.actionAt,
                )
            }
        }
        return doses.sortedWith(
            compareBy<DoseUi> { it.time }.thenBy { it.medicineName.lowercase() }.thenBy { it.medicineId },
        )
    }

    fun stateOf(date: LocalDate, time: LocalTime, log: DoseLog?, now: ZonedDateTime): DoseState {
        val scheduled = date.atTime(time).atZone(now.zone).toInstant()
        if (log != null) {
            return when (log.status) {
                DoseStatus.SKIPPED -> DoseState.SKIPPED
                DoseStatus.TAKEN ->
                    if (log.actionAt <= scheduled.plus(LATE_GRACE)) DoseState.TAKEN else DoseState.TAKEN_LATE
            }
        }
        val today = now.toLocalDate()
        return when {
            date.isBefore(today) -> DoseState.MISSED
            date.isAfter(today) -> DoseState.UPCOMING
            now.toInstant() >= scheduled -> DoseState.OVERDUE
            else -> DoseState.UPCOMING
        }
    }

    fun summarize(doses: List<DoseUi>): DaySummary {
        var taken = 0
        var late = 0
        var skipped = 0
        var missed = 0
        var pending = 0
        for (d in doses) {
            when (d.state) {
                DoseState.TAKEN -> taken++
                DoseState.TAKEN_LATE -> { taken++; late++ }
                DoseState.SKIPPED -> skipped++
                DoseState.MISSED -> missed++
                DoseState.UPCOMING, DoseState.OVERDUE -> pending++
            }
        }
        return DaySummary(taken, late, skipped, missed, pending)
    }

    /** Summaries for every date in [dates]. */
    fun summarizeRange(
        dates: Iterable<LocalDate>,
        now: ZonedDateTime,
        medicines: List<Medicine>,
        logs: List<DoseLog>,
    ): Map<LocalDate, DaySummary> {
        val logsByDate = logs.groupBy { it.date }
        return dates.associateWith { date ->
            summarize(resolve(date, now, medicines, logsByDate[date].orEmpty()))
        }
    }

    /** Scheduled instant of a dose; used to compute "overdue by N min". */
    fun scheduledAt(dose: DoseUi, now: ZonedDateTime): Instant =
        dose.date.atTime(dose.time).atZone(now.zone).toInstant()
}
