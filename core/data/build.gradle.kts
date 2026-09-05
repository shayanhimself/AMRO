plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shayan.amro.core.data"
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.network)
    implementation(projects.core.database)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
