package com.terman37.triplogger.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.terman37.triplogger.MainActivity
import com.terman37.triplogger.R
import com.terman37.triplogger.core.TripRecorder
import java.util.Locale

/**
 * Builds the monitoring notification (the foreground-service notification and
 * the monitoring indicator).
 *
 * Extracted from the service so it can be exercised directly by an instrumented
 * TEST: a missing small icon makes NotificationManager throw ("Invalid
 * notification") and crashes the service — that regression class is covered by
 * `NotificationFactoryTest` without the flakiness of starting a foreground
 * service from instrumentation.
 */
object NotificationFactory {

    const val CHANNEL_ID = "trip_monitor"
    const val NOTIFICATION_ID = 1

    /** Creates the notification channel (idempotent; call once per process). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW, // silent
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Notification text: always leads with "Monitoring active", then what is
     * happening.
     */
    fun textFor(
        context: Context,
        phase: TripRecorder.Phase,
        distanceKm: Double,
    ): String = when (phase) {
        TripRecorder.Phase.RECORDING -> String.format(
            Locale.US, "%s — %s %.1f km",
            context.getString(R.string.notification_monitoring_text),
            context.getString(R.string.notification_recording_label),
            distanceKm,
        )
        TripRecorder.Phase.GRACE -> String.format(
            Locale.US, "%s — %s",
            context.getString(R.string.notification_monitoring_text),
            context.getString(R.string.notification_disconnected_label),
        )
        TripRecorder.Phase.IDLE -> context.getString(R.string.notification_monitoring_text)
    }

    fun create(
        context: Context,
        phase: TripRecorder.Phase,
        distanceKm: Double,
    ): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // Swiping the notification away must stop monitoring (receiver →
        // service), it must never run invisibly.
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationDismissReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(textFor(context, phase, distanceKm))
            // White-on-transparent car glyph: notification icons must be
            // monochrome, and the system rejects notifications without one.
            .setSmallIcon(R.drawable.ic_stat_triplogger)
            .setOngoing(true)
            .setContentIntent(openApp)
            .setDeleteIntent(dismissIntent)
            .build()
    }
}
