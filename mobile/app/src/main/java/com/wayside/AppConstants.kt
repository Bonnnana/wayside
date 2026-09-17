package com.wayside

/** Every magic string the app sends or stores. Nothing endpoint-shaped lives inline. */
object AppConstants {

    object ApiEndpoints {
        const val REGISTER = "auth/register"
        const val LOGIN = "auth/login"
        const val REFRESH = "auth/refresh"
        const val FORGOT_PASSWORD = "auth/forgot-password"
        const val RESET_PASSWORD = "auth/reset-password"
        const val LOGOUT = "auth/logout"
        const val USER_EXISTS = "auth/exists"
        const val UPDATE_PROFILE = "auth/profile"

        const val SUGGESTIONS = "routes/suggestions"
        const val DIRECTIONS = "routes/directions"
        const val PLACES = "places"
        const val AUTOCOMPLETE = "places/autocomplete"
    }

    /** Keys in [cc.infrastructure.android.library.localstorage.interfaces.SecureStorageService]. */
    object StorageKeys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
        const val USER = "user"
        /** Set once the driver has been through the welcome/permission/interests flow. */
        const val ONBOARDING_COMPLETED = "onboarding_completed"
    }

    object NavigationParameters {
        const val EMAIL = "email"
        const val PLACE_ID = "place_id"
    }
}
