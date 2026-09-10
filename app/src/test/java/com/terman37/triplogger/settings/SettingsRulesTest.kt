package com.terman37.triplogger.settings

import com.terman37.triplogger.data.RegisteredDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRulesTest {

    private val car = RegisteredDevice("AA:BB", "Car")
    private val headset = RegisteredDevice("CC:DD", "Headset")

    @Test
    fun clampGrace_keepsLegalValues() {
        assertEquals(1, SettingsRepository.clampGraceMinutes(1))
        assertEquals(7, SettingsRepository.clampGraceMinutes(7))
        assertEquals(15, SettingsRepository.clampGraceMinutes(15))
    }

    @Test
    fun clampGrace_boundsOutOfRangeValues() {
        assertEquals(1, SettingsRepository.clampGraceMinutes(0))
        assertEquals(1, SettingsRepository.clampGraceMinutes(-5))
        assertEquals(15, SettingsRepository.clampGraceMinutes(60))
    }

    @Test
    fun addDevice_appendsNewAddress() {
        val result = SettingsRepository.withDeviceAdded(listOf(car), headset)
        assertEquals(listOf(car, headset), result)
    }

    @Test
    fun addDevice_ignoresDuplicateAddress() {
        val renamedSameAddress = RegisteredDevice("AA:BB", "Car renamed")
        val result = SettingsRepository.withDeviceAdded(listOf(car), renamedSameAddress)
        assertEquals(listOf(car), result) // stored name kept, no duplicates
    }

    @Test
    fun removeDevice_dropsOnlyThatAddress() {
        val result = SettingsRepository.withDeviceRemoved(listOf(car, headset), "AA:BB")
        assertEquals(listOf(headset), result)
    }

    @Test
    fun removeDevice_unknownAddress_changesNothing() {
        val devices = listOf(car, headset)
        assertEquals(devices, SettingsRepository.withDeviceRemoved(devices, "ZZ:ZZ"))
    }
}
