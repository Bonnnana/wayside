package com.wayside.features.place

import androidx.lifecycle.viewModelScope
import com.wayside.AppConstants
import com.wayside.data.Place
import com.wayside.repositories.AppStateRepository
import com.wayside.repositories.PlaceRepository
import com.wayside.services.interfaces.api.PhotoUrlBuilder
import com.wayside.ui.placeOrNull
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import cc.infrastructure.android.library.navigation.models.NavigationParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceDetailUiState(
    val place: Place? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * The only screen that calls `/places/{id}`. A suggestion list carries enough to draw a card,
 * but the summary, tags and review themes exist on the detail endpoint alone.
 */
@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val appStateRepository: AppStateRepository,
    private val photoUrlBuilder: PhotoUrlBuilder,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    /** Photos load from our own API, which redirects to a signed URL. */
    fun photoUrl(placeId: String, index: Int) = photoUrlBuilder.photoUrl(placeId, index)

    private val _uiState = MutableStateFlow(PlaceDetailUiState())
    val uiState = _uiState.asStateFlow()

    /** Saved ids, chosen stops and units all live app-wide, not on this screen. */
    val appState = appStateRepository.state

    private var placeId: String? = null

    /** Called from NavigationConfigImpl on every recomposition — only the first one loads. */
    fun initialize(navParams: NavigationParams) {
        if (placeId != null) return
        val id = navParams.getValue<String>(AppConstants.NavigationParameters.PLACE_ID) ?: return
        placeId = id

        // Show what the list already knew about this place while the full detail loads, so
        // opening a card doesn't flash an empty screen.
        _uiState.update { it.copy(place = appStateRepository.state.value.placeOrNull(id)) }
        load()
    }

    override fun onRetry() = load()

    fun onToggleSaved() {
        // Saving keeps a copy of the place, so it survives falling out of the suggestions.
        _uiState.value.place?.let(appStateRepository::toggleSaved)
    }

    fun onToggleStop() = placeId?.let(appStateRepository::toggleStop)

    fun onBack() = navigationManager.navigateBack()

    /**
     * Detour minutes, distance off route and route progress belong to a drive, not to a place,
     * so `/places/{id}` returns them as zero — it has no route to measure against. The values
     * the suggestion list already carried are the right ones, so they survive the merge.
     */
    private fun merge(known: Place?, detail: Place): Place =
        if (known == null) {
            detail
        } else {
            detail.copy(
                detourMinutes = known.detourMinutes,
                distanceKm = known.distanceKm,
                routeProgress = known.routeProgress,
                mapX = known.mapX,
                mapY = known.mapY,
            )
        }

    private fun load() {
        val id = placeId ?: return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val response = placeRepository.place(id)

            _uiState.update { current ->
                if (response.isSuccessful) {
                    current.copy(place = merge(current.place, response.assertedData),
                        isLoading = false, error = null)
                } else {
                    current.copy(
                        isLoading = false,
                        // Something is already on screen when the summary was cached, so a
                        // failure there is worth reporting only if there is nothing to show.
                        error = if (current.place == null) {
                            response.userMessage.ifBlank { "Couldn't load this place." }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
