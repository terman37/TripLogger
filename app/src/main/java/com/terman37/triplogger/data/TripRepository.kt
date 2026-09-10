package com.terman37.triplogger.data

import com.terman37.triplogger.core.TripDraft
import com.terman37.triplogger.geocoding.GeocoderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The single place that reads and writes trips, used by the trip service and
 * the UI. Pure Kotlin (Room's [TripDao] is behind the interface, the geocoder
 * behind [GeocoderClient]) so it is testable on the JVM with fakes.
 */
class TripRepository(
    private val dao: TripDao,
    private val geocoder: GeocoderClient,
) {

    /**
     * Persists a finished trip (from the recorder). Geocoding happens HERE,
     * once per side: online → store address with the trip; offline → store
     * coordinates only, the lazy retry fills the address later (todo.md).
     */
    suspend fun saveTrip(draft: TripDraft): Long = withContext(Dispatchers.IO) {
        // Build the entity with the coordinates and NO addresses yet.
        val entity = Trip(
            startEpochMillis = draft.startEpochMillis,
            startLat = draft.startLat,
            startLng = draft.startLng,
            startStreet = null,
            startCity = null,
            endEpochMillis = draft.endEpochMillis,
            endLat = draft.endLat,
            endLng = draft.endLng,
            endStreet = null,
            endCity = null,
            distanceKm = draft.distanceKm,
            origin = draft.origin,
        )

        // Try to resolve addresses immediately; null results keep the fields
        // null (still pending). Geocoder calls are blocking network I/O, hence
        // the IO dispatcher.
        val startAddress = resolveAddress(draft.startLat, draft.startLng)
        val endAddress = resolveAddress(draft.endLat, draft.endLng)

        dao.insert(
            entity.copy(
                startStreet = startAddress?.street ?: entity.startStreet,
                startCity = startAddress?.city ?: entity.startCity,
                endStreet = endAddress?.street ?: entity.endStreet,
                endCity = endAddress?.city ?: entity.endCity,
            ),
        )
    }

    /**
     * Lazy retry (todo.md): geocodes every side of every trip whose address is
     * still missing, then persists the changes. Runs at app open and again at
     * report generation.
     *
     * @return number of stored rows updated (one trip can be updated twice when
     *   both its start and end were pending).
     */
    suspend fun retryPendingAddresses(): Int = withContext(Dispatchers.IO) {
        // Work on a mutable map so that when one trip has BOTH sides pending,
        // the second side applies on top of the first side's result instead of
        // overwriting it with the original row.
        val byId = dao.allTrips().associateBy { it.id }.toMutableMap()
        var updatedCount = 0

        for (request in PendingAddresses.pendingRequests(byId.values.toList())) {
            val trip = byId[request.tripId] ?: continue
            val result = geocoder.reverse(request.lat, request.lng)
            val updated = PendingAddresses.apply(trip, request, result)
            if (updated != trip) {
                dao.update(updated)
                byId[request.tripId] = updated
                updatedCount++
            }
        }
        updatedCount
    }

    /** Trips whose start falls in the range, oldest first (Report screen). */
    suspend fun tripsBetween(fromInclusive: Long, untilExclusive: Long): List<Trip> =
        dao.tripsBetween(fromInclusive, untilExclusive)

    /** Reactive version of [tripsSince]; the UI collects this. */
    fun tripsSinceFlow(sinceEpochMillis: Long): kotlinx.coroutines.flow.Flow<List<Trip>> =
        dao.tripsSinceFlow(sinceEpochMillis)

    /** Deletes a trip by its id (Home expand → Delete). */
    suspend fun deleteTripById(id: Long) {
        dao.deleteById(id)
    }

    /** Deletes all trips in the range (Report cleanup action). */
    suspend fun deleteTripsBetween(fromInclusive: Long, untilExclusive: Long) {
        dao.deleteBetween(fromInclusive, untilExclusive)
    }

    private fun resolveAddress(lat: Double?, lng: Double?): com.terman37.triplogger.core.ReverseGeocodeResult? {
        if (lat == null || lng == null) return null // no coordinates → nothing to geocode
        return geocoder.reverse(lat, lng)
    }
}
