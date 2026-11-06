package com.nukebyt.medicinereminder.data.repo

import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.db.toDomain
import com.nukebyt.medicinereminder.data.db.toEntity
import com.nukebyt.medicinereminder.domain.ReminderScheduler
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.Medicine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepository @Inject constructor(
    private val dao: MedicineDao,
    private val scheduler: ReminderScheduler,
    private val time: TimeSource,
) {
    fun observeAll(): Flow<List<Medicine>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Medicine> = dao.getAll().map { it.toDomain() }

    suspend fun get(id: Long): Medicine? = dao.getById(id)?.toDomain()

    suspend fun pastNames(): List<String> = dao.distinctNames()

    /** Inserts when [Medicine.id] is 0, otherwise updates. Returns the id. Reschedules alarms. */
    suspend fun save(medicine: Medicine): Long {
        val id = if (medicine.id == 0L) {
            val entity = medicine.copy(createdAt = time.now().toInstant()).toEntity()
            dao.insertWithTimes(entity, medicine.times)
        } else {
            dao.updateWithTimes(medicine.toEntity(), medicine.times)
            medicine.id
        }
        scheduler.rescheduleAll()
        return id
    }

    suspend fun setPaused(id: Long, paused: Boolean) {
        dao.setPaused(id, paused)
        scheduler.rescheduleAll()
    }

    /** Deletes the medicine; its times and dose logs cascade. */
    suspend fun delete(id: Long) {
        dao.deleteById(id)
        scheduler.rescheduleAll()
    }

    /** Re-inserts a medicine (for undo of a delete) with its original id so logs stay linked only if kept. */
    suspend fun restore(medicine: Medicine): Long = save(medicine.copy(id = 0))

    suspend fun allDistinctSlots(): List<LocalTime> = dao.activeSlots()
}
