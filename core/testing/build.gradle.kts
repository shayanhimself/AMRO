plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.testing"
}

dependencies {
    // The model fixtures are on this module's surface.
    api(projects.core.model)

    api(libs.kotlinx.serialization.json)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.test.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}
