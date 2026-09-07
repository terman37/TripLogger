package com.terman37.triplogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * How a trip was started. AUTO = started because a registered Bluetooth device
 * connected (or resumed within the grace period); MANUAL = started with the
 * fallback button on the Home screen. Stored in the database so the UI can show
 * "Started manually" instead of a connected device name (UI.md).
 */
enum class TripOrigin {
    AUTO,
    MANUAL,
}

/**
 * One recorded car trip (one database row). Column meanings (todo.md "write row
 * in db"):
 *
 * - [startEpochMillis] / [endEpochMillis]: UTC instants in milliseconds since
 *   1970-01-01 (the standard way to store instants; convert to local dates in
 *   the UI layer). start >= end should never happen: the recorder guarantees a
 *   trip has a start and an end before it is saved.
 * - [startLat]/[startLng]/[endLat]/[endLng]: GPS coordinates (degrees).
 * - [startStreet]/[startCity]/[endStreet]/[endCity]: reverse-geocoded address,
 *   stored split into street and city (CSV needs them separately, todo.md).
 *   Null when geocoding failed or is still pending — the UI shows "Address
 *   pending" and retries later.
 * - [distanceKm]: total distance in kilometers, one decimal is enough for
 *   reporting.
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val startLat: Double,
    val startLng: Double,
    val startStreet: String?,
    val startCity: String?,
    val endEpochMillis: Long,
    val endLat: Double,
    val endLng: Double,
    val endStreet: String?,
    val endCity: String?,
    val distanceKm: Double,
    val origin: TripOrigin,
)

/**
 * Room type converter: Room cannot store Kotlin enums directly, so values are
 * converted to/from their name string on write/read.
 */
class TripOriginConverter {
    @TypeConverter
    fun fromString(value: String): TripOrigin = TripOrigin.valueOf(value)

    @TypeConverter
    fun toString(value: TripOrigin): String = value.name
}
