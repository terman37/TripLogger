package com.terman37.triplogger.data

/**
 * In-memory [TripDao] for JVM tests. Implements the Room interface by hand so
 * TripRepository can be tested without an emulator.
 *
 * Guarded by a plain lock (synchronized): the repository switches to
 * Dispatchers.IO internally, so calls may arrive from other threads. No
 * suspension happens inside the lock — the operations are pure list edits.
 */
class FakeTripDao : TripDao {

    private val lock = Any()
    private val trips = mutableListOf<Trip>()
    private var nextId = 1L

    override suspend fun insert(trip: Trip): Long = synchronized(lock) {
        val copy = trip.copy(id = nextId++)
        trips += copy
        copy.id
    }

    override suspend fun tripsBetween(fromInclusive: Long, untilExclusive: Long): List<Trip> =
        synchronized(lock) {
            trips.filter { it.startEpochMillis in fromInclusive until untilExclusive }
                .sortedBy { it.startEpochMillis }
        }

    override suspend fun tripsSince(sinceEpochMillis: Long): List<Trip> =
        synchronized(lock) {
            trips.filter { it.startEpochMillis >= sinceEpochMillis }
                .sortedByDescending { it.startEpochMillis }
        }

    override suspend fun allTrips(): List<Trip> =
        synchronized(lock) { trips.sortedBy { it.startEpochMillis } }

    override suspend fun delete(trip: Trip) {
        synchronized(lock) { trips.removeAll { it.id == trip.id } }
    }

    override suspend fun update(trip: Trip) {
        synchronized(lock) {
            val index = trips.indexOfFirst { it.id == trip.id }
            if (index >= 0) trips[index] = trip
        }
    }

    /** Test helper: current contents. */
    fun snapshot(): List<Trip> = synchronized(lock) { trips.toList() }
}
