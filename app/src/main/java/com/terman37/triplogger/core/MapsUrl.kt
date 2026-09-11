package com.terman37.triplogger.core

import java.net.URLEncoder
import java.util.Locale

/**
 * Builds Google Maps URLs from a trip endpoint (start or end). Used by the UI
 * (directions button, TripRowCard) and by the Excel export (one link per
 * address), so the encoding rules live here once and are unit-tested.
 *
 * Two link kinds exist:
 * - [directions] — route between two endpoints:
 *   `https://www.google.com/maps/dir/?api=1&origin=…&destination=…&travelmode=driving`
 * - [place] — show a single endpoint on the map:
 *   `https://www.google.com/maps/search/?api=1&query=…`
 *
 * Each endpoint prefers the reverse-geocoded street (+ city) when a street is
 * known, otherwise the raw "lat,lng" coordinates (a city-only query would only
 * zoom to the city). With neither it falls back to the bare city, else null
 * (the UI hides the button, the export writes plain text without a link).
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

    /**
     * "street, city" when a street is known, else "lat,lng", else the city
     * alone, else null (nothing to link to). A city-only address is a poor Maps
     * query (it zooms to the whole city), so coordinates take priority over it.
     */
    private fun endpointParam(
        street: String?,
        city: String?,
        lat: Double?,
        lng: Double?,
    ): String? {
        val streetLine = street?.trim()?.takeIf { it.isNotEmpty() }
        if (streetLine != null) {
            return listOfNotNull(streetLine, city?.trim()?.takeIf { it.isNotEmpty() })
                .joinToString(", ")
        }
        if (lat != null && lng != null) {
            // Maps accepts "lat,lng" as a place; keep the dot decimal separator.
            return String.format(Locale.US, "%.6f,%.6f", lat, lng)
        }
        return city?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
