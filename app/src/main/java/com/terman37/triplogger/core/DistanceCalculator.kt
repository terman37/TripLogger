package com.terman37.triplogger.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle distance between two GPS points, using the Haversine formula on
 * a spherical Earth. Accurate to well under 1% for the tens-of-kilometers
 * distances of car trips — plenty for expense reporting (todo.md).
 *
 * Why Haversine and not the Android Location.distanceTo: this class runs on the
 * JVM in unit tests without any Android dependency.
 */
object DistanceCalculator {

    /**
     * @return distance in kilometers between the two points, always >= 0.
     */
    fun haversineKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
    ): Double {
        // Convert degrees to radians; the formula works on radians.
        // (Kotlin has no Double.toRadians(); degrees × π/180 does the same.)
        val rad = PI / 180.0
        val phi1 = lat1 * rad
        val phi2 = lat2 * rad
        val deltaPhi = (lat2 - lat1) * rad
        val deltaLambda = (lng2 - lng1) * rad

        // Haversine: a = sin²(Δφ/2) + cos φ1 · cos φ2 · sin²(Δλ/2)
        val a = sin(deltaPhi / 2).pow(2) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        // c = 2 · atan2(√a, √(1−a)); asin of √a is equivalent and cheaper here.
        val c = 2 * asin(sqrt(a))
        return TrackingPolicy.EARTH_RADIUS_KM * c
    }
}
