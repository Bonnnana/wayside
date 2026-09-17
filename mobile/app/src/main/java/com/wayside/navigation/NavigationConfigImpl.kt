package com.wayside.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wayside.features.activeroute.ActiveRouteScreen
import com.wayside.features.activeroute.ActiveRouteViewModel
import com.wayside.features.authentication.login.LoginScreen
import com.wayside.features.authentication.login.LoginViewModel
import com.wayside.features.authentication.password.ForgotPasswordScreen
import com.wayside.features.authentication.password.ForgotPasswordViewModel
import com.wayside.features.authentication.password.ResetPasswordScreen
import com.wayside.features.authentication.password.ResetPasswordViewModel
import com.wayside.features.authentication.register.RegisterScreen
import com.wayside.features.authentication.register.RegisterViewModel
import com.wayside.features.home.HomeNavBarHost
import com.wayside.features.onboarding.OnboardingCarouselScreen
import com.wayside.features.onboarding.OnboardingViewModel
import com.wayside.features.place.PlaceDetailScreen
import com.wayside.features.place.PlaceDetailViewModel
import com.wayside.features.profile.EditProfileScreen
import com.wayside.features.profile.EditProfileViewModel
import com.wayside.features.profile.ProfileScreen
import com.wayside.features.profile.ProfileViewModel
import com.wayside.features.route.RouteScreen
import com.wayside.features.route.RouteViewModel
import com.wayside.features.search.SearchScreen
import com.wayside.features.search.SearchViewModel
import com.wayside.features.saved.SavedScreen
import com.wayside.features.saved.SavedViewModel
import com.wayside.managers.AppSessionManager
import cc.infrastructure.android.library.navigation.interfaces.NavigationConfig
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for screen → ViewModel wiring. Every destination the app can reach
 * is registered here; nothing navigates to a route that isn't in this file.
 *
 * Screens take only their ViewModel — state and navigation are its job, not this file's.
 */
@Singleton
class NavigationConfigImpl @Inject constructor(
    private val sessionManager: AppSessionManager,
) : NavigationConfig {

    /**
     * A stored refresh token means the app can get back in without asking again. Otherwise, a
     * device that has already been through welcome/permission/interests skips straight to
     * sign-in rather than sitting through the intro a second time.
     */
    override fun determineInitialRoute(): String = when {
        sessionManager.isSignedIn -> AppNavigationRoute.Home.route
        sessionManager.hasCompletedOnboarding -> AppNavigationRoute.Login.route
        else -> AppNavigationRoute.Welcome.route
    }

    override fun registerNavigationScreens(
        systemPadding: PaddingValues,
        navigationManager: NavigationManager,
    ) {
        registerAuthScreens(navigationManager)
        registerMainScreens(navigationManager)
    }

    private fun registerAuthScreens(navigationManager: NavigationManager) {
        // The welcome/permission/interests intro is one swipeable carousel, not three separate
        // routes — see OnboardingCarouselScreen.
        navigationManager.registerForNavigation(AppNavigationRoute.Welcome.route) {
            OnboardingCarouselScreen(onboardingViewModel = hiltViewModel<OnboardingViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Login.route) {
            LoginScreen(viewModel = hiltViewModel<LoginViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Register.route) {
            RegisterScreen(viewModel = hiltViewModel<RegisterViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.ForgotPassword.route) {
            ForgotPasswordScreen(viewModel = hiltViewModel<ForgotPasswordViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.ResetPassword.route) { params ->
            // Keyed by the params uuid so navigating here twice builds a fresh ViewModel.
            val viewModel: ResetPasswordViewModel = hiltViewModel(key = params.uuid.toString())
            // Nav args can't go through the constructor, so they're handed over afterwards.
            viewModel.initialize(params.navParams)
            ResetPasswordScreen(viewModel = viewModel)
        }
    }

    private fun registerMainScreens(navigationManager: NavigationManager) {
        // Home, Saved and Profile all mount inside this one host so the bottom nav bar can sit
        // over all three — see HomeNavBarHost.
        navigationManager.registerForNavigation(AppNavigationRoute.Home.route) {
            HomeNavBarHost()
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Search.route) {
            SearchScreen(viewModel = hiltViewModel<SearchViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Route.route) {
            RouteScreen(viewModel = hiltViewModel<RouteViewModel>(), bottomPadding = BOTTOM_INSET)
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Saved.route) {
            SavedScreen(viewModel = hiltViewModel<SavedViewModel>(), bottomPadding = BOTTOM_INSET)
        }

        navigationManager.registerForNavigation(AppNavigationRoute.Profile.route) {
            ProfileScreen(
                viewModel = hiltViewModel<ProfileViewModel>(),
                bottomPadding = BOTTOM_INSET,
            )
        }

        navigationManager.registerForNavigation(AppNavigationRoute.EditProfile.route) {
            EditProfileScreen(viewModel = hiltViewModel<EditProfileViewModel>())
        }

        navigationManager.registerForNavigation(AppNavigationRoute.PlaceDetail.route) { params ->
            // Keyed by the params uuid so opening a second place builds its own ViewModel.
            val viewModel: PlaceDetailViewModel = hiltViewModel(key = params.uuid.toString())
            viewModel.initialize(params.navParams)
            PlaceDetailScreen(viewModel = viewModel)
        }

        navigationManager.registerForNavigation(AppNavigationRoute.ActiveRoute.route) {
            ActiveRouteScreen(viewModel = hiltViewModel<ActiveRouteViewModel>())
        }
    }

    private companion object {
        /**
         * Screens draw full-bleed and take their own bottom inset. Zero until the bottom bar is
         * mounted as a tab host.
         */
        val BOTTOM_INSET = Dp(0f)
    }
}
