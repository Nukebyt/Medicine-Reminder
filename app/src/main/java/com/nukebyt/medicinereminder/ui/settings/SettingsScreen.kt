package com.nukebyt.medicinereminder.ui.settings

import androidx.core.net.toUri
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.BuildConfig
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.data.prefs.ThemeMode
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onOpenHealthCheck: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val debugAlarms by viewModel.debugAlarms.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    var dialog by remember { mutableStateOf<Dialog?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::prepareImport)
    }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setSound(picked?.toString())
        }
    }

    val customSoundTitle = remember(settings.alarmSoundUri) {
        settings.alarmSoundUri?.let { uri -> runCatching { RingtoneManager.getRingtone(context, uri.toUri()).getTitle(context) }.getOrNull() }
    }
    val soundLabel = customSoundTitle?.let { stringResource(R.string.settings_sound_custom, it) }
        ?: stringResource(R.string.settings_sound_builtin)
    val themeLabel = stringResource(
        when (settings.theme) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        },
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )

        Section(R.string.settings_section_alarm) {
            ValueRow(stringResource(R.string.settings_escalation), minutesLabel(settings.escalationDelayMin)) { dialog = Dialog.Escalation }
            ValueRow(stringResource(R.string.settings_snooze), minutesLabel(settings.snoozeMin)) { dialog = Dialog.Snooze }
            ValueRow(stringResource(R.string.settings_auto_stop), minutesLabel(settings.alarmAutoStopMin)) { dialog = Dialog.AutoStop }
            ValueRow(stringResource(R.string.settings_sound), soundLabel) { dialog = Dialog.Sound }
            SwitchRow(stringResource(R.string.settings_ramp), settings.graduallyIncreaseVolume, viewModel::setRamp)
            SwitchRow(stringResource(R.string.settings_vibrate), settings.vibrate, viewModel::setVibrate)
        }

        Section(R.string.settings_section_appearance) {
            ValueRow(stringResource(R.string.settings_theme), themeLabel) { dialog = Dialog.Theme }
        }

        Section(R.string.settings_section_reliability) {
            ValueRow(stringResource(R.string.settings_health_check), stringResource(R.string.settings_health_check_summary), onClick = onOpenHealthCheck)
        }

        Section(R.string.settings_section_data) {
            ValueRow(stringResource(R.string.settings_export), stringResource(R.string.settings_export_summary)) {
                exportLauncher.launch("medicine-reminder-backup-${LocalDate.now()}.json")
            }
            ValueRow(stringResource(R.string.settings_import), stringResource(R.string.settings_import_summary)) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
            Text(
                stringResource(R.string.settings_data_stays),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Section(R.string.settings_section_about) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                Text(stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                Text(stringResource(R.string.about_fonts), style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
        }

        if (BuildConfig.DEBUG) {
            Section(R.string.settings_section_debug) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::fireTestReminder, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.debug_test_reminder)) }
                    OutlinedButton(onClick = viewModel::fireTestAlarm, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.debug_test_alarm)) }
                    OutlinedButton(onClick = viewModel::showScheduledAlarms, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.debug_show_alarms)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---- dialogs ----
    when (dialog) {
        Dialog.Escalation -> ChoiceDialog(
            title = stringResource(R.string.settings_escalation),
            options = listOf(1, 3, 5, 10, 15, 0), selected = settings.escalationDelayMin, label = { minutesLabel(it) },
            onSelect = { viewModel.setEscalation(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dialog.Snooze -> ChoiceDialog(
            title = stringResource(R.string.settings_snooze),
            options = listOf(5, 10, 15, 30), selected = settings.snoozeMin, label = { minutesLabel(it) },
            onSelect = { viewModel.setSnooze(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dialog.AutoStop -> ChoiceDialog(
            title = stringResource(R.string.settings_auto_stop),
            options = listOf(5, 10, 20), selected = settings.alarmAutoStopMin, label = { minutesLabel(it) },
            onSelect = { viewModel.setAutoStop(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dialog.Theme -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries, selected = settings.theme,
            label = {
                stringResource(
                    when (it) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    },
                )
            },
            onSelect = { viewModel.setTheme(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dialog.Sound -> ChoiceDialog(
            title = stringResource(R.string.settings_sound),
            options = listOf(false, true), selected = settings.alarmSoundUri != null,
            label = { stringResource(if (it) R.string.settings_sound_system else R.string.settings_sound_builtin) },
            onSelect = { system ->
                dialog = null
                if (!system) {
                    viewModel.setSound(null)
                } else {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        settings.alarmSoundUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it.toUri()) }
                    }
                    ringtoneLauncher.launch(intent)
                }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_message, pending.medicines, pending.logs)) },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.import_confirm_action)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    debugAlarms?.let { text ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDebugAlarms,
            title = { Text(stringResource(R.string.debug_alarms_title)) },
            text = { Text(text, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = viewModel::dismissDebugAlarms) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@Composable
private fun minutesLabel(value: Int): String =
    if (value == 0) stringResource(R.string.settings_off) else stringResource(R.string.settings_minutes, value)

private enum class Dialog { Escalation, Snooze, AutoStop, Theme, Sound }

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.muted,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
        )
        Surface(shape = AppShapes.card, color = AppTheme.colors.foil, modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.ink)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
