package com.terman37.triplogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun samePoint_isZero() {
        assertEquals(0.0, DistanceCalculator.haversineKm(48.8566, 2.3522, 48.8566, 2.3522), 1e-9)
    }

    @Test
    fun oneDegreeOfLatitude_isAbout111km() {
        // On a sphere of radius 6371 km, 1 degree = π·R/180 ≈ 111.19 km.
        val km = DistanceCalculator.haversineKm(0.0, 0.0, 1.0, 0.0)
        assertEquals(111.19, km, 0.05)
    }

    @Test
    fun oneDegreeOfLongitudeAtEquator_isAbout111km() {
        val km = DistanceCalculator.haversineKm(0.0, 0.0, 0.0, 1.0)
        assertEquals(111.19, km, 0.05)
    }

    @Test
    fun typicalCityTrip_isPlausible() {
        // Arc de Triomphe → Parc de Vincennes, Paris (~9.2 km straight line).
        val km = DistanceCalculator.haversineKm(48.8738, 2.2950, 48.8405, 2.4107)
        assertTrue("expected roughly 9 km, got $km", km in 8.8..9.6)
    }

    @Test
    fun isSymmetric() {
        val a = DistanceCalculator.haversineKm(40.0, -73.0, 48.85, 2.35)
        val b = DistanceCalculator.haversineKm(48.85, 2.35, 40.0, -73.0)
        assertEquals(a, b, 1e-9)
    }

    @Test
    fun isNeverNegative() {
        // Antipodal points (max distance ≈ half circumference ≈ 20015 km).
        val km = DistanceCalculator.haversineKm(0.0, 0.0, 0.0, 180.0)
        assertTrue(km > 0)
        assertTrue("expected ~20015 km, got $km", km in 19000.0..20500.0)
    }
}
