package com.terman37.triplogger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.terman37.triplogger.ui.home.HomeScreen
import com.terman37.triplogger.ui.devices.DevicesScreen
import com.terman37.triplogger.ui.report.ReportScreen

/**
 * The three top-level destinations shown in the bottom navigation bar
 * (UI.md). Each has a unique route used by the NavHost.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    // Tab order: Home, Report, Settings.
    HOME("home", "Home", Icons.Filled.Home),
    REPORT("report", "Report", Icons.Filled.Assessment),
    // Settings holds the Bluetooth device setup, the grace period and the
    // About entry; the gear icon matches that mixed content better than the
    // old Bluetooth icon.
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

/**
 * Root of the UI: a Scaffold (Material layout container) whose content is the
 * navigation graph and whose bottom bar lets the user switch between the three
 * destinations.
 */
@Composable
fun TripLoggerApp() {
    // The NavController knows which destination is currently displayed and how
    // to navigate between them.
    val navController = rememberNavController()

    // Set when Home sends the user to Settings because no device is registered:
    // the Available list then starts expanded so the "Allow Bluetooth access"
    // button is visible right away. Reset on every other tab switch.
    var expandAvailableInSettings by remember { mutableStateOf(false) }

    // currentBackStackEntryAsState() turns the navigation state into a Compose
    // state: when the user navigates, this recomposes and the bottom bar can
    // highlight the active tab.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Tab switching without save/restore flags: every tab is a flat sibling of
    // the start destination. (Earlier saveState/restoreState + a pushed
    // destination left the back stack in a state where the Home tab could no
    // longer be selected.)
    fun switchTo(route: String) {
        navController.navigate(route) {
            // Pop everything above the start destination, then go to the tab:
            // no tab ever stacks on another.
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            // A plain tab tap is not the first-run detour: leave
                            // the Available list collapsed as usual.
                            expandAvailableInSettings = false
                            switchTo(destination.route)
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            // innerPadding from the Scaffold accounts for the bottom bar, so the
            // screens are not hidden behind it.
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onOpenSettings = {
                        expandAvailableInSettings = true
                        switchTo(TopLevelDestination.SETTINGS.route)
                    },
                )
            }
            composable(TopLevelDestination.SETTINGS.route) {
                DevicesScreen(expandAvailable = expandAvailableInSettings)
            }
            composable(TopLevelDestination.REPORT.route) {
                ReportScreen()
            }
        }
    }
}
