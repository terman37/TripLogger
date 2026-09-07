package com.terman37.triplogger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// App colors. Decision from UI.md: dark theme ONLY — the app never switches to a
// light scheme, so the color scheme is a fixed Material 3 dark palette (the
// default one, with no custom brand colors).
private val DarkColors = darkColorScheme()

/**
 * Theme wrapper used by the whole app. In Compose, a "theme" is just a set of
 * colors/typography provided to all composables below it through the
 * MaterialTheme object (no XML involved).
 */
@Composable
fun TripLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
