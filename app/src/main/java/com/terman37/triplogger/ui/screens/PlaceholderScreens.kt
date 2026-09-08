package com.terman37.triplogger.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder screens for Step 1 (plan.md). Report is replaced in Step 11;
// Home (Step 9) and Devices (Step 10) already have real screens.

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
