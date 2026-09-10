package com.terman37.triplogger.settings

import com.terman37.triplogger.data.RegisteredDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * All user-adjustable settings (UI.md Devices screen ).
 * Implementations persist values (SharedPreferences) and expose them as
 * StateFlows so the UI updates automatically when settings change.
 *
 * Legal values:
 * - monitoring ON is only meaningful with >= 1 registered device (UI enforces).
 * - grace period is clamped to [MIN_GRACE_MINUTES]..[MAX_GRACE_MINUTES].
 */
interface SettingsRepository {

    /** Master switch: auto-start trips when a registered device connects. */
    val monitoringEnabled: StateFlow<Boolean>

    /** Bluetooth disconnect grace period in minutes (default 3). */
    val gracePeriodMinutes: StateFlow<Int>

    /** Devices that trigger recording (ordered as the user added them). */
    val registeredDevices: StateFlow<List<RegisteredDevice>>

    fun setMonitoringEnabled(enabled: Boolean)

    /** Clamps the value to the legal range. */
    fun setGracePeriodMinutes(minutes: Int)

    /** Adds a device; no duplicate addresses. */
    fun addRegisteredDevice(device: RegisteredDevice)

    /** Removes a device by MAC address. */
    fun removeRegisteredDevice(address: String)

    companion object {
        const val MIN_GRACE_MINUTES = 1
        const val MAX_GRACE_MINUTES = 15
        const val DEFAULT_GRACE_MINUTES = 3

        /** Legal grace-period value for any input (pure, unit-tested). */
        fun clampGraceMinutes(minutes: Int): Int =
            minutes.coerceIn(MIN_GRACE_MINUTES, MAX_GRACE_MINUTES)

        /** Adds a device unless its address is already registered (pure). */
        fun withDeviceAdded(
            devices: List<RegisteredDevice>,
            device: RegisteredDevice,
        ): List<RegisteredDevice> =
            if (devices.any { it.address == device.address }) devices else devices + device

        /** Removes the device with [address], if present (pure). */
        fun withDeviceRemoved(
            devices: List<RegisteredDevice>,
            address: String,
        ): List<RegisteredDevice> = devices.filterNot { it.address == address }
    }
}
