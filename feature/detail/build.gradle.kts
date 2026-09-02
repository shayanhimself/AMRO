plugins {
    alias(libs.plugins.amro.android.library.screenshot)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shayan.amro.feature.detail"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.data)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
