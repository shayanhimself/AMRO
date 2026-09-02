plugins {
    alias(libs.plugins.amro.android.library.screenshot)
}

android {
    namespace = "com.shayan.amro.core.ui"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    screenshotTestImplementation(projects.core.testing)

    // Provides the @PreviewTest annotation.
    screenshotTestImplementation(libs.screenshot.validation.api)
}
