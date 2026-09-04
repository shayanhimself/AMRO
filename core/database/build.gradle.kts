plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.amro.robolectric)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shayan.amro.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Both are on this module's contract: `Movie` and `Flow`.
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
}
