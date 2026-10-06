package com.terman37.triplogger.ui.common

import android.content.Intent
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.terman37.triplogger.core.MapsUrl
import com.terman37.triplogger.ui.home.TripRowUi
import com.terman37.triplogger.ui.theme.DestructiveRed

/**
 * One trip row, shared by Home (recent list) and Report (preview, docs/UI.md):
 * collapsed shows date+time, "Home → Office" summary and km; tapping expands
 * to the full detail (times, duration, addresses). Delete appears only when
 * [onDelete] is provided.
 */
@Composable
fun TripRowCard(
    row: TripRowUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    // Delete needs confirmation: the trash only opens a dialog,
    // the actual [onDelete] runs on confirm.
    var confirmDelete by remember { mutableStateOf(false) }
    val mapsUrl = remember(row.id) {
        MapsUrl.directions(
            originStreet = row.startStreet,
            originCity = row.startCity,
            originLat = row.startLat,
            originLng = row.startLng,
            destinationStreet = row.endStreet,
            destinationCity = row.endCity,
            destinationLat = row.endLat,
            destinationLng = row.endLng,
        )
    }

    val startMapsUrl = remember(row.id) {
        MapsUrl.place(row.startStreet, row.startCity, row.startLat, row.startLng)
    }
    val endMapsUrl = remember(row.id) {
        MapsUrl.place(row.endStreet, row.endCity, row.endLat, row.endLng)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onToggle),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.titleMedium)
                    Text(row.summary, style = MaterialTheme.typography.bodyMedium)
                }
                // Directions shortcut on the recap row: the icon
                // opens Google Maps between start and end; hidden when neither
                // side has any location data. Its own click does not expand.
                if (mapsUrl != null) {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "Open directions in Google Maps",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Text(
                    row.distanceText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(row.timeRangeText, style = MaterialTheme.typography.bodyMedium)
                Text("Duration: ${row.durationText}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                AddressLine(
                    label = "From",
                    address = row.startAddressText,
                    mapsUrl = startMapsUrl,
                )
                Spacer(Modifier.height(4.dp))
                AddressLine(
                    label = "To",
                    address = row.endAddressText,
                    mapsUrl = endMapsUrl,
                    trailing = {
                        if (onDelete != null) {
                            IconButton(
                                onClick = { confirmDelete = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = DestructiveRed,
                                ),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete trip")
                            }
                        }
                    },
                )
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trip?") },
            text = {
                Text("This trip will be permanently removed from your records and reports.")
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = DestructiveRed,
                    ),
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * "From"/"To" label + address, preceded by an icon-only button that opens
 * that single place in Google Maps. The button is hidden when the endpoint has
 * neither address nor coordinates.
 */
@Composable
private fun AddressLine(
    label: String,
    address: String,
    mapsUrl: String?,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (mapsUrl != null) {
            val context = LocalContext.current
            IconButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
            }) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Open $label in Google Maps",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(address, style = MaterialTheme.typography.bodyMedium)
        }
        // Trailing action (delete trash) sits at the bottom of the two-line row
        // instead of being vertically centered.
        Box(modifier = Modifier.align(Alignment.Bottom)) { trailing() }
    }
}
