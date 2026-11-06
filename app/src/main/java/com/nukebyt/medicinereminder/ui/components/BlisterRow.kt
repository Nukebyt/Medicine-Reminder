package com.nukebyt.medicinereminder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.rememberAnimationsEnabled

/**
 * The app's one memorable motif: a strip of foil blister cells. A cell "pops" and fills when its dose is taken.
 * One cell per dose, in time order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlisterRow(states: List<DoseState>, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        states.forEach { BlisterCell(it) }
    }
}

@Composable
private fun BlisterCell(state: DoseState) {
    val colors = AppTheme.colors
    val animate = rememberAnimationsEnabled()
    val taken = state.isTaken
    val fill by animateColorAsState(if (taken) colors.mint else colors.paper, label = "fill")
    val scale by animateFloatAsState(
        targetValue = if (taken) 1f else 0.92f,
        animationSpec = if (animate) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium) else spring(stiffness = Spring.StiffnessHigh),
        label = "pop",
    )
    val border = when (state) {
        DoseState.OVERDUE, DoseState.MISSED -> colors.alert
        DoseState.TAKEN, DoseState.TAKEN_LATE -> colors.mint
        else -> colors.muted
    }
    Box(
        Modifier
            .size(28.dp)
            .scale(scale)
            .background(fill, AppShapes.cell)
            .border(2.dp, border, AppShapes.cell),
        contentAlignment = Alignment.Center,
    ) {
        if (taken) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onMint, modifier = Modifier.size(16.dp))
    }
}

/** "3 of 5 taken today" card with the blister strip and the next dose. */
@Composable
fun BlisterSummaryCard(
    taken: Int,
    total: Int,
    states: List<DoseState>,
    nextText: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Surface(modifier.fillMaxWidth(), shape = AppShapes.card, color = colors.foil) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (total > 0 && taken == total) stringResource(R.string.summary_all_done)
                else stringResource(R.string.summary_taken_today, taken, total),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            BlisterRow(states)
            if (nextText != null) {
                Text(nextText, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun BlisterSummaryCardPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BlisterSummaryCard(
            taken = 3, total = 5,
            states = listOf(DoseState.TAKEN, DoseState.TAKEN, DoseState.TAKEN_LATE, DoseState.OVERDUE, DoseState.UPCOMING),
            nextText = "Next: Vitamin D · 9:00 pm",
        )
        BlisterSummaryCard(taken = 2, total = 2, states = listOf(DoseState.TAKEN, DoseState.TAKEN), nextText = null)
    }
}
