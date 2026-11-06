package com.nukebyt.medicinereminder.alarm

import com.nukebyt.medicinereminder.data.repo.DoseRepository
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import dagger.Lazy
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What receivers, the alarm service and the alarm screen use to read and mark doses. Real slots go through
 * [DoseRepository], which holds the shared "mark taken + cancel escalation if the slot is complete" rule.
 * Debug test slots use a made-up dose and never touch the database.
 */
@Singleton
class DoseGateway @Inject constructor(
    private val doses: DoseRepository,
    private val scheduler: Lazy<AlarmScheduler>,
) {
    private val testTaken: MutableSet<SlotKey> = ConcurrentHashMap.newKeySet()

    suspend fun pending(key: SlotKey): List<DoseUi> =
        if (key.test) {
            if (key in testTaken) emptyList() else listOf(testDose(key))
        } else {
            doses.pendingDosesForSlot(key.date, key.slot)
        }

    /** Marks every pending dose at the slot as taken. Returns how many. */
    suspend fun markTaken(key: SlotKey): Int =
        if (key.test) {
            val count = pending(key).size
            testTaken += key
            scheduler.get().onSlotUpdated(key, 0)
            count
        } else {
            doses.markSlotTaken(key.date, key.slot)
        }

    private fun testDose(key: SlotKey) = DoseUi(
        medicineId = -1,
        medicineName = "Test medicine",
        dosageText = "1 tablet",
        form = MedForm.TABLET,
        mealRelation = MealRelation.AFTER_MEAL,
        colorIndex = 0,
        date = key.date,
        time = key.slot,
        state = DoseState.OVERDUE,
        actionAt = null,
    )
}
