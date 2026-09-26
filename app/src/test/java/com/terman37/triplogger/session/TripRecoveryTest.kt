package com.terman37.triplogger.session

import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.FakeGeocoder
import com.terman37.triplogger.data.FakeTripDao
import com.terman37.triplogger.data.TripRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recovery of a trip interrupted by a reboot: close it at the last recorded
 * position, keep the usual 50 m rule, and never store it twice.
 */
class TripRecoveryTest {

    private val home = 48.8566 to 2.3522
    private val destination = 48.9 to 2.4

    private fun active(distanceKm: Double = 12.4) = TripRecorder.ActiveTrip(
        startEpochMillis = 1_000L,
        origin = TripOrigin.AUTO,
        startLat = home.first,
        startLng = home.second,
        distanceKm = distanceKm,
        lastLat = destination.first,
        lastLng = destination.second,
        lastEpochMillis = 300_000L,
    )

    private fun recovery(
        dao: FakeTripDao,
        store: FakeTripSessionStore = FakeTripSessionStore(),
    ) = TripRecovery(store, TripRepository(dao, FakeGeocoder()))

    @Test
    fun takePending_returnsStoredTripAndClearsIt() {
        val store = FakeTripSessionStore(stored = active())
        val pending = recovery(FakeTripDao(), store).takePending()

        assertEquals(active(), pending)
        assertNull(store.stored)
    }

    @Test
    fun takePending_whenNothingStored_isNull() {
        assertNull(recovery(FakeTripDao()).takePending())
    }

    @Test
    fun toDraft_endsAtLastRecordedPosition() {
        val draft = recovery(FakeTripDao()).toDraft(active())
        assertEquals(1_000L, draft?.startEpochMillis)
        assertEquals(300_000L, draft?.endEpochMillis)
        assertEquals(destination.first, draft?.endLat ?: -1.0, 1e-9)
        assertEquals(destination.second, draft?.endLng ?: -1.0, 1e-9)
        assertEquals(12.4, draft?.distanceKm ?: -1.0, 1e-9)
        assertEquals(TripOrigin.AUTO, draft?.origin)
    }

    @Test
    fun toDraft_belowMinimumDistance_isDiscarded() {
        assertNull(recovery(FakeTripDao()).toDraft(active(distanceKm = 0.02)))
    }

    @Test
    fun finish_savesTheRecoveredTrip() = runBlocking {
        val dao = FakeTripDao()
        assertTrue(recovery(dao).finish(active()))

        val saved = dao.snapshot().single()
        assertEquals(1_000L, saved.startEpochMillis)
        assertEquals(300_000L, saved.endEpochMillis)
        assertEquals(12.4, saved.distanceKm, 1e-9)
    }

    @Test
    fun finish_whenAlreadyStored_doesNotInsertADuplicate() = runBlocking {
        val dao = FakeTripDao()
        val recovery = recovery(dao)
        recovery.finish(active())

        assertFalse(recovery.finish(active()))
        assertEquals(1, dao.snapshot().size)
    }

    @Test
    fun finish_belowMinimumDistance_writesNothing() = runBlocking {
        val dao = FakeTripDao()
        assertFalse(recovery(dao).finish(active(distanceKm = 0.01)))
        assertTrue(dao.snapshot().isEmpty())
    }
}
