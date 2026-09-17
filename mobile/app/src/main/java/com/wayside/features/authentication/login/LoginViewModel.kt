package com.wayside.features.authentication.login

import androidx.lifecycle.viewModelScope
import com.wayside.managers.AppSessionManager
import com.wayside.navigation.AppNavigationRoute
import com.wayside.services.interfaces.api.AuthenticationService
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val error: String? = null,
) {
    /** Enough to attempt a sign-in. Real validation is the server's job. */
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val sessionManager: AppSessionManager,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit || isBusyState.value) return

        setIsBusy(true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = authenticationService.login(state.email.trim(), state.password)

            if (response.isSuccessful) {
                sessionManager.store(response.assertedData)
                // Home replaces the auth stack — pressing back must not return to login.
                navigationManager.setHomePage(AppNavigationRoute.Home.route)
            } else {
                _uiState.update {
                    it.copy(
                        error = response.userMessage.ifBlank {
                            "Could not sign in. Check your connection and try again."
                        },
                    )
                }
            }
            setIsBusy(false)
        }
    }

    fun onForgotPassword() =
        navigationManager.navigateTo(AppNavigationRoute.ForgotPassword.route)

    fun onRegister() = navigationManager.navigateTo(AppNavigationRoute.Register.route)

    /**
     * A back arrow only makes sense when it leads back into the onboarding intro, not just
     * anywhere. Login can be the very first screen the app ever shows (no back stack at all, a
     * returning device), and finishing onboarding replaces the stack with just Login (see
     * [com.wayside.features.onboarding.OnboardingViewModel.onContinue]) — neither has a back
     * target. So the back arrow only shows when the screen directly beneath Login on the stack
     * is the onboarding carousel itself.
     */
    fun canGoBackToOnboarding(): Boolean {
        val stack = navigationManager.navigationStack
        if (stack.size < 2) return false
        return stack[stack.size - 2].name == AppNavigationRoute.Welcome.route
    }

    fun onBack() = navigationManager.navigateBack()
}
