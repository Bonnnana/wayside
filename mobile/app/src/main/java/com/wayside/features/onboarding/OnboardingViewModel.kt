package com.wayside.features.onboarding

import com.wayside.managers.AppSessionManager
import com.wayside.navigation.AppNavigationRoute
import com.wayside.repositories.AppStateRepository
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val sessionManager: AppSessionManager,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state

    fun onToggleInterest(interest: String) = appStateRepository.toggleInterest(interest)

    /**
     * The intro is done: this device won't see welcome/permission/interests again, whether or
     * not the driver signs in right now.
     */
    fun onContinue() {
        sessionManager.markOnboardingCompleted()
        navigationManager.setHomePage(AppNavigationRoute.Login.route)
    }
}
