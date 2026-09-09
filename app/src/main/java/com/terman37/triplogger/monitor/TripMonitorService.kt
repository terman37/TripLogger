package com.terman37.triplogger.monitor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.terman37.triplogger.AppContainer
import com.terman37.triplogger.MainActivity
import com.terman37.triplogger.R
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.RegisteredDevice
import com.terman37.triplogger.location.LocationManagerLocationSource
import com.terman37.triplogger.location.LocationSource
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that hosts the whole monitoring/tracking machinery
 * (README architecture). It:
 *
 * 1. listens to Bluetooth connect/disconnect of registered devices,
 * 2. feeds those events into the shared [TripRecorder],
 * 3. samples GPS while a trip is RECORDING and forwards fixes to the recorder,
 * 4. schedules the grace-period timer after a disconnect,
 * 5. persists finished trips and shows the status notification.
 *
 * WHY A FOREGROUND SERVICE: Android kills background work quickly. A service
 * with a visible notification may keep running while the app is closed — that
 * is exactly what trip monitoring needs (todo.md open question, now decided).
 * The UI never binds to it: commands arrive as intents (see companion).
 */
class TripMonitorService : Service() {

    // Shared singletons from the app container.
    private lateinit var container: AppContainer
    private lateinit var recorder: TripRecorder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitor: BluetoothMonitor? = null
    private var locationSource: LocationSource? = null
    private var graceTimerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        container = (application as TripLoggerApplication).container
        recorder = container.tripRecorder
        createNotificationChannel()
        Log.i(TAG, "onCreate done")
    }

    // --- Android service callbacks ----------------------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android contract: a service started via startForegroundService() MUST
        // call startForeground() quickly — even when we will then decide to
        // stop. Doing it here first prevents the process being killed with
        // ForegroundServiceDidNotStartInTimeException (seen on device, B4).
        startForegroundCompat()

        when (intent?.action) {
            ACTION_MANUAL_START -> recorder.onManualStart()
            ACTION_MANUAL_STOP -> recorder.onManualStop()
            // ACTION_START/ACTION_STOP only wake the service; evaluate() reads
            // the real state (settings + recorder) and acts on it.
        }
        persistFinishedTrips()
        evaluate()
        return START_STICKY // system may restart us; evaluate() re-applies state
    }

    override fun onDestroy() {
        locationSource?.stop()
        monitor?.unregister()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null // never bound

    // --- state evaluation -------------------------------------------------

    /**
     * Central rule: look at settings + recorder, then bring the service to the
     * matching state. Called after every event (intent, Bluetooth, timer).
     *
     * Rules (UI.md / todo.md):
     * - Monitoring ON → run as foreground service + listen to Bluetooth.
     * - Trip recording (auto or manual) → also sample GPS.
     * - Monitoring OFF and no recording → stop the service entirely.
     */
    private fun evaluate() {
        val monitoringEnabled = container.settings.monitoringEnabled.value
        val phase = recorder.snapshot().phase
        val wantsToRun = monitoringEnabled || phase != TripRecorder.Phase.IDLE
        Log.i(TAG, "evaluate: monitoring=$monitoringEnabled phase=$phase wantsToRun=$wantsToRun")

        if (!wantsToRun) {
            locationSource?.stop()
            monitor?.unregister()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Foreground with a notification (required while the service runs).
        startForegroundCompat()
        updateNotification(monitoringEnabled)

        // Bluetooth monitoring only matters when it is enabled. Registering
        // requires the BLUETOOTH_CONNECT runtime permission (UI grants it
        // before enabling monitoring).
        if (monitoringEnabled && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (monitor == null) {
                monitor = BluetoothMonitor(
                    context = this,
                    scope = scope,
                    onDeviceConnected = ::handleDeviceConnected,
                    onDeviceDisconnected = ::handleDeviceDisconnected,
                )
            }
            monitor?.register()
        } else {
            monitor?.unregister()
        }

        // GPS only while a trip is actually recording (battery, todo.md).
        when (phase) {
            TripRecorder.Phase.RECORDING -> ensureLocationRunning()
            TripRecorder.Phase.GRACE -> scheduleGraceTimer()
            TripRecorder.Phase.IDLE -> stopLocationAndTimer()
        }
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val registered = isRegistered(device.address)
        Log.i(TAG, "deviceConnected ${device.address} registered=$registered")
        if (!registered) return // not a trigger device: ignore
        recorder.onDeviceConnected(deviceName(device))
        persistFinishedTrips()
        evaluate()
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        val registered = isRegistered(device.address)
        Log.i(TAG, "deviceDisconnected ${device.address} registered=$registered")
        if (!registered) return
        recorder.onDeviceDisconnected()
        persistFinishedTrips()
        evaluate() // schedules the grace timer if the trip entered GRACE
    }

    private fun isRegistered(address: String): Boolean =
        container.settings.registeredDevices.value.any { it.address == address }

    /** Bluetooth device names are cached by the OS; ask it, fall back to the
     * address (which every device has). */
    private fun deviceName(device: BluetoothDevice): String =
        runCatching { device.name }.getOrNull() ?: device.address

    // --- location + grace timer ------------------------------------------

    private fun ensureLocationRunning() {
        // A manual trip must also record GPS; nothing to do if already active.
        if (locationSource != null) return
        val source = LocationManagerLocationSource(applicationContext)
        val started = source.start { sample ->
            // Callback arrives on the main looper (see LocationManager source).
            recorder.onLocationSample(sample)
            updateNotification(container.settings.monitoringEnabled.value)
        }
        if (started) {
            locationSource = source
            // Anchor the trip with the last cached fix so the first 30 s are
            // not lost when GPS is cold.
            source.lastKnown()?.let { recorder.onLocationSample(it) }
        } else {
            // No GPS permission or provider off: keep recording with 0 km and
            // no positions rather than dropping the trip (graceful).
        }
    }

    private fun stopLocationAndTimer() {
        locationSource?.stop()
        locationSource = null
        graceTimerJob?.cancel()
        graceTimerJob = null
    }

    private fun scheduleGraceTimer() {
        if (graceTimerJob != null) return // one timer at a time
        val graceMinutes = container.settings.gracePeriodMinutes.value
        Log.i(TAG, "grace timer scheduled: ${graceMinutes} min")
        graceTimerJob = scope.launch {
            delay(graceMinutes * 60_000L)
            Log.i(TAG, "grace timer fired, snapshot=" + recorder.snapshot().phase)
            recorder.onGraceTimerExpired() // recorder ignores early/late calls
            Log.i(TAG, "after expiry, snapshot=" + recorder.snapshot().phase)
            persistFinishedTrips()
            evaluate()
        }
    }

    private fun persistFinishedTrips() {
        // Save drafts in the background; failures must not crash the service.
        val drafts = recorder.takeFinishedTrips()
        if (drafts.isEmpty()) return
        scope.launch {
            for (draft in drafts) {
                runCatching { container.tripRepository.saveTrip(draft) }
                    .onFailure { android.util.Log.e(TAG, "saveTrip failed", it) }
            }
        }
    }

    // --- notification -----------------------------------------------------

    private fun startForegroundCompat() {
        val notification = buildNotification(container.settings.monitoringEnabled.value)
        val type = computeForegroundType()
        try {
            if (type == 0) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(NOTIFICATION_ID, notification, type)
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException etc.: cannot run, stop
            // cleanly instead of crashing.
            android.util.Log.e(TAG, "startForeground failed", e)
            stopSelf()
        }
    }

    /** The service type must match the runtime permissions we actually hold. */
    private fun computeForegroundType(): Int {
        var type = 0
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        return type
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW, // silent, no sound/badge noise
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun updateNotification(monitoringEnabled: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(monitoringEnabled))
    }

    private fun buildNotification(monitoringEnabled: Boolean): Notification {
        val snap = recorder.snapshot()
        // Text per user request (plan.md Step 12): the notification always
        // leads with "Monitoring active" and appends what is happening.
        // Content stays minimal: no actions, tap opens the app.
        val text = when (snap.phase) {
            TripRecorder.Phase.RECORDING -> String.format(
                Locale.US, "%s — %s %.1f km",
                getString(R.string.notification_monitoring_text),
                getString(R.string.notification_recording_label),
                snap.distanceKm,
            )
            TripRecorder.Phase.GRACE -> String.format(
                Locale.US, "%s — %s",
                getString(R.string.notification_monitoring_text),
                getString(R.string.notification_disconnected_label),
            )
            TripRecorder.Phase.IDLE -> getString(R.string.notification_monitoring_text)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_triplogger) // TODO step 13: real icon
            .setOngoing(true) // cannot be swiped away (monitoring must stay)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val TAG = "TripMonitorService"
        private const val CHANNEL_ID = "trip_monitor"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.terman37.triplogger.action.START"
        const val ACTION_STOP = "com.terman37.triplogger.action.STOP"
        const val ACTION_MANUAL_START = "com.terman37.triplogger.action.MANUAL_START"
        const val ACTION_MANUAL_STOP = "com.terman37.triplogger.action.MANUAL_STOP"

        /** Starts the service with a command. UI helpers call these. */
        fun startWithAction(context: Context, action: String) {
            val intent = Intent(context, TripMonitorService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
