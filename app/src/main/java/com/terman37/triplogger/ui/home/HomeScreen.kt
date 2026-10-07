package com.terman37.triplogger.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terman37.triplogger.R
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.ui.common.TripRowCard
import com.terman37.triplogger.text.rememberTripText
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Home tab (docs/UI.md): live status card on top, then the trip list of today and
 * yesterday. Pure rendering: all state comes from [HomeViewModel.uiState].
 *
 * @param onOpenSettings called when the user switches monitoring on before any
 *   device is registered: there is nothing to monitor yet, so the app sends
 *   them to the Settings tab (with the Available list opened) instead of
 *   silently doing nothing.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // Master switch on Home: enabling needs all monitoring
    // permissions; request them together if missing.
    val context = LocalContext.current
    // Set when the user is sent to system settings for "Allow all the time";
    // ON_RESUME then finishes enabling monitoring once it is granted.
    var pendingEnable by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        when (viewModel.permissionStep()) {
            MonitoringPermissionFlow.Step.READY -> viewModel.setMonitoringEnabled(true)
            MonitoringPermissionFlow.Step.OPEN_BACKGROUND_SETTINGS ->
                showBackgroundLocationDialog = true
            MonitoringPermissionFlow.Step.REQUEST_FOREGROUND -> Unit
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingEnable &&
                viewModel.permissionStep() == MonitoringPermissionFlow.Step.READY
            ) {
                viewModel.setMonitoringEnabled(true)
                pendingEnable = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showBackgroundLocationDialog) {
        BackgroundLocationDialog(
            onOpenSettings = {
                pendingEnable = true
                showBackgroundLocationDialog = false
                // Android 11+ cannot ask for background location in the runtime
                // dialog, so send the user to the app's system settings page.
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
            onDismiss = { showBackgroundLocationDialog = false },
        )
    }

    // Which expanded trip rows the user opened (remembered across tab
    // switches; not in the ViewModel — pure UI concern).
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }
    // Live "now" for the elapsed-time display; ticks every second while the
    // screen is visible.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonitorSwitchRow(
            state = uiState,
            onOpenSettings = onOpenSettings,
            onToggle = { enable ->
                if (enable) {
                    when (viewModel.permissionStep()) {
                        MonitoringPermissionFlow.Step.READY ->
                            viewModel.setMonitoringEnabled(true)
                        MonitoringPermissionFlow.Step.REQUEST_FOREGROUND ->
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ),
                            )
                        MonitoringPermissionFlow.Step.OPEN_BACKGROUND_SETTINGS ->
                            showBackgroundLocationDialog = true
                    }
                } else {
                    viewModel.setMonitoringEnabled(false)
                }
            },
        )

        StatusCard(
            state = uiState.card,
            now = now,
            zone = java.time.ZoneId.systemDefault(),
            onStartManual = viewModel::startTripManually,
            onStop = viewModel::stopTrip,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (!uiState.hasTrips) {
            Text(
                text = stringResource(R.string.home_no_trips),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            // Heading above the list (trip rows carry their own date+time).
            Text(
                text = stringResource(R.string.home_recent_trips),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.recentTrips, key = { it.id }) { row ->
                    TripRowCard(
                        row = row,
                        expanded = row.id in expandedIds,
                        onToggle = {
                            expandedIds = if (row.id in expandedIds) {
                                expandedIds - row.id
                            } else {
                                expandedIds + row.id
                            }
                        },
                        onDelete = { viewModel.deleteTrip(row.id) },
                    )
                }
            }
        }
    }

}

// --- status card ---------------------------------------------------------

@Composable
private fun StatusCard(
    state: CardUiState,
    now: Long,
    zone: ZoneId,
    onStartManual: () -> Unit,
    onStop: () -> Unit,
) {
    // Texts for the live distance / duration, from resources (phase L1b).
    val tripText = rememberTripText()

    // Container color gives an instant visual cue (docs/UI.md: green/neutral/gray).
    val container = when (state) {
        is CardUiState.Recording -> MaterialTheme.colorScheme.primaryContainer
        is CardUiState.GracePeriod -> MaterialTheme.colorScheme.tertiaryContainer
        is CardUiState.Waiting -> MaterialTheme.colorScheme.secondaryContainer
        is CardUiState.MonitoringOff -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state) {
                is CardUiState.MonitoringOff -> {
                    Text(
                        stringResource(R.string.home_card_off_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.home_card_off_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is CardUiState.Waiting -> {
                    val who = if (state.deviceNames.size > 1) {
                        stringResource(R.string.home_card_waiting_many)
                    } else {
                        state.deviceNames.firstOrNull()
                            ?: stringResource(R.string.home_card_waiting_any)
                    }
                    Text(
                        stringResource(R.string.home_card_waiting_title, who),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.home_card_waiting_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStartManual) {
                        Text(stringResource(R.string.home_start_manually))
                    }
                }

                is CardUiState.Recording -> {
                    Text(
                        stringResource(R.string.home_card_recording_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = if (state.origin == TripOrigin.MANUAL) {
                            stringResource(R.string.home_card_recording_manual)
                        } else {
                            stringResource(
                                R.string.home_card_recording_device,
                                state.deviceName
                                    ?: stringResource(R.string.home_card_recording_device_fallback),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = tripText.kmText(state.distanceKm),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = tripText.durationText(state.startedAtEpochMillis, now),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStop) { Text(stringResource(R.string.home_stop)) }
                }

                is CardUiState.GracePeriod -> {
                    Text(
                        stringResource(R.string.home_card_grace_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.home_card_grace_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(
                            R.string.home_card_grace_since,
                            tripText.durationText(state.graceStartedAtEpochMillis, now),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = tripText.kmText(state.distanceKm),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStop) { Text(stringResource(R.string.home_stop_now)) }
                }
            }
        }
    }
}

/** Master "Monitor trips" switch pinned to the top of Home. */
@Composable
private fun MonitorSwitchRow(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_switch_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (state.canEnableMonitoring) {
                    stringResource(R.string.home_switch_hint_ready)
                } else {
                    // First run: switching on cannot work yet (no device), so
                    // the switch routes to Settings instead.
                    stringResource(R.string.home_switch_hint_setup)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = state.monitoringEnabled,
            onCheckedChange = { enable ->
                if (state.canEnableMonitoring) {
                    onToggle(enable)
                } else {
                    onOpenSettings()
                }
            },
            // Always tappable: without a registered device the tap opens the
            // Settings tab instead of toggling monitoring (see above).
            enabled = true,
        )
    }
}

/**
 * Explains why monitoring needs location "Allow all the time" and sends the
 * user to the app's system settings to change it (Android 11+ cannot ask for
 * background location in the runtime dialog).
 */
@Composable
private fun BackgroundLocationDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_location_dialog_title)) },
        text = { Text(stringResource(R.string.background_location_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.background_location_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
