package com.terman37.triplogger.location

import com.terman37.triplogger.core.GpsSample

/**
 * Boundary between Android location APIs and app logic. The trip service talks
 * to this interface only, so the rest of the code stays Android-free and
 * testable; a fake source can feed scripted [GpsSample]s in tests.
 */
interface LocationSource {

    /**
     * Starts delivering fixes to [onSample] at the configured interval.
     *
     * @return true when sampling started; false when it could not (missing
     *   ACCESS_FINE_LOCATION permission, or GPS disabled on the device). The
     *   caller decides what to tell the user.
     */
    fun start(onSample: (GpsSample) -> Unit): Boolean

    /** Stops delivering fixes. Safe to call when not started. */
    fun stop()

    /**
     * The most recent fix the OS cached, or null when none exists. Used right
     * after a trip starts so the recorder gets an anchor immediately instead
     * of waiting up to one sampling interval for the first fix.
     */
    fun lastKnown(): GpsSample?
}
