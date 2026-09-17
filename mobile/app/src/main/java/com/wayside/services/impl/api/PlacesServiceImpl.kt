package com.wayside.services.impl.api

import com.wayside.AppConstants
import com.wayside.models.api.ServiceResponse
import com.wayside.models.api.place.DirectionsDto
import com.wayside.models.api.place.PlaceDetailDto
import com.wayside.models.api.place.PlaceSuggestionDto
import com.wayside.models.api.place.SuggestionsResponseDto
import com.wayside.services.interfaces.api.ConfigurationService
import com.wayside.services.interfaces.api.PlacesService
import kotlinx.serialization.builtins.ListSerializer
import cc.infrastructure.android.library.networking.interfaces.ApiClient
import cc.infrastructure.android.library.networking.models.ApiRequest
import cc.infrastructure.android.library.networking.models.HttpMethod
import javax.inject.Inject

class PlacesServiceImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val configurationService: ConfigurationService,
) : PlacesService {

    override suspend fun suggestions(
        origin: String,
        destination: String,
        budgetMinutes: Int,
        originPlaceId: String?,
        destinationPlaceId: String?,
    ): ServiceResponse<SuggestionsResponseDto> {
        val response = apiClient.send(
            req = buildRequest(
                path = AppConstants.ApiEndpoints.SUGGESTIONS,
                queryParameters = buildMap {
                    put("origin", origin)
                    put("destination", destination)
                    put("budget", budgetMinutes.toString())
                    // Sent only when the user picked from the dropdown: a place ID routes to
                    // the place they chose, where text is re-interpreted every call.
                    originPlaceId?.let { put("originPlaceId", it) }
                    destinationPlaceId?.let { put("destinationPlaceId", it) }
                },
            ),
            serializer = SuggestionsResponseDto.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    override suspend fun directions(
        origin: String,
        destination: String,
        originPlaceId: String?,
        destinationPlaceId: String?,
        stops: List<String>,
    ): ServiceResponse<DirectionsDto> {
        val response = apiClient.send(
            req = buildRequest(
                path = AppConstants.ApiEndpoints.DIRECTIONS,
                queryParameters = buildMap {
                    put("origin", origin)
                    put("destination", destination)
                    originPlaceId?.let { put("originPlaceId", it) }
                    destinationPlaceId?.let { put("destinationPlaceId", it) }
                    // One value per key is all the infra URL builder supports, so the stops
                    // travel pipe-separated. Pipe, not comma: a stop may be a "lat,lng" pair.
                    if (stops.isNotEmpty()) put("stops", stops.joinToString("|"))
                },
            ),
            serializer = DirectionsDto.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    override suspend fun autocomplete(
        query: String,
        latitude: Double?,
        longitude: Double?,
    ): ServiceResponse<List<PlaceSuggestionDto>> {
        val response = apiClient.send(
            req = buildRequest(
                path = AppConstants.ApiEndpoints.AUTOCOMPLETE,
                queryParameters = buildMap {
                    put("query", query)
                    latitude?.let { put("lat", it.toString()) }
                    longitude?.let { put("lng", it.toString()) }
                },
            ),
            serializer = ListSerializer(PlaceSuggestionDto.serializer()),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    override suspend fun place(placeId: String): ServiceResponse<PlaceDetailDto> {
        val response = apiClient.send(
            req = buildRequest(path = "${AppConstants.ApiEndpoints.PLACES}/$placeId"),
            serializer = PlaceDetailDto.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(statusCode = response.statusCode)
        }
    }

    private fun buildRequest(
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
    ) = ApiRequest(
        method = HttpMethod.GET,
        host = configurationService.apiHost,
        basePath = configurationService.apiBasePath,
        scheme = configurationService.apiScheme,
        port = configurationService.apiPort,
        path = path,
        queryParameters = queryParameters,
    )
}
