package com.nukebyt.medicinereminder.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.nukebyt.medicinereminder.ui.medicines.EditMedicineScreen
import com.nukebyt.medicinereminder.ui.medicines.MedicineListScreen
import com.nukebyt.medicinereminder.ui.util.SnackbarController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object TodayRoute
@Serializable data object MedicinesRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute
@Serializable data class EditMedicineRoute(val medicineId: Long = 0L)

private data class Tab(
    val route: Any,
    val routeClass: KClass<*>,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(TodayRoute, TodayRoute::class, R.string.nav_today, Icons.Outlined.Today, Icons.Filled.Today),
    Tab(MedicinesRoute, MedicinesRoute::class, R.string.nav_medicines, Icons.Outlined.Medication, Icons.Filled.Medication),
    Tab(HistoryRoute, HistoryRoute::class, R.string.nav_history, Icons.Outlined.History, Icons.Filled.History),
    Tab(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun AppNavHost(snackbarController: SnackbarController) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarController) {
        snackbarController.requests.collect { request ->
            // Replace whatever is showing: the newest message wins.
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = request.message,
                actionLabel = request.actionLabel,
                withDismissAction = request.actionLabel == null,
                duration = if (request.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            request.result.complete(result == SnackbarResult.ActionPerformed)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = AppTheme.colors.foil) {
                    tabs.forEach { tab ->
                        val selected = destination?.hasRoute(tab.routeClass) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppTheme.colors.onCapsule,
                                selectedTextColor = AppTheme.colors.ink,
                                indicatorColor = AppTheme.colors.capsule,
                                unselectedIconColor = AppTheme.colors.muted,
                                unselectedTextColor = AppTheme.colors.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<TodayRoute> { Placeholder("Today") }
            composable<MedicinesRoute> {
                MedicineListScreen(
                    onAdd = { navController.navigate(EditMedicineRoute()) },
                    onEdit = { navController.navigate(EditMedicineRoute(it)) },
                )
            }
            composable<HistoryRoute> { Placeholder("History") }
            composable<SettingsRoute> { Placeholder("Settings") }
            composable<EditMedicineRoute> {
                EditMedicineScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier, contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
