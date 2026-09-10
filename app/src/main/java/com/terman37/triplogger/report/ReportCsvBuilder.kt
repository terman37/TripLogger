package com.terman37.triplogger.report

import com.terman37.triplogger.core.MapsUrl
import com.terman37.triplogger.data.Trip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds the expense-report CSV (columns decided in todo.md):
 *
 *   start date (ISO yyyy-MM-dd) | start time (HH:mm) | start month (yyyy-MM) |
 *   end date (ISO) | end time (HH:mm) | start city | start address |
 *   end city | end address | km (1 decimal, dot) |
 *   start maps link | end maps link
 *
 * Rows come in chronological order (the caller passes the DAO result which is
 * sorted ascending); a final totals row sums the km. UTF-8 text, RFC-4180
 * escaping (cells containing comma, quote or newline are quoted; quotes are
 * doubled) so the file opens cleanly in Excel / Google Sheets / LibreOffice.
 *
 * Pure Kotlin (no Android), zone injected → JVM-testable.
 */
object ReportCsvBuilder {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val HEADERS = listOf(
        "start date", "start time", "start month",
        "end date", "end time",
        "start city", "start address",
        "end city", "end address",
        "km",
        "start maps link", "end maps link",
    )

    /** @param trips ascending by start time (as the DAO returns them). */
    fun build(trips: List<Trip>, zone: ZoneId): String {
        val totalKm = trips.sumOf { it.distanceKm }
        return buildList {
            add(HEADERS.joinToString(",") { escape(it) })
            trips.forEach { trip -> add(row(trip, zone).joinToString(",") { escape(it) }) }
            add(
                // Totals row: "Total" in the first column and the km sum UNDER
                // the km header (look the column up by name so reordering the
                // headers can never silently shift the total).
                HEADERS.indices.map { index ->
                    when {
                        index == 0 -> "Total"
                        HEADERS[index] == "km" -> kmText(totalKm)
                        else -> ""
                    }
                }.joinToString(",") { escape(it) },
            )
        }.joinToString("\n") + "\n"
    }

    private fun row(trip: Trip, zone: ZoneId): List<String> {
        val start = Instant.ofEpochMilli(trip.startEpochMillis).atZone(zone)
        val end = Instant.ofEpochMilli(trip.endEpochMillis).atZone(zone)
        return listOf(
            start.format(dateFormatter),
            start.format(timeFormatter),
            start.format(monthFormatter),
            end.format(dateFormatter),
            end.format(timeFormatter),
            trip.startCity.orEmpty(),
            trip.startStreet.orEmpty(),
            trip.endCity.orEmpty(),
            trip.endStreet.orEmpty(),
            kmText(trip.distanceKm),
            MapsUrl.place(trip.startStreet, trip.startCity, trip.startLat, trip.startLng).orEmpty(),
            MapsUrl.place(trip.endStreet, trip.endCity, trip.endLat, trip.endLng).orEmpty(),
        )
    }

    /** One decimal, dot separator (decisions, todo.md). */
    private fun kmText(km: Double): String = String.format(Locale.US, "%.1f", km)

    /** RFC-4180: quote only when needed, double embedded quotes. */
    private fun escape(cell: String): String =
        if (cell.contains(',') || cell.contains('"') || cell.contains('\n')) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
}
