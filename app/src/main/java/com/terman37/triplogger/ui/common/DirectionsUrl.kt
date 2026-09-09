package com.terman37.triplogger.ui.common

import java.net.URLEncoder
import java.util.Locale

/**
 * Builds the Google Maps driving-directions URL shown on the expanded trip
 * detail (user request, plan.md Step 12):
 *
 *   https://www.google.com/maps/dir/?api=1&origin=…&destination=…&travelmode=driving
 *
 * Each endpoint prefers the street address when geocoded, else falls back to
 * the raw "lat,lng" coordinates (also accepted by the Maps API). When a side
 * has neither address nor coordinates the whole link is unusable → null, and
 * the UI hides the button.
 *
 * Pure JVM (java.net only) so the encoding rules are unit-tested.
 */
object DirectionsUrl {

    fun build(
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
