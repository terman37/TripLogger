package com.terman37.triplogger.data

/**
 * A Bluetooth device the user registered as a trip trigger (docs/UI.md Devices
 * screen). The MAC [address] is the stable identity — the [name] is what the
 * device reports and can change (user renames it in Android settings).
 */
data class RegisteredDevice(
    val address: String,
    val name: String,
)
