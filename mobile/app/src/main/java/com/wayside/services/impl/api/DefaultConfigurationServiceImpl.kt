package com.wayside.services.impl.api

import com.wayside.BuildConfig
import com.wayside.services.interfaces.api.ConfigurationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultConfigurationServiceImpl @Inject constructor() : ConfigurationService {
    override val apiHost: String = BuildConfig.API_HOST
    override val apiBasePath: String = BuildConfig.API_BASE_PATH
    override val apiScheme: String = BuildConfig.API_SCHEME
    override val apiPort: Int? = BuildConfig.API_PORT.takeIf { it > 0 }
}
