package com.terman37.triplogger.text

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.terman37.triplogger.R
import com.terman37.triplogger.report.XlsxLabels
import com.terman37.triplogger.ui.home.TripText
import com.terman37.triplogger.ui.home.TripTextStrings
import java.util.Locale

/**
 * Bridge between Android resources and the pure text layers.
 *
 * [TripText] and [XlsxLabels] deliberately contain no Android types (they are the
 * JVM-tested part of the app), so somebody has to read the strings: that is the
 * ViewModels for background work, the monitor for the notification, and
 * [rememberTripText] for composables. Keeping the conversion in one place means
 * phase L2 only has to add a second resource folder — no code changes here.
 *
 * Package note: this lives outside `ui/` and `monitor/` so the monitor layer does
 * not have to depend on the UI package for its notification text.
 */

/**
 * Locale used for numbers and dates: the device/app locale, so a French phone
 * shows "12,4 km" and "6 août, 14:32" once French resources exist (phase L2).
 */
private fun Context.appLocale(): Locale =
    resources.configuration.locales[0] ?: Locale.getDefault()

/** Texts for the trip-row formatting layer, read from resources. */
fun tripTextFrom(context: Context): TripText = TripText(
    locale = context.appLocale(),
    strings = TripTextStrings(
        addressPending = context.getString(R.string.trip_text_address_pending),
        noLocation = context.getString(R.string.trip_text_no_location),
        startPlaceholder = context.getString(R.string.trip_text_start_placeholder),
        endPlaceholder = context.getString(R.string.trip_text_end_placeholder),
        durationMinutes = context.getString(R.string.trip_text_duration_minutes),
        durationHours = context.getString(R.string.trip_text_duration_hours),
        kilometres = context.getString(R.string.trip_text_kilometres),
        dateTimePattern = context.getString(R.string.trip_text_datetime_pattern),
        timePattern = context.getString(R.string.trip_text_time_pattern),
    ),
)

/** Labels for the exported Excel workbook, read from resources. */
fun xlsxLabelsFrom(context: Context): XlsxLabels = XlsxLabels(
    startDate = context.getString(R.string.xlsx_column_start_date),
    endDate = context.getString(R.string.xlsx_column_end_date),
    startAddress = context.getString(R.string.xlsx_column_start_address),
    endAddress = context.getString(R.string.xlsx_column_end_address),
    kilometres = context.getString(R.string.xlsx_column_km),
    trip = context.getString(R.string.xlsx_column_trip),
    total = context.getString(R.string.xlsx_total),
    addressNotFound = context.getString(R.string.xlsx_address_not_found),
)

/**
 * [tripTextFrom] for composables. Recreated only when the context (i.e. the
 * configuration/locale) changes, so a language switch is picked up.
 */
@Composable
fun rememberTripText(): TripText {
    val context = LocalContext.current
    return remember(context) { tripTextFrom(context) }
}
