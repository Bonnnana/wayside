package com.wayside.utils

import com.wayside.models.ui.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** The small amount of geometry the map needs to know where the driver is on the route. */
object Geo {

    private const val EARTH_RADIUS_M = 6_371_008.8

    fun distanceMetres(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)

        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /**
     * The part of the route still ahead of the driver.
     *
     * Finds the nearest point on the line and drops everything before it, so the drawn route
     * shortens as the drive is made. Only applied when the driver is actually near the line —
     * someone who hasn't set off yet is not "past" the whole route, they just aren't on it.
     */
    fun remainingRoute(
        points: List<GeoPoint>,
        current: GeoPoint?,
        fromIndex: Int = 0,
        onRouteToleranceM: Double = ON_ROUTE_TOLERANCE_M,
    ): List<GeoPoint> {
        val index = progressIndex(points, current, fromIndex, onRouteToleranceM)
        // Keep the nearest point, so the line still reaches back to the driver rather than
        // starting somewhere up ahead.
        return if (index <= 0) points else points.subList(index, points.size)
    }

    /**
     * How far along the route the driver has got, as an index into [points].
     *
     * Searched forward from [fromIndex] only, and never allowed to go backwards. A route that
     * doubles back — the same road driven twice, a loop through a town — puts the driver
     * physically nearest to a point they passed twenty minutes ago, and a plain nearest-point
     * search would jump the line forward to it.
     */
    fun progressIndex(
        points: List<GeoPoint>,
        current: GeoPoint?,
        fromIndex: Int = 0,
        onRouteToleranceM: Double = ON_ROUTE_TOLERANCE_M,
    ): Int {
        if (current == null || points.size < 2) return fromIndex

        val start = fromIndex.coerceIn(0, points.lastIndex)
        var nearestIndex = start
        var nearest = Double.MAX_VALUE

        for (index in start..points.lastIndex) {
            val distance = distanceMetres(points[index], current)
            if (distance < nearest) {
                nearest = distance
                nearestIndex = index
            }
        }

        // Not on the route yet — hold whatever progress was already made.
        return if (nearest > onRouteToleranceM) start else nearestIndex
    }

    /** Close enough to the start that the approach line would be noise. */
    fun isAtPoint(a: GeoPoint?, b: GeoPoint?, toleranceM: Double = AT_POINT_TOLERANCE_M): Boolean =
        a != null && b != null && distanceMetres(a, b) <= toleranceM

    const val ON_ROUTE_TOLERANCE_M = 250.0
    const val AT_POINT_TOLERANCE_M = 120.0
}
