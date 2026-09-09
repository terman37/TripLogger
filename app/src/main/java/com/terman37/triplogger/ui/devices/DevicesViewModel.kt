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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Devices screen logic: grace period + registered devices + the system's
 * paired devices, mapped to [DevicesUiState]. The master monitoring switch
 * lives on Home (user request).
 *
 * Bluetooth listing permission is requested by the UI; [onBluetoothPermissionResult]
 * only refreshes the list after a grant.
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
            settings.gracePeriodMinutes,
            settings.registeredDevices,
            paired,
        ) { grace, registered, devices ->
            DevicesStateMapper.toUi(
                graceMinutes = grace,
                registered = registered,
                paired = devices,
                hasBluetoothPermission = hasBluetoothPermission(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesStateMapper.toUi(
                graceMinutes = settings.gracePeriodMinutes.value,
                registered = settings.registeredDevices.value,
                paired = paired.value,
                hasBluetoothPermission = hasBluetoothPermission(),
            ),
        )

    /**
     * Called after the standalone "Allow Bluetooth access" request. Only
     * refreshes the paired list.
     */
    fun onBluetoothPermissionResult(granted: Boolean) {
        if (granted) paired.value = pairedSource.list()
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

    private fun hasBluetoothPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
}
