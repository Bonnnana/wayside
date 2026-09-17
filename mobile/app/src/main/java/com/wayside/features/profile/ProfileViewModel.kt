package com.wayside.features.profile

import com.wayside.data.Units
import com.wayside.managers.AppSessionManager
import com.wayside.navigation.AppNavigationRoute
import com.wayside.repositories.AppStateRepository
import com.wayside.ui.theme.ThemeMode
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val sessionManager: AppSessionManager,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    val state = appStateRepository.state
    val currentUser = sessionManager.currentUser

    fun onEditProfile() = navigationManager.navigateTo(AppNavigationRoute.EditProfile.route)

    fun onOpenSaved() = navigationManager.navigateTo(AppNavigationRoute.Saved.route)

    fun onToggleInterest(interest: String) = appStateRepository.toggleInterest(interest)

    fun onBudgetChange(minutes: Int) = appStateRepository.setBudget(minutes)

    fun onUnitsChange(units: Units) = appStateRepository.setUnits(units)

    fun onThemeModeChange(mode: ThemeMode) = appStateRepository.setThemeMode(mode)

    fun onSignOut() {
        sessionManager.clear()
        appStateRepository.clearSessionState()
        // Login replaces the stack: pressing back must not return to a signed-in screen. Not
        // Welcome — onboarding is a once-ever intro (see AppSessionManager.hasCompletedOnboarding),
        // and this driver has long since been through it.
        navigationManager.setHomePage(AppNavigationRoute.Login.route)
    }
}
