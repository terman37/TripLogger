package com.terman37.triplogger.tools

import androidx.test.core.app.ApplicationProvider
import com.terman37.triplogger.TripLoggerApplication
import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.data.RegisteredDevice
import com.terman37.triplogger.data.Trip
import com.terman37.triplogger.data.TripDatabase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seeds the app's **real** database with sample trips so Play listing
 * screenshots can be taken without exposing the owner's own addresses.
 *
 * Why here and not in `main`: production code must not be able to write invented
 * trips. This lives in the instrumented test source set, runs in the target app's
 * process and therefore writes to the same Room database and preferences the app
 * reads — no `runAs`, no root, no debug-only production path.
 *
 * Run it with:
 * `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.terman37.triplogger.tools.SampleDataSeederTest`
 * (needs the **debug** build installed; the release build is not debuggable and
 * its signature would block the earlier instrumentation install — see the
 * incident note in release_guide.md §6.3.1).
 *
 * The data is deliberately fictional-but-plausible: a Paris → Toulouse drive in
 * three legs plus a local Toulouse hop, with well-known public addresses. Timings
 * are relative to "yesterday/today" so the Report tab's default last-7-days range
 * always shows them.
 */
class SampleDataSeederTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Device the app shows as the trip trigger; clearly not a real car. */
    private val sampleDevice = RegisteredDevice(address = "AA:BB:CC:DD:EE:FF", name = "Renault Clio")

    private data class Sample(
        /** 1 = yesterday, 0 = today. */
        val dayOffset: Long,
        /** Start time, minutes after local midnight. */
        val startMinuteOfDay: Long,
        val durationMinutes: Long,
        val startStreet: String,
        val startCity: String,
        val startLat: Double,
        val startLng: Double,
        val endStreet: String,
        val endCity: String,
        val endLat: Double,
        val endLng: Double,
        val distanceKm: Double,
    )

    private val samples = listOf(
        Sample(
            dayOffset = 1, startMinuteOfDay = 8 * 60 + 15, durationMinutes = 85,
            startStreet = "12 Rue de Rivoli", startCity = "Paris", startLat = 48.8566, startLng = 2.3522,
            endStreet = "5 Place du Martroi", endCity = "Orléans", endLat = 47.9029, endLng = 1.9093,
            distanceKm = 118.6,
        ),
        Sample(
            dayOffset = 1, startMinuteOfDay = 10 * 60 + 5, durationMinutes = 122,
            startStreet = "5 Place du Martroi", startCity = "Orléans", startLat = 47.9029, startLng = 1.9093,
            endStreet = "2 Avenue Garibaldi", endCity = "Limoges", endLat = 45.8336, endLng = 1.2611,
            distanceKm = 190.2,
        ),
        Sample(
            dayOffset = 1, startMinuteOfDay = 12 * 60 + 40, durationMinutes = 188,
            startStreet = "2 Avenue Garibaldi", startCity = "Limoges", startLat = 45.8336, startLng = 1.2611,
            endStreet = "Place du Capitole", endCity = "Toulouse", endLat = 43.6045, endLng = 1.4442,
            distanceKm = 289.4,
        ),
        Sample(
            dayOffset = 0, startMinuteOfDay = 9 * 60 + 20, durationMinutes = 17,
            startStreet = "Place du Capitole", startCity = "Toulouse", startLat = 43.6045, startLng = 1.4442,
            endStreet = "1 Allée des Demoiselles", endCity = "Toulouse", endLat = 43.5945, endLng = 1.4530,
            distanceKm = 6.3,
        ),
    )

    private fun epochMillis(dayOffset: Long, minuteOfDay: Long): Long =
        LocalDate.now(zone)
            .minusDays(dayOffset)
            .atStartOfDay(zone)
            .plusMinutes(minuteOfDay)
            .toInstant()
            .toEpochMilli()

    private fun toTrip(sample: Sample): Trip = Trip(
        startEpochMillis = epochMillis(sample.dayOffset, sample.startMinuteOfDay),
        startLat = sample.startLat,
        startLng = sample.startLng,
        startStreet = sample.startStreet,
        startCity = sample.startCity,
        endEpochMillis = epochMillis(sample.dayOffset, sample.startMinuteOfDay + sample.durationMinutes),
        endLat = sample.endLat,
        endLng = sample.endLng,
        endStreet = sample.endStreet,
        endCity = sample.endCity,
        distanceKm = sample.distanceKm,
        origin = TripOrigin.AUTO,
    )

    @Test
    fun seedSampleTripsAndSettings() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TripLoggerApplication>()
        // Same singleton the app itself uses, so the rows are visible immediately.
        val dao = TripDatabase.getInstance(app).tripDao()

        // Start from a clean slate: this is sample data, not a history to keep.
        dao.allTrips().forEach { dao.deleteById(it.id) }

        samples.forEach { dao.insert(toTrip(it)) }

        // A registered device + monitoring enabled make Home show its realistic
        // "waiting for the car" state instead of the first-run card. No service is
        // started: the screenshot only needs the state, not a running recorder.
        //
        // Clear any real device the owner had registered first: the sample state has
        // to be deterministic, and a leftover real device would show up in
        // screenshots *and* change Home's wording to "one of your registered
        // devices" instead of naming the sample car.
        app.container.settings.registeredDevices.value.forEach {
            app.container.settings.removeRegisteredDevice(it.address)
        }
        app.container.settings.addRegisteredDevice(sampleDevice)
        app.container.settings.setGracePeriodMinutes(3)
        app.container.settings.setMonitoringEnabled(true)
        // SettingsRepository writes with SharedPreferences.apply(), which is
        // asynchronous. The instrumentation process is killed as soon as the test
        // returns, so without this forced commit the settings would be lost (the
        // Room inserts above are synchronous and survive on their own).
        app.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .commit()

        val stored = dao.allTrips()
        assertEquals(samples.size, stored.size)
        // Compare the total, with a tolerance: these are one-decimal values.
        assertEquals(samples.sumOf { it.distanceKm }, stored.sumOf { it.distanceKm }, 0.01)
        assertTrue(
            "sample rows must carry addresses, otherwise the UI shows 'address pending'",
            stored.all { it.startStreet != null && it.startCity != null && it.endStreet != null && it.endCity != null },
        )
        assertTrue(
            "monitoring must be enabled for the Home screenshot",
            app.container.settings.monitoringEnabled.value,
        )
        assertEquals(
            "the sample state must contain exactly the sample device: a leftover real",
            listOf(sampleDevice),
            app.container.settings.registeredDevices.value,
        )
        println(
            "Seeded ${stored.size} sample trips (${"%.1f".format(stored.sumOf { it.distanceKm })} km) " +
                "and registered device ${sampleDevice.name}",
        )
    }
}
