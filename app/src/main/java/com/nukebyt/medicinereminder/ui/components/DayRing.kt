package com.nukebyt.medicinereminder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme

/**
 * A day on the History grid: a ring filled by the share of doses taken. [fraction] is null when no dose outcome
 * is known yet (no doses, or only upcoming ones), which draws only the empty track.
 */
@Composable
fun DayRing(
    dayOfMonth: Int,
    fraction: Float?,
    isToday: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .then(if (isToday) Modifier.border(2.dp, colors.capsule, AppShapes.capsule) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val strokeWidth = 4.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = colors.foil,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth),
            )
            if (fraction != null && fraction > 0f) {
                drawArc(
                    color = colors.mint,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = colors.ink)
    }
}

@ThemePreviews
@Composable
private fun DayRingPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayRing(4, 1f, false, "4 November, all taken", {})
        DayRing(5, 0.5f, false, "5 November, half taken", {})
        DayRing(6, 0f, true, "6 November, none taken", {})
        DayRing(7, null, false, "7 November, nothing yet", {})
    }
}
