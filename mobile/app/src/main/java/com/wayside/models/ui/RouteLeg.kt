package com.wayside.models.ui

/**
 * One leg of a drive, as drawn.
 *
 * A leg is not the plan: the plan is origin → destination and stays put, while the leg is
 * whatever the driver is doing right now — heading for a stop they chose to visit, or back on
 * course for the destination.
 */
data class RouteLeg(
    val driveMinutes: Int,
    val distanceKm: Double,
    val points: List<GeoPoint>,
    val originPoint: GeoPoint? = null,
    val destinationPoint: GeoPoint? = null,
    /** What the driver is heading for, for the banner on screen. */
    val headingFor: String = "",
)
