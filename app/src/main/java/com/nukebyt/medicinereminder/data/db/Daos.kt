package com.nukebyt.medicinereminder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<MedicineWithTimes>>

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE, id")
    suspend fun getAll(): List<MedicineWithTimes>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): MedicineWithTimes?

    @Query("SELECT DISTINCT name FROM medicines ORDER BY name COLLATE NOCASE")
    suspend fun distinctNames(): List<String>

    @Insert
    suspend fun insertMedicine(entity: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(entity: MedicineEntity)

    @Insert
    suspend fun insertTimes(times: List<ScheduleTimeEntity>)

    @Query("DELETE FROM schedule_times WHERE medicineId = :medicineId")
    suspend fun deleteTimesFor(medicineId: Long)

    @Query("UPDATE medicines SET isPaused = :paused WHERE id = :id")
    suspend fun setPaused(id: Long, paused: Boolean)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Removes every medicine; times and dose logs cascade. Used by import (replace all). */
    @Query("DELETE FROM medicines")
    suspend fun deleteAll()

    /** Distinct slots of medicines that can ring (not paused). */
    @Query(
        "SELECT DISTINCT t.time FROM schedule_times t JOIN medicines m ON m.id = t.medicineId " +
            "WHERE m.isPaused = 0 ORDER BY t.time",
    )
    suspend fun activeSlots(): List<LocalTime>

    @Transaction
    suspend fun insertWithTimes(entity: MedicineEntity, times: List<LocalTime>): Long {
        val id = insertMedicine(entity)
        insertTimes(times.distinct().map { ScheduleTimeEntity(medicineId = id, time = it) })
        return id
    }

    @Transaction
    suspend fun updateWithTimes(entity: MedicineEntity, times: List<LocalTime>) {
        updateMedicine(entity)
        deleteTimesFor(entity.id)
        insertTimes(times.distinct().map { ScheduleTimeEntity(medicineId = entity.id, time = it) })
    }
}

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DoseLogEntity)

    @Query("DELETE FROM dose_logs WHERE medicineId = :medicineId AND date = :date AND time = :time")
    suspend fun delete(medicineId: Long, date: LocalDate, time: LocalTime)

    @Query("SELECT * FROM dose_logs WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE date BETWEEN :from AND :to")
    suspend fun getBetween(from: LocalDate, to: LocalDate): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs")
    suspend fun getAll(): List<DoseLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<DoseLogEntity>)
}
