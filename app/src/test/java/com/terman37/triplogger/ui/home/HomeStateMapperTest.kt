package com.terman37.triplogger.ui.home

import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.core.TripRecorder
import com.terman37.triplogger.data.Trip
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateMapperTest {

    private val utc = java.time.ZoneId.of("UTC")

    private fun idle() = TripRecorder.Snapshot(
        phase = TripRecorder.Phase.IDLE,
        distanceKm = 0.0,
        startEpochMillis = null,
        origin = null,
        deviceName = null,
    )

    private fun recording(origin: TripOrigin, device: String?) = TripRecorder.Snapshot(
        phase = TripRecorder.Phase.RECORDING,
        distanceKm = 7.3,
        startEpochMillis = 1000L,
        origin = origin,
        deviceName = device,
    )

    private fun grace() = TripRecorder.Snapshot(
        phase = TripRecorder.Phase.GRACE,
        distanceKm = 7.3,
        startEpochMillis = 1000L,
        origin = TripOrigin.AUTO,
        deviceName = "Car",
        graceStartedAtEpochMillis = 5000L,
    )

    private fun trip(
        id: Long,
        street: String?,
        city: String?,
        lat: Double? = 48.0,
        lng: Double? = 2.0,
    ) = Trip(
        id = id,
        startEpochMillis = 1_000L,
        startLat = lat,
        startLng = lng,
        startStreet = street,
        startCity = city,
        endEpochMillis = 3_600_000L,
        endLat = 48.5,
        endLng = 2.5,
        endStreet = street,
        endCity = city,
        distanceKm = 12.4,
        origin = TripOrigin.AUTO,
    )

    @Test
    fun monitoringOff_whenIdleAndDisabled() {
        val state = HomeStateMapper.toUi(
            snapshot = idle(),
            monitoringEnabled = false,
            deviceNames = emptyList(),
            trips = emptyList(),
            zone = utc,
        )
        assertEquals(CardUiState.MonitoringOff, state.card)
        assertEquals(false, state.hasTrips)
    }

    @Test
    fun switchFlags_reflectSettingsAndRegisteredDevices() {
        // No registered devices → switch disabled even when off.
        val empty = HomeStateMapper.toUi(idle(), false, emptyList(), emptyList(), utc)
        assertEquals(false, empty.monitoringEnabled)
        assertEquals(false, empty.canEnableMonitoring)

        val withDevices = HomeStateMapper.toUi(
            snapshot = idle(),
            monitoringEnabled = true,
            deviceNames = listOf("Car"),
            trips = emptyList(),
            zone = utc,
        )
        assertEquals(true, withDevices.monitoringEnabled)
        assertEquals(true, withDevices.canEnableMonitoring)
    }

    @Test
    fun waiting_listsDeviceNames() {
        val state = HomeStateMapper.toUi(
            snapshot = idle(),
            monitoringEnabled = true,
            deviceNames = listOf("Car bluetooth"),
            trips = emptyList(),
            zone = utc,
        )
        assertEquals(CardUiState.Waiting(listOf("Car bluetooth")), state.card)
    }

    @Test
    fun recording_manual_hasNoDeviceName() {
        val state = HomeStateMapper.toUi(
            snapshot = recording(TripOrigin.MANUAL, device = null),
            monitoringEnabled = true,
            deviceNames = emptyList(),
            trips = emptyList(),
            zone = utc,
        )
        val card = state.card as CardUiState.Recording
        assertEquals(TripOrigin.MANUAL, card.origin)
        assertEquals(null, card.deviceName)
        assertEquals(7.3, card.distanceKm, 0.0)
    }

    @Test
    fun recording_auto_hasDeviceName() {
        val state = HomeStateMapper.toUi(
            snapshot = recording(TripOrigin.AUTO, device = "Car bluetooth"),
            monitoringEnabled = true,
            deviceNames = emptyList(),
            trips = emptyList(),
            zone = utc,
        )
        val card = state.card as CardUiState.Recording
        assertEquals("Car bluetooth", card.deviceName)
    }

    @Test
    fun gracePeriod_mapsToGraceCard() {
        val state = HomeStateMapper.toUi(
            snapshot = grace(),
            monitoringEnabled = true,
            deviceNames = emptyList(),
            trips = emptyList(),
            zone = utc,
        )
        assertEquals(
            CardUiState.GracePeriod(distanceKm = 7.3, graceStartedAtEpochMillis = 5000L),
            state.card,
        )
    }

    @Test
    fun tripRow_formatsDatesAddressesAndFallbacks() {
        val start = LocalDateTime.of(2026, 8, 6, 14, 32).toInstant(ZoneOffset.UTC).toEpochMilli()
        val end = LocalDateTime.of(2026, 8, 6, 15, 15).toInstant(ZoneOffset.UTC).toEpochMilli()
        val trips = listOf(
            Trip(
                id = 1L,
                startEpochMillis = start,
                startLat = 48.0, startLng = 2.0,
                startStreet = "12 Rue de Rivoli", startCity = "Paris",
                endEpochMillis = end,
                endLat = 48.5, endLng = 2.5,
                endStreet = null, endCity = null, // pending geocode
                distanceKm = 12.4,
                origin = TripOrigin.AUTO,
            ),
        )

        val state = HomeStateMapper.toUi(idle(), false, emptyList(), trips, utc)

        val row = state.recentTrips.single()
        assertEquals("Aug 6, 14:32", row.title)
        assertEquals("Paris → End", row.summary) // end side unresolved → placeholder
        assertEquals("12.4 km", row.distanceText)
        assertEquals("14:32 – 15:15", row.timeRangeText)
        assertEquals("43 min", row.durationText)
        assertEquals("12 Rue de Rivoli, Paris", row.startAddressText)
        assertEquals("Address pending", row.endAddressText)
    }

    @Test
    fun tripWithoutAnyLocation_showsNoLocation() {
        val trips = listOf(
            trip(id = 2L, street = null, city = null, lat = null, lng = null),
        )
        val state = HomeStateMapper.toUi(idle(), false, emptyList(), trips, utc)
        val row = state.recentTrips.single()
        assertEquals("Start → End", row.summary)
        assertEquals("No location", row.startAddressText)
        assertTrue(state.hasTrips)
    }
}
