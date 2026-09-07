package com.terman37.triplogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressPartsTest {

    @Test
    fun houseNumberAndStreet_areJoined() {
        val result = AddressParts.from("12", "Rue de Rivoli", "Paris")
        assertEquals("12 Rue de Rivoli", result.street)
        assertEquals("Paris", result.city)
    }

    @Test
    fun streetWithoutNumber_keepsStreetOnly() {
        val result = AddressParts.from(null, "Route Nationale 7", "Lyon")
        assertEquals("Route Nationale 7", result.street)
        assertEquals("Lyon", result.city)
    }

    @Test
    fun whitespaceIsTrimmed() {
        val result = AddressParts.from(" 12 ", "  Rue X  ", " Paris ")
        assertEquals("12 Rue X", result.street)
        assertEquals("Paris", result.city)
    }

    @Test
    fun noStreetFound_returnsNullStreet() {
        val result = AddressParts.from(null, null, "Lyon")
        assertNull(result.street)
        assertEquals("Lyon", result.city)
    }

    @Test
    fun nothingFound_returnsBothNull() {
        val result = AddressParts.from(null, null, null)
        assertNull(result.street)
        assertNull(result.city)
    }
}
