package com.yourteam.deliveryagent.core

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Haversine formula to calculate distance between two geographic coordinates.
 * Returns distance in kilometers.
 *
 * Used to calculate distance from delivery agent's current location to order pickup location.
 * Approximate but good enough for MVP distance display.
 */
object DistanceUtils {

    private const val EARTH_RADIUS_KM = 6371.0  // Earth's radius in kilometers

    /**
     * Calculate distance between two points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 (agent's current location)
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2 (pickup location)
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    fun haversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * acos(kotlin.math.sqrt(a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Format distance for display (rounded to 1 decimal place).
     */
    fun formatDistance(distanceKm: Double): String {
        return String.format("%.1f km", distanceKm)
    }
}
