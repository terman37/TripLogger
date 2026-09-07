package com.terman37.triplogger.core

/**
 * A reverse-geocoded place, kept minimal: only the parts the app stores and
 * exports (todo.md CSV: start/end city + address = street line). Null street
 * means the geocoder found no street (e.g. a forest road); null city means
 * none found either — but the trip may still have coordinates.
 */
data class ReverseGeocodeResult(
    val street: String?,
    val city: String?,
)

/**
 * Pure logic assembling a street line from the structured address fields that
 * Android's Geocoder returns. Separated from Android classes so it is JVM
 * testable.
 *
 * Android usually reports the house number in "subThoroughfare" and the street
 * name in "thoroughfare" (e.g. "12" + "Rue de Rivoli"). Some providers only
 * fill a full formatted line; this app then stores the line as street and
 * leaves city null rather than guessing.
 */
object AddressParts {

    fun from(
        subThoroughfare: String?,
        thoroughfare: String?,
        locality: String?,
    ): ReverseGeocodeResult {
        val number = subThoroughfare?.trim().orEmpty()
        val streetName = thoroughfare?.trim().orEmpty()
        val street = when {
            number.isEmpty() && streetName.isEmpty() -> null
            number.isEmpty() -> streetName
            streetName.isEmpty() -> number
            else -> "$number $streetName"
        }
        val city = locality?.trim()?.takeIf { it.isNotEmpty() }
        return ReverseGeocodeResult(street = street, city = city)
    }
}
