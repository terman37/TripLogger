package com.terman37.triplogger

import android.content.Context
import com.terman37.triplogger.core.SystemClock
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.TripDatabase
import com.terman37.triplogger.data.TripRepository
import com.terman37.triplogger.geocoding.AndroidGeocoderClient
import com.terman37.triplogger.geocoding.GeocoderClient
import com.terman37.triplogger.settings.SettingsRepository
import com.terman37.triplogger.settings.SharedPreferencesSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency injection container (plan.md: no Hilt in v1 — the app is
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

    val database: TripDatabase = TripDatabase.getInstance(appContext)

    val tripRepository: TripRepository =
        TripRepository(database.tripDao(), geocoder)

    init {
        // Keep the recorder's grace period in sync with the setting: the user
        // can change it on the Devices screen while the recorder exists.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settings.gracePeriodMinutes.collect { minutes ->
                tripRecorder.updateGracePeriodMillis(minutes * 60_000L)
            }
        }
    }
}
