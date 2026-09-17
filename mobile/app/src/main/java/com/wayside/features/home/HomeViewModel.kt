package com.wayside.features.home

import com.wayside.navigation.AppNavigationRoute
import com.wayside.navigation.navigateToPlace
import com.wayside.repositories.AppStateRepository
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state

    fun onPlaceClick(placeId: String) {
        appStateRepository.selectPin(placeId)
        navigationManager.navigateToPlace(placeId)
    }

    fun onPlanDetour() = navigationManager.navigateTo(AppNavigationRoute.Route.route)

    fun onSearch() = navigationManager.navigateTo(AppNavigationRoute.Search.route)

    override fun onRetry() = appStateRepository.refreshPlaces()

    override fun onPullToRefresh() = appStateRepository.refreshPlaces()
}
