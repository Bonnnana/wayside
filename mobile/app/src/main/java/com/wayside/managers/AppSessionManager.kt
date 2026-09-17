package com.wayside.managers

import com.wayside.AppConstants
import com.wayside.models.api.auth.AuthTokensResponse
import com.wayside.models.api.auth.UserResponse
import cc.infrastructure.android.library.localstorage.interfaces.SecureStorageService
import cc.infrastructure.android.library.localstorage.interfaces.SharedPreferencesService
import cc.infrastructure.android.library.utils.lenientJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the signed-in session: tokens, the current user, and refreshing before a call fails.
 *
 * Tokens live in [SecureStorageService] (EncryptedSharedPreferences), never in plain prefs.
 */
@Singleton
class AppSessionManager @Inject constructor(
    private val secureStorage: SecureStorageService,
    private val sharedPreferences: SharedPreferencesService,
) {
    /**
     * Refreshing is serialised: several screens can hit an expiring token at once, and without
     * this they would each spend the refresh token. The backend rotates on every refresh, so
     * the second caller's token would already be dead.
     */
    private val refreshMutex = Mutex()

    private val _currentUser = MutableStateFlow(readUser())
    val currentUser = _currentUser.asStateFlow()

    val isSignedIn: Boolean get() = refreshToken != null

    /**
     * True once the driver has been through welcome/permission/interests, on this device,
     * regardless of whether they ended up signing in — so a returning, signed-out driver lands
     * on sign-in rather than sitting through the intro again.
     */
    val hasCompletedOnboarding: Boolean
        get() = sharedPreferences.getString(AppConstants.StorageKeys.ONBOARDING_COMPLETED) != null

    fun markOnboardingCompleted() {
        sharedPreferences.putString(AppConstants.StorageKeys.ONBOARDING_COMPLETED, "true")
    }

    val accessToken: String? get() = secureStorage.getString(AppConstants.StorageKeys.ACCESS_TOKEN)

    val refreshToken: String? get() = secureStorage.getString(AppConstants.StorageKeys.REFRESH_TOKEN)

    private val expiresAt: Instant?
        get() = secureStorage.getString(AppConstants.StorageKeys.ACCESS_TOKEN_EXPIRES_AT)
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    /** True when the access token is gone, unparseable, or close enough to expiry to be risky. */
    val needsRefresh: Boolean
        get() {
            val expiry = expiresAt ?: return true
            return Instant.now().plusSeconds(EXPIRY_LEEWAY_SECONDS).isAfter(expiry)
        }

    fun store(tokens: AuthTokensResponse) {
        secureStorage.putString(AppConstants.StorageKeys.ACCESS_TOKEN, tokens.accessToken)
        secureStorage.putString(AppConstants.StorageKeys.REFRESH_TOKEN, tokens.refreshToken)
        secureStorage.putString(
            AppConstants.StorageKeys.ACCESS_TOKEN_EXPIRES_AT,
            tokens.expiresAtUtc,
        )
        secureStorage.putString(
            AppConstants.StorageKeys.USER,
            lenientJson.encodeToString(UserResponse.serializer(), tokens.user),
        )
        _currentUser.value = tokens.user
    }

    /** Wipes the session. Safe to call when already signed out. */
    fun clear() {
        secureStorage.clear()
        _currentUser.value = null
    }

    /**
     * Updates the cached profile after an edit, without touching the tokens — unlike [store],
     * there is no new token pair here, just a changed name to reflect everywhere the current
     * user is read from.
     */
    fun updateCurrentUser(user: UserResponse) {
        secureStorage.putString(
            AppConstants.StorageKeys.USER,
            lenientJson.encodeToString(UserResponse.serializer(), user),
        )
        _currentUser.value = user
    }

    /**
     * Runs [block] with a valid access token, refreshing first when the current one is close to
     * expiry. Returns null when there is no session to refresh — the caller should route to login.
     *
     * @param refresher How to exchange a refresh token for a new pair. Passed in rather than
     *   injected so this manager doesn't depend on the service that depends on it.
     */
    suspend fun <T> runWithToken(
        refresher: suspend (String) -> AuthTokensResponse?,
        block: suspend (String) -> T,
    ): T? {
        if (needsRefresh) {
            refreshMutex.withLock {
                // Re-check inside the lock: whoever held it may have already refreshed.
                if (needsRefresh) {
                    val current = refreshToken ?: return null
                    val renewed = refresher(current) ?: run {
                        clear()
                        return null
                    }
                    store(renewed)
                }
            }
        }

        val token = accessToken ?: return null
        return block(token)
    }

    private fun readUser(): UserResponse? =
        secureStorage.getString(AppConstants.StorageKeys.USER)?.let { raw ->
            runCatching { lenientJson.decodeFromString(UserResponse.serializer(), raw) }.getOrNull()
        }

    private companion object {
        /** Refresh this far ahead of expiry so a request can't expire mid-flight. */
        const val EXPIRY_LEEWAY_SECONDS = 60L
    }
}
