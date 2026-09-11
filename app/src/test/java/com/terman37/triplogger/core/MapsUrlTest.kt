package com.terman37.triplogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapsUrlTest {

    // --- directions (UI button) ---

    @Test
    fun directions_usesAddresses_whenAvailable() {
        val url = MapsUrl.directions(
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
    fun directions_fallsBackToCoordinates_whenAddressMissing() {
        val url = MapsUrl.directions(
            originStreet = null, originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = null, destinationCity = null,
            destinationLat = 48.8738, destinationLng = 2.2950,
        )
        assertTrue(url!!.contains("origin=48.856600%2C2.352200"))
        assertTrue(url.contains("destination=48.873800%2C2.295000"))
    }

    @Test
    fun directions_addressWinsOverCoordinates() {
        val url = MapsUrl.directions(
            originStreet = "1 Main St", originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = "2 Oak Rd", destinationCity = "Town",
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url!!.contains("origin=1+Main+St"))
        assertTrue(!url.contains("48.856600"))
    }

    @Test
    fun directions_neitherAddressNorCoordinates_returnsNull() {
        val url = MapsUrl.directions(
            originStreet = null, originCity = null,
            originLat = null, originLng = null,
            destinationStreet = "2 Oak Rd", destinationCity = null,
            destinationLat = null, destinationLng = null,
        )
        assertNull(url)
    }

    @Test
    fun directions_cityOnly_prefersCoordinatesOverCity() {
        val url = MapsUrl.directions(
            originStreet = null, originCity = "Lyon",
            originLat = 45.7640, originLng = 4.8357,
            destinationStreet = "2 Oak Rd", destinationCity = "Town",
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url!!.contains("origin=45.764000%2C4.835700"))
        assertTrue(!url.contains("origin=Lyon"))
    }

    @Test
    fun directions_specialCharacters_areEncoded() {
        val url = MapsUrl.directions(
            originStreet = "Rue de l'Église & Mairie", originCity = "Ville-sur-Yvette",
            originLat = null, originLng = null,
            destinationStreet = "Chemin des Dames", destinationCity = null,
            destinationLat = null, destinationLng = null,
        )
        assertTrue(url != null)
        assertTrue(url!!.contains("l%27%C3%89glise+%26+Mairie"))
    }

    @Test
    fun directions_decimalSeparator_isDot_always() {
        val url = MapsUrl.directions(
            originStreet = null, originCity = null,
            originLat = 48.8566, originLng = 2.3522,
            destinationStreet = null, destinationCity = null,
            destinationLat = 48.8738, destinationLng = 2.2950,
        )
        // Locale.US formatting: even on comma-locale machines the URL keeps dots.
        assertEquals(48.8566, url!!.substringAfter("origin=").substringBefore("%2C").toDouble(), 1e-9)
    }

    // --- place (export address/trip links) ---

    @Test
    fun place_usesAddress_whenAvailable() {
        val url = MapsUrl.place(
            street = "12 Rue de Rivoli", city = "Paris",
            lat = 48.8566, lng = 2.3522,
        )
        assertTrue(url!!.startsWith("https://www.google.com/maps/search/?api=1&query="))
        assertTrue(url.contains("query=12+Rue+de+Rivoli%2C+Paris"))
        assertTrue(!url.contains("48.856600"))
    }

    @Test
    fun place_fallsBackToCoordinates() {
        val url = MapsUrl.place(street = null, city = null, lat = 48.8566, lng = 2.3522)
        assertTrue(url!!.contains("query=48.856600%2C2.352200"))
    }

    @Test
    fun place_cityOnly_prefersCoordinatesOverCity() {
        // Regression: a city-only address made the link zoom to the whole city
        // instead of the exact trip point, even though coordinates existed.
        val url = MapsUrl.place(street = null, city = "Lyon", lat = 45.7640, lng = 4.8357)
        assertTrue(url!!.contains("query=45.764000%2C4.835700"))
        assertTrue(!url.contains("Lyon"))
    }

    @Test
    fun place_cityOnlyWithoutCoordinates_usesCity() {
        val url = MapsUrl.place(street = null, city = "Lyon", lat = null, lng = null)
        assertTrue(url!!.contains("query=Lyon"))
    }

    @Test
    fun place_withoutAddressOrCoordinates_returnsNull() {
        assertNull(MapsUrl.place(street = null, city = null, lat = null, lng = null))
    }

    @Test
    fun place_specialCharacters_areEncoded() {
        val url = MapsUrl.place(
            street = "Rue de l'Église & Mairie", city = "Ville-sur-Yvette",
            lat = null, lng = null,
        )
        assertTrue(url != null)
        assertTrue(url!!.contains("l%27%C3%89glise+%26+Mairie"))
    }
}
