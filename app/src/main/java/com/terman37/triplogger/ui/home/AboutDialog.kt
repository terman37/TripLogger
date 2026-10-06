package com.terman37.triplogger.ui.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.terman37.triplogger.R

/**
 * About dialog opened by the "?" button on Home: installed version, licence,
 * and links to the source repository and the licence text.
 *
 * Why a dialog instead of a screen: GPL-3.0-or-later requires the source to be
 * reachable while the app is distributed, and this is the smallest way to make
 * that offer visible without adding a tab (docs/UI.md).
 *
 * Everything here is static: no permission, no network request — the links are
 * handed to the browser or the e-mail app by the system.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember { installedVersion(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.about_version, version.name, version.code),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_licence),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                // Documentation lives on GitHub Pages (docs/ folder): the site
                // carries the user guide, the technical docs and the source
                // link in its footer, so the GPL source offer stays one click
                // away without cluttering the dialog.
                LinkButton(
                    labelRes = R.string.about_documentation_link,
                    uri = stringResource(R.string.about_documentation_url),
                    context = context,
                )
                LinkButton(
                    labelRes = R.string.about_privacy_link,
                    uri = stringResource(R.string.about_privacy_url),
                    context = context,
                )
                LinkButton(
                    labelRes = R.string.about_contact_link,
                    uri = "mailto:${stringResource(R.string.about_contact_email)}",
                    context = context,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_close)) }
        },
    )
}

/**
 * A text button that opens [uri] with the system handler (browser for http(s),
 * e-mail app for mailto).
 */
@Composable
private fun LinkButton(@StringRes labelRes: Int, uri: String, context: Context) {
    TextButton(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
        },
    ) {
        Text(stringResource(labelRes))
    }
}

/**
 * Reads `versionName`/`versionCode` from the installed package rather than from
 * `BuildConfig`: AGP 9 leaves `buildFeatures.buildConfig` off by default, and
 * the package manager is the authoritative source for what is installed.
 *
 * `PackageInfoFlags.of(0)` is the API 33+ replacement for the deprecated
 * integer flags (minSdk is 34).
 */
private fun installedVersion(context: Context): AppVersion {
    val info = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(0),
    )
    return appVersion(info.versionName, info.longVersionCode)
}
