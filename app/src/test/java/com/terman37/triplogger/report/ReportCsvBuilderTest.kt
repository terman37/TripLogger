package com.terman37.triplogger.report

import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.data.Trip
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportCsvBuilderTest {

    private val utc = java.time.ZoneId.of("UTC")

    private fun epoch(utcTime: String): Long =
        LocalDateTime.parse(utcTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun trip(
        start: Long,
        end: Long,
        startCity: String? = "Paris",
        startStreet: String? = "12 Rue de Rivoli",
        endCity: String? = "Lyon",
        endStreet: String? = "1 Rue de la République",
        startLat: Double? = null, startLng: Double? = null,
        endLat: Double? = null, endLng: Double? = null,
        km: Double,
    ) = Trip(
        startEpochMillis = start,
        startLat = startLat, startLng = startLng,
        startStreet = startStreet, startCity = startCity,
        endEpochMillis = end,
        endLat = endLat, endLng = endLng,
        endStreet = endStreet, endCity = endCity,
        distanceKm = km,
        origin = TripOrigin.AUTO,
    )

    @Test
    fun headers_matchSpec() {
        val csv = ReportCsvBuilder.build(emptyList(), utc)
        val header = csv.lineSequence().first()
        assertEquals(
            "start date,start time,start month,end date,end time," +
                "start city,start address,end city,end address,km," +
                "start maps link,end maps link",
            header,
        )
    }

    @Test
    fun emptyList_hasOnlyHeaderAndTotalZero() {
        val csv = ReportCsvBuilder.build(emptyList(), utc)
        // trimEnd: builder ends with a trailing newline (RFC wants it).
        val lines = csv.trimEnd().lineSequence().toList()
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("Total"))
        // km total sits under the km header (index 9), maps links empty.
        assertEquals("0.0", lines[1].split(",")[9])
    }

    @Test
    fun rows_chronologicalWithIsoFormatsAndTotals() {
        val t1 = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            km = 12.4,
        )
        val t2 = trip(
            start = epoch("2026-08-07T08:05"), end = epoch("2026-08-07T08:40"),
            startCity = "Lyon", startStreet = null,
            endCity = null, endStreet = "Parking A",
            km = 35.0,
        )
        val csv = ReportCsvBuilder.build(listOf(t1, t2), utc)
        val lines = csv.lineSequence().toList()

        assertEquals(
            "2026-08-06,14:32,2026-08,2026-08-06,15:15,Paris,12 Rue de Rivoli,Lyon,1 Rue de la République,12.4," +
                "https://www.google.com/maps/search/?api=1&query=12+Rue+de+Rivoli%2C+Paris," +
                "https://www.google.com/maps/search/?api=1&query=1+Rue+de+la+R%C3%A9publique%2C+Lyon",
            lines[1],
        )
        assertEquals(
            "2026-08-07,08:05,2026-08,2026-08-07,08:40,Lyon,,,Parking A,35.0," +
                "https://www.google.com/maps/search/?api=1&query=Lyon," +
                "https://www.google.com/maps/search/?api=1&query=Parking+A",
            lines[2],
        )
        // km sum must sit under the "km" header (9th column), with the two
        // maps-link columns empty.
        assertEquals("Total,,,,,,,,,47.4,,", lines[3])
    }

    @Test
    fun specialCharacters_areEscaped() {
        val tricky = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startCity = "Saint-Étienne, France", // comma
            startStreet = "Rue des \"Charmes\"", // quotes
            endCity = "Line1\nLine2",            // newline
            km = 1.0,
        )
        val csv = ReportCsvBuilder.build(listOf(tricky), utc)
        assertTrue(csv.contains("\"Saint-Étienne, France\""))
        assertTrue(csv.contains("\"Rue des \"\"Charmes\"\"\""))
        assertTrue(csv.contains("\"Line1\nLine2\""))
        // 2 joins + 1 trailing newline + the embedded newline inside the
        // quoted cell = 4 line breaks total.
        assertEquals(4, csv.count { it == '\n' })
    }

    @Test
    fun missingAddresses_becomeEmptyCells() {
        val bare = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startCity = null, startStreet = null,
            endCity = null, endStreet = null,
            km = 0.0,
        )
        val csv = ReportCsvBuilder.build(listOf(bare), utc)
        val row = csv.lineSequence().toList()[1]
        // km is followed by the two empty maps-link cells.
        assertTrue(row.endsWith(",0.0,,"))
        assertEquals(12, row.split(",").size) // no empty-cell collapsing
    }

    @Test
    fun mapsLinks_fallBackToCoordinates() {
        val t = trip(
            start = epoch("2026-08-06T14:32"), end = epoch("2026-08-06T15:15"),
            startCity = null, startStreet = null,
            endCity = null, endStreet = null,
            startLat = 48.8566, startLng = 2.3522,
            endLat = 48.8738, endLng = 2.2950,
            km = 3.0,
        )
        val row = ReportCsvBuilder.build(listOf(t), utc).lineSequence().toList()[1]
        assertTrue(row.contains("query=48.856600%2C2.352200"))
        assertTrue(row.contains("query=48.873800%2C2.295000"))
    }

    @Test
    fun timeZone_affectsTheLocalColumns() {
        val start = epoch("2026-08-06T22:30") // UTC 22:30
        val t = trip(start = start, end = start + 3_600_000, km = 5.0)
        val csv = ReportCsvBuilder.build(listOf(t), java.time.ZoneId.of("Europe/Paris"))
        val row = csv.lineSequence().toList()[1]
        // Paris is UTC+2 in August: 22:30 UTC → next day 00:30 local.
        assertTrue("row was: $row", row.startsWith("2026-08-07,00:30,2026-08,"))
    }
}
