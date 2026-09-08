package com.terman37.triplogger.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * [PairedDevicesSource] from the system Bluetooth adapter.
 *
 * Reading the bonded-device list needs the BLUETOOTH_CONNECT runtime
 * permission (Android 12+). Missing permission → empty list; the UI requests
 * the permission separately and refreshes.
 */
class BluetoothPairedDevicesSource(context: Context) : PairedDevicesSource {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?

    @SuppressLint("MissingPermission") // guarded by the checkSelfPermission below
    override fun list(): List<PairedDeviceInfo> {
        val adapter = bluetoothManager?.adapter ?: return emptyList() // no BT hardware
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission || !adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.map { PairedDeviceInfo(it.address, it.name ?: it.address) }
    }
}
