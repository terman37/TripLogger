package com.terman37.triplogger.ui.home

import com.terman37.triplogger.ui.home.MonitoringPermissionFlow.Step
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Permission gate order on the Home switch: ask for the foreground group first,
 * then "Allow all the time" location, then monitoring is ready.
 */
class MonitoringPermissionFlowTest {

    @Test
    fun nothingGranted_requestsForeground() {
        assertEquals(
            Step.REQUEST_FOREGROUND,
            MonitoringPermissionFlow.nextStep(
                foregroundGranted = false,
                backgroundGranted = false,
            ),
        )
    }

    @Test
    fun onlyBackgroundGranted_requestsForeground() {
        // Background location implies foreground on a real device, but the flow
        // must still ask for the foreground group first (defensive, order fixed).
        assertEquals(
            Step.REQUEST_FOREGROUND,
            MonitoringPermissionFlow.nextStep(
                foregroundGranted = false,
                backgroundGranted = true,
            ),
        )
    }

    @Test
    fun foregroundOnly_opensBackgroundSettings() {
        assertEquals(
            Step.OPEN_BACKGROUND_SETTINGS,
            MonitoringPermissionFlow.nextStep(
                foregroundGranted = true,
                backgroundGranted = false,
            ),
        )
    }

    @Test
    fun allGranted_isReady() {
        assertEquals(
            Step.READY,
            MonitoringPermissionFlow.nextStep(
                foregroundGranted = true,
                backgroundGranted = true,
            ),
        )
    }
}
