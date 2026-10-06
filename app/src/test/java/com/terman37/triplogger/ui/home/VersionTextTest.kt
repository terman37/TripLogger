package com.terman37.triplogger.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionTextTest {

    @Test
    fun `keeps the manifest version name and code`() {
        val version = appVersion("1.0", 1L)

        assertEquals("1.0", version.name)
        assertEquals(1L, version.code)
    }

    @Test
    fun `falls back to a question mark when the manifest has no version name`() {
        // versionName is optional in the manifest: never show "null" to the user.
        assertEquals("?", appVersion(null, 42L).name)
    }

    @Test
    fun `treats a blank version name as missing`() {
        assertEquals("?", appVersion("   ", 42L).name)
    }
}
