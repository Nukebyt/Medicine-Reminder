package com.nukebyt.medicinereminder.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nukebyt.medicinereminder.data.db.AppDatabase
import com.nukebyt.medicinereminder.data.db.DoseLogDao
import com.nukebyt.medicinereminder.data.db.DoseLogEntity
import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.db.MedicineEntity
import com.nukebyt.medicinereminder.data.db.toDomain
import com.nukebyt.medicinereminder.domain.model.DoseStatus
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var meds: MedicineDao
    private lateinit var logs: DoseLogDao

    private val date = LocalDate.of(2026, 11, 6)

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        meds = db.medicineDao()
        logs = db.doseLogDao()
    }

    @After fun tearDown() = db.close()

    private fun entity(name: String = "Metformin", paused: Boolean = false) = MedicineEntity(
        name = name, dosageAmount = "500", dosageUnit = "mg", form = MedForm.TABLET,
        mealRelation = MealRelation.AFTER_MEAL, colorIndex = 1, startDate = date, isPaused = paused,
        createdAt = Instant.ofEpochMilli(1_000),
    )

    private fun log(id: Long, time: LocalTime, status: DoseStatus = DoseStatus.TAKEN) =
        DoseLogEntity(medicineId = id, date = date, time = time, status = status, actionAt = Instant.ofEpochMilli(5_000))

    @Test fun insertAndReadBack_roundTripsTypesAndSortsTimes() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(21, 0), LocalTime.of(8, 30)))
        val read = meds.getById(id)!!.toDomain()
        assertEquals("Metformin", read.name)
        assertEquals(listOf(LocalTime.of(8, 30), LocalTime.of(21, 0)), read.times)
        assertEquals(date, read.startDate)
        assertEquals(Instant.ofEpochMilli(1_000), read.createdAt)
        assertEquals(MealRelation.AFTER_MEAL, read.mealRelation)
    }

    @Test fun updateReplacesTimes() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0)))
        meds.updateWithTimes(entity().copy(id = id, name = "Metformin XR"), listOf(LocalTime.of(8, 30)))
        val read = meds.getById(id)!!.toDomain()
        assertEquals("Metformin XR", read.name)
        assertEquals(listOf(LocalTime.of(8, 30)), read.times)
    }

    @Test fun duplicateTimesAreStoredOnce() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0), LocalTime.of(8, 0)))
        assertEquals(1, meds.getById(id)!!.times.size)
    }

    @Test fun deleteCascadesToTimesAndLogs() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0)))
        logs.upsert(log(id, LocalTime.of(8, 0)))
        meds.deleteById(id)
        assertNull(meds.getById(id))
        assertEquals(emptyList<DoseLogEntity>(), logs.getAll())
        assertEquals(emptyList<LocalTime>(), meds.activeSlots())
    }

    @Test fun uniqueIndex_secondLogForSameDoseReplacesTheFirst() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0)))
        logs.upsert(log(id, LocalTime.of(8, 0), DoseStatus.TAKEN))
        logs.upsert(log(id, LocalTime.of(8, 0), DoseStatus.SKIPPED))
        val all = logs.getForDate(date)
        assertEquals(1, all.size)
        assertEquals(DoseStatus.SKIPPED, all.single().status)
    }

    @Test fun deleteLog_removesOnlyThatDose() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0), LocalTime.of(21, 0)))
        logs.upsert(log(id, LocalTime.of(8, 0)))
        logs.upsert(log(id, LocalTime.of(21, 0)))
        logs.delete(id, date, LocalTime.of(8, 0))
        assertEquals(listOf(LocalTime.of(21, 0)), logs.getForDate(date).map { it.time })
    }

    @Test fun activeSlots_areDistinctSortedAndSkipPaused() = runBlocking {
        meds.insertWithTimes(entity("A"), listOf(LocalTime.of(21, 0), LocalTime.of(8, 0)))
        meds.insertWithTimes(entity("B"), listOf(LocalTime.of(8, 0)))
        meds.insertWithTimes(entity("C", paused = true), listOf(LocalTime.of(13, 0)))
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(21, 0)), meds.activeSlots())
    }

    @Test fun pauseTogglesSlots() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0)))
        meds.setPaused(id, true)
        assertEquals(emptyList<LocalTime>(), meds.activeSlots())
        meds.setPaused(id, false)
        assertEquals(listOf(LocalTime.of(8, 0)), meds.activeSlots())
    }

    @Test fun observeBetween_returnsOnlyTheRange() = runBlocking {
        val id = meds.insertWithTimes(entity(), listOf(LocalTime.of(8, 0)))
        logs.upsert(log(id, LocalTime.of(8, 0)))
        logs.upsert(log(id, LocalTime.of(8, 0)).copy(id = 0, date = date.plusDays(10)))
        assertEquals(1, logs.observeBetween(date.minusDays(1), date.plusDays(1)).first().size)
        assertNotNull(logs.observeForDate(date).first().singleOrNull())
    }

    @Test fun distinctNames_forAutocomplete() = runBlocking {
        meds.insertWithTimes(entity("zinc"), emptyList())
        meds.insertWithTimes(entity("Aspirin"), emptyList())
        meds.insertWithTimes(entity("Aspirin"), emptyList())
        assertEquals(listOf("Aspirin", "zinc"), meds.distinctNames())
    }
}
