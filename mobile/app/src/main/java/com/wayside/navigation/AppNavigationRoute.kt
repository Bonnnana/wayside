package com.wayside.navigation

/**
 * Every screen the app can navigate to. `route` is the identifier used to register the screen
 * in [NavigationConfigImpl] and to navigate to it.
 */
sealed class AppNavigationRoute(val route: String) {
    // Auth
    data object Welcome : AppNavigationRoute("welcome")
    data object Login : AppNavigationRoute("login")
    data object Register : AppNavigationRoute("register")
    data object ForgotPassword : AppNavigationRoute("forgot_password")
    data object ResetPassword : AppNavigationRoute("reset_password")

    // Main
    data object Home : AppNavigationRoute("home")
    data object Search : AppNavigationRoute("search")
    data object Route : AppNavigationRoute("route")
    data object Saved : AppNavigationRoute("saved")
    data object Profile : AppNavigationRoute("profile")
    data object PlaceDetail : AppNavigationRoute("place_detail")
    data object EditProfile : AppNavigationRoute("edit_profile")
    data object ActiveRoute : AppNavigationRoute("active_route")

    companion object {
        /** Destinations shown as tabs inside [com.wayside.features.home.HomeNavBarHost]. */
        val TOP_LEVEL = setOf(Home.route, Saved.route, Profile.route)
    }
}
