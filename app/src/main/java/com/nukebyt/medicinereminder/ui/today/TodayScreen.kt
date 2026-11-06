package com.nukebyt.medicinereminder.ui.today

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.ui.util.currentLocale
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.TimeOfDay
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.weekStartOf
import com.nukebyt.medicinereminder.ui.components.BlisterSummaryCard
import com.nukebyt.medicinereminder.ui.components.DoseCard
import com.nukebyt.medicinereminder.ui.components.EmptyState
import com.nukebyt.medicinereminder.ui.components.SectionHeader
import com.nukebyt.medicinereminder.ui.components.WeekStrip
import com.nukebyt.medicinereminder.ui.components.rememberWeekPagerState
import com.nukebyt.medicinereminder.ui.components.weekPageOf
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.formatTime
import com.nukebyt.medicinereminder.ui.util.rememberAnimationsEnabled
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    onAddMedicine: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
    banner: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayContent(
        state = state,
        onAddMedicine = onAddMedicine,
        onSelectDate = viewModel::selectDate,
        onSelectToday = viewModel::selectToday,
        onVisibleWeekChange = viewModel::onVisibleWeekChange,
        onToggle = viewModel::onToggle,
        onConfirmPast = viewModel::confirmPastDose,
        onDismissConfirm = viewModel::dismissConfirm,
        banner = banner,
    )
}

@Composable
fun TodayContent(
    state: TodayUiState,
    onAddMedicine: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectToday: () -> Unit,
    onVisibleWeekChange: (LocalDate) -> Unit,
    onToggle: (DoseUi, Boolean) -> Unit,
    onConfirmPast: () -> Unit,
    onDismissConfirm: () -> Unit,
    banner: @Composable () -> Unit = {},
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val pagerState = rememberWeekPagerState(state.today, state.selectedDate)
    val animate = rememberAnimationsEnabled()
    val onTodayWeek = weekStartOf(state.today) == weekStartOf(state.selectedDate) &&
        pagerState.currentPage == weekPageOf(state.today, state.today)
    val showTodayChip = !state.isToday || !onTodayWeek

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (state.hasAnyMedicine) {
                FloatingActionButton(onClick = onAddMedicine, containerColor = colors.capsule, contentColor = colors.onCapsule) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_medicine))
                }
            }
        },
    ) { padding ->
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(state)
            banner()

            if (!state.hasAnyMedicine) {
                EmptyState(
                    title = stringResource(R.string.empty_medicines_title),
                    message = stringResource(R.string.empty_medicines_message),
                    actionLabel = stringResource(R.string.add_medicine),
                    onAction = onAddMedicine,
                )
                return@Column
            }

            if (state.doses.isNotEmpty()) {
                val title = when {
                    state.isToday && state.takenCount == state.doses.size -> stringResource(R.string.summary_all_done)
                    state.isToday -> stringResource(R.string.summary_taken_today, state.takenCount, state.doses.size)
                    else -> stringResource(R.string.summary_taken, state.takenCount, state.doses.size)
                }
                BlisterSummaryCard(
                    title = title,
                    states = state.doses.map { it.state },
                    nextText = state.nextDose?.let { stringResource(R.string.summary_next, it.medicineName, formatTime(it.time)) },
                )
            }

            Box {
                WeekStrip(
                    selectedDate = state.selectedDate,
                    today = state.today,
                    summaries = state.summaries,
                    onSelect = onSelectDate,
                    onVisibleWeekChange = onVisibleWeekChange,
                    pagerState = pagerState,
                )
            }
            if (showTodayChip) {
                AssistChip(
                    onClick = {
                        onSelectToday()
                        scope.launch { pagerState.animateScrollToPage(weekPageOf(state.today, state.today)) }
                    },
                    label = { Text(stringResource(R.string.week_today_chip)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Crossfade(
                targetState = state.selectedDate,
                animationSpec = tween(if (animate) 220 else 0),
                label = "day",
            ) { date ->
                // Only the current day has data; the outgoing day fades out as an empty block.
                if (date == state.selectedDate) DoseList(state, onToggle) else Spacer(Modifier.fillMaxWidth())
            }
        }
    }

    state.confirmPast?.let { dose ->
        val dateText = dose.date.format(DateTimeFormatter.ofPattern("EEE d MMM", currentLocale()))
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(R.string.confirm_past_title, dateText)) },
            text = { Text(stringResource(R.string.confirm_past_message, dose.medicineName, formatTime(dose.time))) },
            confirmButton = { TextButton(onClick = onConfirmPast) { Text(stringResource(R.string.confirm_past_action)) } },
            dismissButton = { TextButton(onClick = onDismissConfirm) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Header(state: TodayUiState) {
    val greeting = when (state.now.hour) {
        in 0..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = state.today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", currentLocale())),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.muted,
        )
    }
}

@Composable
private fun DoseList(state: TodayUiState, onToggle: (DoseUi, Boolean) -> Unit) {
    if (state.doses.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.empty_day_title),
            message = stringResource(R.string.empty_day_message),
        )
        return
    }
    val groups = state.doses
        .groupBy { TimeOfDay.of(it.time) }
        .toSortedMap()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.isFuture) {
            Text(
                stringResource(R.string.future_day_note),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted,
            )
        }
        groups.forEach { (part, doses) ->
            SectionHeader(
                stringResource(
                    when (part) {
                        TimeOfDay.MORNING -> R.string.section_morning
                        TimeOfDay.AFTERNOON -> R.string.section_afternoon
                        TimeOfDay.EVENING -> R.string.section_evening
                        TimeOfDay.NIGHT -> R.string.section_night
                    },
                ),
            )
            val ordered = if (part == TimeOfDay.NIGHT) doses.sortedBy { if (it.time.hour < 5) it.time.hour + 24 else it.time.hour } else doses
            ordered.forEach { dose ->
                DoseCard(
                    dose = dose,
                    now = state.now,
                    onToggle = { onToggle(dose, it) },
                    toggleEnabled = !state.isFuture,
                )
            }
        }
    }
}
