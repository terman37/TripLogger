package com.terman37.triplogger.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionDiffTest {

    @Test
    fun firstPoll_reportsEveryConnectedDevice() {
        val events = ConnectionDiff.diff(
            previous = emptySet(),
            current = setOf("AA:BB", "CC:DD"),
        )
        assertEquals(setOf("AA:BB", "CC:DD"), events.connected.toSet())
        assertEquals(emptyList<String>(), events.disconnected)
    }

    @Test
    fun newDevice_isConnected() {
        val events = ConnectionDiff.diff(
            previous = setOf("AA:BB"),
            current = setOf("AA:BB", "CC:DD"),
        )
        assertEquals(listOf("CC:DD"), events.connected)
        assertEquals(emptyList<String>(), events.disconnected)
    }

    @Test
    fun removedDevice_isDisconnected() {
        val events = ConnectionDiff.diff(
            previous = setOf("AA:BB", "CC:DD"),
            current = setOf("AA:BB"),
        )
        assertEquals(emptyList<String>(), events.connected)
        assertEquals(listOf("CC:DD"), events.disconnected)
    }

    @Test
    fun unchangedPoll_reportsNothing() {
        val events = ConnectionDiff.diff(
            previous = setOf("AA:BB"),
            current = setOf("AA:BB"),
        )
        assertEquals(emptyList<String>(), events.connected)
        assertEquals(emptyList<String>(), events.disconnected)
    }

    @Test
    fun simultaneousSwap_reportsBoth() {
        val events = ConnectionDiff.diff(
            previous = setOf("AA:BB"),
            current = setOf("CC:DD"),
        )
        assertEquals(listOf("CC:DD"), events.connected)
        assertEquals(listOf("AA:BB"), events.disconnected)
    }
}
