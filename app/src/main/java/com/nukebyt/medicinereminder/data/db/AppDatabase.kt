package com.nukebyt.medicinereminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicineEntity::class, ScheduleTimeEntity::class, DoseLogEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val NAME = "medicine_reminder.db"
    }
}
