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
    /** Hint line under the Available section, or null when none is needed. */
    val bluetoothHint: BluetoothHint?,
)

/**
 * Which hint the Available section shows. Kept as an enum (not a String) so
 * [DevicesStateMapper] stays Android-free and testable, while the text itself
 * comes from resources — phase L1 of the localisation work (release_guide.md §6).
 */
enum class BluetoothHint {
    /** BLUETOOTH_CONNECT not granted: ask for access first. */
    NEED_PERMISSION,

    /** Permission granted, but the phone has nothing paired. */
    NO_PAIRED_DEVICES,
}

data class DeviceRow(
    val address: String,
    val name: String,
)
