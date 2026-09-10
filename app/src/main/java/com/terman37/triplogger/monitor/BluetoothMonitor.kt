package com.terman37.triplogger.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches Bluetooth connections of the registered devices.
 *
 * IMPLEMENTATION NOTE (found on device, Android 17): the
 * classic ACTION_ACL_CONNECTED/DISCONNECTED broadcasts are NOT delivered to
 * this app anymore. There is also no per-device "connection state" API. So
 * this monitor keeps PROFILE PROXIES (A2DP + HEADSET + sink variants — covers
 * car units and headsets) and POLLS [BluetoothProfile.getConnectedDevices].
 *
 * Poll interval 10 s: a trip start is delayed by at most one interval (the
 * first GPS fix takes up to 30 s anyway) and the grace period (minutes)
 * absorbs the same delay on disconnect.
 */
class BluetoothMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onDeviceConnected: (device: BluetoothDevice) -> Unit,
    private val onDeviceDisconnected: (device: BluetoothDevice) -> Unit,
) {
    private val appContext = context.applicationContext
    private val pollIntervalMillis = 10_000L

    // Profiles whose connection state we track (proxy connected = poll it).
    private val profiles = listOf(
        BluetoothProfile.A2DP,      // phone streams music to the car unit / headset
        BluetoothProfile.HEADSET,   // car unit / headset hands-free (HFP)
        BluetoothProfile.LE_AUDIO,  // newer LE-audio car units and headsets
    )

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter

    private val proxies = mutableMapOf<Int, BluetoothProfile>()
    private val proxyRequested = mutableSetOf<Int>()

    private var registered = false
    private var pollJob: Job? = null

    // Addresses connected at the last poll (union over all profiles).
    private var lastConnected = emptySet<String>()

    // One ServiceListener serves all profile requests; onServiceConnected adds
    // the proxy, onServiceDisconnected removes it.
    @SuppressLint("MissingPermission") // guarded in pollOnce
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            synchronized(proxies) { proxies[profile] = proxy }
        }

        override fun onServiceDisconnected(profile: Int) {
            synchronized(proxies) { proxies.remove(profile) }
        }
    }

    /** Starts polling; safe to call twice. */
    fun register() {
        if (registered) return
        registered = true
        pollJob = scope.launch {
            while (isActive) {
                pollOnce()
                delay(pollIntervalMillis)
            }
        }
    }

    /** Stops polling; safe to call twice. */
    fun unregister() {
        if (!registered) return
        registered = false
        pollJob?.cancel()
        pollJob = null
        lastConnected = emptySet()
        adapter?.let { bt ->
            synchronized(proxies) {
                profiles.forEach { profile ->
                    proxies.remove(profile)?.let { bt.closeProfileProxy(profile, it) }
                }
            }
        }
        synchronized(proxyRequested) { proxyRequested.clear() }
    }

    private fun pollOnce() {
        val bt = adapter
        if (bt == null || !bt.isEnabled) return
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        // Ask the system for each proxy until it answers (async ServiceListener).
        synchronized(proxyRequested) {
            for (profile in profiles) {
                if (profile !in proxies && profile !in proxyRequested) {
                    proxyRequested += profile
                    try {
                        bt.getProfileProxy(appContext, serviceListener, profile)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "getProfileProxy failed for $profile", e)
                        proxyRequested -= profile
                    }
                }
            }
        }

        // Union of currently connected devices across ready proxies.
        val connected: Map<String, BluetoothDevice>
        synchronized(proxies) {
            val collected = mutableMapOf<String, BluetoothDevice>()
            for (proxy in proxies.values) {
                try {
                    proxy.getConnectedDevices().forEach { collected[it.address] = it }
                } catch (e: SecurityException) {
                    Log.w(TAG, "getConnectedDevices failed", e)
                }
            }
            connected = collected
        }
        if (connected.isEmpty() && proxies.isEmpty()) return // proxies still coming up

        // Detection rule is pure (ConnectionDiff) and unit-tested; this class
        // only maps addresses back to devices.
        val events = ConnectionDiff.diff(previous = lastConnected, current = connected.keys)
        for (address in events.connected) {
            val device = connected[address]
            if (device != null) {
                onDeviceConnected(device)
            }
        }
        for (address in events.disconnected) {
            onDeviceDisconnected(bt.getRemoteDevice(address))
        }
        lastConnected = connected.keys
    }

    private companion object {
        const val TAG = "BluetoothMonitor"
    }
}
