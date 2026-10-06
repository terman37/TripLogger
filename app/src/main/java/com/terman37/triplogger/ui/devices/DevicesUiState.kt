package com.terman37.triplogger.ui.devices

/**
 * UI state for the Settings tab (docs/UI.md). Rendered directly by
 * DevicesScreen — produced by [DevicesStateMapper] from settings + paired
 * devices, so the mapping rules are JVM-testable.
 *
 * Note: the master "Monitor trips" switch moved to the HOME screen (user
 * request); this screen only manages registered devices, the grace period and
 * Bluetooth listing permission.
 */
data class DevicesUiState(
    /** Grace period in minutes (1–15). */
    val graceMinutes: Int,
    /** True when BLUETOOTH_CONNECT is granted (needed to list paired devices). */
    val hasBluetoothPermission: Boolean,
    /** Registered trigger devices (address + last known name). */
    val registered: List<DeviceRow>,
    /** Paired devices that are NOT registered yet. */
    val available: List<DeviceRow>,
    /** Hint line under the available section, or null. */
    val bluetoothHint: String?,
)

data class DeviceRow(
    val address: String,
    val name: String,
)
