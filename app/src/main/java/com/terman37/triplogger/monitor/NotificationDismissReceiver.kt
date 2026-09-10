package com.terman37.triplogger.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the foreground notification's deleteIntent when the user swipes the
 * notification away (Android allows dismissing FGS notifications).
 *
 * The notification IS the monitoring indicator: once it is gone, monitoring
 * must not stay silently "on" (bug found on device, plan.md Step 12). The
 * receiver forwards the event to the service, which finishes any running trip
 * (saving it), turns the master switch off and stops.
 *
 * Declared in the manifest with exported=false; the PendingIntent targets this
 * exact component, so no other app can trigger it.
 */
class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        TripMonitorService.startWithAction(
            context,
            TripMonitorService.ACTION_NOTIFICATION_DISMISSED,
        )
    }
}
