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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.terman37.triplogger.ui.home.HomeScreen
import com.terman37.triplogger.ui.screens.DevicesScreen
import com.terman37.triplogger.ui.screens.ReportScreen

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

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            // Standard bottom-navigation behavior: re-selecting
                            // a tab must not stack copies of it (launchSingleTop)
                            // and each tab keeps its state when you leave it and
                            // come back (saveState / restoreState).
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                    onOpenDevices = {
                        navController.navigate(TopLevelDestination.DEVICES.route)
                    },
                )
            }
            composable(TopLevelDestination.DEVICES.route) { DevicesScreen() }
            composable(TopLevelDestination.REPORT.route) { ReportScreen() }
        }
    }
}
