package com.terman37.triplogger.geocoding

import com.terman37.triplogger.core.ReverseGeocodeResult

/**
 * Turns GPS coordinates into a street + city (decision: reverse geocoding,
 * see docs/DETAILS.md). Implementations do network I/O (Android Geocoder) and must be
 * called from a background thread.
 *
 * @return null when the lookup failed (no network, service down) — the caller
 *   stores the coordinates only and retries later (lazy retry on app
 *   open and at report generation).
 */
interface GeocoderClient {
    fun reverse(latitude: Double, longitude: Double): ReverseGeocodeResult?
}
