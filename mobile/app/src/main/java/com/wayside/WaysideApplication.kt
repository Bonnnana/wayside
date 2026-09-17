package com.wayside

import android.app.Application
import cc.infrastructure.android.library.LibInitializer
import cc.infrastructure.android.library.config.InfrastructureConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WaysideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before anything touches the library: secure storage, the cache and the
        // API client all read LibInitializer.appContext, and it's a lateinit.
        LibInitializer.init(this)
        // Request/response logging follows the build type, so release builds stay quiet.
        InfrastructureConfig.enableDebugLogging = BuildConfig.DEBUG
    }
}
