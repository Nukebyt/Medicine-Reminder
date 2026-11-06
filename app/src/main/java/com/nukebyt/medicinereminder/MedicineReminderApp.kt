package com.nukebyt.medicinereminder

import android.app.Application
import com.nukebyt.medicinereminder.alarm.AlarmScheduler
import com.nukebyt.medicinereminder.alarm.AppLog
import com.nukebyt.medicinereminder.alarm.ReminderNotifier
import com.nukebyt.medicinereminder.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MedicineReminderApp : Application() {
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        // A force stop clears every alarm and Android offers no way around it. They come back the next time
        // the app opens (ROADMAP section 3, rule 6).
        appScope.launch {
            try {
                scheduler.rescheduleAll(restoreEscalations = false)
            } catch (e: Exception) {
                AppLog.e("rescheduleAll at startup failed", e)
            }
        }
    }
}
