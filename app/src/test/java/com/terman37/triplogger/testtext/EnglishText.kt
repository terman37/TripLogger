package com.terman37.triplogger.testtext

import com.terman37.triplogger.report.XlsxLabels
import com.terman37.triplogger.ui.home.TripText
import com.terman37.triplogger.ui.home.TripTextStrings
import java.util.Locale

/**
 * English reference values for the pure text layers.
 *
 * These live in the **test** source set on purpose: since phase L1b, production
 * code never hard-codes English — it reads `res/values/strings.xml` through
 * `tripTextFrom` / `xlsxLabelsFrom`. Tests therefore have to state their
 * expectations explicitly, and the literals below mirror that file. If a string
 * changes there, the failing test shows which expectation moved.
 */
object EnglishText {

    private val strings = TripTextStrings(
        addressPending = "Address pending",
        noLocation = "No location",
        startPlaceholder = "Start",
        endPlaceholder = "End",
        durationMinutes = "%1\$d min",
        durationHours = "%1\$d h %2\$02d",
        kilometres = "%1\$.1f km",
        dateTimePattern = "MMM d, HH:mm",
        timePattern = "HH:mm",
    )

    /** Formatter as the English UI uses it: US number and date conventions. */
    val tripText = TripText(Locale.US, strings)

    /** Same texts with French number/date conventions — for locale tests. */
    val tripTextFrench = TripText(Locale.FRANCE, strings)

    val xlsxLabels = XlsxLabels(
        startDate = "start date",
        endDate = "end date",
        startAddress = "start address",
        endAddress = "end address",
        kilometres = "km",
        trip = "trip",
        total = "Total",
        addressNotFound = "Address not found",
    )
}
