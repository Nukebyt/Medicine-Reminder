package com.nukebyt.medicinereminder.ui.today

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.data.repo.DoseRepository
import com.nukebyt.medicinereminder.data.repo.MedicineRepository
import com.nukebyt.medicinereminder.data.time.MinuteTicker
import com.nukebyt.medicinereminder.domain.DoseResolver
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.ToggleDecision
import com.nukebyt.medicinereminder.domain.ToggleRules
import com.nukebyt.medicinereminder.domain.model.DaySummary
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.weekStartOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

private typealias DoseKey = Triple<Long, LocalDate, LocalTime>

private val DoseUi.key: DoseKey get() = Triple(medicineId, date, time)

data class TodayUiState(
    val isLoaded: Boolean = false,
    val now: ZonedDateTime,
    val selectedDate: LocalDate,
    val doses: List<DoseUi> = emptyList(),
    val summaries: Map<LocalDate, DaySummary> = emptyMap(),
    val hasAnyMedicine: Boolean = false,
    /** A past-day dose waiting for "Mark as taken for Wed 4 Nov?" confirmation. */
    val confirmPast: DoseUi? = null,
) {
    val today: LocalDate get() = now.toLocalDate()
    val isToday: Boolean get() = selectedDate == today
    val isFuture: Boolean get() = selectedDate.isAfter(today)
    val takenCount: Int get() = doses.count { it.isDone }

    /** The next dose that is still ahead of us today. */
    val nextDose: DoseUi? get() = if (isToday) doses.firstOrNull { it.state == DoseState.UPCOMING } else null
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val doses: DoseRepository,
    medicines: MedicineRepository,
    ticker: MinuteTicker,
    private val time: TimeSource,
) : ViewModel() {

    private val selectedDate = savedStateHandle.getStateFlow(KEY_SELECTED, time.now().toLocalDate().toEpochDay())
        .map(LocalDate::ofEpochDay)

    private val summaryAnchor = MutableStateFlow(weekStartOf(time.now().toLocalDate()))
    private val overrides = MutableStateFlow<Map<DoseKey, Boolean>>(emptyMap())
    private val confirmPast = MutableStateFlow<DoseUi?>(null)

    /** Optimistic UI: the toggle flips at once; the override is dropped when the database agrees. */
    private val dosesForSelected = selectedDate
        .flatMapLatest { doses.observeDosesFor(it) }
        .onEach { fresh ->
            overrides.update { current ->
                current.filter { (key, wanted) -> fresh.firstOrNull { it.key == key }?.isDone != wanted }
            }
        }
        .combine(overrides) { fresh, optimistic ->
            if (optimistic.isEmpty()) fresh else fresh.map { applyOverride(it, optimistic[it.key]) }
        }

    private val summaries = summaryAnchor.flatMapLatest { anchor ->
        // Three weeks either side so the pager has data while it is being swiped.
        doses.observeSummaries(anchor.minusWeeks(3), anchor.plusDays(6).plusWeeks(3))
    }

    val state: StateFlow<TodayUiState> = combine(
        ticker.ticks(),
        selectedDate,
        dosesForSelected,
        summaries,
        medicines.observeAll().map { it.isNotEmpty() },
    ) { now, selected, list, summary, hasMeds ->
        TodayUiState(isLoaded = true, now = now, selectedDate = selected, doses = list, summaries = summary, hasAnyMedicine = hasMeds)
    }.combine(confirmPast) { s, confirm -> s.copy(confirmPast = confirm) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            time.now().let { TodayUiState(now = it, selectedDate = it.toLocalDate()) },
        )

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED] = date.toEpochDay()
    }

    fun selectToday() = selectDate(time.now().toLocalDate())

    fun onVisibleWeekChange(weekStart: LocalDate) {
        summaryAnchor.value = weekStart
    }

    fun onToggle(dose: DoseUi, checked: Boolean) {
        when (ToggleRules.decide(dose.date, time.now().toLocalDate(), checked)) {
            ToggleDecision.BLOCKED_FUTURE -> Unit
            ToggleDecision.NEEDS_CONFIRMATION -> confirmPast.value = dose
            ToggleDecision.APPLY -> apply(dose, checked)
        }
    }

    fun confirmPastDose() {
        val dose = confirmPast.value ?: return
        confirmPast.value = null
        apply(dose, true)
    }

    fun dismissConfirm() {
        confirmPast.value = null
    }

    private fun apply(dose: DoseUi, checked: Boolean) {
        overrides.update { it + (dose.key to checked) }
        viewModelScope.launch {
            try {
                if (checked) doses.markTaken(dose.medicineId, dose.date, dose.time)
                else doses.unmark(dose.medicineId, dose.date, dose.time)
            } catch (e: Exception) {
                overrides.update { it - dose.key }
                throw e
            }
        }
    }

    private fun applyOverride(dose: DoseUi, wanted: Boolean?): DoseUi {
        if (wanted == null || wanted == dose.isDone) return dose
        val now = time.now()
        return if (wanted) {
            dose.copy(state = DoseState.TAKEN, actionAt = now.toInstant())
        } else {
            dose.copy(state = DoseResolver.stateOf(dose.date, dose.time, null, now), actionAt = null)
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_epoch_day"
    }
}
