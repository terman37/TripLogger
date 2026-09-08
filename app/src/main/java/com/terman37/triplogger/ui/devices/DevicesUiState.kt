package com.terman37.triplogger.ui.devices

/**
 * UI state for the Devices screen (UI.md). Rendered directly by
 * DevicesScreen — produced by [DevicesStateMapper] from settings + paired
 * devices, so the mapping rules are JVM-testable.
 */
data class DevicesUiState(
    /** Master "Monitor trips" switch. */
    val monitoringEnabled: Boolean,
    /** Grace period in minutes (1–15). */
    val graceMinutes: Int,
    /** False when no device is registered → switch disabled (UI.md). */
    val canEnableMonitoring: Boolean,
    /** True when all runtime permissions needed for monitoring are granted. */
    val permissionsGranted: Boolean,
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
