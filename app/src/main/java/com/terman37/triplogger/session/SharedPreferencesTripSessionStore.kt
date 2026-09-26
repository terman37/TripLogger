package com.terman37.triplogger.session

import android.content.Context
import com.terman37.triplogger.core.TripRecorder

/**
 * [TripSessionStore] backed by its own SharedPreferences file. Kept separate
 * from the settings file so clearing/reading the active trip never touches the
 * user's settings (and vice versa).
 *
 * Writes use `apply()`: asynchronous but ordered, which is what we want while
 * the service records (a save happens on every GPS fix and must not block).
 */
class SharedPreferencesTripSessionStore(context: Context) : TripSessionStore {

    private val prefs = context.applicationContext
        .getSharedPreferences("trip_session", Context.MODE_PRIVATE)

    override fun load(): TripRecorder.ActiveTrip? {
        val raw = prefs.getString(KEY_ACTIVE_TRIP, null) ?: return null
        return ActiveTripCodec.decode(raw)
    }

    override fun save(trip: TripRecorder.ActiveTrip) {
        prefs.edit().putString(KEY_ACTIVE_TRIP, ActiveTripCodec.encode(trip)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_ACTIVE_TRIP).apply()
    }

    private companion object {
        const val KEY_ACTIVE_TRIP = "active_trip"
    }
}
