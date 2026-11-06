package com.nukebyt.medicinereminder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val STOP_THRESHOLD = 0.85f

/**
 * Capsule track with a draggable thumb. Releasing past 85% fires [onStop], otherwise the thumb springs back.
 * TalkBack users get a "Stop alarm" custom action so they are not forced to swipe.
 */
@Composable
fun SwipeToStop(
    label: String,
    stopActionLabel: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thumb = 56.dp
    val inset = 8.dp
    val thumbPx = with(density) { thumb.toPx() }
    val insetPx = with(density) { inset.toPx() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val maxOffset = (trackWidthPx - thumbPx - insetPx * 2).coerceAtLeast(1f)
    val offset = remember { Animatable(0f) }
    var done by remember { mutableFloatStateOf(0f) }

    fun finish() {
        if (done == 0f) {
            done = 1f
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onStop()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(AppShapes.capsule)
            .background(colors.foil.copy(alpha = 0.35f))
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .semantics {
                contentDescription = label
                customActions = listOf(CustomAccessibilityAction(stopActionLabel) { finish(); true })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = thumb)
                .alpha(1f - (offset.value / maxOffset).coerceIn(0f, 1f)),
        )
        Box(
            Modifier
                .offset { IntOffset((insetPx + offset.value).roundToInt(), 0) }
                .size(thumb)
                .background(colors.paper, AppShapes.cell)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxOffset)) }
                    },
                    onDragStopped = {
                        if (offset.value >= STOP_THRESHOLD * maxOffset) {
                            offset.animateTo(maxOffset)
                            finish()
                        } else {
                            offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.ink)
        }
    }
}

@ThemePreviews
@Composable
private fun SwipeToStopPreview() = PreviewSurface {
    SwipeToStop(label = "Swipe to stop", stopActionLabel = "Stop alarm", onStop = {})
}
