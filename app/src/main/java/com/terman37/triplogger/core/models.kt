package com.terman37.triplogger.core

/**
 * How a trip was started. AUTO = started because a registered Bluetooth device
 * connected (or resumed within the grace period); MANUAL = started with the
 * fallback button on the Home screen. Stored in the database so the UI can show
 * "Started manually" instead of a connected device name (docs/UI.md).
 */
enum class TripOrigin {
    AUTO,
    MANUAL,
}

/**
 * Tuning constants for trip tracking. Kept in ONE place on purpose: they are
 * product decisions and may change without touching logic.
 */
object TrackingPolicy {
    /** Time between two GPS fix requests, milliseconds (fixed at 30 s). */
    const val LOCATION_INTERVAL_MS = 30_000L

    /** A fix closer than this to the previous kept fix is noise (parked car,
     * red light). Smaller than this: GPS jitter around a stopped car. */
    const val MIN_DISPLACEMENT_METERS = 10.0

    /** Fixes less accurate than this are discarded (tunnel, garage, urban
     * canyon). */
    const val MAX_ACCURACY_METERS = 50.0

    /** Steps implying more than this many km/h are GPS jumps, not driving
     * (decided limit: 160). */
    const val MAX_SPEED_KMH = 160.0

    /** Mean Earth radius used by the Haversine distance. */
    const val EARTH_RADIUS_KM = 6371.0

    /**
     * Trips shorter than this are discarded on finish (earlier decisions, user
     * feedback): parked-engine sessions produce ~0 km rows that only pollute
     * reports. 50 m because rows display "0.0 km" up to ~49 m anyway.
     */
    const val MIN_TRIP_DISTANCE_KM = 0.05
}

/**
 * A single GPS observation delivered to the recorder pipeline.
 *
 * @param timestampEpochMillis when the fix was taken (UTC ms).
 * @param latitude/longitude degrees, WGS84.
 * @param accuracyMeters horizontal accuracy reported by the GPS chip; null
 *   when the provider did not report one (treated as "unknown", not "bad").
 */
data class GpsSample(
    val timestampEpochMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

/** Why a sample was not added to the trip distance. */
enum class IgnoreReason {
    /** Too close to the previous kept fix (not moving / GPS jitter). */
    DISPLACEMENT,

    /** GPS accuracy worse than the configured maximum. */
    ACCURACY,

    /** Implied speed between fixes higher than a car can drive. */
    SPEED,
}

/**
 * Result of feeding one GPS sample into the [LocationFilter].
 */
sealed interface FilterDecision {
    /** Sample was added: [distanceKm] is the distance since the previous
     * kept fix (0 for the very first fix, which only anchors the trip). */
    data class Kept(val distanceKm: Double) : FilterDecision

    /** Sample was discarded for [reason]; the distance anchor is unchanged. */
    data class Ignored(val reason: IgnoreReason) : FilterDecision
}
