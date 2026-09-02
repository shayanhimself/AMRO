import java.util.Properties

plugins {
    alias(libs.plugins.amro.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The TMDB v4 read access token is a build input: it is read from local.properties
// and defaults to empty, so a clone without one still builds and runs.
val tmdbReadAccessToken: String =
    Properties()
        .apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) {
                file.inputStream().use { load(it) }
            }
        }.getProperty("tmdb.readAccessToken", "")

android {
    namespace = "com.shayan.amro"
    defaultConfig {
        applicationId = "com.shayan.amro"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.shayan.amro.HiltTestRunner"
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"$tmdbReadAccessToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

// The Accessibility Test Framework drags in an old Material Components library that downgrades
// the app's own dependencies. Nothing on screen uses it, so it is dropped from the device test
// classpath.
configurations.androidTestImplementation {
    exclude(group = "com.google.android.material", module = "material")
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.feature.trending)
    implementation(projects.feature.detail)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.adaptive.navigation3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    kspAndroidTest(libs.hilt.compiler)

    androidTestImplementation(projects.core.network)
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(enforcedPlatform(libs.okhttp.bom))
    androidTestImplementation(libs.okhttp.mockwebserver)
}
