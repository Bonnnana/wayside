package com.wayside.ui.navigation

object WaysideDestination {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val ONBOARDING_PERMISSION = "onboardingPermission"
    const val ONBOARDING_INTERESTS = "onboardingInterests"
    const val HOME = "home"
    const val ROUTE = "route"
    const val SAVED = "saved"
    const val PROFILE = "profile"
    const val ACTIVE_ROUTE = "activeRoute"

    const val PLACE_ID_ARG = "placeId"
    const val PLACE_DETAIL = "placeDetail/{$PLACE_ID_ARG}"

    fun placeDetail(placeId: String) = "placeDetail/$placeId"

    /** Destinations that keep the bottom bar. */
    val TOP_LEVEL = setOf(HOME, ROUTE, SAVED, PROFILE)
}
