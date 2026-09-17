package com.wayside.services.impl.api

import com.wayside.AppConstants
import com.wayside.models.api.ServiceResponse
import com.wayside.models.api.auth.AuthErrorResponse
import com.wayside.models.api.auth.AuthTokensResponse
import com.wayside.models.api.auth.ForgotPasswordRequest
import com.wayside.models.api.auth.LoginRequest
import com.wayside.models.api.auth.RefreshRequest
import com.wayside.models.api.auth.RegisterRequest
import com.wayside.models.api.auth.ResetPasswordRequest
import com.wayside.models.api.auth.UpdateProfileRequest
import com.wayside.models.api.auth.UserExistsResponse
import com.wayside.models.api.auth.UserResponse
import com.wayside.services.interfaces.api.AuthenticationService
import com.wayside.services.interfaces.api.ConfigurationService
import cc.infrastructure.android.library.networking.interfaces.ApiClient
import cc.infrastructure.android.library.networking.models.ApiRequest
import cc.infrastructure.android.library.networking.models.ApiRequestWithBody
import cc.infrastructure.android.library.networking.models.ApiResponse
import cc.infrastructure.android.library.networking.models.HttpMethod
import cc.infrastructure.android.library.utils.lenientJson
import javax.inject.Inject

class AuthenticationServiceImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val configurationService: ConfigurationService,
) : AuthenticationService {

    override suspend fun register(
        request: RegisterRequest,
    ): ServiceResponse<AuthTokensResponse> = postForTokens(
        path = AppConstants.ApiEndpoints.REGISTER,
        body = request,
        serializer = RegisterRequest.serializer(),
    )

    override suspend fun login(
        email: String,
        password: String,
    ): ServiceResponse<AuthTokensResponse> = postForTokens(
        path = AppConstants.ApiEndpoints.LOGIN,
        body = LoginRequest(email, password),
        serializer = LoginRequest.serializer(),
    )

    override suspend fun refresh(
        refreshToken: String,
    ): ServiceResponse<AuthTokensResponse> = postForTokens(
        path = AppConstants.ApiEndpoints.REFRESH,
        body = RefreshRequest(refreshToken),
        serializer = RefreshRequest.serializer(),
    )

    override suspend fun forgotPassword(email: String): ServiceResponse<Unit> = postForUnit(
        path = AppConstants.ApiEndpoints.FORGOT_PASSWORD,
        body = ForgotPasswordRequest(email),
        serializer = ForgotPasswordRequest.serializer(),
    )

    override suspend fun resetPassword(
        request: ResetPasswordRequest,
    ): ServiceResponse<Unit> = postForUnit(
        path = AppConstants.ApiEndpoints.RESET_PASSWORD,
        body = request,
        serializer = ResetPasswordRequest.serializer(),
    )

    override suspend fun logout(refreshToken: String): ServiceResponse<Unit> = postForUnit(
        path = AppConstants.ApiEndpoints.LOGOUT,
        body = RefreshRequest(refreshToken),
        serializer = RefreshRequest.serializer(),
    )

    override suspend fun userExists(email: String): ServiceResponse<Boolean> {
        val response = apiClient.send(
            req = ApiRequest(
                method = HttpMethod.GET,
                host = configurationService.apiHost,
                basePath = configurationService.apiBasePath,
                scheme = configurationService.apiScheme,
                port = configurationService.apiPort,
                path = AppConstants.ApiEndpoints.USER_EXISTS,
                queryParameters = mapOf("email" to email),
            ),
            serializer = UserExistsResponse.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData.exists)
        } else {
            ServiceResponse.fail(userMessage = response.readUserMessage())
        }
    }

    override suspend fun updateProfile(
        accessToken: String,
        request: UpdateProfileRequest,
    ): ServiceResponse<UserResponse> {
        val response = apiClient.send(
            req = ApiRequestWithBody(
                method = HttpMethod.PUT,
                host = configurationService.apiHost,
                basePath = configurationService.apiBasePath,
                scheme = configurationService.apiScheme,
                port = configurationService.apiPort,
                path = AppConstants.ApiEndpoints.UPDATE_PROFILE,
                body = request,
                bodySerializer = UpdateProfileRequest.serializer(),
            ),
            token = accessToken,
            serializer = UserResponse.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(
                userMessage = response.readUserMessage(),
                statusCode = response.statusCode,
            )
        }
    }

    private fun <T : Any> postForTokens(
        path: String,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): ServiceResponse<AuthTokensResponse> {
        val response = apiClient.send(
            req = buildRequest(path, body, serializer),
            serializer = AuthTokensResponse.serializer(),
        )

        return if (response.isSuccessful) {
            ServiceResponse.fromData(response.assertedData)
        } else {
            ServiceResponse.fail(
                userMessage = response.readUserMessage(),
                statusCode = response.statusCode,
            )
        }
    }

    private fun <T : Any> postForUnit(
        path: String,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): ServiceResponse<Unit> {
        val response = apiClient.sendNoResponseBody(req = buildRequest(path, body, serializer))

        return if (response.isSuccessful) {
            ServiceResponse.success()
        } else {
            ServiceResponse.fail(
                userMessage = response.readUserMessage(),
                statusCode = response.statusCode,
            )
        }
    }

    private fun <T : Any> buildRequest(
        path: String,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>,
    ) = ApiRequestWithBody(
        method = HttpMethod.POST,
        host = configurationService.apiHost,
        basePath = configurationService.apiBasePath,
        scheme = configurationService.apiScheme,
        port = configurationService.apiPort,
        path = path,
        body = body,
        bodySerializer = serializer,
    )

    /**
     * Pulls the human-readable part out of the API's error body. Falls back to blank rather than
     * showing raw JSON — the caller decides what a blank message looks like on screen.
     */
    private fun <T> ApiResponse<T>.readUserMessage(): String {
        val raw = errorMessage ?: return ""
        return runCatching {
            lenientJson.decodeFromString(AuthErrorResponse.serializer(), raw).message
        }.getOrDefault("")
    }
}
