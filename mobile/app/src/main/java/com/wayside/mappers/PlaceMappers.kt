package com.wayside.mappers

import com.wayside.data.Place
import com.wayside.data.PlaceCategory
import com.wayside.data.ReviewTheme
import com.wayside.models.api.place.PlaceCategoryDto
import com.wayside.models.api.place.PlaceDetailDto
import com.wayside.models.api.place.PlaceSummaryDto
import com.wayside.models.api.place.ReviewThemeDto
import com.wayside.models.api.place.SuggestionsResponseDto
import com.wayside.models.api.place.GeoPointDto
import com.wayside.models.ui.GeoPoint
import com.wayside.models.ui.RoutePlan
import com.wayside.utils.PolylineDecoder
import com.wayside.ui.components.routeAnchorAt

/**
 * Wire models in, UI models out. The one piece of real work here is map position: the API
 * describes where a place *is* (latitude/longitude), while the stylised map needs where to
 * *draw* it, so a place is anchored to the drawn route by how far along the drive it sits.
 */

private const val MIN_OFFSET = 0.015f
private const val MAX_OFFSET = 0.055f

/** How far off the road a place is, in kilometres, at which the pin sits furthest from it. */
private const val FAR_KM = 12.0

fun PlaceCategoryDto.toUi(): PlaceCategory = when (this) {
    PlaceCategoryDto.Food -> PlaceCategory.Food
    PlaceCategoryDto.Nature -> PlaceCategory.Nature
    PlaceCategoryDto.Culture -> PlaceCategory.Culture
    PlaceCategoryDto.Viewpoints -> PlaceCategory.Viewpoints
    PlaceCategoryDto.HiddenGem -> PlaceCategory.HiddenGem
}

fun ReviewThemeDto.toUi(): ReviewTheme = ReviewTheme(label = label, count = count)

fun SuggestionsResponseDto.toRoutePlan(): RoutePlan = RoutePlan(
    origin = origin,
    destination = destination,
    driveMinutes = driveMinutes,
    distanceKm = distanceKm,
    budgetMinutes = budgetMinutes,
    places = places.map { it.toUi() },
    routePoints = PolylineDecoder.decode(routePolyline),
    originPoint = originPoint?.toUi(),
    destinationPoint = destinationPoint?.toUi(),
)

fun GeoPointDto.toUi(): GeoPoint = GeoPoint(latitude, longitude)

fun PlaceSummaryDto.toUi(): Place {
    val (x, y) = mapPosition(id, routeProgress, distanceKm)
    return Place(
        id = id,
        name = name,
        category = category.toUi(),
        rating = rating,
        ratingCount = ratingCount,
        detourMinutes = detourMinutes,
        distanceKm = distanceKm,
        latitude = latitude,
        longitude = longitude,
        photoCount = photoCount,
        mapX = x,
        mapY = y,
        routeProgress = routeProgress.toFloat(),
    )
}

fun PlaceDetailDto.toUi(): Place {
    val (x, y) = mapPosition(id, routeProgress, distanceKm)
    return Place(
        id = id,
        name = name,
        category = category.toUi(),
        rating = rating,
        ratingCount = ratingCount,
        detourMinutes = detourMinutes,
        distanceKm = distanceKm,
        latitude = latitude,
        longitude = longitude,
        openUntil = openUntil.orEmpty(),
        highlight = highlight,
        aiSummary = aiSummary,
        tags = tags,
        reviewThemes = reviewThemes.map { it.toUi() },
        photoCount = photoCount,
        mapX = x,
        mapY = y,
        routeProgress = routeProgress.toFloat(),
    )
}

/**
 * Deterministic so a place lands in the same spot every time it is mapped — the detail screen
 * re-maps the same place from a different endpoint, and a pin that jumped would look like a bug.
 */
private fun mapPosition(id: String, routeProgress: Double, distanceKm: Double): Pair<Float, Float> {
    val (point, normal) = routeAnchorAt(routeProgress.toFloat())
    val reach = (distanceKm / FAR_KM).coerceIn(0.0, 1.0).toFloat()
    val magnitude = MIN_OFFSET + reach * (MAX_OFFSET - MIN_OFFSET)
    // Alternate sides so a cluster of nearby places doesn't stack into one blob.
    val side = if (id.hashCode() and 1 == 0) 1f else -1f
    val offset = magnitude * side

    return (point.x + normal.x * offset).coerceIn(0.06f, 0.94f) to
        (point.y + normal.y * offset).coerceIn(0.06f, 0.94f)
}
