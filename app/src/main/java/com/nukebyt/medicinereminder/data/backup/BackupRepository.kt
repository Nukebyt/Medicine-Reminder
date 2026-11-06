package com.nukebyt.medicinereminder.data.backup

import androidx.room.withTransaction
import com.nukebyt.medicinereminder.data.db.AppDatabase
import com.nukebyt.medicinereminder.data.db.DoseLogDao
import com.nukebyt.medicinereminder.data.db.DoseLogEntity
import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.db.toDomain
import com.nukebyt.medicinereminder.data.db.toEntity
import com.nukebyt.medicinereminder.domain.ReminderScheduler
import com.nukebyt.medicinereminder.domain.TimeSource
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(val medicines: Int, val logs: Int)

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val medicineDao: MedicineDao,
    private val logDao: DoseLogDao,
    private val scheduler: ReminderScheduler,
    private val time: TimeSource,
) {
    suspend fun export(): String {
        val medicines = medicineDao.getAll().map { it.toDomain() }
        val logs = logDao.getAll().map { it.toDomain() }
        return BackupCodec.encode(medicines, logs, time.now().toInstant())
    }

    /**
     * Replaces everything on the phone with the contents of [text]. Validation happens before anything is deleted, and
     * the replacement is one transaction, so a bad file never leaves the database half-imported.
     */
    suspend fun import(text: String): ImportResult {
        val data = BackupCodec.decode(text)
        db.withTransaction {
            medicineDao.deleteAll()
            val idMap = HashMap<Long, Long>()
            for (medicine in data.medicines) {
                val newId = medicineDao.insertWithTimes(medicine.copy(id = 0).toEntity(), medicine.times)
                idMap[medicine.id] = newId
            }
            logDao.upsertAll(
                data.logs.mapNotNull { log ->
                    idMap[log.medicineId]?.let { newId ->
                        DoseLogEntity(medicineId = newId, date = log.date, time = log.time, status = log.status, actionAt = log.actionAt)
                    }
                },
            )
        }
        scheduler.rescheduleAll()
        return ImportResult(data.medicines.size, data.logs.size)
    }
}
