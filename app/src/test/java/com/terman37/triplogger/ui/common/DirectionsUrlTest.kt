package com.terman37.triplogger.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsUrlTest {

    @Test
    fun usesAddresses_whenAvailable() {
        val url = DirectionsUrl.build(
            originStreet = "12 Rue de Rivoli", originCity = "Paris",
            originLat = null, originLng = null,
            destinationStreet = "1 Rue de la République", destinationCity = "Lyon",
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url!!.startsWith("https://www.google.com/maps/dir/?api=1"))
        assertTrue(url.contains("origin=12+Rue+de+Rivoli%2C+Paris"))
        assertTrue(url.contains("destination=1+Rue+de+la+R%C3%A9publique%2C+Lyon"))
        assertTrue(url.endsWith("&travelmode=driving"))
    }

    @Test
    fun fallsBackToCoordinates_whenAddressMissing() {
        val url = DirectionsUrl.build(
            originStreet = null, originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = null, destinationCity = null,
            destinationLat = 48.8738, destinationLng = 2.2950,
        )
        assertTrue(url!!.contains("origin=48.856600%2C2.352200"))
        assertTrue(url.contains("destination=48.873800%2C2.295000"))
    }

    @Test
    fun addressWinsOverCoordinates() {
        val url = DirectionsUrl.build(
            originStreet = "1 Main St", originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = "2 Oak Rd", destinationCity = "Town",
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url!!.contains("origin=1+Main+St"))
        assertTrue(!url.contains("48.856600"))
    }

    @Test
    fun neitherAddressNorCoordinates_returnsNull() {
        val url = DirectionsUrl.build(
            originStreet = null, originCity = null,
            originLat = null, originLng = null,
            destinationStreet = "2 Oak Rd", destinationCity = null,
            destinationLat = null, destinationLng = null,
        )
        assertNull(url)
    }

    @Test
    fun specialCharacters_areEncoded() {
        val url = DirectionsUrl.build(
            originStreet = "Rue de l'Église & Mairie", originCity = "Ville-sur-Yvette",
            originLat = null, originLng = null,
            destinationStreet = "Chemin des Dames", destinationCity = null,
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url != null)
        assertTrue(url!!.contains("l%27%C3%89glise+%26+Mairie"))
    }

    @Test
    fun decimalSeparator_isDot_always() {
        val url = DirectionsUrl.build(
            originStreet = null, originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = null, destinationCity = null,
            destinationLat = 48.8738, destinationLng = 2.2950,
        )
        // Locale.US formatting: even on comma-locale machines the URL keeps dots.
        assertEquals(48.8566, url!!.substringAfter("origin=").substringBefore("%2C").toDouble(), 1e-9)
    }
}
