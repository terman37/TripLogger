package com.terman37.triplogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFilterTest {

    private val paris = GpsSample(0L, 48.8566, 2.3522, accuracyMeters = 10f)

    private fun sample(
        ts: Long,
        latOffsetDegrees: Double = 0.0,
        lngOffsetDegrees: Double = 0.0,
        accuracy: Float? = 10f,
    ) = GpsSample(ts, paris.latitude + latOffsetDegrees, paris.longitude + lngOffsetDegrees, accuracy)

    // At 48.86°N: 1° latitude ≈ 111.19 km; 1° longitude ≈ 73.2 km.
    private val metersPerLatDegree = 111_190.0
    private val metersPerLngDegree = 73_200.0

    private fun movedNorth(meters: Double, ts: Long) =
        sample(ts, latOffsetDegrees = meters / metersPerLatDegree)

    private fun movedEast(meters: Double, ts: Long) =
        sample(ts, lngOffsetDegrees = meters / metersPerLngDegree)

    private fun assertKept(decision: FilterDecision, expectedKm: Double, tolerance: Double = 0.01) {
        assertTrue("expected Kept, got $decision", decision is FilterDecision.Kept)
        assertEquals(expectedKm, (decision as FilterDecision.Kept).distanceKm, tolerance)
    }

    private fun assertIgnored(decision: FilterDecision, reason: IgnoreReason) {
        assertEquals(FilterDecision.Ignored(reason), decision)
    }

    @Test
    fun firstSample_isKeptAsAnchorWithNoDistance() {
        val filter = LocationFilter()
        assertKept(filter.process(paris), 0.0)
    }

    @Test
    fun parkedCar_jitterIsIgnored() {
        val filter = LocationFilter()
        filter.process(paris)
        // 3 m of GPS drift every 30 s while parked.
        assertIgnored(filter.process(movedNorth(3.0, ts = 30_000)), IgnoreReason.DISPLACEMENT)
        assertIgnored(filter.process(movedNorth(3.0, ts = 60_000)), IgnoreReason.DISPLACEMENT)
        assertIgnored(filter.process(movedNorth(3.0, ts = 90_000)), IgnoreReason.DISPLACEMENT)
    }

    @Test
    fun displacementBoundary_isKeptAtExactly10m() {
        val filter = LocationFilter()
        filter.process(paris)
        assertKept(filter.process(movedNorth(10.0, ts = 30_000)), 0.01, tolerance = 0.001)
    }

    @Test
    fun inaccurateFix_isIgnored() {
        val filter = LocationFilter()
        filter.process(paris)
        val bad = sample(ts = 30_000, latOffsetDegrees = 0.01, accuracy = 80f)
        assertIgnored(filter.process(bad), IgnoreReason.ACCURACY)
    }

    @Test
    fun accuracyBoundary_isKeptAtExactly50m() {
        val filter = LocationFilter()
        filter.process(paris)
        assertKept(filter.process(sample(ts = 30_000, latOffsetDegrees = 0.01, accuracy = 50f)), 1.1, 0.1)
    }

    @Test
    fun unknownAccuracy_isTrusted() {
        val filter = LocationFilter()
        filter.process(paris)
        assertKept(filter.process(sample(ts = 30_000, latOffsetDegrees = 0.01, accuracy = null)), 1.1, 0.1)
    }

    @Test
    fun gpsJump_over160Kmh_isIgnored() {
        val filter = LocationFilter()
        filter.process(paris)
        // 2 km east in 30 s = 240 km/h: impossible for a car → jump.
        val jump = movedEast(2000.0, ts = 30_000)
        assertIgnored(filter.process(jump), IgnoreReason.SPEED)
    }

    @Test
    fun jump_doesNotMoveTheAnchor() {
        val filter = LocationFilter()
        filter.process(paris)
        filter.process(movedEast(2000.0, ts = 30_000)) // jump, ignored

        // Distance to the next real fix is measured from the ORIGINAL anchor,
        // not from the bogus point.
        assertKept(filter.process(movedEast(500.0, ts = 60_000)), 0.5, 0.05)
    }

    @Test
    fun exactly160Kmh_isKept() {
        val filter = LocationFilter()
        filter.process(paris)
        // 160 km/h for 30 s = 1333 m. Threshold is "greater than 160": equal is OK.
        assertKept(filter.process(movedEast(1333.0, ts = 30_000)), 1.33, 0.05)
    }

    @Test
    fun slowDriving_accumulates() {
        val filter = LocationFilter()
        filter.process(paris)
        // City driving: ~500 m of forward motion per 30 s fix (= 60 km/h).
        // Offsets are absolute positions: 500 → 1000 → 1500 m north.
        assertKept(filter.process(movedNorth(500.0, ts = 30_000)), 0.5, 0.05)
        assertKept(filter.process(movedNorth(1000.0, ts = 60_000)), 0.5, 0.05)
        assertKept(filter.process(movedNorth(1500.0, ts = 90_000)), 0.5, 0.05)
    }

    @Test
    fun redLightStop_skipsNoDistanceAndResumes() {
        val filter = LocationFilter()
        filter.process(paris)
        filter.process(movedNorth(500.0, ts = 30_000)) // drive 500 m → anchor
        // Red light: same spot for 30 s → below displacement gate.
        assertIgnored(filter.process(movedNorth(500.0, ts = 60_000)), IgnoreReason.DISPLACEMENT)
        // Green: measured from the last KEPT fix (+500 m), not the ignored one.
        assertKept(filter.process(movedNorth(1000.0, ts = 90_000)), 0.5, 0.05)
    }

    @Test
    fun pathologicalSameTimestamp_isKeptWhenMoved() {
        val filter = LocationFilter()
        filter.process(paris)
        // Same timestamp as the anchor (cannot compute speed): displacement
        // gate alone decides.
        assertKept(filter.process(movedNorth(500.0, ts = 0L)), 0.5, 0.05)
    }
}
