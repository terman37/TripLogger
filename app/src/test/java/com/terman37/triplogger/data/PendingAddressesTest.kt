package com.terman37.triplogger.data

import com.terman37.triplogger.core.ReverseGeocodeResult
import com.terman37.triplogger.core.TripOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingAddressesTest {

    private fun trip(
        id: Long = 1L,
        startLat: Double? = 48.85,
        startLng: Double? = 2.35,
        startStreet: String? = null,
        startCity: String? = null,
        endLat: Double? = 48.87,
        endLng: Double? = 2.29,
        endStreet: String? = null,
        endCity: String? = null,
    ) = Trip(
        id = id,
        startEpochMillis = 1000L,
        startLat = startLat,
        startLng = startLng,
        startStreet = startStreet,
        startCity = startCity,
        endEpochMillis = 2000L,
        endLat = endLat,
        endLng = endLng,
        endStreet = endStreet,
        endCity = endCity,
        distanceKm = 12.3,
        origin = TripOrigin.AUTO,
    )

    @Test
    fun bothSidesWithoutAddress_arePending() {
        val requests = PendingAddresses.pendingRequests(listOf(trip(id = 7)))
        assertEquals(2, requests.size)
        assertEquals(7, requests[0].tripId)
        assertEquals(PendingAddresses.End.START, requests[0].end)
        assertEquals(7, requests[1].tripId)
        assertEquals(PendingAddresses.End.END, requests[1].end)
    }

    @Test
    fun completedSides_areNotPending() {
        val trips = listOf(
            trip(startStreet = "1 Main St", startCity = "Springfield", endStreet = "2 Oak Rd", endCity = "Shelbyville"),
        )
        assertTrue(PendingAddresses.pendingRequests(trips).isEmpty())
    }

    @Test
    fun missingOnlyCity_stillPending() {
        // Street alone is not enough for the export (address includes the city) → retry.
        // End side is complete so only START is pending.
        val trips = listOf(
            trip(
                startStreet = "1 Main St", startCity = null,
                endStreet = "2 Oak Rd", endCity = "Shelbyville",
            ),
        )
        val requests = PendingAddresses.pendingRequests(trips)
        assertEquals(1, requests.size)
        assertEquals(PendingAddresses.End.START, requests.single().end)
    }

    @Test
    fun sideWithoutCoordinates_isNotPending() {
        val trips = listOf(trip(startLat = null, startLng = null))
        val requests = PendingAddresses.pendingRequests(trips)
        assertTrue(requests.none { it.end == PendingAddresses.End.START })
        assertEquals(1, requests.size) // only the END side remains
    }

    @Test
    fun apply_fillsAddressesOnSuccess() {
        val t = trip(id = 3)
        val request = PendingAddresses.Request(3, PendingAddresses.End.START, 48.85, 2.35)
        val result = ReverseGeocodeResult(street = "12 Rue de Rivoli", city = "Paris")

        val updated = PendingAddresses.apply(t, request, result)

        assertEquals("12 Rue de Rivoli", updated.startStreet)
        assertEquals("Paris", updated.startCity)
        assertEquals(3, updated.id)
        // End side untouched.
        assertTrue(updated.endStreet == null && updated.endCity == null)
    }

    @Test
    fun apply_keepsExistingPartsWhenResultLacksThem() {
        val t = trip(id = 3, startStreet = "12 Rue de Rivoli", startCity = null)
        val request = PendingAddresses.Request(3, PendingAddresses.End.START, 48.85, 2.35)

        // Geocoder found only a city this time.
        val updated = PendingAddresses.apply(t, request, ReverseGeocodeResult(null, "Paris"))

        assertEquals("12 Rue de Rivoli", updated.startStreet) // preserved
        assertEquals("Paris", updated.startCity)
    }

    @Test
    fun apply_nullResult_keepsTripPending() {
        val t = trip(id = 3)
        val request = PendingAddresses.Request(3, PendingAddresses.End.START, 48.85, 2.35)

        val updated = PendingAddresses.apply(t, request, null)

        assertEquals(t, updated)
        assertEquals(2, PendingAddresses.pendingRequests(listOf(updated)).size) // still pending
    }
}
