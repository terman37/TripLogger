package com.terman37.triplogger.session

import com.terman37.triplogger.core.TripOrigin
import com.terman37.triplogger.core.TripRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The stored active-trip record must round-trip exactly and must never throw on
 * a corrupt value (a lost trip is acceptable, a crash at startup is not).
 */
class ActiveTripCodecTest {

    private fun activeTrip(
        startLat: Double? = 48.8566,
        startLng: Double? = 2.3522,
        lastLat: Double? = 48.9,
        lastLng: Double? = 2.4,
    ) = TripRecorder.ActiveTrip(
        startEpochMillis = 1_700_000_000_000L,
        origin = TripOrigin.AUTO,
        startLat = startLat,
        startLng = startLng,
        distanceKm = 12.34,
        lastLat = lastLat,
        lastLng = lastLng,
        lastEpochMillis = 1_700_000_600_000L,
    )

    @Test
    fun encodeDecode_roundTripsEveryField() {
        val trip = activeTrip()
        assertEquals(trip, ActiveTripCodec.decode(ActiveTripCodec.encode(trip)))
    }

    @Test
    fun encodeDecode_keepsMissingCoordinatesNull() {
        val trip = activeTrip(
            startLat = null,
            startLng = null,
            lastLat = null,
            lastLng = null,
        )
        assertEquals(trip, ActiveTripCodec.decode(ActiveTripCodec.encode(trip)))
    }

    @Test
    fun encodeDecode_keepsManualOrigin() {
        val trip = activeTrip().copy(origin = TripOrigin.MANUAL)
        assertEquals(trip, ActiveTripCodec.decode(ActiveTripCodec.encode(trip)))
    }

    @Test
    fun decode_corruptValues_returnsNull() {
        assertNull(ActiveTripCodec.decode(""))
        assertNull(ActiveTripCodec.decode("1;AUTO"))
        assertNull(ActiveTripCodec.decode("1;AUTO;1;2;3;4;5")) // too few fields
        assertNull(ActiveTripCodec.decode("1;FLYING;1;2;3;4;5;6")) // unknown origin
        assertNull(ActiveTripCodec.decode("x;AUTO;1;2;3;4;5;6")) // bad start time
        assertNull(ActiveTripCodec.decode("1;AUTO;1;2;x;4;5;6")) // bad distance
        assertNull(ActiveTripCodec.decode("1;AUTO;1;2;3;4;5;x")) // bad end time
    }
}
