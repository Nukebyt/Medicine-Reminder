package com.nukebyt.medicinereminder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.alarm.PermissionMonitor
import com.nukebyt.medicinereminder.alarm.PermissionStatus
import com.nukebyt.medicinereminder.data.prefs.AppSettings
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    private val permissions: PermissionMonitor,
) : ViewModel() {

    /** null until DataStore has been read; the splash screen stays up until then. */
    val appSettings: StateFlow<AppSettings?> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionStatus: StateFlow<PermissionStatus> = permissions.status

    fun refreshPermissions() = permissions.refresh()
}
