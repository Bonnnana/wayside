package com.wayside.services.interfaces.api

import com.wayside.models.api.ServiceResponse
import com.wayside.models.api.auth.AuthTokensResponse
import com.wayside.models.api.auth.RegisterRequest
import com.wayside.models.api.auth.ResetPasswordRequest
import com.wayside.models.api.auth.UpdateProfileRequest
import com.wayside.models.api.auth.UserResponse

/**
 * Every call to the auth endpoints. Most of these are deliberately unauthenticated — the calls
 * made *before* there is a token — so they don't go through `AppSessionManager.runWithToken`.
 * [updateProfile] is the exception: it needs a valid access token, supplied by the caller (see
 * `AppSessionManager.runWithToken`).
 */
interface AuthenticationService {
    suspend fun register(request: RegisterRequest): ServiceResponse<AuthTokensResponse>

    suspend fun login(email: String, password: String): ServiceResponse<AuthTokensResponse>

    suspend fun refresh(refreshToken: String): ServiceResponse<AuthTokensResponse>

    suspend fun forgotPassword(email: String): ServiceResponse<Unit>

    suspend fun resetPassword(request: ResetPasswordRequest): ServiceResponse<Unit>

    suspend fun logout(refreshToken: String): ServiceResponse<Unit>

    suspend fun userExists(email: String): ServiceResponse<Boolean>

    suspend fun updateProfile(
        accessToken: String,
        request: UpdateProfileRequest,
    ): ServiceResponse<UserResponse>
}
