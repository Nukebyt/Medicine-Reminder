package com.nukebyt.medicinereminder.ui.medicines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.data.repo.MedicineRepository
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.Medicine
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicineListUiState(
    val isLoaded: Boolean = false,
    val active: List<Medicine> = emptyList(),
    val paused: List<Medicine> = emptyList(),
    val finished: List<Medicine> = emptyList(),
) {
    val isEmpty: Boolean get() = isLoaded && active.isEmpty() && paused.isEmpty() && finished.isEmpty()
}

@HiltViewModel
class MedicineListViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val time: TimeSource,
    private val snackbar: SnackbarController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Rows hidden while the "Deleted. Undo" snackbar is showing. The delete is committed afterwards. */
    private val pendingDelete = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<MedicineListUiState> =
        combine(repository.observeAll(), pendingDelete) { all, hidden ->
            val today = time.now().toLocalDate()
            val visible = all.filter { it.id !in hidden }
            MedicineListUiState(
                isLoaded = true,
                finished = visible.filter { it.isFinished(today) },
                paused = visible.filter { it.isPaused && !it.isFinished(today) },
                active = visible.filter { !it.isPaused && !it.isFinished(today) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicineListUiState())

    fun setPaused(medicine: Medicine, paused: Boolean) {
        viewModelScope.launch {
            repository.setPaused(medicine.id, paused)
            snackbar.post(context.getString(if (paused) R.string.list_paused_snack else R.string.list_resumed_snack, medicine.name))
        }
    }

    fun delete(medicine: Medicine) {
        pendingDelete.update { it + medicine.id }
        viewModelScope.launch {
            val undone = snackbar.show(
                message = context.getString(R.string.list_deleted, medicine.name),
                actionLabel = context.getString(R.string.action_undo),
            )
            if (undone) {
                pendingDelete.update { it - medicine.id }
            } else {
                repository.delete(medicine.id)
                pendingDelete.update { it - medicine.id }
            }
        }
    }
}
