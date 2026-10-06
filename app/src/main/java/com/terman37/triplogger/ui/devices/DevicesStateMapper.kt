package com.terman37.triplogger.ui.devices

import com.terman37.triplogger.data.RegisteredDevice
import com.terman37.triplogger.monitor.PairedDeviceInfo

/**
 * Pure mapping for the Settings tab (see [DevicesUiState]). Android-free so
 * the composition rules (exclusion, ordering, hints) are unit-tested.
 */
object DevicesStateMapper {

    fun toUi(
        graceMinutes: Int,
        registered: List<RegisteredDevice>,
        paired: List<PairedDeviceInfo>,
        hasBluetoothPermission: Boolean,
    ): DevicesUiState {
        // Display names: prefer the live name the system reports now; fall
        // back to the stored name (device may be temporarily out of range).
        val liveName = paired.associate { it.address to it.name }

        val registeredRows = registered.map { device ->
            DeviceRow(
                address = device.address,
                name = liveName[device.address] ?: device.name,
            )
        }

        val registeredAddresses = registered.mapTo(mutableSetOf()) { it.address }
        val available = paired
            .filterNot { it.address in registeredAddresses }
            .map { DeviceRow(it.address, it.name) }

        val hint = when {
            !hasBluetoothPermission -> "Allow Bluetooth access to see paired devices."
            paired.isEmpty() -> "No paired devices. Pair in Android settings, then come back."
            else -> null
        }

        return DevicesUiState(
            graceMinutes = graceMinutes,
            hasBluetoothPermission = hasBluetoothPermission,
            registered = registeredRows,
            available = available,
            bluetoothHint = hint,
        )
    }
}
