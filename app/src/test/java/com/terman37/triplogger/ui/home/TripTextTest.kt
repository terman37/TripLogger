package com.terman37.triplogger.ui.home

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTextTest {

    private val utc = java.time.ZoneId.of("UTC")

    private fun epoch(utcTime: String): Long =
        LocalDateTime.parse(utcTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun kmText_oneDecimalWithDot() {
        assertEquals("12.4 km", TripText.kmText(12.4))
        assertEquals("0.0 km", TripText.kmText(0.0))
        assertEquals("3.0 km", TripText.kmText(2.96))
    }

    @Test
    fun dateTimeText_usesGivenZone() {
        val e = epoch("2026-08-06T12:32")
        assertEquals("Aug 6, 12:32", TripText.dateTimeText(e, utc))
    }

    @Test
    fun timeText_usesGivenZone() {
        val e = epoch("2026-08-06T09:05")
        assertEquals("09:05", TripText.timeText(e, utc))
    }

    @Test
    fun durationText_formatsMinutesAndHours() {
        assertEquals("4 min", TripText.durationText(0L, 4 * 60_000L))
        assertEquals("1 h 05", TripText.durationText(0L, 65 * 60_000L))
        assertEquals("0 min", TripText.durationText(1_000L, 0L)) // negative clamped
    }

    @Test
    fun addressText_joinsPresentParts() {
        assertEquals(
            "12 Rue de Rivoli, Paris",
            TripText.addressText("12 Rue de Rivoli", "Paris", hasCoordinates = true),
        )
        assertEquals("Paris", TripText.addressText(null, "Paris", hasCoordinates = true))
        assertEquals("A7", TripText.addressText("A7", null, hasCoordinates = true))
    }

    @Test
    fun addressText_pendingVsNoLocation() {
        // Coordinates exist → geocoding may still run → pending.
        assertEquals("Address pending", TripText.addressText(null, null, hasCoordinates = true))
        // No coordinates at all → never resolvable.
        assertEquals("No location", TripText.addressText(null, null, hasCoordinates = false))
    }

    @Test
    fun shortLabel_cityFirstThenStreetThenPlaceholder() {
        assertEquals("Paris", TripText.shortLabel("Rue X", "Paris", "Start"))
        assertEquals("Rue X", TripText.shortLabel("Rue X", null, "Start"))
        assertEquals("Start", TripText.shortLabel(null, null, "Start"))
    }
}
