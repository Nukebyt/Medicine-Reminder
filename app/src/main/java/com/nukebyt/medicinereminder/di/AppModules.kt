package com.nukebyt.medicinereminder.di

import android.content.Context
import androidx.room.Room
import com.nukebyt.medicinereminder.alarm.AlarmScheduler
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides fun medicineDao(db: AppDatabase): MedicineDao = db.medicineDao()
    @Provides fun doseLogDao(db: AppDatabase): DoseLogDao = db.doseLogDao()

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds abstract fun timeSource(impl: SystemTimeSource): TimeSource
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {
    @Binds abstract fun reminderScheduler(impl: AlarmScheduler): ReminderScheduler
}
