package com.terman37.triplogger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    HOME("home", "Home", Icons.Filled.Home),
    DEVICES("devices", "Devices", Icons.Filled.Bluetooth),
    REPORT("report", "Report", Icons.Filled.Assessment),
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

    // currentBackStackEntryAsState() turns the navigation state into a Compose
    // state: when the user navigates, this recomposes and the bottom bar can
    // highlight the active tab.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Tab switching without save/restore flags: every tab is a flat sibling of
    // the start destination. (Earlier saveState/restoreState + a pushed
    // destination left the back stack in a state where the Home tab could no
    // longer be selected — fixed, plan.md Step 12.)
    fun switchTo(route: String) {
        android.util.Log.i("TripNav", "switchTo $route (current=$currentRoute)")
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
                        onClick = { switchTo(destination.route) },
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
                LaunchedEffect(Unit) { android.util.Log.i("TripNav", "entered HOME") }
                HomeScreen(
                    onOpenDevices = { switchTo(TopLevelDestination.DEVICES.route) },
                )
            }
            composable(TopLevelDestination.DEVICES.route) {
                LaunchedEffect(Unit) { android.util.Log.i("TripNav", "entered DEVICES") }
                DevicesScreen()
            }
            composable(TopLevelDestination.REPORT.route) {
                LaunchedEffect(Unit) { android.util.Log.i("TripNav", "entered REPORT") }
                ReportScreen()
            }
        }
    }
}
