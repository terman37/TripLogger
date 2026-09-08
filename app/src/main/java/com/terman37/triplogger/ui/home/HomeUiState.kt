package com.terman37.triplogger.ui.home

import com.terman37.triplogger.core.TripOrigin

/**
 * Immutable UI state for the Home screen, produced by [HomeStateMapper] from
 * recorder snapshot + settings + trips. The screen renders exactly this —
 * no logic left in the composable.
 */
data class HomeUiState(
    val card: CardUiState,
    val recentTrips: List<TripRowUi>,
    val hasTrips: Boolean,
)

/**
 * Status card states (UI.md). One of these is shown at the top of Home.
 */
sealed interface CardUiState {

    /** Monitoring switch off, nothing recording. */
    data object MonitoringOff : CardUiState

    /** Monitoring on, no car connected. [deviceNames] = registered devices. */
    data class Waiting(val deviceNames: List<String>) : CardUiState

    /**
     * A trip is being recorded. [origin] tells whether it was started by the
     * car (device label available) or with the manual fallback button.
     */
    data class Recording(
        val origin: TripOrigin,
        val deviceName: String?,
        val distanceKm: Double,
        val startedAtEpochMillis: Long,
    ) : CardUiState

    /** Bluetooth dropped; the grace timer is running (trip not finished yet). */
    data class GracePeriod(val distanceKm: Double) : CardUiState
}

/**
 * One row in the "today + yesterday" trip list. Collapsed shows [title] +
 * [summary] + [distanceText]; expanded (user taps) shows the remaining fields
 * (UI.md). Text fields are pre-formatted strings so the composable stays dumb.
 */
data class TripRowUi(
    val id: Long,
    /** "Aug 6, 14:32" */
    val title: String,
    /** "Home → Office" */
    val summary: String,
    val distanceText: String,
    val origin: TripOrigin,
    // --- expanded fields ---
    val timeRangeText: String, // "14:32 – 15:15"
    val durationText: String,
    val startAddressText: String,
    val endAddressText: String,
)
