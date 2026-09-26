package com.terman37.triplogger.monitor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The boot/update restart rule: monitoring must only come back when the user
 * switched it on and at least one trigger device exists.
 */
class BootStartDecisionTest {

    @Test
    fun enabledWithDevices_starts() {
        assertTrue(
            BootStartDecision.shouldStartOnBoot(
                monitoringEnabled = true,
                registeredDeviceCount = 1,
            ),
        )
    }

    @Test
    fun enabledWithoutDevices_doesNotStart() {
        assertFalse(
            BootStartDecision.shouldStartOnBoot(
                monitoringEnabled = true,
                registeredDeviceCount = 0,
            ),
        )
    }

    @Test
    fun disabledWithDevices_doesNotStart() {
        assertFalse(
            BootStartDecision.shouldStartOnBoot(
                monitoringEnabled = false,
                registeredDeviceCount = 1,
            ),
        )
    }

    @Test
    fun disabledWithoutDevices_doesNotStart() {
        assertFalse(
            BootStartDecision.shouldStartOnBoot(
                monitoringEnabled = false,
                registeredDeviceCount = 0,
            ),
        )
    }
}
