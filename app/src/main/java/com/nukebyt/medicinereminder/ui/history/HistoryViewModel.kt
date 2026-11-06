package com.nukebyt.medicinereminder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nukebyt.medicinereminder.data.repo.DoseRepository
import com.nukebyt.medicinereminder.domain.Adherence
import com.nukebyt.medicinereminder.domain.AdherenceStats
import com.nukebyt.medicinereminder.domain.DoseResolver
import com.nukebyt.medicinereminder.domain.MedicineAdherence
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.DaySummary
import com.nukebyt.medicinereminder.ui.util.DateJump
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** How far back the history looks. Streaks and the month grid are limited to this window. */
private const val HISTORY_DAYS = 365

data class HistoryUiState(
    val isLoaded: Boolean = false,
    val today: LocalDate,
    val month: YearMonth,
    val days: Map<LocalDate, DaySummary> = emptyMap(),
    val last7: AdherenceStats = AdherenceStats(0, 0, 0, 0),
    val last30: AdherenceStats = AdherenceStats(0, 0, 0, 0),
    val streak: Int = 0,
    val perMedicine: List<MedicineAdherence> = emptyList(),
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
) {
    val hasAnyData: Boolean get() = days.values.any { it.total > 0 }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    doses: DoseRepository,
    private val time: TimeSource,
    private val dateJump: DateJump,
) : ViewModel() {

    private val today0 = time.now().toLocalDate()
    private val month = MutableStateFlow(YearMonth.from(today0))

    val state: StateFlow<HistoryUiState> = combine(
        doses.observeDosesBetween(today0.minusDays(HISTORY_DAYS - 1L), today0.plusDays(31)),
        month,
    ) { all, shownMonth ->
        val today = time.now().toLocalDate()
        val byDate = all.groupBy { it.date }.mapValues { DoseResolver.summarize(it.value) }
        val last30Doses = all.filter { !it.date.isBefore(today.minusDays(29)) && !it.date.isAfter(today) }
        val oldest = YearMonth.from(today.minusDays(HISTORY_DAYS - 1L))
        HistoryUiState(
            isLoaded = true,
            today = today,
            month = shownMonth,
            days = byDate,
            last7 = Adherence.statsForLast(7, today, byDate),
            last30 = Adherence.statsForLast(30, today, byDate),
            streak = Adherence.currentStreak(today, byDate.filterKeys { !it.isAfter(today) }),
            perMedicine = Adherence.perMedicine(last30Doses),
            canGoPrevious = shownMonth.isAfter(oldest),
            canGoNext = shownMonth.isBefore(YearMonth.from(today)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState(today = today0, month = month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }

    fun openDay(date: LocalDate) = dateJump.request(date)
}
