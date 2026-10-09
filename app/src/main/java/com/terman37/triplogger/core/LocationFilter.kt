package com.terman37.triplogger.core

/**
 * Decides which GPS samples become part of the trip distance (GPS
 * decisions):
 *
 * 1. every fix that moved less than [minDisplacementMeters] since the last
 *    kept fix is ignored → a parked car or a red light adds no distance;
 * 2. fixes less accurate than [maxAccuracyMeters] are ignored → tunnels,
 *    garages, urban canyons;
 * 3. a fix implying a speed above [maxSpeedKmh] since the last kept fix is a
 *    GPS jump (a bogus fix far away), not real driving → ignored.
 *
 * The filter is STATEFUL: it remembers the last kept fix as an "anchor".
 * Because it is pure Kotlin (no Android types), the state machine logic around
 * it stays unit-testable on the JVM.
 *
 * A sample that passes all checks is kept and becomes the new anchor; the
 * returned distance is what the trip accumulates.
 */
class LocationFilter(
    private val minDisplacementMeters: Double = TrackingPolicy.MIN_DISPLACEMENT_METERS,
    private val maxAccuracyMeters: Double = TrackingPolicy.MAX_ACCURACY_METERS,
    private val maxSpeedKmh: Double = TrackingPolicy.MAX_SPEED_KMH,
) {
    // Last fix that passed all filters. Null until the first valid fix arrives.
    private var anchor: GpsSample? = null

    /**
     * Sets the anchor without charging any distance, for a fix supplied by the
     * caller as a starting point (the platform's cached fix, see
     * [TripRecorder.onAnchorHint]). The next processed sample is measured from it,
     * so the caller is responsible for only seeding with a trustworthy fix.
     */
    fun seed(sample: GpsSample) {
        anchor = sample
    }

    /**
     * Feeds one GPS sample through the filters.
     */
    fun process(sample: GpsSample): FilterDecision {
        // First fix of a trip has nothing to be compared to: keep it as the
        // anchor and add no distance yet.
        val currentAnchor = anchor
        if (currentAnchor == null) {
            anchor = sample
            return FilterDecision.Kept(distanceKm = 0.0)
        }

        // (2) Accuracy gate. An "unknown" accuracy (null) is trusted; Android
        // reports a number whenever it has one.
        sample.accuracyMeters?.let { accuracy ->
            if (accuracy > maxAccuracyMeters) return FilterDecision.Ignored(IgnoreReason.ACCURACY)
        }

        val distanceMeters =
            DistanceCalculator.haversineKm(
                currentAnchor.latitude, currentAnchor.longitude,
                sample.latitude, sample.longitude,
            ) * 1000.0

        // (1) Displacement gate.
        if (distanceMeters < minDisplacementMeters) {
            return FilterDecision.Ignored(IgnoreReason.DISPLACEMENT)
        }

        // (3) Speed gate: speed = distance / elapsed time.
        val elapsedSeconds = (sample.timestampEpochMillis - currentAnchor.timestampEpochMillis) / 1000.0
        if (elapsedSeconds > 0) {
            val speedKmh = (distanceMeters / 1000.0) / (elapsedSeconds / 3600.0)
            if (speedKmh > maxSpeedKmh) {
                return FilterDecision.Ignored(IgnoreReason.SPEED)
            }
        } else {
            // A fix that is not newer than the anchor cannot be validated: with no
            // elapsed time there is no speed to check, so the displacement gate
            // alone would decide. Keep it as the new anchor and charge nothing
            // instead of adding an unverifiable jump — a stale cached fix takes
            // exactly this path.
            anchor = sample
            return FilterDecision.Kept(distanceKm = 0.0)
        }

        anchor = sample
        return FilterDecision.Kept(distanceKm = distanceMeters / 1000.0)
    }
}
