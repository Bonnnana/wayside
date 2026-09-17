package com.wayside.navigation

import com.wayside.AppConstants
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import cc.infrastructure.android.library.navigation.models.NavigationParams

/**
 * Opening a place is the one navigation every list screen shares, and it always carries the
 * same argument — so it lives here rather than being rebuilt in each ViewModel.
 */
fun NavigationManager.navigateToPlace(placeId: String) = navigateTo(
    screenName = AppNavigationRoute.PlaceDetail.route,
    parameters = NavigationParams().apply {
        addOrReplace(AppConstants.NavigationParameters.PLACE_ID to placeId)
    },
)
