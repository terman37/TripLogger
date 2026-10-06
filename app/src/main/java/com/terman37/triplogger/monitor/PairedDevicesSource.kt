package com.terman37.triplogger.monitor

/**
 * One paired Bluetooth device offered by the system. [address] (MAC) is the
 * stable identity; [name] is what the device reports now.
 */
data class PairedDeviceInfo(
    val address: String,
    val name: String,
)

/**
 * Lists devices paired in Android settings. Thin boundary so the Devices
 * screen logic stays testable; no in-app pairing (decision, docs/UI.md): pairing
 * happens in Android settings.
 */
interface PairedDevicesSource {
    /** Empty when Bluetooth is off, permission missing, or nothing paired. */
    fun list(): List<PairedDeviceInfo>
}
