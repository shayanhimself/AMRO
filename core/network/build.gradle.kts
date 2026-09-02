plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shayan.amro.core.network"
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
}
