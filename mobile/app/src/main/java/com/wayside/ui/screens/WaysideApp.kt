package com.wayside.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wayside.data.placeById
import com.wayside.ui.WaysideViewModel
import com.wayside.ui.components.WaysideNavBar
import com.wayside.ui.navigation.WaysideDestination
import kotlinx.coroutines.launch

@Composable
fun WaysideApp(
    viewModel: WaysideViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun snack(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavBar = currentRoute in WaysideDestination.TOP_LEVEL

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(WaysideDestination.HOME) { inclusive = route == WaysideDestination.HOME }
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showNavBar) {
                WaysideNavBar(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTopLevel,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        // Map screens draw full-bleed behind the bar, so they take the bottom inset as a
        // value rather than as padding.
        val bottomPadding = padding.calculateBottomPadding()

        NavHost(
            navController = navController,
            startDestination = WaysideDestination.WELCOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(200, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(200, easing = FastOutSlowInEasing)) },
        ) {
            composable(WaysideDestination.WELCOME) {
                WelcomeScreen(
                    onGetStarted = { navController.navigate(WaysideDestination.LOGIN) },
                    onSkip = { navController.navigate(WaysideDestination.HOME) },
                )
            }

            composable(WaysideDestination.LOGIN) {
                LoginScreen(
                    onSignedInWithGoogle = {
                        navController.navigate(WaysideDestination.ONBOARDING_PERMISSION)
                        snack("Signed in as marta.bento@gmail.com")
                    },
                    onEmail = { snack("Coming soon") },
                    onGuest = { navController.navigate(WaysideDestination.HOME) },
                )
            }

            composable(WaysideDestination.ONBOARDING_PERMISSION) {
                OnboardingPermissionScreen(
                    onAllow = { navController.navigate(WaysideDestination.ONBOARDING_INTERESTS) },
                    onNotNow = { navController.navigate(WaysideDestination.ONBOARDING_INTERESTS) },
                )
            }

            composable(WaysideDestination.ONBOARDING_INTERESTS) {
                OnboardingInterestsScreen(
                    interests = state.interests,
                    onToggleInterest = viewModel::toggleInterest,
                    onContinue = { navController.navigate(WaysideDestination.HOME) },
                )
            }

            composable(WaysideDestination.HOME) {
                HomeScreen(
                    state = state,
                    onPlaceClick = { id ->
                        viewModel.selectPin(id)
                        navController.navigate(WaysideDestination.placeDetail(id))
                    },
                    onPlanDetour = { navigateTopLevel(WaysideDestination.ROUTE) },
                    onOpenSaved = { navigateTopLevel(WaysideDestination.SAVED) },
                    onSearch = { snack("Search is coming soon") },
                    bottomPadding = bottomPadding,
                )
            }

            composable(WaysideDestination.ROUTE) {
                RouteScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onBudgetChange = viewModel::setBudget,
                    onSelectPin = viewModel::selectPin,
                    onToggleStop = viewModel::toggleStop,
                    onOpenPlace = { id ->
                        navController.navigate(WaysideDestination.placeDetail(id))
                    },
                    onStartRoute = { navController.navigate(WaysideDestination.ACTIVE_ROUTE) },
                    bottomPadding = bottomPadding,
                )
            }

            composable(WaysideDestination.SAVED) {
                SavedScreen(
                    state = state,
                    onFilterChange = viewModel::setSavedFilter,
                    onUnsave = viewModel::toggleSaved,
                    onOpenPlace = { id ->
                        navController.navigate(WaysideDestination.placeDetail(id))
                    },
                    onExplore = { navigateTopLevel(WaysideDestination.HOME) },
                    bottomPadding = bottomPadding,
                )
            }

            composable(WaysideDestination.PROFILE) {
                ProfileScreen(
                    state = state,
                    onToggleInterest = viewModel::toggleInterest,
                    onBudgetChange = viewModel::setBudget,
                    onUnitsChange = viewModel::setUnits,
                    onThemeModeChange = viewModel::setThemeMode,
                    onSignOut = {
                        navController.navigate(WaysideDestination.WELCOME) {
                            popUpTo(0)
                        }
                    },
                    bottomPadding = bottomPadding,
                )
            }

            composable(WaysideDestination.PLACE_DETAIL) { entry ->
                val placeId = entry.arguments?.getString(WaysideDestination.PLACE_ID_ARG)
                    ?: WaysideDestination.PLACE_ID_ARG
                val place = placeById(placeId)
                PlaceDetailScreen(
                    place = place,
                    saved = place.id in state.savedIds,
                    added = place.id in state.stops,
                    units = state.units,
                    onBack = { navController.popBackStack() },
                    onToggleSaved = {
                        viewModel.toggleSaved(place.id)
                        snack(
                            if (place.id in state.savedIds) {
                                "Removed ${place.name} from saved"
                            } else {
                                "Saved ${place.name}"
                            },
                        )
                    },
                    onToggleStop = { viewModel.toggleStop(place.id) },
                )
            }

            composable(WaysideDestination.ACTIVE_ROUTE) {
                ActiveRouteScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRemoveStop = viewModel::removeStop,
                    onEnd = {
                        viewModel.clearStops()
                        navigateTopLevel(WaysideDestination.HOME)
                    },
                    onEditStops = { navController.popBackStack() },
                )
            }
        }
    }
}
