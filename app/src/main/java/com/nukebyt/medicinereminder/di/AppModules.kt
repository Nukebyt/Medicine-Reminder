package com.nukebyt.medicinereminder.di

import android.content.Context
import androidx.room.Room
import com.nukebyt.medicinereminder.data.db.AppDatabase
import com.nukebyt.medicinereminder.data.db.DoseLogDao
import com.nukebyt.medicinereminder.data.db.MedicineDao
import com.nukebyt.medicinereminder.data.time.SystemTimeSource
import com.nukebyt.medicinereminder.domain.ReminderScheduler
import com.nukebyt.medicinereminder.domain.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides fun medicineDao(db: AppDatabase): MedicineDao = db.medicineDao()
    @Provides fun doseLogDao(db: AppDatabase): DoseLogDao = db.doseLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds abstract fun timeSource(impl: SystemTimeSource): TimeSource
}

/** Temporary no-op until Phase 5 provides the AlarmManager-backed scheduler. */
@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {
    @Provides @Singleton
    fun reminderScheduler(): ReminderScheduler = object : ReminderScheduler {
        override suspend fun rescheduleAll() = Unit
        override suspend fun onSlotUpdated(date: LocalDate, slot: LocalTime, pendingCount: Int) = Unit
    }
}
