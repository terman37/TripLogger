package com.terman37.triplogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.terman37.triplogger.ui.TripLoggerApp
import com.terman37.triplogger.ui.theme.TripLoggerTheme

/**
 * Single entry point of the app (docs/UI.md: one screen container with a bottom
 * navigation bar). All screens are Compose composables rendered inside this
 * activity; TripLoggerApp builds the navigation structure.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Modern Android draws the app content edge-to-edge, behind the status
        // and navigation bars; our dark Scaffold handles the padding.
        enableEdgeToEdge()
        setContent {
            TripLoggerTheme {
                TripLoggerApp()
            }
        }
    }
}
