package com.terman37.triplogger.ui.home

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure text formatting for the Home screen (English-only, UI.md). Separated
 * from Compose so it is JVM-testable; the zone is injected so tests are
 * deterministic.
 */
object TripText {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /** "12.4 km" — one decimal, dot separator (decision, todo.md). */
    fun kmText(km: Double): String = String.format(Locale.US, "%.1f km", km)

    /** "Aug 6, 14:32" — date + start time of a trip row. */
    fun dateTimeText(epochMillis: Long, zone: ZoneId): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    /** "14:32" */
    fun timeText(epochMillis: Long, zone: ZoneId): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    /** Elapsed time between two instants: "4 min", "1 h 05". */
    fun durationText(startEpochMillis: Long, endEpochMillis: Long): String {
        val minutes = ((endEpochMillis - startEpochMillis) / 60_000L).coerceAtLeast(0)
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours == 0L) "${rest} min" else String.format(Locale.US, "%d h %02d", hours, rest)
    }

    /**
     * One address side for display:
     * - street + city present → "12 Rue de Rivoli, Paris"
     * - only one part → that part
     * - nothing, but coordinates exist → "Address pending" (geocoding retries
     *   later, todo.md)
     * - nothing and no coordinates → "No location"
     */
    fun addressText(street: String?, city: String?, hasCoordinates: Boolean): String {
        val parts = listOfNotNull(street, city)
        return when {
            parts.isNotEmpty() -> parts.joinToString(", ")
            hasCoordinates -> "Address pending"
            else -> "No location"
        }
    }

    /**
     * Short label for the "Home → Office" summary: city, else street, else the
     * given placeholder ("Start"/"End").
     */
    fun shortLabel(street: String?, city: String?, placeholder: String): String =
        city ?: street ?: placeholder
}
