package com.terman37.triplogger.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.terman37.triplogger.core.GpsSample
import com.terman37.triplogger.core.TrackingPolicy

/**
 * [LocationSource] backed by the system [LocationManager] GPS provider.
 *
 * Why plain LocationManager and not Google's FusedLocationProvider: fused needs
 * Play Services and adds a dependency; for one fix every 30 seconds the plain
 * provider is enough and works everywhere.
 *
 * MINIMUM SDK NOTE: this class only works when the caller already holds the
 * ACCESS_FINE_LOCATION runtime permission (requested by the UI before the trip
 * service is started). [start] returns false instead of crashing when the
 * permission or the GPS provider is unavailable.
 */
class LocationManagerLocationSource(
    context: Context,
    private val intervalMillis: Long = TrackingPolicy.LOCATION_INTERVAL_MS,
) : LocationSource {

    // applicationContext: never an Activity/Service, so holding it does not
    // leak UI objects (Android best practice for long-lived holders).
    private val appContext = context.applicationContext

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile
    private var callback: ((GpsSample) -> Unit)? = null

    // Legacy onLocationChanged(Location) is deprecated but still the simplest
    // reliable callback; Android delivers it on the provided looper.
    @SuppressLint("MissingPermission")
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            callback?.invoke(location.toSample())
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(onSample: (GpsSample) -> Unit): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return false

        // GPS may be switched off by the user (quick settings). When it is, the
        // request below would throw.
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return false

        callback = onSample
        // Request one fix every intervalMillis. minDistance 0: movement
        // filtering is the LocationFilter's job (policy in core), not the
        // provider's.
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            intervalMillis,
            0f,
            listener,
            Looper.getMainLooper(),
        )
        return true
    }

    override fun stop() {
        callback = null
        locationManager.removeUpdates(listener)
    }

    @SuppressLint("MissingPermission")
    override fun lastKnown(): GpsSample? = try {
        // May throw SecurityException if the permission was revoked between
        // start() and now — return null in that case.
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.toSample()
    } catch (_: SecurityException) {
        null
    }
}

/** Converts an Android [Location] into our Android-free [GpsSample]. */
private fun Location.toSample(): GpsSample = GpsSample(
    timestampEpochMillis = time, // Location.time is epoch milliseconds (UTC)
    latitude = latitude,
    longitude = longitude,
    // accuracy < 0 means "unknown" on some devices → null (treated as trusted
    // by the filter, same as a missing value).
    accuracyMeters = accuracy.takeIf { it >= 0f },
)
