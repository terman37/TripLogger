package com.terman37.triplogger.core

/**
 * The trip session state machine. Inputs are events (Bluetooth connect/
 * disconnect, GPS samples, manual start/stop, grace timer) and outputs are
 * finished trips ([TripDraft]) that the caller persists.
 *
 * States (see README "How a trip is recorded" and todo.md decisions):
 *
 * ```
 *                deviceConnected / manualStart
 *     IDLE ────────────────────────────────▶ RECORDING
 *      ▲                                        │
 *      │ graceTimerExpired                      │ deviceDisconnected
 *      │ (auto trip)                            ▼
 *      │                                  GRACE PERIOD
 *      │                                        │
 *      └────── deviceConnected (reconnect) ─────┘  → resumes RECORDING
 * ```
 *
 * - RECORDING: GPS samples are filtered and accumulated as distance.
 * - GRACE PERIOD: a Bluetooth disconnect only ENDS a trip after the grace
 *   period (configurable, default 3 min, todo.md). Reconnecting within the
 *   period resumes the SAME trip (same start time, accumulated distance).
 *   A reconnect after the timer expired starts a new trip, because the old
 *   one was already finished.
 * - A MANUAL trip (fallback button) has no Bluetooth device: it ignores
 *   disconnects and ends only via manualStop.
 *
 * The class is pure Kotlin: no Android types, clock injected → fully
 * unit-testable on the JVM.
 */
class TripRecorder(
    private val clock: Clock,
    private val gracePeriodMillis: Long = 3 * 60_000L,
) {
    // The active recording session, or null when idle.
    private var session: Session? = null

    // When the grace period ends (epoch ms), only meaningful in GRACE state.
    private var graceUntilMillis: Long? = null

    // Trips finished but not yet collected by the caller.
    private val finishedTrips = mutableListOf<TripDraft>()

    /** Mutable trip state; recreated on every new trip (fresh distance
     * filter = fresh anchor). */
    private class Session(
        val startEpochMillis: Long,
        val origin: TripOrigin,
        val deviceName: String?,
        var startLat: Double? = null,
        var startLng: Double? = null,
        var distanceKm: Double = 0.0,
        // Last fix that PASSED the filters: the "current position" used as
        // trip end position.
        var lastKeptSample: GpsSample? = null,
        val filter: LocationFilter = LocationFilter(),
    )

    /**
     * Snapshot of the current state for the UI (status card on Home).
     */
    data class Snapshot(
        val phase: Phase,
        /** Accumulated km so far (0 when idle). */
        val distanceKm: Double,
        /** Start time of the active trip (null when idle). */
        val startEpochMillis: Long?,
        val origin: TripOrigin?,
        /** Name of the connected device, or null for manual trips. */
        val deviceName: String?,
    )

    enum class Phase { IDLE, RECORDING, GRACE }

    fun snapshot(): Snapshot {
        val s = session
        return if (s == null) {
            Snapshot(Phase.IDLE, 0.0, null, null, null)
        } else {
            Snapshot(
                phase = if (graceUntilMillis == null) Phase.RECORDING else Phase.GRACE,
                distanceKm = s.distanceKm,
                startEpochMillis = s.startEpochMillis,
                origin = s.origin,
                deviceName = s.deviceName,
            )
        }
    }

    /** Collects trips finished since the last call (used by the service to
     * persist them). */
    fun takeFinishedTrips(): List<TripDraft> =
        finishedTrips.toList().also { finishedTrips.clear() }

    // --- Events -----------------------------------------------------------

    /** A Bluetooth device (already filtered to registered ones by the caller)
     * connected. */
    fun onDeviceConnected(deviceName: String) {
        val s = session
        if (s == null) {
            // New auto trip.
            startTrip(clock.nowMillis(), TripOrigin.AUTO, deviceName)
        } else if (graceUntilMillis != null) {
            // Reconnect inside the grace period: resume the SAME trip (start
            // time and accumulated distance are preserved).
            graceUntilMillis = null
        }
        // Recording already active: another device connected — ignore.
    }

    /** A Bluetooth device disconnected. Ends the trip only after the grace
     * period (see class docs). */
    fun onDeviceDisconnected() {
        val s = session ?: return
        // Manual trips have no device: a disconnect is not about them.
        if (s.origin != TripOrigin.AUTO) return
        // Only a recording (not an already-graceful) trip enters grace.
        if (graceUntilMillis != null) return
        graceUntilMillis = clock.nowMillis() + gracePeriodMillis
    }

    /**
     * New GPS fix while recording. Ignored outside RECORDING (also during the
     * grace period: the car is not connected, so no movement should count).
     */
    fun onLocationSample(sample: GpsSample) {
        val s = session ?: return
        if (graceUntilMillis != null) return

        when (val decision = s.filter.process(sample)) {
            is FilterDecision.Ignored -> Unit
            is FilterDecision.Kept -> {
                s.distanceKm += decision.distanceKm
                s.lastKeptSample = sample
                // First kept fix defines the trip start position.
                if (s.startLat == null) {
                    s.startLat = sample.latitude
                    s.startLng = sample.longitude
                }
            }
        }
    }

    /** Fallback start button (no Bluetooth). */
    fun onManualStart() {
        if (session != null) return // already recording (any kind)
        startTrip(clock.nowMillis(), TripOrigin.MANUAL, deviceName = null)
    }

    /** Fallback stop button; ends any active trip immediately. */
    fun onManualStop() {
        if (session == null) return
        finishTrip()
    }

    /** Called by the service when the grace timer elapses. */
    fun onGraceTimerExpired() {
        // Ignore if not in grace, or if the event arrived before the period
        // actually ended (spurious early timer).
        val s = session ?: return
        val until = graceUntilMillis ?: return
        if (clock.nowMillis() < until) return
        finishTrip()
    }

    // --- Internals --------------------------------------------------------

    private fun startTrip(now: Long, origin: TripOrigin, deviceName: String?) {
        session = Session(
            startEpochMillis = now,
            origin = origin,
            deviceName = deviceName,
        )
        graceUntilMillis = null
    }

    private fun finishTrip() {
        val s = session ?: return
        val endPosition = s.lastKeptSample
        finishedTrips += TripDraft(
            startEpochMillis = s.startEpochMillis,
            startLat = s.startLat,
            startLng = s.startLng,
            endEpochMillis = clock.nowMillis(),
            endLat = endPosition?.latitude,
            endLng = endPosition?.longitude,
            distanceKm = s.distanceKm,
            origin = s.origin,
        )
        session = null
        graceUntilMillis = null
    }
}
