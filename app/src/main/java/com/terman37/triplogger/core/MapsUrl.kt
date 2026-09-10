package com.terman37.triplogger.core

import java.net.URLEncoder
import java.util.Locale

/**
 * Builds Google Maps URLs from a trip endpoint (start or end). Used by the UI
 * (directions button, TripRowCard) and by the CSV export (one link per
 * address), so the encoding rules live here once and are unit-tested.
 *
 * Two link kinds exist:
 * - [directions] — route between two endpoints:
 *   `https://www.google.com/maps/dir/?api=1&origin=…&destination=…&travelmode=driving`
 * - [place] — show a single endpoint on the map:
 *   `https://www.google.com/maps/search/?api=1&query=…`
 *
 * Each endpoint prefers the reverse-geocoded street+city, else falls back to
 * the raw "lat,lng" coordinates (also accepted by the Maps API). When an
 * endpoint has neither, its link is unusable → null (the UI hides the button,
 * the CSV writes an empty cell).
 *
 * Pure JVM (java.net only) so the encoding rules can be tested without Android.
 */
object MapsUrl {

    fun directions(
        originStreet: String?,
        originCity: String?,
        originLat: Double?,
        originLng: Double?,
        destinationStreet: String?,
        destinationCity: String?,
        destinationLat: Double?,
        destinationLng: Double?,
    ): String? {
        val origin = endpointParam(originStreet, originCity, originLat, originLng) ?: return null
        val destination = endpointParam(
            destinationStreet, destinationCity, destinationLat, destinationLng,
        ) ?: return null
        return "https://www.google.com/maps/dir/?api=1" +
            "&origin=${encode(origin)}&destination=${encode(destination)}&travelmode=driving"
    }

    fun place(
        street: String?,
        city: String?,
        lat: Double?,
        lng: Double?,
    ): String? {
        val endpoint = endpointParam(street, city, lat, lng) ?: return null
        return "https://www.google.com/maps/search/?api=1&query=${encode(endpoint)}"
    }

    /** Address when available, else "lat,lng", else null (nothing to link to). */
    private fun endpointParam(
        street: String?,
        city: String?,
        lat: Double?,
        lng: Double?,
    ): String? {
        val address = listOfNotNull(street?.trim(), city?.trim())
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
        return if (address != null) {
            address
        } else if (lat != null && lng != null) {
            // Maps accepts "lat,lng" as a place; keep the dot decimal separator.
            String.format(Locale.US, "%.6f,%.6f", lat, lng)
        } else {
            null
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
