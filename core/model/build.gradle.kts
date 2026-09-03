plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.model"
}

dependencies {
    api(libs.kotlinx.datetime)
}
