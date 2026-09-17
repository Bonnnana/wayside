package com.wayside.features.saved

import com.wayside.navigation.navigateToPlace
import com.wayside.repositories.AppStateRepository
import com.wayside.services.interfaces.api.PhotoUrlBuilder
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val photoUrlBuilder: PhotoUrlBuilder,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state

    /** Photos load from our own API, which redirects to a signed URL. */
    fun photoUrl(placeId: String, index: Int) = photoUrlBuilder.photoUrl(placeId, index)

    fun onFilterChange(filter: String) = appStateRepository.setSavedFilter(filter)

    fun onUnsave(placeId: String) = appStateRepository.removeSaved(placeId)

    fun onOpenPlace(placeId: String) {
        appStateRepository.selectPin(placeId)
        navigationManager.navigateToPlace(placeId)
    }
}
