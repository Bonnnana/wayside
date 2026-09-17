package com.wayside.features.activeroute

import com.wayside.navigation.AppNavigationRoute
import com.wayside.repositories.AppStateRepository
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActiveRouteViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state

    fun onRemoveStop(placeId: String) = appStateRepository.removeStop(placeId)

    fun onBack() = navigationManager.navigateBack()

    /** Editing stops is going back to the route screen, which still holds them. */
    fun onEditStops() = navigationManager.navigateBack()

    fun onEnd() {
        appStateRepository.clearStops()
        navigationManager.setHomePage(AppNavigationRoute.Home.route)
    }
}
