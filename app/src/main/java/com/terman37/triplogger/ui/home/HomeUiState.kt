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
    /** Master "Monitor trips" switch state (moved to Home, user request). */
    val monitoringEnabled: Boolean,
    /** False until at least one device is registered (switch stays disabled). */
    val canEnableMonitoring: Boolean,
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

    /** Bluetooth dropped; the grace timer is running (trip not finished yet).
     * [graceStartedAtEpochMillis] drives the "Disconnected since …" line. */
    data class GracePeriod(
        val distanceKm: Double,
        val graceStartedAtEpochMillis: Long,
    ) : CardUiState
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
    // Raw location data for the Google Maps directions button (fall back to
    // coordinates when an address is missing). Null on both → no button.
    val startStreet: String?,
    val startCity: String?,
    val startLat: Double?,
    val startLng: Double?,
    val endStreet: String?,
    val endCity: String?,
    val endLat: Double?,
    val endLng: Double?,
)
