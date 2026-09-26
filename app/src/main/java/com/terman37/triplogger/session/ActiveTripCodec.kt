package com.terman37.triplogger.session

import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.core.TripRecorder

/**
 * Turns an [TripRecorder.ActiveTrip] into one stored string and back.
 *
 * WHY NOT JSON: the record is only numbers, one enum name and no free-form text,
 * so a simple `;`-separated format is enough (delimiters cannot appear in the
 * values). Unlike the device list in the settings, nothing here can contain a
 * `;`. This keeps the codec a plain Kotlin object that is unit-tested on the JVM
 * (Android's `org.json` is not available in JVM unit tests).
 *
 * Doubles may be absent (no GPS fix): they are written as an empty field and
 * read back as null. Decoding never throws: a corrupt value yields null, which
 * callers treat as "no active trip" — losing the persisted trip at worst, never
 * crashing.
 */
object ActiveTripCodec {

    private const val FIELD_COUNT = 8
    private const val SEPARATOR = ';'

    fun encode(trip: TripRecorder.ActiveTrip): String = listOf(
        trip.startEpochMillis.toString(),
        trip.origin.name,
        trip.startLat?.toString() ?: "",
        trip.startLng?.toString() ?: "",
        trip.distanceKm.toString(),
        trip.lastLat?.toString() ?: "",
        trip.lastLng?.toString() ?: "",
        trip.lastEpochMillis.toString(),
    ).joinToString(SEPARATOR.toString())

    fun decode(raw: String): TripRecorder.ActiveTrip? {
        val parts = raw.split(SEPARATOR, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null

        val startEpochMillis = parts[0].toLongOrNull() ?: return null
        val origin = runCatching { TripOrigin.valueOf(parts[1]) }.getOrNull() ?: return null
        val distanceKm = parts[4].toDoubleOrNull() ?: return null
        val lastEpochMillis = parts[7].toLongOrNull() ?: return null

        return TripRecorder.ActiveTrip(
            startEpochMillis = startEpochMillis,
            origin = origin,
            startLat = parts[2].toDoubleOrNull(),
            startLng = parts[3].toDoubleOrNull(),
            distanceKm = distanceKm,
            lastLat = parts[5].toDoubleOrNull(),
            lastLng = parts[6].toDoubleOrNull(),
            lastEpochMillis = lastEpochMillis,
        )
    }
}
