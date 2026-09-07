package com.terman37.triplogger.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder screens for Step 1 (plan.md): the bottom-navigation shell with
// empty tabs. Each screen is replaced by its real implementation in a later
// step (Home → Step 9, Devices → Step 10, Report → Step 11).

@Composable
fun HomeScreen() {
    PlaceholderText("Home — trip status and recent trips (Step 9)")
}

@Composable
fun DevicesScreen() {
    PlaceholderText("Devices — monitoring settings (Step 10)")
}

@Composable
fun ReportScreen() {
    PlaceholderText("Report — date range and export (Step 11)")
}

/** Centers a hint text in the screen body. */
@Composable
private fun PlaceholderText(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
