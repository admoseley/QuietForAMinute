import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

// Release signing credentials live outside the repo (see .gitignore) in keystore.properties.
// Loaded lazily so debug builds and any environment without a release key still configure fine
// (a fresh clone, CI). Store and key password MUST match: keytool defaults to the PKCS12
// keystore type since JDK 8u, and PKCS12 requires storePassword == keyPassword — a mismatched
// pair fails signing with a cryptic "Given final block not properly padded" error rather than a
// clear one, which is what happened the first time this keystore was generated.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
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
        // Calendar versioning (CalVer) from 2026.09.03 onward — see CHANGELOG.md for the
        // switchover from semver, and CLAUDE.md for the full rule.
        //
        //   versionName = "YYYY.MM.DD", dated to when the work was completed. A second release on
        //                 the same day appends a sequence: "2026.09.03.1", "2026.09.03.2".
        //   versionCode = YYYYMMDD * 10 + N, where N is that day's 0-based sequence.
        //
        // versionCode is derived rather than a literal timestamp because it is a signed 32-bit int
        // that Google Play caps at 2,100,000,000. A full timestamp (202609031415) is ~100x over
        // that ceiling and even YYMMDDhhmm (2609031415) overflows it. This form stays around
        // 2.0e8, strictly increases forever, and still reads as the date.
        versionCode = 202609030
        versionName = "2026.09.03"
    }

    buildFeatures {
        compose = true
        // Generates BuildConfig so the Settings screen can read VERSION_NAME rather than
        // hardcoding a version string that would silently drift from defaultConfig above.
        buildConfig = true
    }

    signingConfigs {
        // Only registered when keystore.properties is present, so a checkout without release
        // credentials (CI, a fresh clone) can still run `assembleDebug` / `bundleDebug` cleanly.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
}
