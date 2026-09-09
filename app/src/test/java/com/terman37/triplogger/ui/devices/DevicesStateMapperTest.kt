package com.terman37.triplogger.ui.devices

import com.terman37.triplogger.data.RegisteredDevice
import com.terman37.triplogger.monitor.PairedDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevicesStateMapperTest {

    private fun reg(address: String, name: String) = RegisteredDevice(address, name)
    private fun paired(address: String, name: String) = PairedDeviceInfo(address, name)

    private fun map(
        registered: List<RegisteredDevice> = emptyList(),
        pairedDevices: List<PairedDeviceInfo> = emptyList(),
        btPermission: Boolean = true,
    ) = DevicesStateMapper.toUi(
        graceMinutes = 3,
        registered = registered,
        paired = pairedDevices,
        hasBluetoothPermission = btPermission,
    )

    @Test
    fun available_excludesRegistered() {
        val state = map(
            registered = listOf(reg("AA:BB", "Car")),
            pairedDevices = listOf(
                paired("AA:BB", "Car"), // already registered → not available
                paired("CC:DD", "Headphones"),
            ),
        )
        assertEquals(1, state.available.size)
        assertEquals("CC:DD", state.available.single().address)
        assertEquals(1, state.registered.size)
    }

    @Test
    fun registeredName_prefersLiveName() {
        val state = map(
            registered = listOf(reg("AA:BB", "Old name")),
            pairedDevices = listOf(paired("AA:BB", "New name")),
        )
        assertEquals("New name", state.registered.single().name)
    }

    @Test
    fun registeredName_fallsBackToStoredWhenNotPairedAnymore() {
        val state = map(registered = listOf(reg("AA:BB", "Old name")))
        assertEquals("Old name", state.registered.single().name)
    }

    @Test
    fun hints_onlyWhenRelevant() {
        // Bluetooth permission missing → ask for access first.
        assertEquals(
            "Allow Bluetooth access to see paired devices.",
            map(btPermission = false).bluetoothHint,
        )
        // Permission ok but nothing paired → point to Android settings.
        assertEquals(
            "No paired devices. Pair in Android settings, then come back.",
            map().bluetoothHint,
        )
        // Paired devices exist → no hint needed.
        assertNull(
            map(pairedDevices = listOf(paired("CC:DD", "Headphones"))).bluetoothHint,
        )
    }
}
