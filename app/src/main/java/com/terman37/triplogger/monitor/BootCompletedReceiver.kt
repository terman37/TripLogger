package com.terman37.triplogger.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.terman37.triplogger.TripLoggerApplication

/**
 * Restarts trip monitoring after the phone reboots or the app is updated.
 *
 * WHY THIS IS NEEDED: a foreground service is killed by a reboot, and Android
 * does NOT restart it automatically (`START_STICKY` only covers a process kill
 * while the device stays on). Without this receiver the persisted
 * "monitoring = ON" setting would survive the reboot while the service stays
 * dead: the Home switch would show ON, but no notification would appear and
 * nothing would be recorded — silent monitoring, which the app must never do
 * (DETAILS.md "Notification = monitoring indicator").
 *
 * It handles two system broadcasts:
 * - `ACTION_BOOT_COMPLETED` — the device finished booting.
 * - `ACTION_MY_PACKAGE_REPLACED` — the app was updated; installing an update
 *   also kills the running foreground service.
 *
 * The actual decision lives in [BootStartDecision] (pure, unit-tested); this
 * class is only Android glue.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Defence in depth: the manifest intent-filter already limits the
        // actions, but never act on anything unexpected.
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val settings = (context.applicationContext as TripLoggerApplication).container.settings
        val shouldStart = BootStartDecision.shouldStartOnBoot(
            monitoringEnabled = settings.monitoringEnabled.value,
            registeredDeviceCount = settings.registeredDevices.value.size,
        )
        if (!shouldStart) return

        runCatching {
            TripMonitorService.startWithAction(context, TripMonitorService.ACTION_START)
        }.onFailure { error ->
            // The foreground service could not be started (for example an OEM
            // autostart/battery restriction). Turn the switch off so the UI
            // never shows monitoring as "on" while nothing runs.
            Log.e(TAG, "could not restart monitoring after boot, switching it off", error)
            settings.setMonitoringEnabled(false)
        }
    }

    private companion object {
        const val TAG = "BootCompletedReceiver"
    }
}
