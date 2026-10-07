package com.terman37.triplogger.ui.home

import com.terman37.triplogger.testtext.EnglishText
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTextTest {

    private val utc = java.time.ZoneId.of("UTC")

    // English reference formatter (see EnglishText): the production code gets
    // this from resources instead.
    private val text = EnglishText.tripText

    private fun epoch(utcTime: String): Long =
        LocalDateTime.parse(utcTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun kmText_oneDecimalWithDot() {
        assertEquals("12.4 km", text.kmText(12.4))
        assertEquals("0.0 km", text.kmText(0.0))
        assertEquals("3.0 km", text.kmText(2.96))
    }

    @Test
    fun localeDrivesNumberAndDateConventions() {
        // Phase L1b: the formatter is locale-parameterised, so the same texts
        // produce French conventions on a French device. This is what L2 relies
        // on; production takes the locale from the configuration, tests state it.
        val fr = EnglishText.tripTextFrench
        assertEquals("86,1 km", fr.kmText(86.1))
        assertEquals("12,4 km", fr.kmText(12.4))

        val e = epoch("2026-08-06T12:32")
        // Pattern "MMM d, HH:mm" with a French locale spells the month in French
        // (the field order is the pattern's job, not the locale's).
        assertEquals("août 6, 12:32", fr.dateTimeText(e, utc))
        assertEquals("12:32", fr.timeText(e, utc))
        // Durations keep the "N min" wording: the text layer is locale-driven,
        // the words themselves come from resources.
        assertEquals("4 min", fr.durationText(0L, 4 * 60_000L))
    }

    @Test
    fun dateTimeText_usesGivenZone() {
        val e = epoch("2026-08-06T12:32")
        assertEquals("Aug 6, 12:32", text.dateTimeText(e, utc))
    }

    @Test
    fun timeText_usesGivenZone() {
        val e = epoch("2026-08-06T09:05")
        assertEquals("09:05", text.timeText(e, utc))
    }

    @Test
    fun durationText_formatsMinutesAndHours() {
        assertEquals("4 min", text.durationText(0L, 4 * 60_000L))
        assertEquals("1 h 05", text.durationText(0L, 65 * 60_000L))
        assertEquals("0 min", text.durationText(1_000L, 0L)) // negative clamped
    }

    @Test
    fun addressText_joinsPresentParts() {
        assertEquals(
            "12 Rue de Rivoli, Paris",
            text.addressText("12 Rue de Rivoli", "Paris", hasCoordinates = true),
        )
        assertEquals("Paris", text.addressText(null, "Paris", hasCoordinates = true))
        assertEquals("A7", text.addressText("A7", null, hasCoordinates = true))
    }

    @Test
    fun addressText_pendingVsNoLocation() {
        // Coordinates exist → geocoding may still run → pending.
        assertEquals("Address pending", text.addressText(null, null, hasCoordinates = true))
        // No coordinates at all → never resolvable.
        assertEquals("No location", text.addressText(null, null, hasCoordinates = false))
    }

    @Test
    fun shortLabel_cityFirstThenStreetThenPlaceholder() {
        assertEquals("Paris", text.shortLabel("Rue X", "Paris", "Start"))
        assertEquals("Rue X", text.shortLabel("Rue X", null, "Start"))
        assertEquals("Start", text.shortLabel(null, null, "Start"))
    }
}
