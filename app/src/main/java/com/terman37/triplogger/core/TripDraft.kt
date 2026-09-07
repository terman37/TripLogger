package com.terman37.triplogger.core

/**
 * A finished trip, ready to be stored. Mirrors the database entity (data.Trip)
 * WITHOUT the generated id and WITHOUT address fields: addresses are filled in
 * later by reverse geocoding (step 6). Kept in core so the recorder is pure
 * JVM logic with no Room dependency.
 *
 * Coordinates are nullable because a trip can run with no GPS fix at all
 * (indoor parking start, GPS cold start): such a trip still records time and
 * origin, with distance 0.
 */
data class TripDraft(
    val startEpochMillis: Long,
    val startLat: Double?,
    val startLng: Double?,
    val endEpochMillis: Long,
    val endLat: Double?,
    val endLng: Double?,
    val distanceKm: Double,
    val origin: TripOrigin,
)

/**
 * Injectable time source so tests can control "now" (the recorder otherwise
 * would be untestable — everything is time-based).
 */
fun interface Clock {
    fun nowMillis(): Long
}

/** The real app clock. */
val SystemClock: Clock = Clock { System.currentTimeMillis() }
