package com.wayside.features.profile

import androidx.lifecycle.viewModelScope
import com.wayside.managers.AppSessionManager
import com.wayside.models.api.auth.UpdateProfileRequest
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

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val error: String? = null,
) {
    val canSubmit: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val sessionManager: AppSessionManager,
    private val authenticationService: AuthenticationService,
    private val navigationManager: NavigationManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        sessionManager.currentUser.value.let { user ->
            EditProfileUiState(firstName = user?.firstName ?: "", lastName = user?.lastName ?: "")
        },
    )
    val uiState = _uiState.asStateFlow()

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, error = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit || isBusyState.value) return

        setIsBusy(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = sessionManager.runWithToken(
                refresher = { refreshToken ->
                    authenticationService.refresh(refreshToken)
                        .let { if (it.isSuccessful) it.assertedData else null }
                },
                block = { accessToken ->
                    authenticationService.updateProfile(
                        accessToken = accessToken,
                        request = UpdateProfileRequest(
                            firstName = state.firstName.trim(),
                            lastName = state.lastName.trim(),
                        ),
                    )
                },
            )

            when {
                // No session to refresh with — the same dead end LoginViewModel treats as
                // "sign in again", since there is no valid token this call could have used.
                result == null -> _uiState.update {
                    it.copy(error = "Sign in again to update your profile.")
                }
                result.isSuccessful -> {
                    sessionManager.updateCurrentUser(result.assertedData)
                    navigationManager.navigateBack()
                }
                else -> _uiState.update {
                    it.copy(error = result.userMessage.ifBlank { "Could not save your changes." })
                }
            }
            setIsBusy(false)
        }
    }

    fun onBack() = navigationManager.navigateBack()
}
