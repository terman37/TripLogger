package com.terman37.triplogger.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.ui.common.TripRowCard
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Home tab (UI.md): live status card on top, then the trip list of today and
 * yesterday. Pure rendering: all state comes from [HomeViewModel.uiState].
 *
 * @param onOpenDevices jump to the Devices tab (used by the card when
 *   monitoring is off, UI.md).
 */
@Composable
fun HomeScreen(
    onOpenDevices: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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
        StatusCard(
            state = uiState.card,
            now = now,
            zone = java.time.ZoneId.systemDefault(),
            onOpenDevices = onOpenDevices,
            onStartManual = viewModel::startTripManually,
            onStop = viewModel::stopTrip,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (!uiState.hasTrips) {
            Text(
                text = "No trips yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        } else {
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
    onOpenDevices: () -> Unit,
    onStartManual: () -> Unit,
    onStop: () -> Unit,
) {
    // Container color gives an instant visual cue (UI.md: green/neutral/gray).
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
                    Text("Monitoring off", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Trips are not recorded automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenDevices) { Text("Enable monitoring") }
                }

                is CardUiState.Waiting -> {
                    val who = if (state.deviceNames.size > 1) {
                        "one of your registered devices"
                    } else {
                        state.deviceNames.firstOrNull() ?: "a registered device"
                    }
                    Text("Waiting for $who…", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Trip starts automatically when it connects.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStartManual) { Text("Start manually") }
                }

                is CardUiState.Recording -> {
                    Text("Recording", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (state.origin == TripOrigin.MANUAL) {
                            "Started manually"
                        } else {
                            "Connected: ${state.deviceName ?: "car"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = TripText.kmText(state.distanceKm),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = TripText.durationText(state.startedAtEpochMillis, now),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStop) { Text("Stop") }
                }

                is CardUiState.GracePeriod -> {
                    Text("Disconnected", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Finishing trip — reconnect to resume.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = TripText.kmText(state.distanceKm),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStop) { Text("Stop now") }
                }
            }
        }
    }
}
