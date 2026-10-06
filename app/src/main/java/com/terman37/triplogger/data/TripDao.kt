package com.terman37.triplogger.data

import androidx.room.Dao
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
     * Newest-first trips since a time as a reactive Flow: Room re-emits
     * whenever the table changes, so the Home list refreshes itself after
     * insert/delete (no manual reloads).
     */
    @Query(
        "SELECT * FROM trips WHERE startEpochMillis >= :sinceEpochMillis " +
            "ORDER BY startEpochMillis DESC",
    )
    fun tripsSinceFlow(sinceEpochMillis: Long): kotlinx.coroutines.flow.Flow<List<Trip>>

    /**
     * Every trip, oldest first. Used by the lazy address retry:
     * pending addresses may belong to any past trip.
     */
    @Query("SELECT * FROM trips ORDER BY startEpochMillis ASC")
    suspend fun allTrips(): List<Trip>

    /** Removes a trip by id (UI calls this with the row id). */
    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Removes every trip whose start falls in [fromInclusive, untilExclusive)
     * — the Report page "cleanup the displayed trips" action. Same boundary
     * semantics as [tripsBetween].
     */
    @Query(
        "DELETE FROM trips WHERE startEpochMillis >= :fromInclusive " +
            "AND startEpochMillis < :untilExclusive",
    )
    suspend fun deleteBetween(fromInclusive: Long, untilExclusive: Long)

    /**
     * Updates an existing trip (same id). Used to fill in addresses after a
     * delayed reverse geocode (lazy retry).
     */
    @Update
    suspend fun update(trip: Trip)

    /**
     * Number of trips starting at exactly this instant. Read-only dedupe key for
     * the recovery path: a trip that was already inserted must not be stored a
     * second time if the app crashed before clearing the active-trip store.
     */
    @Query("SELECT COUNT(*) FROM trips WHERE startEpochMillis = :startEpochMillis")
    suspend fun countWithStart(startEpochMillis: Long): Int
}
