package com.terman37.triplogger.report

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDatesTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    @Test
    fun defaultRange_isLastSevenDaysIncludingToday() {
        val today = LocalDate.of(2026, 9, 10)
        assertEquals(LocalDate.of(2026, 9, 4), ReportDates.defaultFrom(today))
        assertEquals(today, ReportDates.defaultTo(today))
        // 7 distinct days (4,5,6,7,8,9,10).
        assertEquals(6, ReportDates.defaultTo(today).toEpochDay() - ReportDates.defaultFrom(today).toEpochDay())
    }

    @Test
    fun rangeMillis_startsAtFromAndIncludesAllOfTo() {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 3)

        val (fromMillis, untilMillis) = ReportDates.rangeMillis(from, to, utc)

        assertEquals(epochUtc("2026-09-01T00:00"), fromMillis)
        // Exclusive upper bound = start of the day AFTER "to".
        assertEquals(epochUtc("2026-09-04T00:00"), untilMillis)
        // A trip at the last minute of "to" is inside the range.
        val lastMinute = epochUtc("2026-09-03T23:59")
        assertTrue(lastMinute >= fromMillis && lastMinute < untilMillis)
    }

    @Test
    fun rangeMillis_usesTheGivenZone() {
        val day = LocalDate.of(2026, 8, 6)
        val paris = ReportDates.rangeMillis(day, day, ZoneId.of("Europe/Paris"))

        // Summer: Paris is UTC+2 → local midnight is 22:00 UTC the day before.
        assertEquals(epochUtc("2026-08-05T22:00"), paris.first)
        assertEquals(epochUtc("2026-08-06T22:00"), paris.second)
    }

    private fun epochUtc(local: String): Long =
        LocalDateTime.parse(local).toInstant(ZoneOffset.UTC).toEpochMilli()
}
