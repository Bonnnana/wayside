package com.wayside.di

import com.wayside.managers.impl.FusedLocationManager
import com.wayside.managers.interfaces.LocationManager
import com.wayside.navigation.NavigationConfigImpl
import com.wayside.services.impl.api.AuthenticationServiceImpl
import com.wayside.services.impl.api.DefaultConfigurationServiceImpl
import com.wayside.services.impl.api.DefaultPhotoUrlBuilder
import com.wayside.services.impl.api.PlacesServiceImpl
import com.wayside.services.interfaces.api.AuthenticationService
import com.wayside.services.interfaces.api.ConfigurationService
import com.wayside.services.interfaces.api.PhotoUrlBuilder
import com.wayside.services.interfaces.api.PlacesService
import cc.infrastructure.android.library.navigation.interfaces.NavigationConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level bindings. The infrastructure library supplies `ApiClient`, `NavigationManager`,
 * storage and logging through its own `ServiceModule` — only what this app defines goes here.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class AppServiceModule {

    @Binds
    @Singleton
    abstract fun bindNavigationConfig(impl: NavigationConfigImpl): NavigationConfig

    @Binds
    @Singleton
    abstract fun bindConfigurationService(
        impl: DefaultConfigurationServiceImpl,
    ): ConfigurationService

    @Binds
    @Singleton
    abstract fun bindAuthenticationService(
        impl: AuthenticationServiceImpl,
    ): AuthenticationService

    @Binds
    @Singleton
    abstract fun bindPlacesService(impl: PlacesServiceImpl): PlacesService

    @Binds
    @Singleton
    abstract fun bindLocationManager(impl: FusedLocationManager): LocationManager

    @Binds
    @Singleton
    abstract fun bindPhotoUrlBuilder(impl: DefaultPhotoUrlBuilder): PhotoUrlBuilder
}
