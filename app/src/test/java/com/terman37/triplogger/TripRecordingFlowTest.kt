package com.terman37.triplogger

import com.terman37.triplogger.testtext.EnglishText
import com.terman37.triplogger.core.Clock
import com.terman37.triplogger.core.GpsSample
import com.terman37.triplogger.core.ReverseGeocodeResult
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.FakeGeocoder
import com.terman37.triplogger.data.FakeTripDao
import com.terman37.triplogger.data.PendingAddresses
import com.terman37.triplogger.data.TripRepository
import com.terman37.triplogger.report.ReportXlsxBuilder
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end (minus Android) flow of the main use cases: the recorder state
 * machine feeds the repository through the same drafts the service passes, and
 * the Excel builder consumes the stored rows. Catches integration mistakes that
 * isolated unit tests miss (field mapping, distance/address/total coherence).
 */
class TripRecordingFlowTest {

    private class FakeClock(var now: Long) : Clock {
        override fun nowMillis(): Long = now
    }

    private val base = 1_700_000_000_000L
    private val graceMs = 3 * 60_000L
    private val zone: ZoneId = ZoneId.of("UTC")

    private val start = 48.8566 to 2.3522
    private val stepLat = 500.0 / 111_190.0 // ~500 m north per step
    private val endLat = start.first + 3 * stepLat
    private val startAddress = ReverseGeocodeResult("12 Rue de Rivoli", "Paris")
    private val endAddress = ReverseGeocodeResult("5 Rue de Belleville", "Paris")

    private fun sampleAt(t: Long, latOffset: Double = 0.0) = GpsSample(
        timestampEpochMillis = t,
        latitude = start.first + latOffset,
        longitude = start.second,
        accuracyMeters = 10f,
    )

    private fun onlineGeocoder() = FakeGeocoder(
        mutableMapOf(start to startAddress, (endLat to start.second) to endAddress),
    )

    /** Drive 1.5 km north in three 500 m steps, then finish via a manual stop. */
    private fun recordDrivenTrip(recorder: TripRecorder, clock: FakeClock, endAt: Long) {
        recorder.onLocationSample(sampleAt(base + 30_000))            // anchor 0 km
        recorder.onLocationSample(sampleAt(base + 60_000, stepLat))   // +0.5
        recorder.onLocationSample(sampleAt(base + 90_000, 2 * stepLat)) // +0.5
        recorder.onLocationSample(sampleAt(base + 120_000, 3 * stepLat)) // +0.5
        clock.now = endAt
        recorder.onManualStop()
    }

    @Test
    fun autoTrip_isRecordedGeocodedStoredAndExported() = runBlocking {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        val dao = FakeTripDao()
        val repo = TripRepository(dao, onlineGeocoder())

        recorder.onDeviceConnected("Car")
        recordDrivenTrip(recorder, clock, endAt = base + 150_000)

        val draft = recorder.takeFinishedTrips().single()
        repo.saveTrip(draft)

        val stored = dao.snapshot().single()
        assertEquals(TripOrigin.AUTO, stored.origin)
        assertEquals(base, stored.startEpochMillis)
        assertEquals(base + 150_000, stored.endEpochMillis)
        assertEquals(1.5, stored.distanceKm, 0.05)
        assertEquals("12 Rue de Rivoli", stored.startStreet)
        assertEquals("5 Rue de Belleville", stored.endStreet)
        assertEquals("Paris", stored.endCity)

        val workbook = ReportXlsxBuilder.build(dao.snapshot(), zone, EnglishText.xlsxLabels)
        val sheet = zipText(workbook, "xl/worksheets/sheet1.xml")
        val rels = zipText(workbook, "xl/worksheets/_rels/sheet1.xml.rels")
        assertTrue(sheet.contains("Rue de Rivoli"))
        // Hyperlink targets live in the worksheet relationships, not the sheet.
        assertTrue(rels.contains("https://www.google.com/maps/search"))
        // Total row (header + trip + total) with the km sum under the km column.
        val km = String.format(Locale.US, "%.1f", stored.distanceKm)
        assertTrue(sheet.contains("<c r=\"E3\" s=\"6\"><f>SUM(E2:E2)</f><v>$km</v></c>"))
    }

    /** Reads one entry back out of the generated .xlsx ZIP as UTF-8 text. */
    private fun zipText(bytes: ByteArray, name: String): String {
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) {
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                    }
                    return out.toString(Charsets.UTF_8.name())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("$name missing from the generated workbook")
    }

    @Test
    fun graceReconnect_keepsOneTrip() = runBlocking {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        val dao = FakeTripDao()
        val repo = TripRepository(dao, onlineGeocoder())

        recorder.onDeviceConnected("Car")
        recorder.onLocationSample(sampleAt(base + 30_000))            // anchor 0
        recorder.onLocationSample(sampleAt(base + 60_000, stepLat))   // +0.5

        clock.now = base + 90_000
        recorder.onDeviceDisconnected()                                // → grace
        clock.now = base + 120_000
        recorder.onDeviceConnected("Car")                              // resume same trip
        recorder.onLocationSample(sampleAt(base + 150_000, 3 * stepLat)) // +1.0

        clock.now = base + 180_000
        recorder.onManualStop()
        recorder.takeFinishedTrips().forEach { repo.saveTrip(it) }

        val stored = dao.snapshot().single() // one trip, not two
        assertEquals(base, stored.startEpochMillis)
        assertEquals(1.5, stored.distanceKm, 0.05)
    }

    @Test
    fun manualTrip_isStoredAsManual() = runBlocking {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        val dao = FakeTripDao()
        val repo = TripRepository(dao, onlineGeocoder())

        recorder.onManualStart()
        recordDrivenTrip(recorder, clock, endAt = base + 150_000)
        recorder.takeFinishedTrips().forEach { repo.saveTrip(it) }

        assertEquals(TripOrigin.MANUAL, dao.snapshot().single().origin)
    }

    @Test
    fun shortTrip_isDiscardedByThreshold() = runBlocking {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)

        recorder.onDeviceConnected("Car")
        recorder.onLocationSample(sampleAt(base + 30_000))                    // anchor
        recorder.onLocationSample(sampleAt(base + 60_000, 20.0 / 111_190.0))  // +20 m
        clock.now = base + 60_000
        recorder.onDeviceDisconnected()
        clock.now = base + 60_000 + graceMs
        recorder.onGraceTimerExpired()

        assertTrue(recorder.takeFinishedTrips().isEmpty()) // < 50 m → no record
    }

    @Test
    fun offlineTrip_isStoredPending_thenLazyRetryFillsAddresses() = runBlocking {
        val clock = FakeClock(base)
        val recorder = TripRecorder(clock, graceMs)
        val dao = FakeTripDao()
        val geocoder = FakeGeocoder(emptyMap()) // no network
        val repo = TripRepository(dao, geocoder)

        recorder.onDeviceConnected("Car")
        recordDrivenTrip(recorder, clock, endAt = base + 150_000)
        recorder.takeFinishedTrips().forEach { repo.saveTrip(it) }

        val offlineRow = dao.snapshot().single()
        assertNull(offlineRow.startStreet)
        assertEquals(2, PendingAddresses.pendingRequests(dao.snapshot()).size)

        // Network is back: the retry (Report open / before export) fills both.
        geocoder.results = mutableMapOf(
            start to startAddress,
            (endLat to start.second) to endAddress,
        )
        repo.retryPendingAddresses()

        assertTrue(PendingAddresses.pendingRequests(dao.snapshot()).isEmpty())
        assertEquals("12 Rue de Rivoli", dao.snapshot().single().startStreet)
    }
}
