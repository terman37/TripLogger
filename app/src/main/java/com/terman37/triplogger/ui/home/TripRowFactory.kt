package com.terman37.triplogger.ui.home

import com.terman37.triplogger.data.Trip

/**
 * Builds a display [TripRowUi] from a database trip. Shared by the Home and
 * Report screens (both expand rows with the same detail, docs/UI.md).
 */
fun tripToRowUi(trip: Trip, zone: java.time.ZoneId, text: TripText): TripRowUi {
    val startAddress = text.addressText(
        trip.startStreet, trip.startCity,
        hasCoordinates = trip.startLat != null && trip.startLng != null,
    )
    val endAddress = text.addressText(
        trip.endStreet, trip.endCity,
        hasCoordinates = trip.endLat != null && trip.endLng != null,
    )
    val summary = text.shortLabel(trip.startStreet, trip.startCity, text.startPlaceholder) +
        " → " +
        text.shortLabel(trip.endStreet, trip.endCity, text.endPlaceholder)
    return TripRowUi(
        id = trip.id,
        title = text.dateTimeText(trip.startEpochMillis, zone),
        summary = summary,
        distanceText = text.kmText(trip.distanceKm),
        timeRangeText = "${text.timeText(trip.startEpochMillis, zone)} – " +
            text.timeText(trip.endEpochMillis, zone),
        durationText = text.durationText(trip.startEpochMillis, trip.endEpochMillis),
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
