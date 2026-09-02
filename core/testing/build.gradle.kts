plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.test.core)
}
