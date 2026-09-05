plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.testing"
}

dependencies {
    api(projects.core.model)
    api(projects.core.data)
    api(projects.core.database)
    api(projects.core.network)
    api(libs.kotlinx.coroutines.core)

    api(libs.kotlinx.serialization.json)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.test.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
