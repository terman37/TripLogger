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
 * is exactly what trip monitoring needs (decided on the device).
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
        container = (application as TripLoggerApplication).container
        recorder = container.tripRecorder
        NotificationFactory.ensureChannel(this)
    }

    // --- Android service callbacks ----------------------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android contract: a service started via startForegroundService() MUST
        // call startForeground() quickly — even when we will then decide to
        // stop. Doing it here first prevents the process being killed with
        // ForegroundServiceDidNotStartInTimeException (seen on device, B4).
        if (!startForegroundCompat()) {
            // Cannot run as a foreground service: background-start restriction,
            // OEM autostart block or a missing foreground-service-type
            // permission. The app must never claim monitoring is on while
            // nothing runs, so turn the switch off instead of leaving a silent
            // state behind. START_NOT_STICKY: do not loop on a failing start.
            container.settings.setMonitoringEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_MANUAL_START -> recorder.onManualStart()
            ACTION_MANUAL_STOP -> recorder.onManualStop()
            ACTION_NOTIFICATION_DISMISSED -> onNotificationDismissed()
            // Explicit stop: turn the master switch off here too, so any caller
            // (UI, tests, external intent) gets the same consistent state.
            ACTION_STOP -> container.settings.setMonitoringEnabled(false)
            // ACTION_START only wakes the service; evaluate() reads the real
            // state (settings + recorder) and acts on it.
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
     * Rules (docs/UI.md):
     * - Monitoring ON → run as foreground service + listen to Bluetooth.
     * - Trip recording (auto or manual) → also sample GPS.
     * - Monitoring OFF and no recording → stop the service entirely.
     */
    private fun evaluate() {
        val monitoringEnabled = container.settings.monitoringEnabled.value
        val phase = recorder.snapshot().phase
        val wantsToRun = monitoringEnabled || phase != TripRecorder.Phase.IDLE

        if (!wantsToRun) {
            locationSource?.stop()
            monitor?.unregister()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Foreground with a notification (required while the service runs).
        if (!startForegroundCompat()) {
            // No foreground service and no visible indicator: tear down instead
            // of registering Bluetooth/GPS silently (same rule as onStartCommand).
            container.settings.setMonitoringEnabled(false)
            locationSource?.stop()
            monitor?.unregister()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        updateNotification()
        persistSession()

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

        // GPS only while a trip is actually recording (battery).
        when (phase) {
            TripRecorder.Phase.RECORDING -> ensureLocationRunning()
            TripRecorder.Phase.GRACE -> scheduleGraceTimer()
            TripRecorder.Phase.IDLE -> stopLocationAndTimer()
        }
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val registered = isRegistered(device.address)
        if (!registered) return // not a trigger device: ignore
        recorder.onDeviceConnected(deviceName(device))
        persistFinishedTrips()
        evaluate()
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        val registered = isRegistered(device.address)
        if (!registered) return
        recorder.onDeviceDisconnected()
        persistFinishedTrips()
        evaluate() // schedules the grace timer if the trip entered GRACE
    }

    private fun isRegistered(address: String): Boolean =
        container.settings.registeredDevices.value.any { it.address == address }

    /** Bluetooth device names are cached by the OS; ask it, fall back to the
     * address (which every device has). */
    private fun deviceName(device: BluetoothDevice): String {
        // Reading .name needs BLUETOOTH_CONNECT. Monitoring is only registered
        // when it is granted, but check anyway: the permission can be revoked
        // while we run, and that must not crash the service. The check is
        // written inline (not via hasPermission) because lint's MissingPermission
        // analysis only recognises a direct checkSelfPermission call.
        val connectGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!connectGranted) return device.address
        return runCatching { device.name }.getOrNull() ?: device.address
    }

    // --- location + grace timer ------------------------------------------

    private fun ensureLocationRunning() {
        // A manual trip must also record GPS; nothing to do if already active.
        if (locationSource != null) return
        val source = LocationManagerLocationSource(applicationContext)
        val started = source.start { sample ->
            // Callback arrives on the main looper (see LocationManager source).
            recorder.onLocationSample(sample)
            updateNotification()
            persistSession()
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
        // Only one ACTIVE timer at a time. Checking isActive (not just null)
        // matters: a completed job would otherwise block rescheduling and the
        // grace period could hang forever (bug seen on device).
        if (graceTimerJob?.isActive == true) return

        val graceMinutes = container.settings.gracePeriodMinutes.value
        // Fire at the recorder's deadline, not "full period from now": if this
        // is a re-schedule after an early/duplicate evaluation, the remaining
        // time is shorter than the whole period.
        val startedAt = recorder.snapshot().graceStartedAtEpochMillis
        val deadline = (startedAt ?: System.currentTimeMillis()) + graceMinutes * 60_000L
        val remainingMillis = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)

        graceTimerJob = scope.launch {
            try {
                delay(remainingMillis)
                recorder.onGraceTimerExpired() // recorder ignores early/late calls
                persistFinishedTrips()
                evaluate()
            } finally {
                // Always release the slot so the next grace can be scheduled.
                graceTimerJob = null
            }
        }
    }

    /**
     * The user swiped the monitoring notification away. Treat it as "stop
     * monitoring": finish and save any running trip, turn the switch off and
     * let evaluate() tear the service down (notification gone = no silent
     * monitoring).
     */
    private fun onNotificationDismissed() {
        recorder.onManualStop() // finishes a running trip (discarded if < 50 m)
        container.settings.setMonitoringEnabled(false)
        persistFinishedTrips()
    }

    /**
     * Mirrors the in-progress trip to the store so a reboot / force-stop can
     * finish it at the last recorded position. No-op when idle.
     */
    private fun persistSession() {
        val active = recorder.activeTrip() ?: return
        container.tripSessionStore.save(active)
    }

    private fun persistFinishedTrips() {
        // Save drafts in the background; failures must not crash the service.
        val drafts = recorder.takeFinishedTrips()
        if (drafts.isEmpty()) return
        val finishedStart = drafts.first().startEpochMillis
        scope.launch {
            var allSaved = true
            for (draft in drafts) {
                runCatching { container.tripRepository.saveTrip(draft) }
                    .onFailure {
                        allSaved = false
                        android.util.Log.e(TAG, "saveTrip failed", it)
                    }
            }
            // Drop the persisted session once the trip is stored; keep it when a
            // save failed so the next start can still recover it. Clear only
            // when the store still holds THIS trip (never wipe a newer one).
            val stored = container.tripSessionStore.load()
            if (allSaved && stored?.startEpochMillis == finishedStart) {
                container.tripSessionStore.clear()
            }
        }
    }

    // --- notification -----------------------------------------------------

    private fun startForegroundCompat(): Boolean {
        val notification = buildNotification()
        val type = computeForegroundType()
        return try {
            if (type == 0) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(NOTIFICATION_ID, notification, type)
            }
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException etc.: cannot run. The
            // caller decides how to react; false means the foreground service
            // did not come up.
            android.util.Log.e(TAG, "startForeground failed", e)
            false
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


    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val snapshot = recorder.snapshot()
        return NotificationFactory.create(
            context = this,
            phase = snapshot.phase,
            distanceKm = snapshot.distanceKm,
        )
    }


    companion object {
        private const val TAG = "TripMonitorService"
        // Notification constants live in NotificationFactory (shared with its test).
        private const val NOTIFICATION_ID = NotificationFactory.NOTIFICATION_ID

        const val ACTION_START = "com.terman37.triplogger.action.START"
        const val ACTION_STOP = "com.terman37.triplogger.action.STOP"
        const val ACTION_MANUAL_START = "com.terman37.triplogger.action.MANUAL_START"
        const val ACTION_MANUAL_STOP = "com.terman37.triplogger.action.MANUAL_STOP"
        const val ACTION_NOTIFICATION_DISMISSED =
            "com.terman37.triplogger.action.NOTIFICATION_DISMISSED"

        /** Starts the service with a command. UI helpers call these. */
        fun startWithAction(context: Context, action: String) {
            val intent = Intent(context, TripMonitorService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
