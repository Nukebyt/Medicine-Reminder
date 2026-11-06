package com.nukebyt.medicinereminder.domain

import com.nukebyt.medicinereminder.domain.model.DoseLog
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PerMedicineAdherenceTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 6)
    private val now = today.atTime(22, 0).atZone(zone)

    private fun med(id: Long, name: String) = Medicine(
        id = id, name = name, dosageAmount = "1", dosageUnit = "tablet", form = MedForm.TABLET,
        mealRelation = MealRelation.ANY_TIME, colorIndex = id.toInt(), startDate = today.minusDays(10),
        times = listOf(LocalTime.of(8, 0)),
    )

    @Test fun resolveRange_coversEveryDateInOrder() {
        val dates = (0..2).map { today.minusDays(2).plusDays(it.toLong()) }
        val doses = DoseResolver.resolveRange(dates, now, listOf(med(1, "A")), emptyList())
        assertEquals(dates, doses.map { it.date })
    }

    @Test fun perMedicine_sortsWorstFirstAndSkipsUnmeasured() {
        val a = med(1, "Aspirin")
        val b = med(2, "Zinc")
        val dates = (1..4).map { today.minusDays(it.toLong()) } // four past days
        // Aspirin taken on all four days, Zinc taken on one.
        val logs = dates.map { DoseLog(1, it, LocalTime.of(8, 0), DoseStatus.TAKEN, it.atTime(8, 5).atZone(zone).toInstant()) } +
            DoseLog(2, dates[0], LocalTime.of(8, 0), DoseStatus.TAKEN, dates[0].atTime(8, 5).atZone(zone).toInstant())
        val doses = DoseResolver.resolveRange(dates, now, listOf(a, b), logs)
        val result = Adherence.perMedicine(doses)
        assertEquals(listOf("Zinc", "Aspirin"), result.map { it.name })
        assertEquals(25, result[0].stats.percent)
        assertEquals(100, result[1].stats.percent)
    }

    @Test fun perMedicine_ignoresMedicinesWithOnlyPendingDoses() {
        val doses = DoseResolver.resolveRange(listOf(today), today.atTime(7, 0).atZone(zone), listOf(med(1, "A")), emptyList())
        assertEquals(emptyList<MedicineAdherence>(), Adherence.perMedicine(doses))
    }
}
