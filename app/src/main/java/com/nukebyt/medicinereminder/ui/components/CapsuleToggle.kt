package com.nukebyt.medicinereminder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.rememberAnimationsEnabled

private val TrackWidth = 56.dp
private val TrackHeight = 32.dp
private val ThumbSize = 24.dp
private val ThumbInset = 4.dp

/**
 * The Done toggle. Fully rounded, with a "pop" (0.9 -> 1.05 -> 1) and a haptic tick when it changes.
 * Pass [stateText] ("Taken" / "Not taken") and [label] so TalkBack announces "Metformin 8 am, taken".
 */
@Composable
fun CapsuleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    stateText: String? = null,
) {
    val colors = AppTheme.colors
    val animate = rememberAnimationsEnabled()
    val haptics = LocalHapticFeedback.current
    val duration = if (animate) 160 else 0

    val thumbX by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset else ThumbInset,
        animationSpec = tween(duration),
        label = "thumbX",
    )
    val trackColor by animateColorAsState(if (checked) colors.mint else colors.paper, tween(duration), label = "track")
    val thumbColor by animateColorAsState(if (checked) colors.onMint else colors.muted, tween(duration), label = "thumb")

    val pop = remember { Animatable(1f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstComposition) {
            firstComposition = false
        } else if (animate) {
            pop.snapTo(0.9f)
            pop.animateTo(1.05f, tween(90))
            pop.animateTo(1f, tween(90))
        }
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .scale(pop.value)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                if (label != null) contentDescription = label
                if (stateText != null) stateDescription = stateText
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(TrackWidth, TrackHeight)
                .background(trackColor, AppShapes.capsule)
                .border(2.dp, if (checked) colors.mint else colors.muted, AppShapes.capsule),
        ) {
            Box(
                Modifier
                    .offset(x = thumbX - 2.dp, y = 2.dp)
                    .size(ThumbSize)
                    .background(thumbColor, AppShapes.cell),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.mint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CapsuleTogglePreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CapsuleToggle(checked = false, onCheckedChange = {})
            CapsuleToggle(checked = true, onCheckedChange = {})
            CapsuleToggle(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}
