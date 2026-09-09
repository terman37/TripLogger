package com.terman37.triplogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRecorderTest {

    // Mutable fake clock: tests control "now" precisely.
    private class FakeClock(var now: Long) : Clock {
        override fun nowMillis(): Long = now
    }

    private val graceMs = 3 * 60_000L // default grace period
    private val base = 1_000_000L

    // ~111 m per 0.001° latitude at the test latitude.
    private fun sampleAt(t: Long, latOffsetDegrees: Double = 0.0) = GpsSample(
        timestampEpochMillis = t,
        latitude = 48.8566 + latOffsetDegrees,
        longitude = 2.3522,
        accuracyMeters = 10f,
    )

    /** 500 m north ≈ 0.0045° latitude. */
    private val step500m = 500.0 / 111_190.0

    private fun assertPhase(recorder: TripRecorder, phase: TripRecorder.Phase) {
        assertEquals(phase, recorder.snapshot().phase)
    }

    // --- auto trip lifecycle ---------------------------------------------

    @Test
    fun fullAutoTrip_connectsRecordsDisconnectsAndFinishesOnGraceExpiry() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        assertPhase(recorder, TripRecorder.Phase.RECORDING)
        assertEquals(TripOrigin.AUTO, recorder.snapshot().origin)

        // Drive: one fix per 30 s, 500 m each. The FIRST fix only anchors the
        // trip (0 km); each following fix adds its step.
        recorder.onLocationSample(sampleAt(base + 30_000, step500m))          // anchor
        recorder.onLocationSample(sampleAt(base + 60_000, 2 * step500m))      // +0.5
        recorder.onLocationSample(sampleAt(base + 90_000, 3 * step500m))      // +0.5
        recorder.onLocationSample(sampleAt(base + 120_000, 4 * step500m))     // +0.5

        clock.now = base + 150_000
        recorder.onDeviceDisconnected()
        assertPhase(recorder, TripRecorder.Phase.GRACE)

        // Timer fires before the grace period ends → nothing happens.
        clock.now = base + 150_000 + graceMs - 1
        recorder.onGraceTimerExpired()
        assertPhase(recorder, TripRecorder.Phase.GRACE)
        assertTrue(recorder.takeFinishedTrips().isEmpty())

        // Timer fires at the end of the grace period → trip finished.
        clock.now = base + 150_000 + graceMs
        recorder.onGraceTimerExpired()
        assertPhase(recorder, TripRecorder.Phase.IDLE)

        val trip = recorder.takeFinishedTrips().single()
        assertEquals(base, trip.startEpochMillis)
        assertEquals(base + 150_000 + graceMs, trip.endEpochMillis)
        assertEquals(TripOrigin.AUTO, trip.origin)
        assertEquals(1.5, trip.distanceKm, 0.05)
        // Start position = first fix; end position = last kept fix.
        assertEquals(48.8566 + step500m, trip.startLat!!, 1e-6)
        assertEquals(48.8566 + 4 * step500m, trip.endLat!!, 1e-6)
        assertNull(recorder.snapshot().startEpochMillis)
    }

    @Test
    fun graceStartedAt_isExposedInSnapshot() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        clock.now = base + 7_000
        recorder.onDeviceDisconnected()

        val snap = recorder.snapshot()
        assertEquals(TripRecorder.Phase.GRACE, snap.phase)
        assertEquals(base + 7_000, snap.graceStartedAtEpochMillis)
    }

    @Test
    fun reconnectInsideGrace_resumesSameTrip() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        recorder.onLocationSample(sampleAt(base + 30_000, step500m)) // anchor (0 km)

        clock.now = base + 60_000
        recorder.onDeviceDisconnected()
        assertPhase(recorder, TripRecorder.Phase.GRACE)

        // Car reconnects 1 minute later (inside the 3 min grace window).
        clock.now = base + 120_000
        recorder.onDeviceConnected("Car")
        assertPhase(recorder, TripRecorder.Phase.RECORDING)

        // 1 km further north = 1000 m from the pre-disconnect anchor.
        recorder.onLocationSample(sampleAt(base + 150_000, step500m + 0.0090))

        recorder.onManualStop()
        val trip = recorder.takeFinishedTrips().single()

        // Same trip: original start time, accumulated distance carried over.
        assertEquals(base, trip.startEpochMillis)
        assertEquals(1.0, trip.distanceKm, 0.05)
    }

    @Test
    fun reconnectAfterGraceFinished_startsNewTrip() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        clock.now = base + 10_000
        recorder.onDeviceDisconnected()
        clock.now = base + 10_000 + graceMs
        recorder.onGraceTimerExpired() // finishes trip 1 (0 km → discarded)
        assertTrue(recorder.takeFinishedTrips().isEmpty())

        // Reconnect much later = brand new trip.
        clock.now = base + 1_000_000
        recorder.onDeviceConnected("Car")
        assertEquals(TripRecorder.Phase.RECORDING, recorder.snapshot().phase)
        assertEquals(base + 1_000_000, recorder.snapshot().startEpochMillis)
        assertEquals(0.0, recorder.snapshot().distanceKm, 0.0)
    }

    // --- manual trips ----------------------------------------------------

    @Test
    fun manualTrip_ignoresBluetoothEvents_andStopsOnManualStop() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onManualStart()
        assertEquals(TripOrigin.MANUAL, recorder.snapshot().origin)
        assertNull(recorder.snapshot().deviceName)

        // A Bluetooth disconnect must NOT affect a manual trip.
        recorder.onDeviceDisconnected()
        assertPhase(recorder, TripRecorder.Phase.RECORDING)

        recorder.onLocationSample(sampleAt(base + 30_000, step500m))      // anchor
        recorder.onLocationSample(sampleAt(base + 60_000, 2 * step500m))  // +0.5
        recorder.onManualStop()

        val trip = recorder.takeFinishedTrips().single()
        assertEquals(TripOrigin.MANUAL, trip.origin)
        assertEquals(0.5, trip.distanceKm, 0.05)
    }

    @Test
    fun manualStopDuringGrace_endsTripImmediately() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        clock.now = base + 5_000
        recorder.onDeviceDisconnected()
        assertPhase(recorder, TripRecorder.Phase.GRACE)

        recorder.onManualStop() // user aborts while in grace
        // No movement happened → the aborted trip is discarded.
        assertTrue(recorder.takeFinishedTrips().isEmpty())
        assertPhase(recorder, TripRecorder.Phase.IDLE)
    }

    @Test
    fun eventsInWrongStates_areNoOps() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        // Disconnect / stop / sample while idle do nothing.
        recorder.onDeviceDisconnected()
        recorder.onManualStop()
        recorder.onLocationSample(sampleAt(base))
        recorder.onGraceTimerExpired()
        assertPhase(recorder, TripRecorder.Phase.IDLE)
        assertTrue(recorder.takeFinishedTrips().isEmpty())

        // manualStart while an auto trip runs is ignored (no second session);
        // stopping a 0-km trip discards it.
        recorder.onDeviceConnected("Car")
        recorder.onManualStart()
        recorder.onManualStop()
        assertPhase(recorder, TripRecorder.Phase.IDLE)
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }

    // --- GPS edge cases --------------------------------------------------

    @Test
    fun samplesDuringGrace_doNotCount() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        recorder.onLocationSample(sampleAt(base + 30_000, step500m)) // anchor (0 km)
        clock.now = base + 60_000
        recorder.onDeviceDisconnected()
        assertPhase(recorder, TripRecorder.Phase.GRACE)

        // Car disconnected → fixes during grace must not add distance.
        recorder.onLocationSample(sampleAt(base + 90_000, 3 * step500m))
        clock.now = base + 60_000 + graceMs
        recorder.onGraceTimerExpired()

        // Only the (zero-distance) anchor fix existed → trip discarded.
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }

    @Test
    fun noGpsFixAtAll_discardsTrip() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        clock.now = base + 30_000
        recorder.onDeviceDisconnected()
        clock.now = base + 30_000 + graceMs
        recorder.onGraceTimerExpired()

        // 0 km is parked noise: the state machine finishes but nothing is
        // stored (TrackingPolicy.MIN_TRIP_DISTANCE_KM, plan.md Step 12).
        assertPhase(recorder, TripRecorder.Phase.IDLE)
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }

    @Test
    fun parkedTrip_finishesWithoutSavingRow() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        // GPS jitter below the displacement gate: distance stays 0.
        recorder.onLocationSample(sampleAt(base + 30_000, 0.00002))
        clock.now = base + 30_000
        recorder.onDeviceDisconnected()
        clock.now = base + 30_000 + graceMs
        recorder.onGraceTimerExpired()

        assertPhase(recorder, TripRecorder.Phase.IDLE)
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }

    @Test
    fun tripShorterThanThreshold_isDiscarded() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        recorder.onDeviceConnected("Car")
        // Total movement 40 m (< 50 m threshold): anchor fix, then one 40 m step.
        val offset40m = 40.0 / 111_190.0
        recorder.onLocationSample(sampleAt(base + 30_000, 0.0))          // anchor (0 km)
        recorder.onLocationSample(sampleAt(base + 60_000, offset40m))    // +0.04 km
        clock.now = base + 60_000
        recorder.onDeviceDisconnected()
        clock.now = base + 60_000 + graceMs
        recorder.onGraceTimerExpired()
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }

    @Test
    fun tripAboveThreshold_isSaved() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        recorder.onDeviceConnected("Car")
        // Total movement 60 m (≥ 50 m threshold).
        val offset60m = 60.0 / 111_190.0
        recorder.onLocationSample(sampleAt(base + 30_000, 0.0))          // anchor (0 km)
        recorder.onLocationSample(sampleAt(base + 60_000, offset60m))    // +0.06 km
        clock.now = base + 60_000
        recorder.onDeviceDisconnected()
        clock.now = base + 60_000 + graceMs
        recorder.onGraceTimerExpired()
        assertEquals(1, recorder.takeFinishedTrips().size)
    }

    @Test
    fun snapshot_reflectsLiveState() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car bluetooth")
        recorder.onLocationSample(sampleAt(base + 30_000, step500m))

        val snap = recorder.snapshot()
        assertEquals(TripRecorder.Phase.RECORDING, snap.phase)
        assertEquals(0.0, snap.distanceKm, 0.0) // first fix only anchors
        assertEquals(base, snap.startEpochMillis)
        assertEquals("Car bluetooth", snap.deviceName)
        assertEquals(TripOrigin.AUTO, snap.origin)
    }

    @Test
    fun updatedGracePeriod_isUsedForNextDisconnect() {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        recorder.updateGracePeriodMillis(60_000L) // user set 1 minute

        recorder.onDeviceConnected("Car")
        clock.now = base + 5_000
        recorder.onDeviceDisconnected()

        // 59 s after disconnect: still in grace (not yet 60 s).
        clock.now = base + 5_000 + 59_000
        recorder.onGraceTimerExpired()
        assertEquals(TripRecorder.Phase.GRACE, recorder.snapshot().phase)

        // 1 s later: grace over, trip finished (and discarded: 0 km).
        clock.now = base + 5_000 + 60_000
        recorder.onGraceTimerExpired()
        assertEquals(TripRecorder.Phase.IDLE, recorder.snapshot().phase)
        assertTrue(recorder.takeFinishedTrips().isEmpty())
    }
}
