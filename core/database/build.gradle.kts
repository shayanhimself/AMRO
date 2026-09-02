plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.shayan.amro.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room.compiler)
}
