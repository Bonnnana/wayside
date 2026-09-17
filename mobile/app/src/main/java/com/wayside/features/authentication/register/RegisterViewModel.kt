package com.wayside.features.authentication.register

import androidx.lifecycle.viewModelScope
import com.wayside.managers.AppSessionManager
import com.wayside.models.api.auth.RegisterRequest
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

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
) {
    val passwordsMatch: Boolean
        get() = confirmPassword.isEmpty() || password == confirmPassword

    /**
     * Mirrors the server's rule (8+, upper, lower, digit, symbol) so the user sees the problem
     * while typing instead of after a round trip. The server still enforces it.
     */
    val passwordHint: String?
        get() = when {
            password.isEmpty() -> null
            password.length < 8 -> "At least 8 characters"
            password.none { it.isUpperCase() } -> "Add an uppercase letter"
            password.none { it.isLowerCase() } -> "Add a lowercase letter"
            password.none { it.isDigit() } -> "Add a number"
            password.all { it.isLetterOrDigit() } -> "Add a symbol"
            else -> null
        }

    val canSubmit: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
            passwordHint == null && password.isNotEmpty() && password == confirmPassword
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val sessionManager: AppSessionManager,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, error = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit || isBusyState.value) return

        setIsBusy(true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = authenticationService.register(
                RegisterRequest(
                    email = state.email.trim(),
                    password = state.password,
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                ),
            )

            if (response.isSuccessful) {
                // Registration returns tokens, so there is no second sign-in step. Onboarding
                // is a device-level intro shown before sign-in (see AppSessionManager
                // .hasCompletedOnboarding), not a per-account step, so a fresh account goes
                // straight in.
                sessionManager.store(response.assertedData)
                navigationManager.setHomePage(AppNavigationRoute.Home.route)
            } else {
                _uiState.update {
                    it.copy(
                        error = response.userMessage.ifBlank {
                            "Could not create the account. Try again."
                        },
                    )
                }
            }
            setIsBusy(false)
        }
    }

    fun onSignIn() = navigationManager.navigateTo(AppNavigationRoute.Login.route)

    fun onBack() = navigationManager.navigateBack()
}
