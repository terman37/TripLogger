package com.terman37.triplogger.ui.devices

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.data.RegisteredDevice
import com.terman37.triplogger.monitor.BluetoothPairedDevicesSource
import com.terman37.triplogger.monitor.TripMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Devices screen logic: settings (monitoring, grace, registered devices) plus
 * the system's paired devices, mapped to [DevicesUiState].
 *
 * Permissions are requested by the UI (Compose launcher); this ViewModel only
 * reports whether they are all granted and reacts to the result via
 * [onPermissionsResult]. Enabling monitoring also (re)starts the trip service;
 * disabling stops it.
 */
class DevicesViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TripLoggerApplication).container
    private val settings = container.settings
    private val pairedSource = BluetoothPairedDevicesSource(application)

    // Paired list is not reactive on Android, so the UI asks us to re-read it
    // (after granting the permission, on resume, after a system re-pair).
    private val paired = MutableStateFlow(pairedSource.list())

    val uiState: StateFlow<DevicesUiState> =
        combine(
            settings.monitoringEnabled,
            settings.gracePeriodMinutes,
            settings.registeredDevices,
            paired,
        ) { monitoring, grace, registered, devices ->
            DevicesStateMapper.toUi(
                monitoringEnabled = monitoring,
                graceMinutes = grace,
                registered = registered,
                paired = devices,
                permissionsGranted = permissionsGranted(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesStateMapper.toUi(
                monitoringEnabled = settings.monitoringEnabled.value,
                graceMinutes = settings.gracePeriodMinutes.value,
                registered = settings.registeredDevices.value,
                paired = paired.value,
                permissionsGranted = permissionsGranted(),
            ),
        )

    /** Called by the UI after the runtime-permission dialog result. */
    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            paired.value = pairedSource.list() // list may need the permission
            settings.setMonitoringEnabled(true)
            TripMonitorService.startWithAction(getApplication(), TripMonitorService.ACTION_START)
        }
    }

    /** User toggled the master switch (permissions already granted). */
    fun setMonitoringEnabled(enabled: Boolean) {
        settings.setMonitoringEnabled(enabled)
        val action = if (enabled) TripMonitorService.ACTION_START
        else TripMonitorService.ACTION_STOP
        TripMonitorService.startWithAction(getApplication(), action)
    }

    fun setGracePeriodMinutes(minutes: Int) {
        settings.setGracePeriodMinutes(minutes)
    }

    fun addDevice(address: String, name: String) {
        settings.addRegisteredDevice(RegisteredDevice(address, name))
    }

    fun removeDevice(address: String) {
        settings.removeRegisteredDevice(address)
    }

    fun refreshPairedDevices() {
        paired.value = pairedSource.list()
    }

    /** Are all runtime permissions that monitoring needs granted? */
    fun permissionsGranted(): Boolean = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    ).all { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED }
}
