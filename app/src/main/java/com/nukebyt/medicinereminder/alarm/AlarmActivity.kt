package com.nukebyt.medicinereminder.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.ui.components.SwipeToStop
import com.nukebyt.medicinereminder.ui.theme.AlarmClockStyle
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.theme.MedicineReminderTheme
import com.nukebyt.medicinereminder.ui.util.labelRes
import com.nukebyt.medicinereminder.ui.util.rememberAnimationsEnabled
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Full-screen alarm over the lock screen. Back and Home do not stop the alarm (the service keeps ringing and the
 * notification stays). Only swipe, "I took them", snooze or the notification's Stop action stop it.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    @Inject lateinit var alarmState: AlarmStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            // The alarm is always dark: deep indigo, easy on eyes at 3 a.m.
            MedicineReminderTheme(darkTheme = true) {
                val state by alarmState.state.collectAsStateWithLifecycle()
                val current = state
                LaunchedEffect(current == null) { if (current == null) finish() }
                BackHandler { /* back does not stop the alarm */ }
                if (current != null) {
                    val context = LocalContext.current
                    AlarmScreen(
                        doses = current.doses,
                        snoozeMinutes = current.snoozeMinutes,
                        onTook = { AlarmService.send(context, AlarmContract.ACTION_ALARM_TOOK) },
                        onSnooze = { AlarmService.send(context, AlarmContract.ACTION_ALARM_SNOOZE) },
                        onStop = { AlarmService.send(context, AlarmContract.ACTION_ALARM_STOP) },
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

@Composable
fun AlarmScreen(
    doses: List<DoseUi>,
    snoozeMinutes: Int,
    onTook: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = AppTheme.colors
    // alert -> deep indigo, toned down so text stays readable on the red end.
    val background = Brush.verticalGradient(listOf(lerp(colors.alert, colors.paper, 0.55f), colors.paper))

    Box(Modifier.fillMaxSize().background(background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PulsingRing()
                Spacer(Modifier.height(16.dp))
                Text(text = rememberClockText(), style = AlarmClockStyle, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.alarm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                doses.forEach { dose ->
                    Text(
                        text = doseLine(dose),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            Button(
                onClick = onTook,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = colors.onMint),
            ) {
                Text(
                    stringResource(if (doses.size > 1) R.string.alarm_took_them else R.string.alarm_took_it),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = onSnooze, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(stringResource(R.string.alarm_snooze, snoozeMinutes), color = colors.ink, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            SwipeToStop(
                label = stringResource(R.string.alarm_swipe_to_stop),
                stopActionLabel = stringResource(R.string.alarm_stop_a11y),
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun PulsingRing() {
    val colors = AppTheme.colors
    val animate = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (animate) 1.12f else 0.92f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .scale(pulse)
                .alpha(0.5f)
                .border(6.dp, colors.ink, AppShapes.cell),
        )
        Box(Modifier.size(88.dp).background(colors.alert, AppShapes.cell), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = colors.onAlert, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun rememberClockText(): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    var text by remember { mutableStateOf(LocalTime.now().format(formatter)) }
    LaunchedEffect(formatter) {
        while (true) {
            text = LocalTime.now().format(formatter)
            delay(1_000)
        }
    }
    return text
}

@Composable
private fun doseLine(dose: DoseUi): String {
    val name = listOf(dose.medicineName, dose.dosageText).filter { it.isNotEmpty() }.joinToString(" ")
    if (dose.mealRelation == MealRelation.ANY_TIME) return name
    return stringResource(R.string.notif_dose_line, name, stringResource(dose.mealRelation.labelRes()).lowercase())
}
