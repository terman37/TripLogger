package com.terman37.triplogger.session

import com.terman37.triplogger.core.TripRecorder

/**
 * Stores the trip that is currently being recorded, so it survives the process
 * (reboot / force-stop / crash).
 *
 * WHY AN INTERFACE: the recorder and the service must keep working without an
 * Android dependency, and tests need an in-memory fake. The real implementation
 * writes app-private SharedPreferences.
 */
interface TripSessionStore {

    /** The in-progress trip, or null when none is stored. */
    fun load(): TripRecorder.ActiveTrip?

    /** Overwrites the stored trip with the latest state. */
    fun save(trip: TripRecorder.ActiveTrip)

    /** Removes the stored trip (called once the finished trip is saved). */
    fun clear()
}
