package com.terman37.triplogger.session

import com.terman37.triplogger.core.TripRecorder

/**
 * In-memory [TripSessionStore] for JVM tests.
 */
class FakeTripSessionStore(
    var stored: TripRecorder.ActiveTrip? = null,
) : TripSessionStore {

    override fun load(): TripRecorder.ActiveTrip? = stored

    override fun save(trip: TripRecorder.ActiveTrip) {
        stored = trip
    }

    override fun clear() {
        stored = null
    }
}
