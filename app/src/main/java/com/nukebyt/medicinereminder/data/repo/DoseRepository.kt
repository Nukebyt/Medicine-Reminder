package com.nukebyt.medicinereminder.data.repo

import com.nukebyt.medicinereminder.data.db.DoseLogDao
import com.nukebyt.medicinereminder.data.db.DoseLogEntity
import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.db.toDomain
import com.nukebyt.medicinereminder.data.time.MinuteTicker
import com.nukebyt.medicinereminder.domain.DoseResolver
import com.nukebyt.medicinereminder.domain.ReminderScheduler
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.DaySummary
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.DoseUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where doses are marked and unmarked, so the Today toggle, the notification actions and the
 * alarm screen all share the same "cancel escalation when the slot is complete" rule (ROADMAP section 3, rule 1).
 */
@Singleton
class DoseRepository @Inject constructor(
    private val medicineDao: MedicineDao,
    private val logDao: DoseLogDao,
    private val time: TimeSource,
    private val ticker: MinuteTicker,
    private val scheduler: ReminderScheduler,
) {
    /** Doses on [date], re-resolved every minute so Upcoming becomes Overdue live. */
    fun observeDosesFor(date: LocalDate): Flow<List<DoseUi>> =
        combine(medicineDao.observeAll(), logDao.observeForDate(date), ticker.ticks()) { meds, logs, now ->
            DoseResolver.resolve(date, now, meds.map { it.toDomain() }, logs.map { it.toDomain() })
        }.distinctUntilChanged()

    /** Taken/total per day for the seven days starting at [weekStart]. */
    fun observeWeekSummary(weekStart: LocalDate): Flow<Map<LocalDate, DaySummary>> =
        observeSummaries(weekStart, weekStart.plusDays(6))

    fun observeSummaries(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DaySummary>> =
        combine(medicineDao.observeAll(), logDao.observeBetween(from, to), ticker.ticks()) { meds, logs, now ->
            val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.asIterable()
            DoseResolver.summarizeRange(dates, now, meds.map { it.toDomain() }, logs.map { it.toDomain() })
        }.distinctUntilChanged()

    /** Every dose from [from] to [to] inclusive, for the history screen. */
    fun observeDosesBetween(from: LocalDate, to: LocalDate): Flow<List<DoseUi>> =
        combine(medicineDao.observeAll(), logDao.observeBetween(from, to), ticker.ticks()) { meds, logs, now ->
            val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.asIterable()
            DoseResolver.resolveRange(dates, now, meds.map { it.toDomain() }, logs.map { it.toDomain() })
        }.distinctUntilChanged()

    suspend fun dosesFor(date: LocalDate): List<DoseUi> {
        val meds = medicineDao.getAll().map { it.toDomain() }
        val logs = logDao.getForDate(date).map { it.toDomain() }
        return DoseResolver.resolve(date, time.now(), meds, logs)
    }

    /** Doses at one slot that the user has not acted on yet. */
    suspend fun pendingDosesForSlot(date: LocalDate, slot: LocalTime): List<DoseUi> =
        dosesFor(date).filter { it.time == slot && it.isPending }

    /** Returns false (and does nothing) for a future date: future days are read-only. */
    suspend fun markTaken(medicineId: Long, date: LocalDate, slot: LocalTime): Boolean {
        val now = time.now()
        if (date.isAfter(now.toLocalDate())) return false
        logDao.upsert(DoseLogEntity(medicineId = medicineId, date = date, time = slot, status = DoseStatus.TAKEN, actionAt = now.toInstant()))
        notifySlot(date, slot)
        return true
    }

    /** Un-toggle: the dose goes back to its derived status. It does not ring again. */
    suspend fun unmark(medicineId: Long, date: LocalDate, slot: LocalTime) {
        logDao.delete(medicineId, date, slot)
        notifySlot(date, slot)
    }

    /** "I took them" / notification Taken: marks every pending dose at the slot. Returns how many. */
    suspend fun markSlotTaken(date: LocalDate, slot: LocalTime): Int {
        val now = time.now()
        val pending = pendingDosesForSlot(date, slot)
        if (pending.isEmpty()) {
            notifySlot(date, slot)
            return 0
        }
        logDao.upsertAll(
            pending.map {
                DoseLogEntity(medicineId = it.medicineId, date = date, time = slot, status = DoseStatus.TAKEN, actionAt = now.toInstant())
            },
        )
        notifySlot(date, slot)
        return pending.size
    }

    private suspend fun notifySlot(date: LocalDate, slot: LocalTime) {
        scheduler.onSlotUpdated(date, slot, pendingDosesForSlot(date, slot).size)
    }
}
