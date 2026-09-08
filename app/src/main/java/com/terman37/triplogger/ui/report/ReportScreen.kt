@file:OptIn(ExperimentalMaterial3Api::class) // DatePicker APIs are still experimental

package com.terman37.triplogger.ui.report

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terman37.triplogger.ui.common.TripRowCard
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Report tab (UI.md): pick a date range (default last month) → Generate →
 * summary + preview → Export spreadsheet (CSV, shared through the Android
 * share sheet).
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val scope = rememberCoroutineScope()

    // Screen-owned state (nothing persisted between app runs).
    var from by remember { mutableStateOf(viewModel.defaultFrom()) }
    var to by remember { mutableStateOf(viewModel.defaultTo()) }
    var generating by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<ReportData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerFor by remember { mutableStateOf<DateField?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }

    // Live selection inside the date picker dialog (reset when opened).
    val datePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("From", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { pickerFor = DateField.FROM }) { Text(from.toString()) }

        Text("To", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { pickerFor = DateField.TO }) { Text(to.toString()) }

        if (from.isAfter(to)) {
            Text(
                "From must not be after To.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    generating = true
                    error = null
                    data = viewModel.generate(from, to)
                    expandedIds = emptySet()
                    generating = false
                }
            },
            enabled = !generating && !from.isAfter(to),
        ) {
            Text(if (generating) "Generating…" else "Generate")
        }

        val current = data
        if (current != null) {
            Spacer(Modifier.height(16.dp))
            val rowById = remember(current) {
                viewModel.toRows(current).associateBy { it.id }
            }
            Text(
                text = String.format(
                    Locale.US, "%d %s · %.1f km",
                    current.trips.size,
                    if (current.trips.size == 1) "trip" else "trips",
                    current.totalKm,
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            if (current.trips.isEmpty()) {
                Text(
                    "No trips in this period",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            } else {
                // Chronological preview (DAO order); expanding shows the same
                // detail as Home. No delete here: this view mirrors the data.
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(current.trips, key = { it.id }) { trip ->
                        val row = rowById[trip.id] ?: return@items
                        TripRowCard(
                            row = row,
                            expanded = trip.id in expandedIds,
                            onToggle = {
                                expandedIds = if (trip.id in expandedIds) {
                                    expandedIds - trip.id
                                } else {
                                    expandedIds + trip.id
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            error = null
                            val file = viewModel.exportCsv(from, to)
                            if (file != null) {
                                shareCsv(context, file)
                            } else {
                                error = "Export failed"
                            }
                        }
                    },
                ) {
                    Text("Export spreadsheet")
                }
            }
        }
    }

    pickerFor?.let { field ->
        DatePickerDialog(
            onDismissRequest = { pickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                            if (field == DateField.FROM) from = date else to = date
                        }
                        pickerFor = null
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickerFor = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** The two date fields of the range picker. */
private enum class DateField { FROM, TO }

/** Sends the CSV via the system share sheet (decision, todo.md). */
private fun shareCsv(context: Context, file: File) {
    val uri: android.net.Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export report"))
}
