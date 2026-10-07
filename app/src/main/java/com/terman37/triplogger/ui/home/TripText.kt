package com.terman37.triplogger.ui.home

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The texts [TripText] needs, as plain data.
 *
 * Why a data class: the formatting layer must stay Android-free so it can be
 * unit-tested on the JVM (that is a deliberate design choice of this project).
 * Production code fills this from resources — see `tripTextFrom(context)` in
 * `text/AppTextResources.kt`. There is deliberately **no English default
 * here**: a missing translation must fail loudly rather than silently fall back
 * to English (phase L1b of the localisation work, release_guide.md §6).
 */
data class TripTextStrings(
    /** Shown when an endpoint has coordinates but no address yet. */
    val addressPending: String,
    /** Shown when an endpoint has no address and no coordinates. */
    val noLocation: String,
    /** Summary placeholder when the start side has no usable address. */
    val startPlaceholder: String,
    /** Summary placeholder when the end side has no usable address. */
    val endPlaceholder: String,
    /** Duration under an hour, e.g. "12 min". Takes the minute count. */
    val durationMinutes: String,
    /** Duration of an hour or more, e.g. "1 h 05". Takes hours and minutes. */
    val durationHours: String,
    /** Distance, e.g. "12.4 km". Takes the kilometre value. */
    val kilometres: String,
    /** Date + time of a trip row, e.g. "Aug 6, 14:32". */
    val dateTimePattern: String,
    /** Time only, e.g. "14:32". */
    val timePattern: String,
)

/**
 * Pure text formatting for the trip rows, notification-independent parts of the
 * UI and the report (docs/UI.md). Separated from Compose so it is JVM-testable.
 *
 * It is a class rather than an object because both the **locale** (number and
 * date conventions) and the **texts** are injected: the app builds one instance
 * from resources, tests build one explicitly. Both the zone and the locale are
 * parameters, never global state, so the output is deterministic.
 */
class TripText(
    private val locale: Locale,
    private val strings: TripTextStrings,
) {

    // Formatters are immutable and thread-safe; build them once per instance.
    private val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern(strings.dateTimePattern, locale)
    }
    private val timeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern(strings.timePattern, locale)
    }

    /** Placeholder for a start side without address (used by the row summary). */
    val startPlaceholder: String get() = strings.startPlaceholder

    /** Placeholder for an end side without address. */
    val endPlaceholder: String get() = strings.endPlaceholder

    /** "12.4 km" in English, "12,4 km" in French — the locale decides. */
    fun kmText(km: Double): String = String.format(locale, strings.kilometres, km)

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
        return if (hours == 0L) {
            String.format(locale, strings.durationMinutes, minutes)
        } else {
            String.format(locale, strings.durationHours, hours, rest)
        }
    }

    /**
     * One address side for display:
     * - street + city present → "12 Rue de Rivoli, Paris"
     * - only one part → that part
     * - nothing, but coordinates exist → "Address pending" (geocoding retries
     *   later)
     * - nothing and no coordinates → "No location"
     */
    fun addressText(street: String?, city: String?, hasCoordinates: Boolean): String {
        val parts = listOfNotNull(street, city)
        return when {
            parts.isNotEmpty() -> parts.joinToString(", ")
            hasCoordinates -> strings.addressPending
            else -> strings.noLocation
        }
    }

    /**
     * Short label for the "Home → Office" summary: city, else street, else the
     * given placeholder ("Start"/"End").
     */
    fun shortLabel(street: String?, city: String?, placeholder: String): String =
        city ?: street ?: placeholder
}
