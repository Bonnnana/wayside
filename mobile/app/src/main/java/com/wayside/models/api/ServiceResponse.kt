package com.wayside.models.api

/**
 * What every service method returns. Wraps success/failure so ViewModels branch on one shape
 * instead of catching exceptions or reading HTTP codes.
 *
 * @property userMessage Text safe to show the user. Blank when the failure has no message worth
 *   surfacing — treat that as "something went wrong" rather than printing an empty toast.
 */
class ServiceResponse<T> private constructor(
    val isSuccessful: Boolean,
    private val data: T? = null,
    val userMessage: String = "",
    val statusCode: Int? = null,
) {
    val assertedData: T get() = data!!

    companion object {
        fun <T> fromData(data: T): ServiceResponse<T> =
            ServiceResponse(isSuccessful = true, data = data)

        fun <T> success(): ServiceResponse<T> = ServiceResponse(isSuccessful = true)

        fun <T> fail(userMessage: String = "", statusCode: Int? = null): ServiceResponse<T> =
            ServiceResponse(isSuccessful = false, userMessage = userMessage, statusCode = statusCode)
    }
}
