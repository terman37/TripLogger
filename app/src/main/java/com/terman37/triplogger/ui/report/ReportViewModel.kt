package com.terman37.triplogger.ui.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.data.Trip
import com.terman37.triplogger.report.ReportCsvBuilder
import com.terman37.triplogger.ui.home.tripToRowUi
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    init {
        // Lazy address retry (todo.md): the old "Generate" button used to
        // trigger it; without Generate it runs once when the screen opens.
        viewModelScope.launch {
            runCatching { container.tripRepository.retryPendingAddresses() }
                .onFailure { Log.e(tag, "initial address retry failed", it) }
        }
    }

    /** Default range = the last 7 days including today (user request). */
    fun defaultFrom(): LocalDate = LocalDate.now(zone).minusDays(6)

    fun defaultTo(): LocalDate = LocalDate.now(zone)

    /**
     * Loads the trips for [from]..[to] (both inclusive). Pure local query —
     * the list refreshes whenever the dates change, no Generate button.
     */
    suspend fun load(from: LocalDate, to: LocalDate): ReportData? = withContext(Dispatchers.IO) {
        try {
            val trips = queryRange(from, to)
            ReportData(trips = trips, totalKm = trips.sumOf { it.distanceKm })
        } catch (e: Exception) {
            Log.e(tag, "load failed", e)
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
            container.tripRepository.retryPendingAddresses() // freshest addresses in the file
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
        val (fromMillis, untilMillis) = rangeMillis(from, to)
        return container.tripRepository.tripsBetween(fromMillis, untilMillis)
    }

    /**
     * Permanently deletes every trip whose start is in the range (Report
     * footer trash action). Returns false on failure.
     */
    suspend fun deleteRange(from: LocalDate, to: LocalDate): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val (fromMillis, untilMillis) = rangeMillis(from, to)
                container.tripRepository.deleteTripsBetween(fromMillis, untilMillis)
                true
            } catch (e: Exception) {
                Log.e(tag, "deleteRange failed", e)
                false
            }
        }

    /** [from, to+1day) in epoch millis — shared by all range operations. */
    private fun rangeMillis(from: LocalDate, to: LocalDate): Pair<Long, Long> {
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        // +1 day: the DAO range is [from, until) — end of "to" must be included.
        val untilMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return fromMillis to untilMillis
    }
}
