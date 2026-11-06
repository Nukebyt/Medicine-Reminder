package com.nukebyt.medicinereminder.ui.medicines

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.data.repo.MedicineRepository
import com.nukebyt.medicinereminder.domain.MedicineFormError
import com.nukebyt.medicinereminder.domain.MedicineValidator
import com.nukebyt.medicinereminder.domain.NextDose
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.DaysMask
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.domain.model.Medicine
import com.nukebyt.medicinereminder.ui.nav.EditMedicineRoute
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import com.nukebyt.medicinereminder.ui.util.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

enum class EndMode { ONGOING, DATE, DAYS }

val DosageUnits = listOf("mg", "mcg", "g", "ml", "IU", "tablet", "capsule", "drops", "puffs", "units")

val QuickTimes = listOf(
    QuickTime.MORNING to LocalTime.of(8, 0),
    QuickTime.AFTERNOON to LocalTime.of(14, 0),
    QuickTime.NIGHT to LocalTime.of(21, 0),
)

enum class QuickTime { MORNING, AFTERNOON, NIGHT }

const val MaxDurationDays = 3650

data class EditMedicineUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val dosageAmount: String = "",
    val dosageUnit: String = "mg",
    val form: MedForm = MedForm.TABLET,
    val mealRelation: MealRelation = MealRelation.AFTER_MEAL,
    val times: List<LocalTime> = emptyList(),
    val daysMask: Int = DaysMask.ALL,
    val startDate: LocalDate = LocalDate.now(),
    val endMode: EndMode = EndMode.ONGOING,
    val endDate: LocalDate? = null,
    val durationDays: String = "7",
    val colorIndex: Int = 0,
    val notes: String = "",
    val nameSuggestions: List<String> = emptyList(),
    val errors: Set<MedicineFormError> = emptySet(),
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
) {
    val everyDay: Boolean get() = daysMask == DaysMask.ALL

    /** The end date the form currently describes, or null for ongoing / an unusable duration. */
    val effectiveEndDate: LocalDate?
        get() = when (endMode) {
            EndMode.ONGOING -> null
            EndMode.DATE -> endDate
            EndMode.DAYS -> durationDays.toIntOrNull()?.takeIf { it in 1..MaxDurationDays }?.let { startDate.plusDays(it - 1L) }
        }
}

@HiltViewModel
class EditMedicineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MedicineRepository,
    private val time: TimeSource,
    private val snackbar: SnackbarController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val medicineId: Long = savedStateHandle.toRoute<EditMedicineRoute>().medicineId

    private val _state = MutableStateFlow(EditMedicineUiState(startDate = time.now().toLocalDate()))
    val state: StateFlow<EditMedicineUiState> = _state.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits once after a successful save so the screen can navigate back. */
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    private var original: Medicine? = null
    private var pastNames: List<String> = emptyList()

    init {
        viewModelScope.launch {
            pastNames = repository.pastNames()
            val existing = if (medicineId != 0L) repository.get(medicineId) else null
            original = existing
            _state.update { s ->
                if (existing == null) {
                    s.copy(isLoading = false)
                } else {
                    s.copy(
                        isLoading = false,
                        isEditing = true,
                        name = existing.name,
                        dosageAmount = existing.dosageAmount,
                        dosageUnit = existing.dosageUnit,
                        form = existing.form,
                        mealRelation = existing.mealRelation,
                        times = existing.times.sorted(),
                        daysMask = existing.daysOfWeekMask,
                        startDate = existing.startDate,
                        endMode = if (existing.endDate == null) EndMode.ONGOING else EndMode.DATE,
                        endDate = existing.endDate,
                        colorIndex = existing.colorIndex,
                        notes = existing.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = change { it.copy(name = value, nameSuggestions = suggestionsFor(value)) }
    fun onSuggestionPicked(value: String) = change { it.copy(name = value, nameSuggestions = emptyList()) }
    fun onDosageAmountChange(value: String) =
        change { it.copy(dosageAmount = value.filter { c -> c.isDigit() || c == '.' || c == '/' }.take(10)) }
    fun onUnitChange(value: String) = change { it.copy(dosageUnit = value) }
    fun onFormChange(value: MedForm) = change { it.copy(form = value) }
    fun onMealChange(value: MealRelation) = change { it.copy(mealRelation = value) }
    fun onColorChange(index: Int) = change { it.copy(colorIndex = index) }
    fun onNotesChange(value: String) = change { it.copy(notes = value.take(500)) }

    fun addTime(value: LocalTime) = change { s ->
        val normalized = value.withSecond(0).withNano(0)
        if (normalized in s.times) {
            s.copy(errors = s.errors + MedicineFormError.DUPLICATE_TIME, showErrors = true)
        } else {
            s.copy(times = (s.times + normalized).sorted(), errors = s.errors - MedicineFormError.DUPLICATE_TIME - MedicineFormError.TIME_REQUIRED)
        }
    }

    fun removeTime(value: LocalTime) = change { it.copy(times = it.times - value, errors = it.errors - MedicineFormError.DUPLICATE_TIME) }

    fun onEveryDay() = change { it.copy(daysMask = DaysMask.ALL, errors = it.errors - MedicineFormError.NO_DAYS_SELECTED) }
    fun onSpecificDays() = change { if (it.everyDay) it.copy(daysMask = DaysMask.WEEKDAYS) else it }
    fun toggleDay(day: DayOfWeek) = change {
        val mask = DaysMask.toggle(it.daysMask, day)
        it.copy(daysMask = mask, errors = if (mask != 0) it.errors - MedicineFormError.NO_DAYS_SELECTED else it.errors)
    }

    fun onStartDateChange(date: LocalDate) = change { it.copy(startDate = date) }
    fun onEndModeChange(mode: EndMode) = change {
        it.copy(
            endMode = mode,
            endDate = if (mode == EndMode.DATE && it.endDate == null) it.startDate.plusDays(6) else it.endDate,
        )
    }
    fun onEndDateChange(date: LocalDate) = change { it.copy(endDate = date) }
    fun onDurationChange(value: String) = change { it.copy(durationDays = value.filter(Char::isDigit).take(4)) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        val endDate = s.effectiveEndDate
        val errors = MedicineValidator.validate(s.name, s.times, s.daysMask, s.startDate, endDate).toMutableSet()
        if (s.endMode == EndMode.DAYS && endDate == null) errors += MedicineFormError.DURATION_INVALID
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, showErrors = true) }
            return
        }
        _state.update { it.copy(isSaving = true, errors = emptySet()) }
        viewModelScope.launch {
            val base = original
            val medicine = Medicine(
                id = base?.id ?: 0L,
                name = s.name.trim(),
                dosageAmount = s.dosageAmount.trim(),
                dosageUnit = s.dosageUnit,
                form = s.form,
                mealRelation = s.mealRelation,
                colorIndex = s.colorIndex,
                notes = s.notes.trim().ifEmpty { null },
                startDate = s.startDate,
                endDate = endDate,
                daysOfWeekMask = s.daysMask,
                isPaused = base?.isPaused ?: false,
                createdAt = base?.createdAt ?: time.now().toInstant(),
                times = s.times,
            )
            repository.save(medicine)
            _saved.emit(Unit)
            snackbar.post(savedMessage(medicine, time.now()))
        }
    }

    private fun savedMessage(medicine: Medicine, now: ZonedDateTime): String {
        val next = NextDose.nextReminder(medicine, now) ?: return context.getString(R.string.saved_plain, medicine.name)
        val timeText = formatTime(next.toLocalTime())
        val `when` = when (next.toLocalDate()) {
            now.toLocalDate() -> context.getString(R.string.next_today, timeText)
            now.toLocalDate().plusDays(1) -> context.getString(R.string.next_tomorrow, timeText)
            else -> context.getString(
                R.string.next_on_date,
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(next),
                timeText,
            )
        }
        return context.getString(R.string.saved_with_next, medicine.name, `when`)
    }

    private fun suggestionsFor(query: String): List<String> {
        val q = query.trim()
        if (q.length < 1) return emptyList()
        return pastNames.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }.take(4)
    }

    private inline fun change(crossinline block: (EditMedicineUiState) -> EditMedicineUiState) {
        _state.update { current ->
            val next = block(current)
            if (!next.showErrors) {
                next
            } else {
                // Keep inline errors in sync with edits once the user has tried to save.
                val live = MedicineValidator.validate(next.name, next.times, next.daysMask, next.startDate, next.effectiveEndDate).toMutableSet()
                if (next.endMode == EndMode.DAYS && next.effectiveEndDate == null) live += MedicineFormError.DURATION_INVALID
                next.copy(errors = live)
            }
        }
    }
}
