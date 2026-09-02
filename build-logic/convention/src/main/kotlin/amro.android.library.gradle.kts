// A plain Android library module.
plugins {
    id("com.android.library")
    id("amro.kover")
}

android {
    compileSdk = COMPILE_SDK
    defaultConfig {
        minSdk = MIN_SDK
    }
    compileOptions {
        targetCompatibility = JavaVersion.toVersion(JDK_VERSION)
    }
}
