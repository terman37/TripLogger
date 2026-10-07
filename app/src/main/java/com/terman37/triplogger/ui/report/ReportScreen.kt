@file:OptIn(ExperimentalMaterial3Api::class) // DatePicker APIs are still experimental

package com.terman37.triplogger.ui.report

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terman37.triplogger.R
import com.terman37.triplogger.ui.common.TripRowCard
import com.terman37.triplogger.ui.theme.DestructiveRed
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Report tab:
 * - From/To date filters side by side on top (default: last 7 days),
 * - the trip list below, ALWAYS shown for the selected range (no Generate —
 *   it reloads when the dates change),
 * - export button pinned bottom-right.
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val scope = rememberCoroutineScope()

    var from by remember { mutableStateOf(viewModel.defaultFrom()) }
    var to by remember { mutableStateOf(viewModel.defaultTo()) }
    var data by remember { mutableStateOf<ReportData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerFor by remember { mutableStateOf<DateField?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }
    var deletePending by remember { mutableStateOf(false) }

    // Error and share-sheet texts are resolved here: stringResource() is a
    // @Composable function and cannot be called inside the click lambdas below.
    val deletionFailed = stringResource(R.string.error_deletion_failed)
    val exportFailed = stringResource(R.string.error_export_failed)
    val exportSubject = stringResource(R.string.report_export_subject)

    val datePickerState = rememberDatePickerState()

    // Live reload: every date change re-queries (no Generate button anymore).
    LaunchedEffect(from, to) {
        data = viewModel.load(from, to)
        expandedIds = emptySet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- top: date filters side by side --------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateFieldButton(
                label = stringResource(R.string.report_date_from),
                date = from,
                onClick = { pickerFor = DateField.FROM },
                modifier = Modifier.weight(1f),
            )
            DateFieldButton(
                label = stringResource(R.string.report_date_to),
                date = to,
                onClick = { pickerFor = DateField.TO },
                modifier = Modifier.weight(1f),
            )
        }
        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // --- middle: always-visible trip list ------------------------------
        val current = data
        if (current != null) {
            Text(
                text = pluralStringResource(
                    R.plurals.report_summary,
                    current.trips.size,
                    current.trips.size,
                    current.totalKm,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (current == null || current.trips.isEmpty()) {
            Text(
                stringResource(R.string.report_empty),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 48.dp),
            )
        } else {
            val rowById = remember(current) {
                viewModel.toRows(current).associateBy { it.id }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(current.trips, key = { it.id }) { trip ->
                    val row = rowById[trip.id] ?: return@items
                    TripRowCard(
                        row = row,
                        expanded = trip.id in expandedIds,
                        onDelete = {
                            scope.launch {
                                val ok = viewModel.deleteTrip(trip.id)
                                if (ok) {
                                    data = viewModel.load(from, to)
                                    expandedIds = expandedIds - trip.id
                                } else {
                                    error = deletionFailed
                                }
                            }
                        },
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
        }

        // --- bottom-right footer: export -----------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            // Trash on the far left, export on the far right.
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cleanup of ALL trips shown for the current filter dates.
            val displayedCount = current?.trips?.size ?: 0
            // Bulk delete: filled red button with a white trash icon, sized like
            // the export button.
            Button(
                onClick = { deletePending = true },
                enabled = displayedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DestructiveRed,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.report_delete_all_description),
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        error = null
                        val file = viewModel.exportXlsx(from, to)
                        if (file != null) {
                            // Export triggers the lazy address retry: refresh
                            // the preview so newly geocoded addresses show up.
                            data = viewModel.load(from, to)
                            shareReport(context, file, exportSubject)
                        } else {
                            error = exportFailed
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.report_export))
            }
        }
    }

    // Bulk-delete confirmation: several trips may be affected,
    // and deletion is permanent.
    if (deletePending) {
        val count = data?.trips?.size ?: 0
        AlertDialog(
            onDismissRequest = { deletePending = false },
            title = {
                Text(pluralStringResource(R.plurals.report_delete_confirm, count, count))
            },
            text = {
                Text(
                    stringResource(
                        R.string.report_delete_body,
                        from.toString(),
                        to.toString(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = DestructiveRed,
                    ),
                    onClick = {
                        deletePending = false
                        scope.launch {
                            val ok = viewModel.deleteRange(from, to)
                            if (ok) {
                                data = viewModel.load(from, to)
                                expandedIds = emptySet()
                            } else {
                                error = deletionFailed
                            }
                        }
                    },
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletePending = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // --- date picker dialog (shared by both fields) ------------------------
    pickerFor?.let { field ->
        DatePickerDialog(
            onDismissRequest = { pickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val picked = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                            if (field == DateField.FROM) {
                                from = picked
                                // Keep the range valid: From after To clamps To.
                                if (picked.isAfter(to)) to = picked
                            } else {
                                to = picked
                                if (picked.isBefore(from)) from = picked
                            }
                        }
                        pickerFor = null
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerFor = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** One half of the side-by-side range filter. */
@Composable
private fun DateFieldButton(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(date.toString(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** The two date fields of the range picker. */
private enum class DateField { FROM, TO }

/** Sends the Excel report via the system share sheet (decision). */
private fun shareReport(context: Context, file: File, chooserTitle: String) {
    val uri: android.net.Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        // MIME type of an .xlsx workbook, so mail clients/sheets offer to open it.
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
