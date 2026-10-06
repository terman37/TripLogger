package com.terman37.triplogger.monitor

/**
 * The rule that decides whether the app should restart trip monitoring after
 * the phone reboots or the app is updated (both kill the foreground service).
 *
 * WHY A PURE OBJECT: the boot receiver is thin Android glue that is hard to
 * test on the JVM (BroadcastReceiver, Context). Keeping the decision here means
 * the interesting part — "is monitoring actually supposed to run?" — is a plain
 * function covered by unit tests.
 *
 * Monitoring is only meaningful when it is switched on AND at least one trigger
 * device is registered (docs/UI.md: the Home switch cannot be enabled otherwise).
 * Starting the service with no device would show a notification that never
 * records anything.
 */
object BootStartDecision {

    fun shouldStartOnBoot(
        monitoringEnabled: Boolean,
        registeredDeviceCount: Int,
    ): Boolean = monitoringEnabled && registeredDeviceCount > 0
}
