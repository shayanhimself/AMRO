// The Android application module: the app plugin, Compose, and coverage reporting. Apply to the
// single module that produces the APK.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("amro.kover")
}

android {
    compileSdk = COMPILE_SDK
    defaultConfig {
        minSdk = MIN_SDK
        targetSdk = TARGET_SDK
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JDK_VERSION)
        targetCompatibility = JavaVersion.toVersion(JDK_VERSION)
    }
    // Goes with the Compose compiler plugin above. A module needs both halves.
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(JDK_VERSION)
}
