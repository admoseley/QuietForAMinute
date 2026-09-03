plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.admoseley.quietforaminute"
    // Google Play requires targetSdk 36 for new uploads since 31 Aug 2026 (35 since Aug 2025).
    // API 37 is published but has not been validated against this app's foreground-service and
    // overlay behaviour yet; bump both values together when that is done.
    // Compose BOM 2026.08.00 (Compose UI 1.12.0, core-ktx 1.19.0, Navigation 2.10.0, Hilt
    // Navigation Compose 1.4.0, ...) declares a min compileSdk of 37 in its AAR metadata — building
    // against 36 fails at checkDebugAarMetadata even though targetSdk 36 satisfies Play's own
    // minimum. compileSdk and targetSdk are independent knobs; bump only compileSdk here unless
    // there's a reason to target 37's behavioural changes too.
    compileSdk = 37
    // The installed SDK only has the android-37.2 minor platform, not a bare "android-37"; AGP's
    // compileSdk DSL needs the minor called out explicitly in that case.
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.admoseley.quietforaminute"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.coroutines.android)
}
