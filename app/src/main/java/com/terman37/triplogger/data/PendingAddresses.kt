package com.terman37.triplogger.data

import com.terman37.triplogger.core.ReverseGeocodeResult

/**
 * Pure helper deciding which trips still need a reverse geocode and applying
 * the result. Kept free of Android code so it is JVM testable; the repository
 * (data layer) drives it with real trips and the geocoder client.
 *
 * Retry rule: a side needs geocoding when the trip HAS coordinates
 * for it but the stored address is incomplete (street or city missing). This
 * also covers the case where a previous lookup returned only a city: the next
 * retry overwrites whatever it gets.
 */
object PendingAddresses {

    /** Which end of a trip needs its address resolved. */
    enum class End { START, END }

    /** One geocode request: resolve [lat]/[lng] into the START or END address
     * of [tripId]. */
    data class Request(val tripId: Long, val end: End, val lat: Double, val lng: Double)

    /** All requests pending for the given trips, in a stable order. */
    fun pendingRequests(trips: List<Trip>): List<Request> = buildList {
        for (trip in trips) {
            val startLat = trip.startLat
            val startLng = trip.startLng
            if (startLat != null && startLng != null && needsGeocoding(
                    trip.startStreet, trip.startCity,
                )
            ) {
                add(Request(trip.id, End.START, startLat, startLng))
            }
            val endLat = trip.endLat
            val endLng = trip.endLng
            if (endLat != null && endLng != null && needsGeocoding(
                    trip.endStreet, trip.endCity,
                )
            ) {
                add(Request(trip.id, End.END, endLat, endLng))
            }
        }
    }

    /**
     * Applies one geocoder result to a trip copy. Returns the trip unchanged
     * when the result is null (lookup failed → keep pending, retry later).
     */
    fun apply(trip: Trip, request: Request, result: ReverseGeocodeResult?): Trip {
        if (result == null) return trip
        return when (request.end) {
            End.START -> trip.copy(
                startStreet = result.street ?: trip.startStreet,
                startCity = result.city ?: trip.startCity,
            )
            End.END -> trip.copy(
                endStreet = result.street ?: trip.endStreet,
                endCity = result.city ?: trip.endCity,
            )
        }
    }

    private fun needsGeocoding(street: String?, city: String?): Boolean =
        street == null || city == null
}
