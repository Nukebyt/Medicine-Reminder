package com.nukebyt.medicinereminder.alarm

import android.content.Context
import com.nukebyt.medicinereminder.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current [PermissionStatus]. The UI calls [refresh] from onResume of the main activity because the
 * user flips these switches in system Settings. When the exact-alarm permission changes, every alarm is re-armed.
 */
@Singleton
class PermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: AlarmScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(Permissions.read(context))
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    fun refresh() {
        val old = _status.value
        val new = Permissions.read(context)
        _status.value = new
        if (old.exactAlarms != new.exactAlarms) {
            AppLog.d("exact-alarm permission changed to ${new.exactAlarms}: rescheduling")
            scope.launch { scheduler.rescheduleAll(restoreEscalations = false) }
        }
    }
}
