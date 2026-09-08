# Adding a module

## build-logic

Every module's Android and Kotlin setup lives in one place: `build-logic`. A module applies one instead of repeating a compileSdk,
a minSdk, a JDK level and a Compose flag.

So a module's own build script says what that module is and what it depends on.


## The plugins we own

| Plugin | Does | Apply to                                                                                                                                                                                                                                                                                           |
|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `amro.android.application` | The Android application plugin, Compose, the SDK and JDK levels, coverage | `:app`, and nothing else. It is the only module producing an APK                                                                                                                                         |
| `amro.android.library` | The Android library plugin, the SDK and JDK levels, coverage | Every library module with no composables in it (ex: `:core:model`, `:core:network`, `:core:data`, `:core:testing`)                                                                                                                  |
| `amro.android.library.screenshot` | The above plus Compose, Robolectric, the `screenshotTest` source set, and the pixel tolerance. It also makes `check` validate the goldens, which the screenshot plugin alone does not | Every module holding composables (`:core:ui` and each feature)                             |
| `amro.robolectric` | Runs the unit tests on the newer test JDK, which Robolectric needs to emulate the compiled SDK | A library with no composables whose JVM tests reach the Android framework (ex: `:core:database`). Already inside the screenshot plugin, so a Compose module never applies it                   |
| `amro.api.tokens` | One `BuildConfig` field per API credential, read from `local.properties` first and the environment second | The module that reads a source's credential, which is the one holding that source (`:core:network` alone)                                                                                  |
| `amro.kover` | The coverage report's shared exclusion filters | Nothing directly. The three module plugins above already apply it                                                                                                                                                                                |


Three files hold the values those plugins read:

| File | Holds                                                                                                        |
|---|--------------------------------------------------------------------------------------------------------------|
| `AndroidVersions.kt` | `COMPILE_SDK`, `MIN_SDK`, `TARGET_SDK`, `JDK_VERSION`, and the newer `TEST_JDK_VERSION` Robolectric needs    |
| `ApiTokens.kt` | Every API source's credential: the `BuildConfig` field, the `local.properties` key, the environment variable |
| `ScreenshotTolerance.kt` | The share of pixels a screenshot golden may differ by                                                        |

## Adding a new module

1. Add the path to `settings.gradle.kts`.
2. **Write its build script:** apply exactly one of the first three plugins above, which defines what
   kind of module this is, then apply other plugins if needed. (And of course the namespace and the dependencies):

   ```kotlin
   plugins {
       alias(libs.plugins.amro.android.library.screenshot)
       alias(libs.plugins.ksp)
       alias(libs.plugins.hilt)
   }

   android {
       namespace = "com.shayan.amro.feature.example"
   }

   dependencies {
       implementation(projects.core.ui)
       implementation(projects.core.data)
   }
   ```

   Dependencies use the type-safe `projects.` accessors.

3. **Check the dependency rules** in [architecture.md](architecture.md) before depending on anything.
   A feature depends on `:core:ui` and `:core:data` and never on another feature; `:core:testing` is
   never a production dependency.
4. Create the source root
