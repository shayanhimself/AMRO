// Runs unit tests on the newer test JDK so Robolectric can emulate the compiled SDK.
plugins {
    id("amro.kover")
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions
            .getByType<JavaToolchainService>()
            .launcherFor {
                languageVersion.set(JavaLanguageVersion.of(TEST_JDK_VERSION))
            },
    )
}
