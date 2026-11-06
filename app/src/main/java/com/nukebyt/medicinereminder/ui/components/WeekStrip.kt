package com.nukebyt.medicinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.DaySummary
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val PAST_WEEKS = 156
private const val FUTURE_WEEKS = 52
private const val MAX_DOTS = 5

fun weekStartOf(date: LocalDate, first: DayOfWeek = DayOfWeek.MONDAY): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(first))

/**
 * Week pager with weekday + date number, and a tiny row of dots per day (filled = taken).
 * Today has a capsule outline, the selected day is filled.
 */
@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    summaries: Map<LocalDate, DaySummary>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onVisibleWeekChange: (LocalDate) -> Unit = {},
    pagerState: PagerState = rememberWeekPagerState(today, selectedDate),
) {
    val base = remember(today) { weekStartOf(today) }
    fun weekStartForPage(page: Int): LocalDate = base.plusWeeks((page - PAST_WEEKS).toLong())

    // Follow external selection changes (e.g. the "Today" chip) by scrolling to that week.
    LaunchedEffect(selectedDate) {
        val target = PAST_WEEKS + ChronoUnit.WEEKS.between(base, weekStartOf(selectedDate)).toInt()
        if (target in 0 until PAST_WEEKS + FUTURE_WEEKS + 1 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { onVisibleWeekChange(weekStartForPage(it)) }
    }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxWidth()) { page ->
        val start = weekStartForPage(page)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0..6) {
                val date = start.plusDays(i.toLong())
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    summary = summaries[date],
                    onClick = { onSelect(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun rememberWeekPagerState(today: LocalDate, selected: LocalDate): PagerState {
    val base = weekStartOf(today)
    val initial = (PAST_WEEKS + ChronoUnit.WEEKS.between(base, weekStartOf(selected)).toInt()).coerceIn(0, PAST_WEEKS + FUTURE_WEEKS)
    return rememberPagerState(initialPage = initial) { PAST_WEEKS + FUTURE_WEEKS + 1 }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    summary: DaySummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val fullName = date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL))
    val total = summary?.total ?: 0
    val description = if (total > 0) {
        stringResource(R.string.week_day_a11y, fullName, summary?.taken ?: 0, total)
    } else {
        stringResource(R.string.week_day_a11y_none, fullName)
    }
    Column(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekday, style = MaterialTheme.typography.labelSmall, color = colors.muted, maxLines = 1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(40.dp)
                .then(if (isSelected) Modifier.background(colors.capsule, AppShapes.capsule) else Modifier)
                .then(if (isToday && !isSelected) Modifier.border(2.dp, colors.capsule, AppShapes.capsule) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) colors.onCapsule else colors.ink,
            )
        }
        Spacer(Modifier.height(4.dp))
        Dots(summary)
    }
}

@Composable
private fun Dots(summary: DaySummary?) {
    val colors = AppTheme.colors
    val total = (summary?.total ?: 0).coerceAtMost(MAX_DOTS)
    val taken = (summary?.taken ?: 0).coerceAtMost(total)
    Row(Modifier.height(8.dp).clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val filled = index < taken
            Box(
                Modifier
                    .size(6.dp)
                    .then(if (filled) Modifier.background(colors.mint, AppShapes.cell) else Modifier.border(1.dp, colors.muted, AppShapes.cell)),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun WeekStripPreview() = PreviewSurface {
    val today = PreviewData.date
    val start = weekStartOf(today)
    val summaries = mapOf(
        start to DaySummary(taken = 3),
        start.plusDays(1) to DaySummary(taken = 2, missed = 1),
        start.plusDays(2) to DaySummary(taken = 3),
        start.plusDays(3) to DaySummary(taken = 1, missed = 2),
        today to DaySummary(taken = 3, pending = 2),
        start.plusDays(5) to DaySummary(pending = 3),
    )
    WeekStrip(selectedDate = today, today = today, summaries = summaries, onSelect = {})
}
