package com.terman37.triplogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Database queries for trips. Room checks the @Query SQL at compile time: a
 * typo in a column name breaks the build instead of failing at runtime.
 *
 * All functions are suspend functions: Room executes them on its own thread
 * pool, callers must be in a coroutine.
 */
@Dao
interface TripDao {

    /**
     * Inserts a finished trip. Returns the generated row id.
     */
    @Insert
    suspend fun insert(trip: Trip): Long

    /**
     * Trips whose START falls in [fromInclusive, untilExclusive) (epoch
     * millis), oldest first. The caller converts local dates to millis:
     * a report "from Aug 1 to Aug 31" queries from = Aug 1 00:00 local and
     * until = Sep 1 00:00 local, so Aug 31 23:59 is included.
     */
    @Query(
        "SELECT * FROM trips WHERE startEpochMillis >= :fromInclusive " +
            "AND startEpochMillis < :untilExclusive " +
            "ORDER BY startEpochMillis ASC",
    )
    suspend fun tripsBetween(fromInclusive: Long, untilExclusive: Long): List<Trip>

    /**
     * Newest-first list of trips that started at or after [sinceEpochMillis].
     * Used by the Home screen to show today's and yesterday's trips.
     */
    @Query(
        "SELECT * FROM trips WHERE startEpochMillis >= :sinceEpochMillis " +
            "ORDER BY startEpochMillis DESC",
    )
    suspend fun tripsSince(sinceEpochMillis: Long): List<Trip>

    /**
     * Every trip, oldest first. Used by the lazy address retry (todo.md):
     * pending addresses may belong to any past trip.
     */
    @Query("SELECT * FROM trips ORDER BY startEpochMillis ASC")
    suspend fun allTrips(): List<Trip>

    /**
     * Removes one trip (UI.md: delete from the expanded row on Home).
     */
    @Delete
    suspend fun delete(trip: Trip)

    /**
     * Updates an existing trip (same id). Used to fill in addresses after a
     * delayed reverse geocode (todo.md lazy retry).
     */
    @Update
    suspend fun update(trip: Trip)
}
