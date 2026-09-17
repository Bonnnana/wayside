package com.wayside.models.api.auth

import kotlinx.serialization.Serializable

/**
 * Wire models for the auth endpoints. Field names match the API's JSON exactly — the backend serialises
 * camelCase, so no `@SerialName` is needed. Changing a name here means changing
 * `Models/Api/AuthModels.cs` on the backend in the same commit.
 */

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val newPassword: String,
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    /** ISO-8601 UTC. Lets the app refresh before a call fails rather than after. */
    val expiresAtUtc: String,
    val user: UserResponse,
)

/** Error body returned by every failed auth call. */
@Serializable
data class AuthErrorResponse(
    val message: String = "",
    val errors: List<String>? = null,
)

@Serializable
data class UserExistsResponse(val exists: Boolean)
