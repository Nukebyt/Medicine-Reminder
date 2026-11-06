package com.nukebyt.medicinereminder.data.backup

import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class BackupData(val medicines: List<Medicine>, val logs: List<DoseLog>)

@Serializable
private data class BackupFile(
    val format: String = FORMAT_NAME,
    val version: Int,
    val exportedAt: String,
    val medicines: List<BackupMedicine>,
    val doseLogs: List<BackupLog>,
)

@Serializable
private data class BackupMedicine(
    val id: Long,
    val name: String,
    val dosageAmount: String,
    val dosageUnit: String,
    val form: String,
    val mealRelation: String,
    val colorIndex: Int,
    val notes: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val daysOfWeekMask: Int,
    val isPaused: Boolean,
    val createdAt: Long,
    /** "HH:mm" */
    val times: List<String>,
)

@Serializable
private data class BackupLog(
    val medicineId: Long,
    val date: String,
    val time: String,
    val status: String,
    val actionAt: Long,
)

private const val FORMAT_NAME = "medicine-reminder-backup"

/** The versioned JSON export/import format. Pure Kotlin so it is unit-testable. */
object BackupCodec {
    const val FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(medicines: List<Medicine>, logs: List<DoseLog>, exportedAt: Instant): String {
        val file = BackupFile(
            version = FORMAT_VERSION,
            exportedAt = exportedAt.toString(),
            medicines = medicines.map { m ->
                BackupMedicine(
                    id = m.id, name = m.name, dosageAmount = m.dosageAmount, dosageUnit = m.dosageUnit,
                    form = m.form.name, mealRelation = m.mealRelation.name, colorIndex = m.colorIndex, notes = m.notes,
                    startDate = m.startDate.toString(), endDate = m.endDate?.toString(), daysOfWeekMask = m.daysOfWeekMask,
                    isPaused = m.isPaused, createdAt = m.createdAt.toEpochMilli(), times = m.times.map { it.toString() },
                )
            },
            doseLogs = logs.map { l ->
                BackupLog(l.medicineId, l.date.toString(), l.time.toString(), l.status.name, l.actionAt.toEpochMilli())
            },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Parses and validates [text]. Throws [BackupException] with a readable message on any problem. */
    fun decode(text: String): BackupData {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupException("This is not a Medicine Reminder backup file.", e)
        }
        if (file.format != FORMAT_NAME) throw BackupException("This is not a Medicine Reminder backup file.")
        if (file.version < 1 || file.version > FORMAT_VERSION) {
            throw BackupException("This backup was made by a newer version of the app (format ${file.version}).")
        }
        try {
            val medicines = file.medicines.map { m ->
                require(m.name.isNotBlank()) { "a medicine has no name" }
                Medicine(
                    id = m.id,
                    name = m.name.trim(),
                    dosageAmount = m.dosageAmount,
                    dosageUnit = m.dosageUnit,
                    form = MedForm.valueOf(m.form),
                    mealRelation = MealRelation.valueOf(m.mealRelation),
                    colorIndex = m.colorIndex.coerceIn(0, 7),
                    notes = m.notes,
                    startDate = LocalDate.parse(m.startDate),
                    endDate = m.endDate?.let(LocalDate::parse),
                    daysOfWeekMask = m.daysOfWeekMask and 0b1111111,
                    isPaused = m.isPaused,
                    createdAt = Instant.ofEpochMilli(m.createdAt),
                    times = m.times.map(LocalTime::parse).distinct().sorted(),
                )
            }
            val knownIds = medicines.map { it.id }.toSet()
            // A log that points at a medicine missing from the file cannot be restored; drop it.
            val logs = file.doseLogs.filter { it.medicineId in knownIds }.map { l ->
                DoseLog(l.medicineId, LocalDate.parse(l.date), LocalTime.parse(l.time), DoseStatus.valueOf(l.status), Instant.ofEpochMilli(l.actionAt))
            }
            return BackupData(medicines, logs)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException("The backup file is damaged: ${e.message}", e)
        }
    }
}
