package com.wayside.features.route

import com.wayside.navigation.AppNavigationRoute
import com.wayside.navigation.navigateToPlace
import com.wayside.repositories.AppStateRepository
import com.wayside.services.interfaces.api.PhotoUrlBuilder
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val photoUrlBuilder: PhotoUrlBuilder,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state

    /** Photos load from our own API, which redirects to a signed URL. */
    fun photoUrl(placeId: String, index: Int) = photoUrlBuilder.photoUrl(placeId, index)

    /** A new budget is a new request — the API decides what fits, not the client. */
    fun onBudgetChange(minutes: Int) = appStateRepository.setBudget(minutes)

    fun onSelectPin(placeId: String?) = appStateRepository.selectPin(placeId)

    /** Moves a stop earlier (-1) or later (+1) in the drive, and redraws the route. */
    fun onMoveStop(placeId: String, by: Int) = appStateRepository.moveStop(placeId, by)

    fun onRemoveStop(placeId: String) = appStateRepository.removeStop(placeId)



    fun onToggleStop(placeId: String) = appStateRepository.toggleStop(placeId)

    fun onOpenPlace(placeId: String) {
        appStateRepository.selectPin(placeId)
        navigationManager.navigateToPlace(placeId)
    }

    fun onStartRoute() {
        // Arrival times are counted from the moment the driver sets off, not from when the
        // plan was made — those can be an hour apart.
        appStateRepository.startRoute()
        navigationManager.navigateTo(AppNavigationRoute.ActiveRoute.route)
    }

    /** Swaps the drive and refetches — the old suggestions belong to the old direction. */
    fun onSwapRoute() = appStateRepository.swapRoute()

    /** Changing either end is the search screen's job. */
    fun onEditRoute() = navigationManager.navigateTo(AppNavigationRoute.Search.route)

    fun onBack() = navigationManager.navigateBack()

    override fun onRetry() = appStateRepository.refreshPlaces()

    override fun onPullToRefresh() = appStateRepository.refreshPlaces()
}
