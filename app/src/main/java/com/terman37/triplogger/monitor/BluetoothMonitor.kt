package com.terman37.triplogger.monitor

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Watches Bluetooth ACL connection events for the whole device.
 *
 * Why ACL broadcasts (decision, plan.md): Android broadcasts
 * ACTION_ACL_CONNECTED / ACTION_ACL_DISCONNECTED whenever ANY paired device
 * establishes/drops its link-layer connection — including a car head unit. No
 * profile (HFP/A2DP) support is required. The receiver must be REGISTERED
 * (dynamic) — it only works while the trip service is running, which is what
 * we want: no service, no monitoring.
 *
 * The listener is called with the raw device; the service decides whether the
 * device is registered and forwards the event to the recorder.
 */
class BluetoothMonitor(
    private val context: Context,
    private val onDeviceConnected: (device: BluetoothDevice) -> Unit,
    private val onDeviceDisconnected: (device: BluetoothDevice) -> Unit,
) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // EXTRA_DEVICE carries the device the event is about; Bluetooth
            // may deliver duplicates (one per profile), which the recorder's
            // state machine tolerates (events in the wrong state are no-ops).
            val device: BluetoothDevice =
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> onDeviceConnected(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDeviceDisconnected(device)
            }
        }
    }

    /** Registers the receiver; safe to call twice (no-op). */
    fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        // Android 14 requires an explicit exported flag for dynamic receivers.
        // NOT_EXPORTED is correct here: ACL broadcasts are protected system
        // broadcasts, delivered to our receiver regardless.
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
    }

    /** Unregisters the receiver; safe to call twice. */
    fun unregister() {
        if (!registered) return
        context.unregisterReceiver(receiver)
        registered = false
    }
}
