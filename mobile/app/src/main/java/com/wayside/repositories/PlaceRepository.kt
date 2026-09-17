package com.wayside.repositories

import com.wayside.data.Place
import com.wayside.mappers.toRoutePlan
import com.wayside.mappers.toUi
import com.wayside.models.api.ServiceResponse
import com.wayside.models.ui.PlaceSuggestion
import com.wayside.models.ui.RouteLeg
import com.wayside.models.ui.RoutePlan
import com.wayside.services.interfaces.api.PlacesService
import com.wayside.utils.PolylineDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Places, as the app thinks about them. Owns the call to `PlacesService` and the conversion to
 * UI models, so nothing above this line handles a wire type.
 */
@Singleton
class PlaceRepository @Inject constructor(
    private val placesService: PlacesService,
) {
    suspend fun suggestions(
        origin: String,
        destination: String,
        budgetMinutes: Int,
        originPlaceId: String? = null,
        destinationPlaceId: String? = null,
    ): ServiceResponse<RoutePlan> {
        val response = placesService.suggestions(
            origin = origin,
            destination = destination,
            budgetMinutes = budgetMinutes,
            originPlaceId = originPlaceId,
            destinationPlaceId = destinationPlaceId,
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData.toRoutePlan())
        } else {
            ServiceResponse.fail(
                userMessage = response.userMessage,
                statusCode = response.statusCode,
            )
        }
    }

    /** The drive between two points — what the map draws while rerouting. */
    suspend fun directions(
        origin: String,
        destination: String,
        originPlaceId: String? = null,
        destinationPlaceId: String? = null,
        stops: List<String> = emptyList(),
        headingFor: String = "",
    ): ServiceResponse<RouteLeg> {
        val response = placesService.directions(
            origin = origin,
            destination = destination,
            originPlaceId = originPlaceId,
            destinationPlaceId = destinationPlaceId,
            stops = stops,
        )

        return if (response.isSuccessful) {
            val leg = response.assertedData
            ServiceResponse.fromData(
                RouteLeg(
                    driveMinutes = leg.driveMinutes,
                    distanceKm = leg.distanceKm,
                    points = PolylineDecoder.decode(leg.polyline),
                    originPoint = leg.originPoint?.toUi(),
                    destinationPoint = leg.destinationPoint?.toUi(),
                    headingFor = headingFor,
                ),
            )
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    /** Place predictions for the search dropdown. */
    suspend fun autocomplete(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
    ): ServiceResponse<List<PlaceSuggestion>> {
        val response = placesService.autocomplete(query, latitude, longitude)

        return if (response.isSuccessful) {
            ServiceResponse.fromData(
                response.assertedData.map {
                    PlaceSuggestion(it.placeId, it.primaryText, it.secondaryText)
                },
            )
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    suspend fun place(placeId: String): ServiceResponse<Place> {
        val response = placesService.place(placeId)

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData.toUi())
        } else {
            ServiceResponse.fail(
                userMessage = response.userMessage,
                statusCode = response.statusCode,
            )
        }
    }
}
