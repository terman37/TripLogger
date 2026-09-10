package com.terman37.triplogger.ui.devices

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Devices tab (UI.md): registered device list (remove with X), paired devices
 * (add with +), reconnect grace slider. The master "Monitor trips" switch now
 * lives at the top of Home (user request). No in-app pairing — pairing happens
 * in Android settings; refreshPairedDevices re-reads on resume.
 *
 * The two lists are collapsible (user request): their headers stay visible with
 * the device count, the bodies hide. Registered starts expanded; Available
 * starts collapsed so the page stays short.
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

    // Listing paired devices needs only BLUETOOTH_CONNECT — granted on demand.
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
        Text(
            "Bluetooth devices",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // --- registered devices ------------------------------------------
        CollapsibleSection(
            title = "Registered",
            subtitle = "Devices that trigger a trip when they connect.",
            count = uiState.registered.size,
            initiallyExpanded = true,
        ) {
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
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        // --- available paired devices ------------------------------------
        CollapsibleSection(
            title = "Available",
            subtitle = "Devices paired in Android settings.",
            count = uiState.available.size,
            initiallyExpanded = false,
        ) {
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

/**
 * Section header (title + device count + expand/collapse chevron) that shows
 * [content] only while expanded. The header and [subtitle] are always visible,
 * so a collapsed list still explains itself and tells how many devices it
 * holds. State survives rotation through [rememberSaveable].
 */
@Composable
private fun CollapsibleSection(
    title: String,
    subtitle: String,
    count: Int,
    initiallyExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
        )
    }
    Text(subtitle, style = MaterialTheme.typography.bodySmall)
    if (expanded) {
        Spacer(Modifier.height(8.dp))
        content()
    }
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
