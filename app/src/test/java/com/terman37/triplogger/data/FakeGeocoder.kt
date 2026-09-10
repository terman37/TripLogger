package com.terman37.triplogger.data

import com.terman37.triplogger.core.ReverseGeocodeResult
import com.terman37.triplogger.geocoding.GeocoderClient

/**
 * Scriptable geocoder for JVM tests: coordinate pair → fixed address result.
 * [results] can be reassigned to simulate a network coming back (lazy retry
 * tests).
 */
class FakeGeocoder(
    var results: Map<Pair<Double, Double>, ReverseGeocodeResult> = emptyMap(),
) : GeocoderClient {
    var calls = 0
        private set

    override fun reverse(latitude: Double, longitude: Double): ReverseGeocodeResult? {
        calls++
        return results[latitude to longitude]
    }
}
