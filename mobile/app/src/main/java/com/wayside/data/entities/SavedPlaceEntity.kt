package com.wayside.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wayside.data.Place
import com.wayside.data.PlaceCategory

/**
 * A place the user saved.
 *
 * Saved is not a view of the current suggestions — lowering the detour budget, or driving
 * somewhere else entirely, must not empty the Saved tab — so the place is copied here rather
 * than referenced by id.
 *
 * Detour minutes, distance off route and route progress are kept as they were when the place
 * was saved. They belong to the drive it was found on, so they are a record of that, not a
 * claim about any future one.
 */
@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val ratingCount: Int,
    val detourMinutes: Int,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val routeProgress: Float,
    /** How many photos this place has, so the Saved tab can still show one after the place
     * falls out of the current suggestions and this row becomes its only source. */
    val photoCount: Int = 0,
    val savedAtUtc: Long,
)

fun SavedPlaceEntity.toPlace(): Place = Place(
    id = id,
    name = name,
    category = runCatching { PlaceCategory.valueOf(category) }
        .getOrDefault(PlaceCategory.HiddenGem),
    rating = rating,
    ratingCount = ratingCount,
    detourMinutes = detourMinutes,
    distanceKm = distanceKm,
    latitude = latitude,
    longitude = longitude,
    mapX = 0f,
    mapY = 0f,
    routeProgress = routeProgress,
    photoCount = photoCount,
)

fun Place.toSavedEntity(savedAtUtc: Long = System.currentTimeMillis()): SavedPlaceEntity =
    SavedPlaceEntity(
        id = id,
        name = name,
        category = category.name,
        rating = rating,
        ratingCount = ratingCount,
        detourMinutes = detourMinutes,
        distanceKm = distanceKm,
        latitude = latitude,
        longitude = longitude,
        routeProgress = routeProgress,
        photoCount = photoCount,
        savedAtUtc = savedAtUtc,
    )
