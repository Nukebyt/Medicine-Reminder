package com.nukebyt.medicinereminder.alarm

import com.nukebyt.medicinereminder.domain.model.DoseUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What [AlarmActivity] shows while the alarm rings. */
data class AlarmUiState(
    val slots: Set<SlotKey>,
    val doses: List<DoseUi>,
    val snoozeMinutes: Int,
    val startedAt: Instant,
)

/**
 * Shared between [AlarmService] (writer) and [AlarmActivity] (reader). `null` means no alarm is ringing, which
 * is the signal for the activity to finish itself.
 */
@Singleton
class AlarmStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<AlarmUiState?>(null)
    val state: StateFlow<AlarmUiState?> = _state.asStateFlow()

    val isRinging: Boolean get() = _state.value != null

    fun set(state: AlarmUiState?) {
        _state.value = state
    }
}
