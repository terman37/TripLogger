package com.terman37.triplogger.ui.common

import android.content.Intent
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.terman37.triplogger.ui.home.TripRowUi

/**
 * One trip row, shared by Home (recent list) and Report (preview, UI.md):
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
    val mapsUrl = remember(row.id) {
        DirectionsUrl.build(
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
                // Directions shortcut on the recap row (user request): the icon
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
                Text("From", style = MaterialTheme.typography.labelLarge)
                Text(row.startAddressText, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("To", style = MaterialTheme.typography.labelLarge)
                Text(row.endAddressText, style = MaterialTheme.typography.bodyMedium)
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDelete) { Text("Delete") }
                    }
                }
            }
        }
    }
}
