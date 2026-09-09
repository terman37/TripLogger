package com.terman37.triplogger.ui.home

import com.terman37.triplogger.data.Trip

/**
 * Builds a display [TripRowUi] from a database trip. Shared by the Home and
 * Report screens (both expand rows with the same detail, UI.md).
 */
fun tripToRowUi(trip: Trip, zone: java.time.ZoneId): TripRowUi {
    val startAddress = TripText.addressText(
        trip.startStreet, trip.startCity,
        hasCoordinates = trip.startLat != null && trip.startLng != null,
    )
    val endAddress = TripText.addressText(
        trip.endStreet, trip.endCity,
        hasCoordinates = trip.endLat != null && trip.endLng != null,
    )
    val summary = TripText.shortLabel(trip.startStreet, trip.startCity, "Start") +
        " → " +
        TripText.shortLabel(trip.endStreet, trip.endCity, "End")
    return TripRowUi(
        id = trip.id,
        title = TripText.dateTimeText(trip.startEpochMillis, zone),
        summary = summary,
        distanceText = TripText.kmText(trip.distanceKm),
        origin = trip.origin,
        timeRangeText = "${TripText.timeText(trip.startEpochMillis, zone)} – " +
            TripText.timeText(trip.endEpochMillis, zone),
        durationText = TripText.durationText(trip.startEpochMillis, trip.endEpochMillis),
        startAddressText = startAddress,
        endAddressText = endAddress,
        startStreet = trip.startStreet,
        startCity = trip.startCity,
        startLat = trip.startLat,
        startLng = trip.startLng,
        endStreet = trip.endStreet,
        endCity = trip.endCity,
        endLat = trip.endLat,
        endLng = trip.endLng,
    )
}
