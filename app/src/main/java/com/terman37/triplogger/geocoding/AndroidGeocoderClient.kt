package com.terman37.triplogger.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.terman37.triplogger.core.AddressParts
import com.terman37.triplogger.core.ReverseGeocodeResult

/**
 * [GeocoderClient] using Android's built-in [Geocoder].
 *
 * Notes:
 * - Requires network (it queries an online service); failure returns null and
 *   the caller retries later — this is the "offline at trip end" path decided
 *   in todo.md.
 * - Android's Geocoder does NOT need Play Services (it uses the platform
 *   geocoding service), so it fits the no-Play-Services decision.
 * - Blocking: call from Dispatchers.IO, never the main thread.
 */
class AndroidGeocoderClient(context: Context) : GeocoderClient {

    private val geocoder = Geocoder(context.applicationContext)

    @Suppress("DEPRECATION")
    // getFromLocation is marked deprecated in recent SDKs; the replacement is
    // the geocoding backend of Play services, which this app deliberately
    // avoids. The platform Geocoder still works on all devices.
    override fun reverse(latitude: Double, longitude: Double): ReverseGeocodeResult? {
        // Geocoder throws IOException on network problems and
        // IllegalArgumentException on bad coordinates — both mean "no result".
        return try {
            // getFromLocation returns null when no result was found.
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            val first = addresses?.firstOrNull() ?: return null
            AddressParts.from(
                subThoroughfare = first.subThoroughfare,
                thoroughfare = first.thoroughfare,
                locality = first.locality,
            )
        } catch (_: java.io.IOException) {
            null // no network / service unavailable
        } catch (_: IllegalArgumentException) {
            null // invalid coordinates
        }
    }
}
