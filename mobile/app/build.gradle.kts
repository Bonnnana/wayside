import java.util.Properties

/**
 * The Maps SDK key ships inside the APK — it has to, the SDK reads it from the manifest. That
 * is safe only because the key is restricted to this package plus the signing certificate's
 * SHA-1 in the Google Cloud console. Restrict it there before using a real key.
 *
 * The file is git-ignored, and a missing key leaves the placeholder empty: the app still
 * builds and runs, the map just renders blank.
 */
val mapsApiKey: String = rootProject.file("secrets.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    ?.getProperty("MAPS_API_KEY")
    .orEmpty()

/**
 * Where a debug build looks for the API.
 *
 * `10.0.2.2` is the host machine as seen from the Android emulator, and is the default because
 * that is where most builds run. A physical phone can't use it — put your machine's address on
 * the shared network into `local.properties` instead:
 *
 *     wayside.apiHost=192.168.1.20
 *
 * That file is git-ignored, so the address stays on the machine it belongs to. Find it with
 * `ipconfig getifaddr en0` on macOS, and start the API with
 * `dotnet run --urls http://0.0.0.0:5263` so it listens on more than loopback.
 */
val debugApiHost: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    ?.getProperty("wayside.apiHost")
    ?.takeIf { it.isNotBlank() }
    ?: "10.0.2.2"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.wayside"
    // Stable SDK. Compiling against 37.1 stamps the APK with a preview codename, and a device
    // on a stable platform then refuses to resolve the launcher activity — the app installs but
    // "Activity class does not exist". Keep this in step with the emulator's system image.
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.wayside"
        // 29 is the floor the infrastructure library declares.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_PATH", "\"api\"")

        // Read by the Maps SDK from the merged manifest.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        debug {
            // Defaults to the emulator's alias for the host machine; override in
            // local.properties for a physical device. Debug builds permit cleartext — see
            // src/debug/res/xml/network_security_config.xml.
            buildConfigField("String", "API_HOST", "\"$debugApiHost\"")
            buildConfigField("String", "API_SCHEME", "\"http\"")
            buildConfigField("int", "API_PORT", "5263")
        }
        release {
            buildConfigField("String", "API_HOST", "\"api.wayside.app\"")
            buildConfigField("String", "API_SCHEME", "\"https\"")
            buildConfigField("int", "API_PORT", "0")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Infrastructure library — navigation, ApiClient, BaseViewModel, storage, alerts.
    implementation(project(":InfrastructureLibrary"))

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    implementation(libs.kotlinx.serialization.json)

    // Room — saved places survive the app closing.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Google Maps, through the Compose wrapper.
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)

    // Place photos.
    implementation(libs.coil.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}