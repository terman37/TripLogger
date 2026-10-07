package com.terman37.triplogger.report

/**
 * Column headers and fixed labels of the exported Excel workbook.
 *
 * Why this exists: [ReportXlsxBuilder] is pure JVM code (no Android types) so it
 * can be unit-tested, which means it cannot read string resources itself.
 * `ReportViewModel` builds this from resources and passes it in — see
 * `xlsxLabelsFrom(context)` in `text/AppTextResources.kt`. The parameter is
 * required (no default), so a translation cannot be forgotten silently
 * (phase L1b of the localisation work, release_guide.md §6).
 *
 * Note: the workbook stores **real Excel dates**, so month and weekday names are
 * rendered by Excel in the reader's language — only these labels need translating.
 */
data class XlsxLabels(
    val startDate: String,
    val endDate: String,
    val startAddress: String,
    val endAddress: String,
    val kilometres: String,
    val trip: String,
    val total: String,
    /** Replaces the address text when an endpoint has none. */
    val addressNotFound: String,
) {
    /** Header row, in column order. */
    fun headers(): List<String> = listOf(
        startDate, endDate, startAddress, endAddress, kilometres, trip,
    )
}
