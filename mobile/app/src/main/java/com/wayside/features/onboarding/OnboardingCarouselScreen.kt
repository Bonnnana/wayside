package com.wayside.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wayside.features.welcome.WelcomeContent
import kotlinx.coroutines.launch

/**
 * The onboarding intro as one swipeable carousel — welcome, the location heads-up, then
 * interests — rather than three separate pushes on the navigation stack. A driver can flick
 * between pages as well as tap through them. [OnboardingViewModel.onContinue] is the only real
 * navigation out of here; every button before it just moves the pager.
 *
 * This is the single screen registered at [com.wayside.navigation.AppNavigationRoute.Welcome] —
 * there is no separate route for the permission or interests pages any more.
 */
@Composable
fun OnboardingCarouselScreen(onboardingViewModel: OnboardingViewModel) {
    val state by onboardingViewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    fun goToPage(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    // Mirrors the old per-screen back stack: back steps to the previous page instead of doing
    // nothing, but only takes over once there's a previous page in the carousel to step to.
    BackHandler(enabled = pagerState.currentPage > 0) {
        goToPage(pagerState.currentPage - 1)
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> WelcomeContent(onGetStarted = { goToPage(1) })
            1 -> OnboardingPermissionContent(
                onAllow = { goToPage(2) },
                onNotNow = { goToPage(2) },
            )
            else -> OnboardingInterestsContent(
                interests = state.interests,
                onToggleInterest = onboardingViewModel::onToggleInterest,
                onContinue = onboardingViewModel::onContinue,
            )
        }
    }
}

private const val PAGE_COUNT = 3
