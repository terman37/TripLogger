package com.terman37.triplogger.ui.home

import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.Trip

/**
 * Pure mapping: recorder snapshot + settings + trips → [HomeUiState].
 * Android-free and deterministic (zone injected) so the mapping rules are JVM
 * unit-tested; the ViewModel only collects sources and calls [toUi].
 */
object HomeStateMapper {

    fun toUi(
        snapshot: TripRecorder.Snapshot,
        monitoringEnabled: Boolean,
        deviceNames: List<String>,
        trips: List<Trip>,
        zone: java.time.ZoneId,
    ): HomeUiState {
        val card = when {
            snapshot.phase == TripRecorder.Phase.RECORDING -> CardUiState.Recording(
                origin = snapshot.origin ?: com.terman37.triplogger.core.TripOrigin.MANUAL,
                deviceName = snapshot.deviceName,
                distanceKm = snapshot.distanceKm,
                startedAtEpochMillis = snapshot.startEpochMillis ?: 0L,
            )
            snapshot.phase == TripRecorder.Phase.GRACE -> CardUiState.GracePeriod(
                distanceKm = snapshot.distanceKm,
            )
            monitoringEnabled -> CardUiState.Waiting(deviceNames)
            else -> CardUiState.MonitoringOff
        }

        val rows = trips.map { it.toRowUi(zone) }
        return HomeUiState(card = card, recentTrips = rows, hasTrips = rows.isNotEmpty())
    }
}

/** Formats one database trip into a display row. */
private fun Trip.toRowUi(zone: java.time.ZoneId): TripRowUi {
    val startAddress = TripText.addressText(startStreet, startCity, hasCoordinates = startLat != null && startLng != null)
    val endAddress = TripText.addressText(endStreet, endCity, hasCoordinates = endLat != null && endLng != null)
    val summary = TripText.shortLabel(startStreet, startCity, "Start") +
        " → " +
        TripText.shortLabel(endStreet, endCity, "End")
    return TripRowUi(
        id = id,
        title = TripText.dateTimeText(startEpochMillis, zone),
        summary = summary,
        distanceText = TripText.kmText(distanceKm),
        origin = origin,
        timeRangeText = "${TripText.timeText(startEpochMillis, zone)} – " +
            TripText.timeText(endEpochMillis, zone),
        durationText = TripText.durationText(startEpochMillis, endEpochMillis),
        startAddressText = startAddress,
        endAddressText = endAddress,
    )
}
