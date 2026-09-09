package com.terman37.triplogger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// App colors. Decision from UI.md: dark theme ONLY — the app never switches to
// a light scheme, so the color scheme is a fixed Material 3 dark palette.
//
// Accent (user request, plan.md Step 12): pop green/blue instead of the
// default lilac. Palette logic: primary = mint green (buttons, active UI),
// secondary = sky blue (selection/support), tertiary = warm amber container
// (the "Disconnected" grace card keeps a distinct warm look). Dark
// backgrounds stay near-neutral; on* colors guarantee contrast.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF41D8B7),          // pop mint green
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0E5A48), // deep green surface behind "Recording"
    onPrimaryContainer = Color(0xFFB9F3E5),
    secondary = Color(0xFF7AB8FF),        // pop sky blue
    onSecondary = Color(0xFF003256),
    secondaryContainer = Color(0xFF1E4A80),
    onSecondaryContainer = Color(0xFFD6E7FF),
    tertiary = Color(0xFFFFD166),         // warm amber accent
    onTertiary = Color(0xFF4A3200),
    tertiaryContainer = Color(0xFF574419),
    onTertiaryContainer = Color(0xFFFFE9BF),
)

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
