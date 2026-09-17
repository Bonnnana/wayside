package com.wayside.services.interfaces.api

import com.wayside.models.api.ServiceResponse
import com.wayside.models.api.place.DirectionsDto
import com.wayside.models.api.place.PlaceDetailDto
import com.wayside.models.api.place.PlaceSuggestionDto
import com.wayside.models.api.place.SuggestionsResponseDto

/**
 * The read surface for places. Unauthenticated for now — the API serves suggestions to anyone,
 * so these calls don't go through `AppSessionManager.runWithToken`.
 */
interface PlacesService {
    /** Places worth stopping at along a drive, already filtered to the detour budget. */
    suspend fun suggestions(
        origin: String,
        destination: String,
        budgetMinutes: Int,
        originPlaceId: String? = null,
        destinationPlaceId: String? = null,
    ): ServiceResponse<SuggestionsResponseDto>

    /** The drive between two points, with no suggestions attached. */
    suspend fun directions(
        origin: String,
        destination: String,
        originPlaceId: String? = null,
        destinationPlaceId: String? = null,
        /** Places to pass through, in order. The API keeps the order given. */
        stops: List<String> = emptyList(),
    ): ServiceResponse<DirectionsDto>

    /** Place predictions for a partly typed search. */
    suspend fun autocomplete(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
    ): ServiceResponse<List<PlaceSuggestionDto>>

    /** Full detail for one place, including its generated summary. */
    suspend fun place(placeId: String): ServiceResponse<PlaceDetailDto>
}
