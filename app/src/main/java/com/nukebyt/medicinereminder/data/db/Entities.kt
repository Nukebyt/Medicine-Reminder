package com.nukebyt.medicinereminder.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosageAmount: String,
    val dosageUnit: String,
    val form: MedForm,
    val mealRelation: MealRelation,
    val colorIndex: Int,
    val notes: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val daysOfWeekMask: Int = 0b1111111,
    val isPaused: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "schedule_times",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicineId")],
)
data class ScheduleTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val time: LocalTime,
)

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medicineId", "date", "time"], unique = true), Index("date")],
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    val actionAt: Instant,
)

data class MedicineWithTimes(
    @Embedded val medicine: MedicineEntity,
    @Relation(parentColumn = "id", entityColumn = "medicineId")
    val times: List<ScheduleTimeEntity>,
)

fun MedicineWithTimes.toDomain(): Medicine = Medicine(
    id = medicine.id,
    name = medicine.name,
    dosageAmount = medicine.dosageAmount,
    dosageUnit = medicine.dosageUnit,
    form = medicine.form,
    mealRelation = medicine.mealRelation,
    colorIndex = medicine.colorIndex,
    notes = medicine.notes,
    startDate = medicine.startDate,
    endDate = medicine.endDate,
    daysOfWeekMask = medicine.daysOfWeekMask,
    isPaused = medicine.isPaused,
    createdAt = medicine.createdAt,
    times = times.map { it.time }.distinct().sorted(),
)

fun Medicine.toEntity(): MedicineEntity = MedicineEntity(
    id = id,
    name = name.trim(),
    dosageAmount = dosageAmount.trim(),
    dosageUnit = dosageUnit.trim(),
    form = form,
    mealRelation = mealRelation,
    colorIndex = colorIndex,
    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
    startDate = startDate,
    endDate = endDate,
    daysOfWeekMask = daysOfWeekMask,
    isPaused = isPaused,
    createdAt = createdAt,
)

fun DoseLogEntity.toDomain(): DoseLog = DoseLog(medicineId, date, time, status, actionAt)
