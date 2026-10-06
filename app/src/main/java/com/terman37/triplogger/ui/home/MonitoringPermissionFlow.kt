package com.terman37.triplogger.ui.home

/**
 * Decides which permission step the Home switch must take before monitoring can
 * be enabled.
 *
 * WHY A PURE OBJECT: checking and requesting permissions needs a Context and the
 * system settings screen (Android-only, not JVM-testable). The branch that
 * actually matters — foreground first, then "Allow all the time" location, then
 * ready — is a plain function and is unit-tested.
 *
 * Background location is required because Android 14+ refuses to start a
 * location foreground service from the background (for example after a reboot)
 * unless the app holds `ACCESS_BACKGROUND_LOCATION`. Without it, monitoring
 * could not resume by itself (see docs/DETAILS.md).
 */
object MonitoringPermissionFlow {

    enum class Step {
        /** Bluetooth, fine location and notifications: one combined dialog. */
        REQUEST_FOREGROUND,

        /**
         * Foreground permissions are granted, but location is not "Allow all the
         * time" yet. On Android 11+ this cannot be part of the runtime dialog,
         * so the user must change it on the app's system settings page.
         */
        OPEN_BACKGROUND_SETTINGS,

        /** Everything granted: monitoring can be switched on. */
        READY,
    }

    fun nextStep(
        foregroundGranted: Boolean,
        backgroundGranted: Boolean,
    ): Step = when {
        !foregroundGranted -> Step.REQUEST_FOREGROUND
        !backgroundGranted -> Step.OPEN_BACKGROUND_SETTINGS
        else -> Step.READY
    }
}
