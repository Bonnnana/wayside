package com.wayside.features.search

import androidx.lifecycle.viewModelScope
import com.wayside.managers.interfaces.LocationManager
import com.wayside.models.ui.GeoPoint
import com.wayside.models.ui.PlaceSuggestion
import com.wayside.navigation.AppNavigationRoute
import com.wayside.repositories.AppStateRepository
import com.wayside.repositories.PlaceRepository
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which field the dropdown currently belongs to. */
enum class SearchField { Origin, Destination }

data class SearchUiState(
    val origin: String = "",
    val destination: String = "",
    /**
     * Set only when the text came from a suggestion. Routing prefers a place ID, because free
     * text is re-interpreted on every call and "Sintra" can resolve somewhere else.
     */
    val originPlaceId: String? = null,
    val destinationPlaceId: String? = null,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val activeField: SearchField? = null,
    val isSearching: Boolean = false,
    /**
     * Set when the origin is "where I am". The field reads as words, but the drive is planned
     * from these coordinates — a geocoder would make nonsense of the phrase.
     */
    val myLocationOrigin: GeoPoint? = null,
    val isLocatingMe: Boolean = false,
    /** Set when Find detours was tapped with a field that isn't a real, picked place. */
    val error: String? = null,
) {
    val canSubmit: Boolean get() = origin.isNotBlank() && destination.isNotBlank()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val placeRepository: PlaceRepository,
    private val locationManager: LocationManager,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        // Seeded with the current drive, so opening this to change one end doesn't clear both.
        with(appStateRepository.state.value) {
            SearchUiState(
                origin = origin,
                destination = destination,
                originPlaceId = originPlaceId,
                destinationPlaceId = destinationPlaceId,
            )
        },
    )
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onOriginChange(value: String) {
        // Typing over a chosen place unpicks it: the id no longer matches what's on screen.
        _uiState.update {
            it.copy(
                origin = value,
                originPlaceId = null,
                myLocationOrigin = null,
                activeField = SearchField.Origin,
                error = null,
            )
        }
        search(value)
    }

    fun onDestinationChange(value: String) {
        _uiState.update {
            it.copy(
                destination = value,
                destinationPlaceId = null,
                activeField = SearchField.Destination,
                error = null,
            )
        }
        search(value)
    }

    fun onSuggestionPicked(suggestion: PlaceSuggestion) {
        searchJob?.cancel()
        _uiState.update { current ->
            when (current.activeField) {
                SearchField.Origin -> current.copy(
                    origin = suggestion.label,
                    originPlaceId = suggestion.placeId,
                )

                SearchField.Destination -> current.copy(
                    destination = suggestion.label,
                    destinationPlaceId = suggestion.placeId,
                )

                null -> current
            }.copy(suggestions = emptyList(), activeField = null, isSearching = false, error = null)
        }
    }

    /** Starts the drive from wherever the phone is. */
    fun onUseMyLocation() {
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLocatingMe = true) }
            val point = locationManager.awaitLocation()

            _uiState.update { current ->
                if (point == null) {
                    // No permission, or no fix yet — say nothing and leave the field alone.
                    current.copy(isLocatingMe = false)
                } else {
                    current.copy(
                        origin = MY_LOCATION_LABEL,
                        originPlaceId = null,
                        myLocationOrigin = point,
                        suggestions = emptyList(),
                        activeField = null,
                        isLocatingMe = false,
                        error = null,
                    )
                }
            }
        }
    }

    /** Clears the dropdown without touching either field — a tap outside it, not a pick. */
    fun onDismissSuggestions() = _uiState.update {
        if (it.activeField == null) it else it.copy(activeField = null, suggestions = emptyList())
    }

    fun onSwap() = _uiState.update {
        it.copy(
            origin = it.destination,
            destination = it.origin,
            originPlaceId = it.destinationPlaceId,
            destinationPlaceId = it.originPlaceId,
            suggestions = emptyList(),
            activeField = null,
        )
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        // Free text alone is only ever a guess — "Sintra" could be the town or a street of the
        // same name somewhere else. Require an actual picked place (or a real GPS fix for "my
        // location") before planning a route on it, and say so rather than routing on a guess.
        val originIsReal = state.myLocationOrigin != null || state.originPlaceId != null
        val destinationIsReal = state.destinationPlaceId != null
        if (!originIsReal || !destinationIsReal) {
            _uiState.update {
                it.copy(
                    error = "Pick a place from the list for " +
                        when {
                            !originIsReal && !destinationIsReal -> "both fields"
                            !originIsReal -> "\"From\""
                            else -> "\"To\""
                        } + " — typing alone won't plan a route.",
                )
            }
            return
        }

        appStateRepository.setRoute(
            // Routes takes free text, and "lat,lng" is text every geocoder understands.
            origin = state.myLocationOrigin
                ?.let { "${it.latitude},${it.longitude}" }
                ?: state.origin,
            destination = state.destination,
            originPlaceId = state.originPlaceId,
            destinationPlaceId = state.destinationPlaceId,
            // "Your location" on screen; the coordinates only ever go to the API.
            originLabel = state.myLocationOrigin?.let { MY_LOCATION_LABEL },
        )
        // Route replaces this screen: coming back to a search you've already run is pointless.
        navigationManager.navigateBack()
        navigationManager.navigateTo(AppNavigationRoute.Route.route)
    }

    fun onBack() = navigationManager.navigateBack()

    /**
     * Debounced, because every keystroke would otherwise be a billed Places call — and the
     * answers would arrive out of order, so the dropdown would flicker between queries.
     */
    private fun search(query: String) {
        searchJob?.cancel()

        if (query.trim().length < MIN_QUERY) {
            _uiState.update { it.copy(suggestions = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }

            val response = placeRepository.autocomplete(query.trim())

            _uiState.update { current ->
                current.copy(
                    suggestions = if (response.isSuccessful) response.assertedData else emptyList(),
                    isSearching = false,
                )
            }
        }
    }

    private companion object {
        const val MY_LOCATION_LABEL = "Your location"
        const val MIN_QUERY = 2
        const val DEBOUNCE_MS = 250L
    }
}
