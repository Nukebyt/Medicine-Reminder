package com.nukebyt.medicinereminder.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.alarm.PermissionMonitor
import com.nukebyt.medicinereminder.alarm.PermissionStatus
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissions: PermissionMonitor,
    private val settings: SettingsRepository,
) : ViewModel() {
    val status: StateFlow<PermissionStatus> = permissions.status

    /** Called from onResume: the user may just have flipped a switch in system Settings. */
    fun refresh() = permissions.refresh()

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingDone(true)
            onDone()
        }
    }
}
