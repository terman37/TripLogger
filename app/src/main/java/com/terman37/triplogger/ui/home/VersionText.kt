package com.terman37.triplogger.ui.home

/**
 * Version of the installed app, as shown in the About dialog.
 *
 * Kept as a pure data class + factory so the fallback rules can be unit-tested
 * on the JVM: the manifest's `versionName` is optional, and reading it in a
 * Compose test would need a device.
 */
data class AppVersion(
    /** `versionName` from the manifest, or "?" when the manifest omits it. */
    val name: String,
    /** `versionCode` from the manifest. */
    val code: Long,
)

/**
 * Builds the display value for the About dialog.
 *
 * A blank or missing `versionName` becomes "?" instead of the literal "null":
 * `versionName` is not mandatory in the manifest, and no build here sets it to
 * an empty string, so this only guards against a malformed install.
 */
fun appVersion(name: String?, code: Long): AppVersion =
    AppVersion(name = name?.takeIf { it.isNotBlank() } ?: "?", code = code)
