package com.wayside.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wayside.features.profile.ProfileScreen
import com.wayside.features.profile.ProfileViewModel
import com.wayside.features.saved.SavedScreen
import com.wayside.features.saved.SavedViewModel
import com.wayside.navigation.AppNavigationRoute
import com.wayside.ui.components.WaysideNavBar

/**
 * The single route the three bottom-tab screens mount under (see `CLAUDE.md`: "the bottom-nav
 * tabs mount as a single HomeNavBar route"). Switching tabs is local Compose state, not a
 * [cc.infrastructure.android.library.navigation.interfaces.NavigationManager] navigation — there
 * is nothing to push or animate, and the screens' `@HiltViewModel`s are already activity-scoped
 * singletons (no navigation-args key), so each tab's state simply survives the switch.
 *
 * Screens still draw full-bleed behind the bar: its measured height is threaded down as
 * `bottomPadding` rather than reserved with a `Scaffold` `bottomBar` slot, matching every other
 * top-level screen's `bottomPadding: Dp` parameter.
 */
@Composable
fun HomeNavBarHost() {
    var selectedTab by rememberSaveable { mutableStateOf(AppNavigationRoute.Home.route) }
    // A sane guess before the first layout pass measures the bar's real height.
    var barHeight by remember { mutableStateOf(80.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        when (selectedTab) {
            AppNavigationRoute.Saved.route -> SavedScreen(
                viewModel = hiltViewModel<SavedViewModel>(),
                bottomPadding = barHeight,
            )
            AppNavigationRoute.Profile.route -> ProfileScreen(
                viewModel = hiltViewModel<ProfileViewModel>(),
                bottomPadding = barHeight,
            )
            else -> HomeScreen(
                viewModel = hiltViewModel<HomeViewModel>(),
                bottomPadding = barHeight,
            )
        }

        WaysideNavBar(
            currentRoute = selectedTab,
            onNavigate = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size ->
                    barHeight = with(density) { size.height.toDp() }
                },
        )
    }
}
