package com.terman37.triplogger.report

import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure date-range rules for the Report screen. Extracted from the ViewModel so
 * they are JVM-testable (no Android dependency).
 */
object ReportDates {

    /** Default range: the last 7 days INCLUDING today. */
    fun defaultFrom(today: LocalDate): LocalDate = today.minusDays(6)

    fun defaultTo(today: LocalDate): LocalDate = today

    /**
     * Converts an inclusive local-date range to the DAO half-open range
     * `[fromInclusive, untilExclusive)` in epoch millis: `to` is included by
     * using the start of the following day.
     */
    fun rangeMillis(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val untilMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return fromMillis to untilMillis
    }
}
