package com.nukebyt.medicinereminder.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.alarm.Permissions
import com.nukebyt.medicinereminder.domain.Oem
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isFirstRun: Boolean,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    // The user flips these in system Settings: re-check as soon as they come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refresh()
        val canAskAgain = activity != null && Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        // Denied for good: the system dialog will not show again, so send them to the settings page.
        if (!granted && !canAskAgain) Permissions.launch(context, Permissions.notificationSettingsIntent(context))
    }

    fun requestNotifications() {
        val needsRuntimePrompt = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsRuntimePrompt) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Permissions.launch(context, Permissions.notificationSettingsIntent(context))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (isFirstRun) R.string.onboarding_title else R.string.health_check_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    if (!isFirstRun) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge, color = colors.muted)

            StepCard(R.string.step_notifications_title, R.string.step_notifications_why, status.notifications, R.string.step_allow, ::requestNotifications)
            StepCard(
                R.string.step_alarms_title, R.string.step_alarms_why, status.exactAlarms, R.string.step_open_settings,
                { Permissions.launch(context, Permissions.exactAlarmIntent(context)) },
            )
            StepCard(
                R.string.step_fullscreen_title, R.string.step_fullscreen_why, status.fullScreen, R.string.step_open_settings,
                { Permissions.launch(context, Permissions.fullScreenIntent(context)) },
            )
            StepCard(
                R.string.step_battery_title, R.string.step_battery_why, status.batteryUnrestricted, R.string.step_open_settings,
                { Permissions.launch(context, Permissions.batteryIntent(context)) },
            )

            OemCard(Oem.detect(Build.MANUFACTURER, Build.BRAND))
            DisclaimerCard()

            if (isFirstRun) {
                Button(onClick = { viewModel.finish(onDone) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepCard(
    @StringRes title: Int,
    @StringRes why: Int,
    granted: Boolean,
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
) {
    val colors = AppTheme.colors
    val titleText = stringResource(title)
    val stateText = stringResource(if (granted) R.string.step_status_allowed else R.string.step_status_needed)
    val a11y = stringResource(R.string.step_status_a11y, titleText, stateText)
    Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = a11y
                },
            ) {
                Icon(
                    if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = if (granted) colors.mint else colors.alert,
                    modifier = Modifier.size(28.dp),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(titleText, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text(stateText, style = MaterialTheme.typography.bodySmall, color = if (granted) colors.ink else colors.alert)
                }
            }
            Text(stringResource(why), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            if (!granted) {
                Button(onClick = onAction) { Text(stringResource(actionLabel)) }
            }
        }
    }
}

@Composable
private fun OemCard(oem: Oem) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val steps = stringArrayResource(
        when (oem) {
            Oem.XIAOMI -> R.array.oem_xiaomi_steps
            Oem.REALME_OPPO -> R.array.oem_realme_oppo_steps
            Oem.VIVO -> R.array.oem_vivo_steps
            Oem.ONEPLUS -> R.array.oem_oneplus_steps
            Oem.SAMSUNG -> R.array.oem_samsung_steps
            Oem.HUAWEI -> R.array.oem_huawei_steps
            Oem.OTHER -> R.array.oem_other_steps
        },
    )
    Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.oem_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            steps.forEachIndexed { index, step ->
                Row {
                    Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.padding(end = 8.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                }
            }
            OutlinedButton(onClick = { uriHandler.openUri("https://dontkillmyapp.com/") }) {
                Text(stringResource(R.string.oem_more_help))
            }
        }
    }
}

@Composable
private fun DisclaimerCard() {
    val colors = AppTheme.colors
    Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(stringResource(R.string.disclaimer_body), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            Text(stringResource(R.string.privacy_line), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
    }
}
