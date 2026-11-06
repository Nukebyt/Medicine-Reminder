package com.nukebyt.medicinereminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nukebyt.medicinereminder.ui.nav.AppNavHost
import com.nukebyt.medicinereminder.ui.theme.MedicineReminderTheme
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var snackbarController: SnackbarController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicineReminderTheme {
                AppNavHost(snackbarController)
            }
        }
    }
}
