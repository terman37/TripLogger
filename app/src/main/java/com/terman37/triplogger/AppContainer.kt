package com.terman37.triplogger

import android.content.Context
import com.terman37.triplogger.core.SystemClock
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.TripDatabase
import com.terman37.triplogger.data.TripRepository
import com.terman37.triplogger.geocoding.AndroidGeocoderClient
import com.terman37.triplogger.geocoding.GeocoderClient
import com.terman37.triplogger.session.SharedPreferencesTripSessionStore
import com.terman37.triplogger.session.TripRecovery
import com.terman37.triplogger.session.TripSessionStore
import com.terman37.triplogger.settings.SettingsRepository
import com.terman37.triplogger.settings.SharedPreferencesSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency injection container (no Hilt in v1 — the app is
 * small, one object wiring everything is easier to follow for a first app).
 *
 * One AppContainer per app process, created lazily by [TripLoggerApplication].
 * Services and ViewModels ask it for the pieces they need; nothing is created
 * twice (e.g. one TripRecorder shared by the trip service and the Home UI).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** One recorder for the whole process: the service feeds it events, the
     * UI reads its snapshot. */
    val tripRecorder: TripRecorder = TripRecorder(clock = SystemClock)

    val settings: SettingsRepository =
        SharedPreferencesSettingsRepository(appContext)

    val geocoder: GeocoderClient =
        AndroidGeocoderClient(appContext)

    private val database: TripDatabase = TripDatabase.getInstance(appContext)

    val tripRepository: TripRepository =
        TripRepository(database.tripDao(), geocoder)

    /** Persists the trip currently being recorded so a process death can finish
     * it at the last known position (see DETAILS.md). */
    val tripSessionStore: TripSessionStore =
        SharedPreferencesTripSessionStore(appContext)

    private val tripRecovery = TripRecovery(tripSessionStore, tripRepository)

    init {
        // Keep the recorder's grace period in sync with the setting: the user
        // can change it on the Devices screen while the recorder exists.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settings.gracePeriodMinutes.collect { minutes ->
                tripRecorder.updateGracePeriodMillis(minutes * 60_000L)
            }
        }

        // Finish a trip that was being recorded when the process died (reboot,
        // force-stop, crash). takePending() clears the store synchronously so
        // the service cannot overwrite it before it is saved; the save runs in
        // the background.
        val pending = tripRecovery.takePending()
        if (pending != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                runCatching { tripRecovery.finish(pending) }
                    .onFailure { error ->
                        // Not saved yet: restore it so the next start retries.
                        tripSessionStore.save(pending)
                        android.util.Log.e(TAG, "trip recovery failed", error)
                    }
            }
        }
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
