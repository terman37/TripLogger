package com.terman37.triplogger.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.Trip
import com.terman37.triplogger.monitor.TripMonitorService
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home screen logic. It combines three sources into one [HomeUiState]:
 *
 * - the shared [TripRecorder] snapshot (live status card),
 * - the monitoring setting + registered device names,
 * - recent trips (today + yesterday) from the database (reactive Flow: Room
 *   re-emits when a trip is inserted or deleted, so no manual refresh).
 *
 * The ViewModel does NOT call the recorder directly for start/stop: commands
 * go through [TripMonitorService] intents, the service owns the recorder and
 * the GPS plumbing (single entry point).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TripLoggerApplication).container
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<HomeUiState> =
        combine(
            container.tripRecorder.snapshotFlow,
            container.settings.monitoringEnabled,
            container.settings.registeredDevices,
            recentTripsFlow(),
        ) { snapshot, monitoring, devices, trips ->
            HomeStateMapper.toUi(
                snapshot = snapshot,
                monitoringEnabled = monitoring,
                deviceNames = devices.map { it.name },
                trips = trips,
                zone = zone,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeStateMapper.toUi(
                snapshot = TripRecorder.Snapshot(
                    TripRecorder.Phase.IDLE, 0.0, null, null, null,
                ),
                monitoringEnabled = container.settings.monitoringEnabled.value,
                deviceNames = container.settings.registeredDevices.value.map { it.name },
                trips = emptyList(),
                zone = zone,
            ),
        )

    /** Recent trips: everything from the start of YESTERDAY (UI.md). */
    private fun recentTripsFlow(): kotlinx.coroutines.flow.Flow<List<Trip>> {
        val startOfYesterday = LocalDate.now(zone)
            .minusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return container.tripRepository.tripsSinceFlow(startOfYesterday)
    }

    /** True when all runtime permissions that monitoring needs are granted. */
    fun monitoringPermissionsGranted(): Boolean = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    ).all { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED }

    /** Called after the runtime-permission dialog result (switch on Home). */
    fun onPermissionsResult(granted: Boolean) {
        if (granted) setMonitoringEnabled(true)
    }

    /** Master switch: starts/stops the trip service accordingly. */
    fun setMonitoringEnabled(enabled: Boolean) {
        container.settings.setMonitoringEnabled(enabled)
        val action = if (enabled) TripMonitorService.ACTION_START
        else TripMonitorService.ACTION_STOP
        TripMonitorService.startWithAction(getApplication(), action)
    }

    /** Fallback start button → trip service (which starts GPS etc.). */
    fun startTripManually() {
        TripMonitorService.startWithAction(
            getApplication(),
            TripMonitorService.ACTION_MANUAL_START,
        )
    }

    /** Stop button → ends the trip now (also aborts an ongoing grace wait). */
    fun stopTrip() {
        TripMonitorService.startWithAction(
            getApplication(),
            TripMonitorService.ACTION_MANUAL_STOP,
        )
    }

    /** Delete button on an expanded trip row. */
    fun deleteTrip(id: Long) {
        viewModelScope.launch {
            container.tripRepository.deleteTripById(id)
        }
    }
}
