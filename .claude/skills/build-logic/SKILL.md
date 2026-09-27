---
name: build-logic
description: This project's Gradle convention plugins and what each module build file may declare. Use when creating or editing any build.gradle.kts, adding a module, applying a Gradle plugin, or changing SDK and JVM levels.
metadata:
  keywords:
  - gradle
  - build.gradle.kts
  - build-logic
  - convention plugin
  - version catalog
  - libs.versions.toml
  - compileSdk
  - minSdk
  - jvmToolchain
  - new module
---

Shared Android and Kotlin configuration lives in convention plugins under `build-logic/`,
an included build. A module script declares only what is unique to that module.

## The plugins

Applied by catalog alias, for example `alias(libs.plugins.amro.android.library)`.

| Plugin | What it configures | Apply to |
|---|---|---|
| `amro.android.application` | Application plugin, SDK and JVM levels, Compose, coverage | the app module |
| `amro.android.library` | Library plugin, SDK and JVM levels, coverage | a library with no Compose UI |
| `amro.android.library.screenshot` | the above, plus Compose, screenshot tests and the Robolectric runtime | a Compose library whose previews are screenshot-tested |
| `amro.kover` | coverage reporting and its exclusions | already inside the application and library plugins |
| `amro.robolectric` | the JVM that `Test` tasks run on | already inside the screenshot plugin; apply alone for a module that runs Robolectric without it |

There is no multiplatform plugin. Every module is an Android or JVM module.

## The modules

| Module | Plugin | Also declares |
|---|---|---|
| `:app` | `amro.android.application` | serialization, KSP, Hilt |
| `:core:model` | `amro.android.library` | nothing. Pure Kotlin types |
| `:core:network` | `amro.android.library` | serialization, Ktor, KSP, Hilt |
| `:core:database` | `amro.android.library` | KSP, `androidx.room`, `room { schemaDirectory(...) }` |
| `:core:data` | `amro.android.library` | KSP, Hilt |
| `:core:ui` | `amro.android.library.screenshot` | |
| `:core:testing` | `amro.android.library` | |
| `:feature:trending` | `amro.android.library.screenshot` | KSP, Hilt |
| `:feature:detail` | `amro.android.library.screenshot` | KSP, Hilt |

The dependency rules in `docs/architecture.md` are hard. A build file declaring a
dependency they forbid is wrong even when it compiles.

## A new module

1. Add it to `include(...)` in `settings.gradle.kts`.
2. Apply exactly one plugin from the first table.
3. Set `namespace` under `com.shayan.amro`, then declare dependencies. Nothing else
   belongs there.

## Rules

- **A module never sets `compileSdk`, `minSdk`, `targetSdk`, `compileOptions`,
  `jvmToolchain`, or `compose = true`.** The convention plugin owns them. A module keeps
  `namespace`, its own `buildFeatures`, `dependencies`, and per-module plugins such as KSP,
  Hilt, serialization or Room.
- **SDK and JVM levels are `Int` constants in
  `build-logic/convention/src/main/kotlin/AndroidVersions.kt`.** Edit there and every module
  follows. They are deliberately not catalog entries, whose values are always strings.
- **Dependency and third-party plugin versions stay in `gradle/libs.versions.toml`.** The
  convention plugins are catalog entries carrying an id and no version, since the build
  compiles them from source.
- **Inside a convention plugin, plugin ids are literal strings.** Gradle extracts that
  `plugins` block and compiles it before any project exists, so `libs` and `alias()` cannot
  resolve. Only module scripts get the catalog.
- **A convention plugin that applies a new third-party plugin also needs that plugin's
  artifact in `build-logic/convention/build.gradle.kts`.** `compileOnly` covers a plugin
  named in a `plugins` block; one applied by id from the script body has to be
  `implementation` to reach the consuming build at execution time.
- **The TMDB read access token reaches the app as a `buildConfigField` in `:app`,** read
  from `local.properties` and defaulting to an empty string. A clone without a token builds
  and runs. No other module reads `local.properties`.

## Verify

Configuration and convention-plugin compilation errors surface without a full build:

```
./gradlew help
```
