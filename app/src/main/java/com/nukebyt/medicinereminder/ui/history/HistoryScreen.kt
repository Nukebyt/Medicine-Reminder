package com.nukebyt.medicinereminder.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.ui.util.currentLocale
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.AdherenceStats
import com.nukebyt.medicinereminder.ui.components.ColorTag
import com.nukebyt.medicinereminder.ui.components.DayRing
import com.nukebyt.medicinereminder.ui.components.EmptyState
import com.nukebyt.medicinereminder.ui.components.SectionHeader
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpenDay: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (!state.isLoaded) return@Column

        if (!state.hasAnyData) {
            EmptyState(
                title = stringResource(R.string.history_empty_title),
                message = stringResource(R.string.history_empty_message),
            )
            return@Column
        }

        MonthGrid(
            state = state,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onDayClick = { date ->
                viewModel.openDay(date)
                onOpenDay()
            },
        )

        Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatRow(stringResource(R.string.history_last7), state.last7)
                StatRow(stringResource(R.string.history_last30), state.last30)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.history_streak), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                    Text(
                        pluralStringResource(R.plurals.history_streak_days, state.streak, state.streak),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                }
            }
        }

        if (state.perMedicine.isNotEmpty()) {
            SectionHeader(stringResource(R.string.history_by_medicine))
            state.perMedicine.forEach { item ->
                Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorTag(item.colorIndex, size = 14.dp)
                            Spacer(Modifier.padding(start = 10.dp))
                            Text(item.name, style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                            Text(percentText(item.stats), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                        }
                        ProgressBar(item.stats.percent)
                        Text(
                            stringResource(R.string.history_detail, item.stats.taken, item.stats.late, item.stats.missed),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun percentText(stats: AdherenceStats): String =
    stats.percent?.let { stringResource(R.string.history_percent, it) } ?: stringResource(R.string.history_no_data)

@Composable
private fun StatRow(label: String, stats: AdherenceStats) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.ink)
            Text(percentText(stats), style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.ink)
        }
        ProgressBar(stats.percent)
    }
}

@Composable
private fun ProgressBar(percent: Int?) {
    val colors = AppTheme.colors
    Box(Modifier.fillMaxWidth().height(8.dp).background(colors.paper, AppShapes.capsule)) {
        if (percent != null && percent > 0) {
            Box(Modifier.fillMaxWidth(percent / 100f).height(8.dp).background(colors.mint, AppShapes.capsule))
        }
    }
}

@Composable
private fun MonthGrid(
    state: HistoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val colors = AppTheme.colors
    val locale = currentLocale()
    val month: YearMonth = state.month
    val monthTitle = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}"
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val rows = (lead + month.lengthOfMonth() + 6) / 7

    Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, enabled = state.canGoPrevious) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.history_prev_month))
                }
                Text(
                    monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext, enabled = state.canGoNext) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.history_next_month))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                java.time.DayOfWeek.entries.forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val dayNumber = row * 7 + col - lead + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNumber)
                                val summary = state.days[date]
                                val fullDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                                val description = if (summary != null && summary.total > 0) {
                                    stringResource(R.string.history_day_a11y, fullDate, summary.taken, summary.total)
                                } else {
                                    stringResource(R.string.history_day_none, fullDate)
                                }
                                DayRing(
                                    dayOfMonth = dayNumber,
                                    fraction = summary?.takeIf { it.countable > 0 }?.let { it.taken.toFloat() / it.countable },
                                    isToday = date == state.today,
                                    description = description,
                                    onClick = { onDayClick(date) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
