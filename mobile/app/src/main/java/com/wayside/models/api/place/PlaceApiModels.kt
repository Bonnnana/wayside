package com.wayside.models.api.place

import kotlinx.serialization.Serializable

/**
 * Wire models for the place endpoints. Field names match the API's JSON exactly, and the
 * category names must stay identical to `PlaceCategory` in `Models/Api/PlaceModels.cs` —
 * the backend serialises enums as names, so a rename on either side breaks the other.
 */

@Serializable
enum class PlaceCategoryDto {
    Food,
    Nature,
    Culture,
    Viewpoints,
    HiddenGem,
}

@Serializable
data class ReviewThemeDto(
    val label: String,
    val count: Int,
)

/** A place as it appears in a suggestion list — enough to draw a pin and a card. */
@Serializable
data class PlaceSummaryDto(
    val id: String,
    val name: String,
    val category: PlaceCategoryDto,
    val rating: Double,
    val ratingCount: Int,
    val detourMinutes: Int,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    /** How far along the drive this place sits, 0..1. Drives stop ordering. */
    val routeProgress: Double,
    /** How many photos this place has; 0 means the card falls back to the placeholder. */
    val photoCount: Int = 0,
)

/** Everything the detail screen shows, including the generated summary. */
@Serializable
data class PlaceDetailDto(
    val id: String,
    val name: String,
    val category: PlaceCategoryDto,
    val rating: Double,
    val ratingCount: Int,
    val detourMinutes: Int,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val routeProgress: Double,
    val openUntil: String? = null,
    val highlight: String = "",
    val aiSummary: String = "",
    val tags: List<String> = emptyList(),
    val reviewThemes: List<ReviewThemeDto> = emptyList(),
    /** How many photos the place has; the app builds the URLs from the count. */
    val photoCount: Int = 0,
)

/** Suggestions along one origin → destination drive, within a detour budget. */
@Serializable
data class SuggestionsResponseDto(
    val origin: String,
    val destination: String,
    val driveMinutes: Int,
    val distanceKm: Double,
    val budgetMinutes: Int,
    val places: List<PlaceSummaryDto> = emptyList(),
    /** The drive itself, as Google's encoded polyline. Null when routing was unavailable. */
    val routePolyline: String? = null,
    val originPoint: GeoPointDto? = null,
    val destinationPoint: GeoPointDto? = null,
)

@Serializable
data class GeoPointDto(val latitude: Double, val longitude: Double)

/** One drive with no suggestions attached — used when rerouting to a stop and back again. */
@Serializable
data class DirectionsDto(
    val driveMinutes: Int,
    val distanceKm: Double,
    val polyline: String? = null,
    val originPoint: GeoPointDto? = null,
    val destinationPoint: GeoPointDto? = null,
)

/** One row of the search dropdown. */
@Serializable
data class PlaceSuggestionDto(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
)
