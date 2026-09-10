package com.terman37.triplogger.data

import com.terman37.triplogger.core.ReverseGeocodeResult
import com.terman37.triplogger.core.TripDraft
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.geocoding.GeocoderClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking


class TripRepositoryTest {

    private val home = 48.8566 to 2.3522
    private val office = 48.8738 to 2.2950

    private fun draft() = TripDraft(
        startEpochMillis = 1_000L,
        startLat = home.first,
        startLng = home.second,
        endEpochMillis = 3_600_000L,
        endLat = office.first,
        endLng = office.second,
        distanceKm = 12.4,
        origin = TripOrigin.AUTO,
    )


    @Test
    fun saveTrip_geocodesBothSidesOnline() = runBlocking {
        val dao = FakeTripDao()
        val geocoder = FakeGeocoder(
            mapOf(
                home to ReverseGeocodeResult("12 Rue de Rivoli", "Paris"),
                office to ReverseGeocodeResult("5 Avenue des Champs-Élysées", "Paris"),
            ),
        )
        val repo = TripRepository(dao, geocoder)

        val id = repo.saveTrip(draft())

        val saved = dao.snapshot().single()
        assertTrue(id > 0)
        assertEquals("12 Rue de Rivoli", saved.startStreet)
        assertEquals("Paris", saved.startCity)
        assertEquals("5 Avenue des Champs-Élysées", saved.endStreet)
        assertEquals("Paris", saved.endCity)
        assertEquals(12.4, saved.distanceKm, 0.0)
        assertEquals(2, geocoder.calls)
    }

    @Test
    fun saveTrip_offline_storesNullAddresses() = runBlocking {
        val dao = FakeTripDao()
        val repo = TripRepository(dao, FakeGeocoder(emptyMap()))

        repo.saveTrip(draft())

        val saved = dao.snapshot().single()
        assertNull(saved.startStreet)
        assertNull(saved.startCity)
        assertNull(saved.endStreet)
        assertNull(saved.endCity)
        // Coordinates kept: the lazy retry can geocode later.
        assertEquals(home.first, saved.startLat!!, 0.0)
        assertEquals(office.first, saved.endLat!!, 0.0)
    }

    @Test
    fun saveTrip_withoutCoordinates_neverCallsGeocoder() = runBlocking {
        val dao = FakeTripDao()
        val geocoder = FakeGeocoder(emptyMap())
        val repo = TripRepository(dao, geocoder)

        repo.saveTrip(draft().copy(startLat = null, startLng = null, endLat = null, endLng = null))

        assertEquals(0, geocoder.calls)
        val saved = dao.snapshot().single()
        assertNull(saved.startLat)
    }

    @Test
    fun retryPendingAddresses_fillsMissingAndReportsCount() = runBlocking {
        val dao = FakeTripDao()
        val id = dao.insert(
            Trip(
                startEpochMillis = 100L, startLat = home.first, startLng = home.second,
                endEpochMillis = 200L, endLat = office.first, endLng = office.second,
                distanceKm = 1.0, origin = TripOrigin.AUTO,
            ),
        )
        val repo = TripRepository(
            dao,
            FakeGeocoder(mapOf(home to ReverseGeocodeResult("1 Home St", "Paris"))),
        )

        val changed = repo.retryPendingAddresses()

        assertEquals(1, changed) // only the START side had a known coordinate→result
        val saved = dao.snapshot().single()
        assertEquals("1 Home St", saved.startStreet)
        assertEquals(id, saved.id)
    }

    @Test
    fun retryPendingAddresses_secondRun_isNoOp() = runBlocking {
        val dao = FakeTripDao()
        dao.insert(
            Trip(
                startEpochMillis = 100L, startLat = home.first, startLng = home.second,
                endEpochMillis = 200L, endLat = office.first, endLng = office.second,
                distanceKm = 1.0, origin = TripOrigin.AUTO,
            ),
        )
        val geocoder = FakeGeocoder(
            mapOf(
                home to ReverseGeocodeResult("1 Home St", "Paris"),
                office to ReverseGeocodeResult("2 Work Rd", "Paris"),
            ),
        )
        val repo = TripRepository(dao, geocoder)

        assertEquals(2, repo.retryPendingAddresses())
        assertEquals(0, repo.retryPendingAddresses()) // nothing pending anymore
    }

    @Test
    fun deleteTripById_removesOnlyThatTrip() = runBlocking {
        val dao = FakeTripDao()
        dao.insert(
            Trip(
                startEpochMillis = 100L, startLat = null, startLng = null,
                endEpochMillis = 200L, endLat = null, endLng = null,
                distanceKm = 1.0, origin = TripOrigin.MANUAL,
            ),
        )
        val kept = dao.insert(
            Trip(
                startEpochMillis = 300L, startLat = null, startLng = null,
                endEpochMillis = 400L, endLat = null, endLng = null,
                distanceKm = 2.0, origin = TripOrigin.MANUAL,
            ),
        )
        val repo = TripRepository(dao, FakeGeocoder(emptyMap()))

        repo.deleteTripById(dao.snapshot().first { it.id != kept }.id)

        assertEquals(listOf(kept), dao.snapshot().map { it.id })
    }

    @Test
    fun tripsBetween_delegatesToDao() = runBlocking {
        val dao = FakeTripDao()
        dao.insert(tripRow(start = 100L))
        dao.insert(tripRow(start = 200L))
        dao.insert(tripRow(start = 300L))
        val repo = TripRepository(dao, FakeGeocoder(emptyMap()))

        assertEquals(listOf(200L, 300L), repo.tripsBetween(150L, 350L).map { it.startEpochMillis })
    }

    private fun tripRow(start: Long) = Trip(
        startEpochMillis = start, startLat = null, startLng = null,
        endEpochMillis = start + 1000L, endLat = null, endLng = null,
        distanceKm = 0.0, origin = TripOrigin.MANUAL,
    )
}
