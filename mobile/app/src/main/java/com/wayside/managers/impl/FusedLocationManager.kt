package com.wayside.managers.impl

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wayside.managers.interfaces.LocationManager
import com.wayside.models.ui.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import android.location.LocationManager as PlatformLocationManager

/**
 * Where the driver is, from whichever provider will answer.
 *
 * Fused location is the good one — it blends GPS, wifi and cell, and costs the least battery.
 * But it lives inside Google Play services, which on some devices refuses the app with
 * `DEVELOPER_ERROR` and simply never calls back. So the platform providers are asked in
 * parallel: they are cruder and hungrier, but they are part of Android itself and answer even
 * when Play services won't. Whichever speaks first wins, and later fixes overwrite earlier ones.
 */
@Singleton
class FusedLocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationManager {

    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val platform =
        context.getSystemService(Context.LOCATION_SERVICE) as PlatformLocationManager

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation = _currentLocation.asStateFlow()

    override val hasPermission: Boolean
        get() = PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private var listening = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { publish(it, "fused") }
        }
    }

    private val platformListener = LocationListener { location -> publish(location, "platform") }

    // Every path below is guarded by hasPermission, which the linter can't see through.
    @SuppressLint("MissingPermission")
    override fun start() {
        if (listening) return

        if (!hasPermission) {
            Log.i(TAG, "No location permission; not starting updates.")
            return
        }

        listening = true
        seedFromLastKnown()
        startFused()
        startPlatform()
    }

    override fun stop() {
        if (!listening) return
        runCatching { fused.removeLocationUpdates(fusedCallback) }
        runCatching { platform.removeUpdates(platformListener) }
        listening = false
    }

    override suspend fun awaitLocation(): GeoPoint? {
        if (!hasPermission) return null

        _currentLocation.value?.let { return it }
        start()

        // A first fix can take a few seconds outdoors and never arrive indoors, so don't
        // leave the caller waiting on it forever.
        return withTimeoutOrNull(FIX_TIMEOUT_MS) { currentLocation.first { it != null } }
    }

    @SuppressLint("MissingPermission")
    private fun seedFromLastKnown() {
        // Usually seconds old, and available immediately — something on the map now beats the
        // right thing on the map in two minutes.
        fused.lastLocation
            .addOnSuccessListener { location -> location?.let { publish(it, "fused-last") } }
            .addOnFailureListener { error -> Log.w(TAG, "Fused last location failed: ${error.message}") }

        PLATFORM_PROVIDERS
            .filter { runCatching { platform.isProviderEnabled(it) }.getOrDefault(false) }
            .forEach { provider ->
                runCatching { platform.getLastKnownLocation(provider) }
                    .getOrNull()
                    ?.let { publish(it, "$provider-last") }
            }
    }

    @SuppressLint("MissingPermission")
    private fun startFused() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVAL_MS)
            // Driving, so a fix every few seconds is plenty and costs far less battery than
            // the high-accuracy provider.
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .build()

        runCatching { fused.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper()) }
            .onSuccess { Log.i(TAG, "Listening via fused provider.") }
            .onFailure { Log.w(TAG, "Fused updates unavailable: ${it.message}") }
    }

    @SuppressLint("MissingPermission")
    private fun startPlatform() {
        val enabled = PLATFORM_PROVIDERS.filter {
            runCatching { platform.isProviderEnabled(it) }.getOrDefault(false)
        }

        if (enabled.isEmpty()) {
            Log.w(TAG, "No platform provider enabled — is Location switched on?")
            return
        }

        enabled.forEach { provider ->
            runCatching {
                platform.requestLocationUpdates(
                    provider,
                    INTERVAL_MS,
                    MIN_DISTANCE_M,
                    platformListener,
                    Looper.getMainLooper(),
                )
            }
                .onSuccess { Log.i(TAG, "Listening via $provider.") }
                .onFailure { Log.w(TAG, "$provider unavailable: ${it.message}") }
        }
    }

    private fun publish(location: Location, source: String) {
        Log.i(TAG, "Fix from $source: ${location.latitude}, ${location.longitude}")
        _currentLocation.value = GeoPoint(location.latitude, location.longitude)
    }

    private companion object {
        const val TAG = "WaysideLocation"

        val PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        val PLATFORM_PROVIDERS = listOf(
            PlatformLocationManager.GPS_PROVIDER,
            PlatformLocationManager.NETWORK_PROVIDER,
        )

        const val INTERVAL_MS = 5_000L
        const val MIN_INTERVAL_MS = 2_000L
        const val MIN_DISTANCE_M = 10f
        const val FIX_TIMEOUT_MS = 8_000L
    }
}
