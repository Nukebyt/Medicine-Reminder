package com.nukebyt.medicinereminder

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.data.prefs.ThemeMode
import com.nukebyt.medicinereminder.ui.MainViewModel
import com.nukebyt.medicinereminder.ui.nav.AppNavHost
import com.nukebyt.medicinereminder.ui.theme.MedicineReminderTheme
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var snackbarController: SnackbarController
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash until DataStore has told us the theme and whether onboarding is done.
        splash.setKeepOnScreenCondition { viewModel.appSettings.value == null }
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.appSettings.collectAsStateWithLifecycle()
            val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val dark = when (current.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep status and navigation bar icons readable when the in-app theme differs from the system theme.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.argb(0xe6, 0xFF, 0xFF, 0xFF), Color.argb(0x80, 0x1b, 0x1b, 0x1b)) { dark },
                )
                onDispose { }
            }
            MedicineReminderTheme(darkTheme = dark) {
                AppNavHost(
                    snackbarController = snackbarController,
                    startWithOnboarding = !current.onboardingDone,
                    permissionStatus = permissionStatus,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed a permission in system Settings; re-check and re-arm alarms if needed.
        viewModel.refreshPermissions()
    }
}
