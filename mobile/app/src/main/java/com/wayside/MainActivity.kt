package com.wayside

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wayside.managers.AppSessionManager
import com.wayside.repositories.AppStateRepository
import com.wayside.ui.components.LocationRequiredDialog
import com.wayside.ui.theme.WaysideTheme
import cc.infrastructure.android.library.navigation.interfaces.NavigationConfig
import cc.infrastructure.android.library.navigation.interfaces.NavigationManager
import cc.infrastructure.android.library.navigation.ui.AppNavigationHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigationManager: NavigationManager

    @Inject lateinit var navigationConfig: NavigationConfig

    @Inject lateinit var appState: AppStateRepository

    @Inject lateinit var sessionManager: AppSessionManager

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    /**
     * `true` while the system will still show its own rationale/prompt if asked again — `false`
     * once it has given up on us ("don't ask again", or a denial with no rationale owed), which
     * is the only case Android gives no way back into except the app's own settings page.
     */
    private var canRequestLocationAgain by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Asked once for the whole app rather than per screen: the driver's location is on
        // every map, and a permission dialog that appears on the third screen reads as a bug.
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { granted ->
            val isGranted = granted.values.any { it }
            appState.onLocationPermissionChanged(isGranted)
            if (!isGranted) {
                canRequestLocationAgain = LOCATION_PERMISSIONS.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            }
        }

        requestLocationPermission()

        setContent {
            // Collected here so the theme picker in Profile repaints the whole app.
            val state by appState.state.collectAsState()

            WaysideTheme(themeMode = state.themeMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(Modifier.fillMaxSize()) {
                        // AppNavigationHost registers the screens and then navigates to
                        // determineInitialRoute() itself — setting the home page here would run
                        // before registration and throw "Screen welcome not registered".
                        AppNavigationHost(
                            systemPadding = padding,
                            navigationManager = navigationManager,
                            navigationConfig = navigationConfig,
                        )

                        // The driver's location is on every map behind this point, so the app
                        // genuinely can't do anything useful without it — same pattern as the
                        // Bluetooth gate in Zinga: block, don't just nag. Gated on this device's
                        // onboarding being done rather than on being signed in: the onboarding
                        // intro's own permission page only ever asks softly ("Allow location" /
                        // "Not now" lead to the same place), so once that's behind us — whether
                        // or not the driver has signed in yet — a missing permission is a real
                        // problem, not a first-run nicety.
                        if (sessionManager.hasCompletedOnboarding && !state.hasLocationPermission) {
                            LocationRequiredDialog(
                                canRequestPermission = canRequestLocationAgain,
                                onRequestPermission = ::requestLocationPermission,
                                onOpenSettings = ::openAppSettings,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    /**
     * Catches the one path the launcher's own callback can't: coming back from the app's
     * settings page after granting the permission there instead of through our own prompt.
     */
    override fun onResume() {
        super.onResume()
        val granted = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        appState.onLocationPermissionChanged(granted)
        if (granted) canRequestLocationAgain = true
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
