package com.wayside.features.authentication.password

import androidx.lifecycle.viewModelScope
import com.wayside.AppConstants
import com.wayside.models.api.auth.ResetPasswordRequest
import com.wayside.navigation.AppNavigationRoute
import com.wayside.services.interfaces.api.AuthenticationService
import cc.infrastructure.android.library.base.ui.BaseViewModel
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import cc.infrastructure.android.library.navigation.models.NavigationParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Digits in a password-reset code, as issued by the API's phone token provider. */
private const val CODE_LENGTH = 6

data class ForgotPasswordUiState(
    val email: String = "",
    val sent: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit || isBusyState.value) return

        setIsBusy(true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = authenticationService.forgotPassword(state.email.trim())

            // The API answers 200 whether or not the account exists, so success here means
            // "we sent it if there was anywhere to send it" — never "this email is registered".
            if (response.isSuccessful) {
                _uiState.update { it.copy(sent = true) }
            } else {
                _uiState.update {
                    it.copy(error = response.userMessage.ifBlank { "Could not send the code." })
                }
            }
            setIsBusy(false)
        }
    }

    fun onEnterCode() = navigationManager.navigateTo(
        screenName = AppNavigationRoute.ResetPassword.route,
        parameters = NavigationParams().apply {
            addOrReplace(AppConstants.NavigationParameters.EMAIL to _uiState.value.email.trim())
        },
    )

    fun onBack() = navigationManager.navigateBack()
}

data class ResetPasswordUiState(
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
) {
    val passwordsMatch: Boolean
        get() = confirmPassword.isEmpty() || password == confirmPassword

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
        get() = email.isNotBlank() && code.length == CODE_LENGTH && passwordHint == null &&
            password.isNotEmpty() && password == confirmPassword
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()

    /** Called from NavigationConfigImpl — the email is carried over from Forgot password. */
    fun initialize(navParams: NavigationParams) {
        navParams.getValue<String>(AppConstants.NavigationParameters.EMAIL)?.let { email ->
            _uiState.update { it.copy(email = email) }
        }
    }

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    /** Six digits, nothing else — a pasted code often arrives with stray whitespace. */
    fun onCodeChange(v: String) =
        _uiState.update { it.copy(code = v.filter(Char::isDigit).take(CODE_LENGTH), error = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit || isBusyState.value) return

        setIsBusy(true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = authenticationService.resetPassword(
                ResetPasswordRequest(
                    email = state.email.trim(),
                    token = state.code,
                    newPassword = state.password,
                ),
            )

            if (response.isSuccessful) {
                // Back to login rather than straight in: resetting does not sign you in, and
                // the API has just invalidated every existing session for this account.
                navigationManager.setHomePage(AppNavigationRoute.Login.route)
            } else {
                _uiState.update {
                    it.copy(
                        error = response.userMessage.ifBlank {
                            "That code is not valid. Ask for a new one."
                        },
                    )
                }
            }
            setIsBusy(false)
        }
    }

    fun onBack() = navigationManager.navigateBack()
}
