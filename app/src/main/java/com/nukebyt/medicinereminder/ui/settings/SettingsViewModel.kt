package com.nukebyt.medicinereminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.alarm.AlarmScheduler
import com.nukebyt.medicinereminder.alarm.AlarmService
import com.nukebyt.medicinereminder.alarm.AppLog
import com.nukebyt.medicinereminder.alarm.SlotKey
import com.nukebyt.medicinereminder.data.backup.BackupCodec
import com.nukebyt.medicinereminder.data.backup.BackupException
import com.nukebyt.medicinereminder.data.backup.BackupRepository
import com.nukebyt.medicinereminder.data.prefs.AppSettings
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import com.nukebyt.medicinereminder.data.prefs.ThemeMode
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A backup that has been read and validated, waiting for the user to confirm replacing their data. */
data class PendingImport(val text: String, val medicines: Int, val logs: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val backup: BackupRepository,
    private val scheduler: AlarmScheduler,
    private val time: TimeSource,
    private val snackbar: SnackbarController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _debugAlarms = MutableStateFlow<String?>(null)
    val debugAlarms: StateFlow<String?> = _debugAlarms.asStateFlow()

    fun setEscalation(min: Int) = launch { repository.setEscalationDelay(min) }
    fun setSnooze(min: Int) = launch { repository.setSnooze(min) }
    fun setAutoStop(min: Int) = launch { repository.setAlarmAutoStop(min) }
    fun setSound(uri: String?) = launch { repository.setAlarmSound(uri) }
    fun setRamp(on: Boolean) = launch { repository.setGradualVolume(on) }
    fun setVibrate(on: Boolean) = launch { repository.setVibrate(on) }
    fun setTheme(mode: ThemeMode) = launch { repository.setTheme(mode) }

    // ---- export / import ----

    fun exportTo(uri: Uri) = launch {
        try {
            val text = backup.export()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    ?: error("could not open the file")
            }
            snackbar.post(context.getString(R.string.export_done))
        } catch (e: Exception) {
            AppLog.e("export failed", e)
            snackbar.post(context.getString(R.string.export_failed))
        }
    }

    /** Read and validate first; nothing is replaced until the user confirms. */
    fun prepareImport(uri: Uri) = launch {
        try {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: error("could not open the file")
            }
            val data = BackupCodec.decode(text)
            _pendingImport.value = PendingImport(text, data.medicines.size, data.logs.size)
        } catch (e: BackupException) {
            snackbar.post(context.getString(R.string.import_failed, e.message.orEmpty()))
        } catch (e: Exception) {
            AppLog.e("import read failed", e)
            snackbar.post(context.getString(R.string.import_failed, e.message.orEmpty()))
        }
    }

    fun confirmImport() = launch {
        val pending = _pendingImport.value ?: return@launch
        _pendingImport.value = null
        try {
            val result = backup.import(pending.text)
            snackbar.post(context.getString(R.string.import_done, result.medicines))
        } catch (e: Exception) {
            AppLog.e("import failed", e)
            snackbar.post(context.getString(R.string.import_failed, e.message.orEmpty()))
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    // ---- debug tools (debug builds only) ----

    fun fireTestReminder() {
        scheduler.scheduleTestReminder(30)
        snackbar.post(context.getString(R.string.debug_test_scheduled))
    }

    fun fireTestAlarm() {
        val now = time.now()
        AlarmService.start(context, SlotKey(now.toLocalDate(), now.toLocalTime().withSecond(0).withNano(0), test = true))
    }

    fun showScheduledAlarms() = launch { _debugAlarms.value = scheduler.describe() }

    fun dismissDebugAlarms() {
        _debugAlarms.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
