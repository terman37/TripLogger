package com.terman37.triplogger.session

import com.terman37.triplogger.core.TrackingPolicy
import com.terman37.triplogger.core.TripDraft
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.TripRepository

/**
 * Finishes a trip that was still being recorded when the process died (reboot,
 * force-stop, crash).
 *
 * The trip is NOT resumed: it is closed at the last recorded position and saved,
 * so the data is kept (owner decision). The next drive becomes a new trip through
 * the normal recorder path.
 *
 * Kept separate from the recorder (pure state machine) and the service (Android
 * glue) so the decision logic — minimum distance and duplicate protection — is
 * unit-testable.
 */
class TripRecovery(
    private val store: TripSessionStore,
    private val repository: TripRepository,
) {

    /**
     * Loads the stored session AND clears it. Call once at process start, before
     * the service can begin a new trip: clearing first means the store then
     * belongs to this process, and a new recording cannot overwrite the
     * recovered one while it is being saved.
     */
    fun takePending(): TripRecorder.ActiveTrip? {
        val active = store.load() ?: return null
        store.clear()
        return active
    }

    /**
     * Builds the trip to store from the persisted session, ending at the last
     * recorded position/time. Null when the trip is too short to keep, exactly
     * like the normal finish path ([TrackingPolicy.MIN_TRIP_DISTANCE_KM]).
     */
    fun toDraft(active: TripRecorder.ActiveTrip): TripDraft? {
        if (active.distanceKm < TrackingPolicy.MIN_TRIP_DISTANCE_KM) return null
        return TripDraft(
            startEpochMillis = active.startEpochMillis,
            startLat = active.startLat,
            startLng = active.startLng,
            endEpochMillis = active.lastEpochMillis,
            endLat = active.lastLat,
            endLng = active.lastLng,
            distanceKm = active.distanceKm,
            origin = active.origin,
        )
    }

    /**
     * Saves the recovered trip unless it is too short or already stored (the app
     * may have crashed after inserting the row but before clearing the store).
     *
     * @return true when a new row was written.
     */
    suspend fun finish(active: TripRecorder.ActiveTrip): Boolean {
        val draft = toDraft(active) ?: return false
        if (repository.hasTripStartingAt(draft.startEpochMillis)) return false
        repository.saveTrip(draft)
        return true
    }
}
