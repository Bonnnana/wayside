package com.wayside.services.impl.api

import com.wayside.AppConstants
import com.wayside.services.interfaces.api.ConfigurationService
import com.wayside.services.interfaces.api.PhotoUrlBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPhotoUrlBuilder @Inject constructor(
    private val configurationService: ConfigurationService,
) : PhotoUrlBuilder {

    override fun photoUrl(placeId: String, index: Int): String {
        val port = configurationService.apiPort?.let { ":$it" }.orEmpty()
        return buildString {
            append(configurationService.apiScheme)
            append("://")
            append(configurationService.apiHost)
            append(port)
            append("/")
            append(configurationService.apiBasePath)
            append("/")
            append(AppConstants.ApiEndpoints.PLACES)
            append("/")
            append(placeId)
            append("/photos/")
            append(index)
        }
    }
}
