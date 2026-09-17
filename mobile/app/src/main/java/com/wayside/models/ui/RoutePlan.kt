package com.wayside.models.ui

import com.wayside.data.Place

/**
 * One origin → destination drive and the places worth stopping at along it, as the screens
 * need them. Built from `SuggestionsResponseDto` in `mappers/` — no wire type reaches a
 * composable.
 */
data class RoutePlan(
    val origin: String,
    val destination: String,
    val driveMinutes: Int,
    val distanceKm: Double,
    val budgetMinutes: Int,
    val places: List<Place>,
    /** The road itself, decoded. Empty when the API had no routing available. */
    val routePoints: List<GeoPoint> = emptyList(),
    val originPoint: GeoPoint? = null,
    val destinationPoint: GeoPoint? = null,
)
