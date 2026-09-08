package com.terman37.triplogger.ui.devices

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Devices tab (UI.md): master switch, grace-period slider, registered device
 * list (remove with X) and paired devices (add with +). No in-app pairing —
 * pairing happens in Android settings; refreshPairedDevices re-reads the list
 * on resume so newly paired devices appear.
 */
@Composable
fun DevicesScreen(viewModel: DevicesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh the paired-device list when the screen comes back to the front
    // (user may have paired something in Android settings meanwhile).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPairedDevices()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Runtime permissions are requested together, once, when the user tries to
    // enable monitoring without them. BLUETOOTH_CONNECT + FINE_LOCATION are
    // needed by the service, POST_NOTIFICATIONS for its notification.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onPermissionsResult(results.values.all { it })
    }

    // Standalone Bluetooth access request: listing paired devices needs only
    // BLUETOOTH_CONNECT, so the user can grant it BEFORE registering any
    // device (no monitoring enable required — avoids a UX deadlock).
    val bluetoothAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onBluetoothPermissionResult(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        MasterSwitchRow(
            state = uiState,
            onToggle = { enable ->
                if (enable) {
                    if (uiState.permissionsGranted) {
                        viewModel.setMonitoringEnabled(true)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ),
                        )
                    }
                } else {
                    viewModel.setMonitoringEnabled(false)
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()

        // --- registered devices ------------------------------------------
        SectionTitle("Registered")
        Text(
            "Devices that trigger a trip when they connect.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        if (uiState.registered.isEmpty()) {
            Text(
                "No device registered",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        uiState.registered.forEach { row ->
            DeviceRowItem(
                name = row.name,
                address = row.address,
                trailing = {
                    IconButton(onClick = { viewModel.removeDevice(row.address) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${row.name}")
                    }
                },
            )
        }

        // --- available paired devices ------------------------------------
        SectionTitle("Available")
        Text(
            "Devices paired in Android settings.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        if (!uiState.hasBluetoothPermission) {
            Text(
                "Bluetooth access is needed to see paired devices.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    bluetoothAccessLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                },
            ) {
                Text("Allow Bluetooth access")
            }
        } else {
            uiState.bluetoothHint?.let { hint ->
                Text(hint, style = MaterialTheme.typography.bodyMedium)
            }
        }
        uiState.available.forEach { row ->
            DeviceRowItem(
                name = row.name,
                address = row.address,
                trailing = {
                    IconButton(onClick = { viewModel.addDevice(row.address, row.name) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Register ${row.name}")
                    }
                },
            )
        }

        // --- grace period slider ------------------------------------------
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reconnect grace period", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Minutes after a Bluetooth drop before the trip ends.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "${uiState.graceMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Slider(
            value = uiState.graceMinutes.toFloat(),
            onValueChange = { viewModel.setGracePeriodMinutes(it.toInt()) },
            valueRange = 1f..15f,
            steps = 13, // 15 positions total, labels handled by the row above
        )
    }
}

@Composable
private fun MasterSwitchRow(state: DevicesUiState, onToggle: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Monitor trips", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Record a trip whenever a registered device connects.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = state.monitoringEnabled,
                onCheckedChange = onToggle,
                // UI.md: can only be ON with at least one registered device.
                enabled = state.canEnableMonitoring || state.monitoringEnabled,
            )
        }
        if (state.monitoringEnabled && !state.permissionsGranted) {
            Text(
                "Some permissions were revoked — monitoring is paused.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (!state.canEnableMonitoring) {
            Text(
                "Register a device below to enable monitoring.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun DeviceRowItem(
    name: String,
    address: String,
    trailing: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(address, style = MaterialTheme.typography.bodySmall) },
        trailingContent = trailing,
    )
}
