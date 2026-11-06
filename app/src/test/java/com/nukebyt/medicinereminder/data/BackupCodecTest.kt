package com.nukebyt.medicinereminder.data

import com.nukebyt.medicinereminder.data.backup.BackupCodec
import com.nukebyt.medicinereminder.data.backup.BackupException
import com.nukebyt.medicinereminder.domain.model.DaysMask
import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {
    private val medicine = Medicine(
        id = 7, name = "Metformin", dosageAmount = "500", dosageUnit = "mg", form = MedForm.TABLET,
        mealRelation = MealRelation.AFTER_MEAL, colorIndex = 3, notes = "With water",
        startDate = LocalDate.of(2026, 11, 1), endDate = LocalDate.of(2026, 12, 1),
        daysOfWeekMask = DaysMask.WEEKDAYS, isPaused = true, createdAt = Instant.ofEpochMilli(1_700_000_000_000),
        times = listOf(LocalTime.of(8, 0), LocalTime.of(21, 30)),
    )
    private val log = DoseLog(7, LocalDate.of(2026, 11, 5), LocalTime.of(8, 0), DoseStatus.TAKEN, Instant.ofEpochMilli(1_700_000_100_000))
    private val exportedAt = Instant.parse("2026-11-06T10:00:00Z")

    private fun failsWith(text: String): BackupException {
        try {
            BackupCodec.decode(text)
        } catch (e: BackupException) {
            return e
        }
        fail("expected a BackupException")
        throw IllegalStateException()
    }

    @Test fun roundTripPreservesEverything() {
        val text = BackupCodec.encode(listOf(medicine), listOf(log), exportedAt)
        val data = BackupCodec.decode(text)
        assertEquals(listOf(medicine), data.medicines)
        assertEquals(listOf(log), data.logs)
    }

    @Test fun nullableFieldsRoundTrip() {
        val plain = medicine.copy(notes = null, endDate = null)
        val data = BackupCodec.decode(BackupCodec.encode(listOf(plain), emptyList(), exportedAt))
        assertEquals(plain, data.medicines.single())
    }

    @Test fun emptyBackupIsValid() {
        val data = BackupCodec.decode(BackupCodec.encode(emptyList(), emptyList(), exportedAt))
        assertTrue(data.medicines.isEmpty() && data.logs.isEmpty())
    }

    @Test fun unknownKeysAreIgnored_soNewerCompatibleFilesStillLoad() {
        val text = BackupCodec.encode(listOf(medicine), emptyList(), exportedAt).replaceFirst("{", "{\n  \"futureField\": 123,")
        assertEquals(1, BackupCodec.decode(text).medicines.size)
    }

    @Test fun newerVersionIsRejectedWithAClearMessage() {
        val text = BackupCodec.encode(listOf(medicine), emptyList(), exportedAt).replace("\"version\": 1", "\"version\": 99")
        assertTrue(failsWith(text).message!!.contains("newer version"))
    }

    @Test fun garbageIsRejected() {
        failsWith("this is not json")
        failsWith("{}")
        failsWith("")
    }

    @Test fun wrongFormatNameIsRejected() {
        val text = BackupCodec.encode(listOf(medicine), emptyList(), exportedAt).replace("medicine-reminder-backup", "something-else")
        failsWith(text)
    }

    @Test fun unknownEnumValueIsRejected() {
        val text = BackupCodec.encode(listOf(medicine), emptyList(), exportedAt).replace("AFTER_MEAL", "WHENEVER")
        failsWith(text)
    }

    @Test fun blankNameIsRejected() {
        val text = BackupCodec.encode(listOf(medicine), emptyList(), exportedAt).replace("\"name\": \"Metformin\"", "\"name\": \"  \"")
        failsWith(text)
    }

    @Test fun logsForUnknownMedicinesAreDropped() {
        val orphan = log.copy(medicineId = 999)
        val data = BackupCodec.decode(BackupCodec.encode(listOf(medicine), listOf(log, orphan), exportedAt))
        assertEquals(listOf(log), data.logs)
    }

    @Test fun outOfRangeColourAndMaskAreCorrected() {
        val text = BackupCodec.encode(listOf(medicine.copy(colorIndex = 99)), emptyList(), exportedAt)
        assertEquals(7, BackupCodec.decode(text).medicines.single().colorIndex)
    }
}
