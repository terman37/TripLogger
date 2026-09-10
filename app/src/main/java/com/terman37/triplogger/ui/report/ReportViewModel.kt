package com.terman37.triplogger.ui.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.data.Trip
import com.terman37.triplogger.report.ReportCsvBuilder
import com.terman37.triplogger.report.ReportDates
import com.terman37.triplogger.ui.home.TripRowUi
import com.terman37.triplogger.ui.home.tripToRowUi
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One report dataset: the trips in the range (chronological) and the total km
 * for the summary line ("12 trips · 386.4 km").
 */
data class ReportData(
    val trips: List<Trip>,
    val totalKm: Double,
)

/**
 * Report screen logic: date range → live trip list → export CSV → delete.
 * Pure date rules live in [ReportDates]; this class only touches Android for
 * the cache file and the database.
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TripLoggerApplication).container
    private val zone: ZoneId = ZoneId.systemDefault()
    private val tag = "ReportViewModel"

    init {
        // Lazy address retry (offline trips fill in as soon as possible). The
        // export runs it again before building the file.
        viewModelScope.launch {
            runCatching { container.tripRepository.retryPendingAddresses() }
                .onFailure { Log.e(tag, "initial address retry failed", it) }
        }
    }

    /** Default range: the last 7 days including today. */
    fun defaultFrom(): LocalDate = ReportDates.defaultFrom(LocalDate.now(zone))

    fun defaultTo(): LocalDate = ReportDates.defaultTo(LocalDate.now(zone))

    /** Loads the trips for [from]..[to] (both inclusive). */
    suspend fun load(from: LocalDate, to: LocalDate): ReportData? = withContext(Dispatchers.IO) {
        try {
            val trips = queryRange(from, to)
            ReportData(trips = trips, totalKm = trips.sumOf { it.distanceKm })
        } catch (e: Exception) {
            Log.e(tag, "load failed", e)
            null
        }
    }

    /** Display rows for the preview (same card as Home). */
    fun toRows(data: ReportData): List<TripRowUi> =
        data.trips.map { tripToRowUi(it, zone) }

    /** Deletes one trip (per-row trash in the report list). */
    suspend fun deleteTrip(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            container.tripRepository.deleteTripById(id)
            true
        } catch (e: Exception) {
            Log.e(tag, "deleteTrip failed", e)
            false
        }
    }

    /**
     * Deletes every trip whose start is in the range (footer trash). Returns
     * false on failure.
     */
    suspend fun deleteRange(from: LocalDate, to: LocalDate): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val (fromMillis, untilMillis) = ReportDates.rangeMillis(from, to, zone)
                container.tripRepository.deleteTripsBetween(fromMillis, untilMillis)
                true
            } catch (e: Exception) {
                Log.e(tag, "deleteRange failed", e)
                false
            }
        }

    /**
     * Writes the CSV file for the range into the app cache and returns it (the
     * screen shares it through a FileProvider). Returns null on failure.
     */
    suspend fun exportCsv(from: LocalDate, to: LocalDate): File? = withContext(Dispatchers.IO) {
        try {
            container.tripRepository.retryPendingAddresses() // freshest addresses
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
        val (fromMillis, untilMillis) = ReportDates.rangeMillis(from, to, zone)
        return container.tripRepository.tripsBetween(fromMillis, untilMillis)
    }
}
