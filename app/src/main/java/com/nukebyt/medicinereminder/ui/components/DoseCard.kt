package com.nukebyt.medicinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.DoseResolver
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.formatTime
import com.nukebyt.medicinereminder.ui.util.labelRes
import java.time.Duration
import java.time.ZonedDateTime

/** Status text for a dose: "Taken at 8:04 am", "Overdue by 12 min", ... */
@Composable
fun doseStatusText(dose: DoseUi, now: ZonedDateTime): String = when (dose.state) {
    DoseState.UPCOMING -> stringResource(R.string.dose_upcoming)
    DoseState.OVERDUE -> {
        val minutes = Duration.between(DoseResolver.scheduledAt(dose, now), now.toInstant()).toMinutes().coerceAtLeast(0)
        if (minutes < 60) stringResource(R.string.dose_overdue_min, minutes.toInt())
        else stringResource(R.string.dose_overdue_hours, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
    DoseState.TAKEN -> stringResource(R.string.dose_taken_at, dose.actionAt?.atZone(now.zone)?.toLocalTime()?.let(::formatTime).orEmpty())
    DoseState.TAKEN_LATE -> stringResource(R.string.dose_taken_late_at, dose.actionAt?.atZone(now.zone)?.toLocalTime()?.let(::formatTime).orEmpty())
    DoseState.SKIPPED -> stringResource(R.string.dose_skipped)
    DoseState.MISSED -> stringResource(R.string.dose_missed)
}

/** Status never relies on colour alone: every state has its own icon and text. */
private fun DoseState.icon(): ImageVector = when (this) {
    DoseState.UPCOMING -> Icons.Rounded.Schedule
    DoseState.OVERDUE -> Icons.Rounded.ErrorOutline
    DoseState.TAKEN, DoseState.TAKEN_LATE -> Icons.Rounded.CheckCircle
    DoseState.SKIPPED -> Icons.Rounded.SkipNext
    DoseState.MISSED -> Icons.Rounded.Cancel
}

/**
 * One dose. Left edge is the medicine colour tag, or `alert` when overdue.
 * No business logic: it takes state and a lambda.
 */
@Composable
fun DoseCard(
    dose: DoseUi,
    now: ZonedDateTime,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    toggleEnabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val overdue = dose.state == DoseState.OVERDUE
    val statusColor: Color = when (dose.state) {
        DoseState.OVERDUE, DoseState.MISSED -> colors.alert
        DoseState.TAKEN, DoseState.TAKEN_LATE -> colors.ink
        else -> colors.muted
    }
    val timeText = formatTime(dose.time)
    val statusText = doseStatusText(dose, now)
    val takenText = stringResource(R.string.dose_a11y_state_taken)
    val notTakenText = stringResource(R.string.dose_a11y_state_not_taken)

    Surface(modifier = modifier.fillMaxWidth(), shape = AppShapes.card, color = colors.foil) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (overdue) colors.alert else colors.medicineColor(dose.colorIndex)),
            )
            Column(Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 8.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = dose.medicineName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (dose.dosageText.isNotEmpty()) {
                            Text(dose.dosageText, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                        }
                    }
                    Text(timeText, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealTag(dose.mealRelation)
                    Text(
                        text = stringResource(dose.form.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(dose.state.icon(), contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        modifier = Modifier.weight(1f),
                    )
                    CapsuleToggle(
                        checked = dose.isDone,
                        onCheckedChange = onToggle,
                        enabled = toggleEnabled,
                        label = stringResource(R.string.dose_a11y_summary, dose.medicineName, dose.dosageText, timeText, statusText),
                        stateText = if (dose.isDone) takenText else notTakenText,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DoseCardPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DoseCard(PreviewData.dose(state = DoseState.TAKEN), PreviewData.now, onToggle = {})
        DoseCard(PreviewData.dose(state = DoseState.OVERDUE, color = 3), PreviewData.now, onToggle = {})
        DoseCard(
            PreviewData.dose(name = "Vitamin D", dosage = "1000 IU", state = DoseState.UPCOMING, meal = com.nukebyt.medicinereminder.domain.model.MealRelation.BEFORE_MEAL, color = 2),
            PreviewData.now,
            onToggle = {},
        )
        DoseCard(PreviewData.dose(state = DoseState.MISSED, color = 5), PreviewData.now, onToggle = {}, toggleEnabled = false)
    }
}
