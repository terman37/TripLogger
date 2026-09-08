package com.terman37.triplogger.ui.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.data.Trip
import com.terman37.triplogger.report.ReportCsvBuilder
import com.terman37.triplogger.ui.home.tripToRowUi
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One generated report: the trips in the range (chronological) plus their
 * display rows and the total km for the summary line ("12 trips · 386.4 km").
 */
data class ReportData(
    val trips: List<Trip>,
    val totalKm: Double,
)

/**
 * Report screen logic (UI.md): date range → generate (also retries pending
 * addresses, todo.md) → export CSV + share. Pure suspend helpers called from
 * the composable's coroutine scope; no state kept here, the screen owns the
 * two chosen dates.
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TripLoggerApplication).container
    private val zone: ZoneId = ZoneId.systemDefault()
    private val tag = "ReportViewModel"

    /** Default range = the previous calendar month (todo.md "last month"). */
    fun defaultFrom(): LocalDate = LocalDate.now(zone).minusMonths(1).withDayOfMonth(1)

    fun defaultTo(): LocalDate = defaultFrom().withDayOfMonth(defaultFrom().lengthOfMonth())

    /**
     * Generates the report for [from]..[to] (both inclusive). Before reading
     * the trips it retries pending reverse geocodes so addresses are as fresh
     * as possible (decision, todo.md).
     */
    suspend fun generate(from: LocalDate, to: LocalDate): ReportData? = withContext(Dispatchers.IO) {
        try {
            container.tripRepository.retryPendingAddresses()
            val trips = queryRange(from, to)
            ReportData(trips = trips, totalKm = trips.sumOf { it.distanceKm })
        } catch (e: Exception) {
            Log.e(tag, "generate failed", e)
            null
        }
    }

    /** Rows for the on-screen preview (same expandable row as Home). */
    fun toRows(data: ReportData): List<com.terman37.triplogger.ui.home.TripRowUi> =
        data.trips.map { tripToRowUi(it, zone) }

    /**
     * Writes the CSV file for the range into the app cache and returns it
     * (the screen shares it through a FileProvider). Returns null on failure.
     */
    suspend fun exportCsv(from: LocalDate, to: LocalDate): File? = withContext(Dispatchers.IO) {
        try {
            val trips = queryRange(from, to)
            val csv = ReportCsvBuilder.build(trips, zone)
            val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "trips_${from}_${to}.csv")
            file.writeText(csv, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            Log.e(tag, "export failed", e)
            null
        }
    }

    private suspend fun queryRange(from: LocalDate, to: LocalDate): List<Trip> {
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        // +1 day: the DAO range is [from, until) — end of "to" must be included.
        val untilMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return container.tripRepository.tripsBetween(fromMillis, untilMillis)
    }
}
