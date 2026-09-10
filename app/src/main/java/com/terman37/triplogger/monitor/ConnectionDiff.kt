package com.terman37.triplogger.monitor

/**
 * Pure diff between two polls of connected Bluetooth addresses. Kept Android-
 * free so the core detection rule ("what connected / disconnected since the
 * previous poll") is unit-tested; [BluetoothMonitor] only supplies the sets.
 *
 * First poll semantics: previous is empty, so every currently connected
 * device counts as a connect event — a device already linked when monitoring
 * is enabled starts a trip immediately (intended).
 */
object ConnectionDiff {

    data class Events(
        val connected: List<String>,
        val disconnected: List<String>,
    )

    fun diff(previous: Set<String>, current: Set<String>): Events = Events(
        connected = (current - previous).toList(),
        disconnected = (previous - current).toList(),
    )
}
