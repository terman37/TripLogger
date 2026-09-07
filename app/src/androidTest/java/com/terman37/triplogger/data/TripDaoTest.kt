package com.terman37.triplogger.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.terman37.triplogger.core.TripOrigin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO behavior tests against a real (in-memory) Room database.
 *
 * These are INSTRUMENTED tests: they need an emulator or device because Room
 * runs on Android. Run with:
 *   ./gradlew :app:connectedDebugAndroidTest
 *
 * An in-memory database behaves like the real one but lives only for the test
 * and needs no file cleanup.
 */
@RunWith(AndroidJUnit4::class)
class TripDaoTest {

    private lateinit var db: TripDatabase
    private lateinit var dao: TripDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries() // test convenience; app code never does this
            .build()
        dao = db.tripDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Small helper building a trip with a fixed id-less shape; dayOffsetMillis
    // shifts the start time so tests can place trips on different days.
    private fun tripAt(startEpochMillis: Long, distanceKm: Double = 12.4) = Trip(
        startEpochMillis = startEpochMillis,
        startLat = 48.8566,
        startLng = 2.3522,
        startStreet = "Rue de Rivoli",
        startCity = "Paris",
        endEpochMillis = startEpochMillis + 3_600_000L, // + 1 hour
        endLat = 48.8738,
        endLng = 2.2950,
        endStreet = "Avenue des Champs-Élysées",
        endCity = "Paris",
        distanceKm = distanceKm,
        origin = TripOrigin.AUTO,
    )

    @Test
    fun insert_returnsPositiveId() {
        val id = runBlockingTest { dao.insert(tripAt(0L)) }
        assertTrue(id > 0)
    }

    @Test
    fun tripsBetween_filtersByStartAndOrdersOldestFirst() {
        runBlockingTest {
            dao.insert(tripAt(1_000L))  // in range, oldest
            dao.insert(tripAt(2_000L))  // in range
            dao.insert(tripAt(9_999L))  // in range (untilExclusive)
            dao.insert(tripAt(10_000L)) // out of range: equals untilExclusive
            dao.insert(tripAt(-500L))   // out of range: before fromInclusive

            val result = dao.tripsBetween(fromInclusive = 0L, untilExclusive = 10_000L)

            assertEquals(listOf(1_000L, 2_000L, 9_999L), result.map { it.startEpochMillis })
        }
    }

    @Test
    fun tripsBetween_returnsEmptyWhenNothingMatches() {
        runBlockingTest {
            val result = dao.tripsBetween(fromInclusive = 0L, untilExclusive = 10L)
            assertEquals(emptyList<Trip>(), result)
        }
    }

    @Test
    fun tripsSince_returnsNewestFirst() {
        runBlockingTest {
            dao.insert(tripAt(1_000L))
            dao.insert(tripAt(3_000L))
            dao.insert(tripAt(2_000L))

            val result = dao.tripsSince(sinceEpochMillis = 1_500L)

            assertEquals(listOf(3_000L, 2_000L), result.map { it.startEpochMillis })
        }
    }

    @Test
    fun delete_removesOnlyGivenTrip() {
        runBlockingTest {
            val keepId = dao.insert(tripAt(1_000L))
            val deleteId = dao.insert(tripAt(2_000L))

            dao.delete(dao.tripsBetween(0L, 100_000L).first { it.id == deleteId })

            val remaining = dao.tripsBetween(0L, 100_000L)
            assertEquals(listOf(keepId), remaining.map { it.id })
        }
    }

    @Test
    fun converter_roundTripsOriginEnum() {
        val converter = TripOriginConverter()
        assertEquals("AUTO", converter.toString(TripOrigin.AUTO))
        assertEquals(TripOrigin.MANUAL, converter.fromString("MANUAL"))
        // valueOf throws for names that are not in the enum (never stored by
        // our own code, but guards against corrupt data).
        assertThrows(IllegalArgumentException::class.java) {
            converter.fromString("UNKNOWN")
        }
    }

    // Runs a suspend DAO call on the test thread. (Room test helper for
    // coroutines would need runTest; plain runBlocking is enough here because
    // the in-memory DB allows main-thread queries.)
    @Test
    fun update_changesAddressAndKeepsId() {
        runBlockingTest {
            // A trip stored without addresses (geocoding was offline at end).
            val noAddress = tripAt(1_000L).copy(startStreet = null, startCity = null)
            val id = dao.insert(noAddress)

            val withAddress = dao.tripsBetween(0L, 100_000L).single()
                .copy(startStreet = "12 Rue de Rivoli", startCity = "Paris")
            dao.update(withAddress)

            val reloaded = dao.tripsBetween(0L, 100_000L).single()
            assertEquals(id, reloaded.id)
            assertEquals("12 Rue de Rivoli", reloaded.startStreet)
            assertEquals("Paris", reloaded.startCity)
        }
    }

    private fun <T> runBlockingTest(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
