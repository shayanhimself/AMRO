# 002: Foundation ported from the Chatbot project

Source repository: `/Users/shayan/Development/Projects/Chatbot`. Referred to below as *source*.

The source is a working Android build with the same toolchain, the same test strategy, and the same
design system this project needs. This plan carries that foundation across so the data layer, the UI
layer and the tests are written on top of it rather than beside it.

The target module map and dependency rules are owned by `docs/architecture.md`. The technology
choices are owned by `docs/superpowers/specs/001-tech-stack.md`. Where this plan and those documents
disagree, they win.

## Two global rules

**Rename.** Every `chatbot` identifier becomes `amro`: convention plugin ids (`chatbot.android.library`
becomes `amro.android.library`), catalog plugin aliases, and the theme composable (`ChatbotTheme`
becomes `AmroTheme`).

The package root changes in both segments: `com.shayanaryan.chatbot` becomes **`com.shayan.amro`**.
That is the namespace of every module, the `applicationId` of `:app`, and the directory path each
source file sits under.

**The source is Kotlin Multiplatform, this project is not.** Anything reached for in a
`commonMain` source set moves to an ordinary Android or JVM module. The `chatbot.kmp.library`
convention plugin, the `kotlin-multiplatform` plugin, and the `android-kotlin-multiplatform-library`
plugin are all dropped.

Work the chunks in order. Each one leaves the build green.

---

## Chunk 1: Skills and agent config — done

The skills come first, because they are what the remaining chunks are carried out with.

Copy into `.claude/skills/` unchanged. These are general Android or vendor skills:

`android-cli`, `adaptive`, `navigation-3`, `edge-to-edge`, `styles`, `testing-setup`,
`android-intent-security`, `prose-review`.

Copy `pull-design` and edit two things in it. Both projects read the same design system, so the
design system project id is unchanged:

| Project | id | Note |
|---|---|---|
| Design system, canonical tokens and component contracts | `c5a6030b-52d3-4ecc-ab51-4460eebdc7df` | Named "Chatbot Design System" and shared by both apps. The name is not a mistake |
| AMRO designs, the screen mockups | `ce754ce1-efac-4206-9801-f8136b96493b` | Holds `Screen-TrendingList`, `Screen-MovieDetail`, `Component-TrendingFrame`, `Component-DetailFrame`, `Component-TrendingTwoPane`. A design project, so it does not appear in `list_projects`: address it by id |

- `references/projects.md`: replace the designs project id and its file list.
- `SKILL.md`: point the sync target at `docs/design-system.md`. The rest, the read-only rule, the
  `list_files` before `get_file` rule, the token file map and the badge convention, is unchanged.

The DS snapshot under `_ds/` in the designs project is not canonical. Token values come from the
design system project.

Rewrite for this project, keeping the shape and replacing the content:

| Skill | Written from | Verified at |
|---|---|---|
| `architecture` | `docs/architecture.md`, whose module map and dependency rules differ from the source's | Chunk 4 |
| `build-logic` | The convention plugins and module rules in Chunks 2 and 4 | Chunk 4 |
| `design-system` | The token accessors and catalog components as they land in `:core:ui`. Write the sync procedure now, and fill in the accessor names in Chunk 5 | Chunk 5 |
| `journeys` | The runner is unchanged; the acceptance criteria it maps to are this project's | Chunk 7 |

Copy `.claude/settings.json`. Its deny list already blocks `git add`, `git commit` and `git push`,
which is what `CLAUDE.md` requires.

**Done when** the skills resolve and `architecture` describes the modules in `docs/architecture.md`.

---

## Chunk 2: Build foundation — done

Copy unchanged:

- `gradlew`, `gradlew.bat`, `gradle/wrapper/`
- `gradle/gradle-daemon-jvm.properties`
- `gradle.properties`
- `.editorconfig`
- `build-logic/settings.gradle.kts`, `build-logic/convention/build.gradle.kts`,
  `build-logic/convention/.gitignore`
- `build-logic/convention/src/main/kotlin/ScreenshotTolerance.kt`

Copy and modify:

| File | Change |
|---|---|
| `settings.gradle.kts` | `rootProject.name = "amro"`. `include(…)` lists the modules in Chunk 4 |
| root `build.gradle.kts` | Drop the `kotlin.multiplatform` and `android.kotlin.multiplatform.library` plugin aliases. Spotless block unchanged |
| `AndroidVersions.kt` | `MIN_SDK = 26`. `COMPILE_SDK`, `TARGET_SDK`, `JDK_VERSION`, `TEST_JDK_VERSION` unchanged |
| `chatbot.android.application.gradle.kts` | Rename to `amro.android.application.gradle.kts`, rename the `chatbot.kover` id inside it |
| `chatbot.android.library.gradle.kts` | Rename |
| `chatbot.android.library.compose.gradle.kts` | Do not create. Every Compose module in this project is screenshot tested, so its setup folds into the screenshot plugin below |
| `chatbot.android.library.screenshot.gradle.kts` | Rename, including the ids it lists. Inline the Compose plugins and `buildFeatures { compose = true }` from `library.compose` |
| `chatbot.kover.gradle.kts` | Rename. Exclusion list unchanged |
| `chatbot.robolectric.gradle.kts` | Rename |

Do not copy `chatbot.kmp.library.gradle.kts`.

The convention plugin group is `com.shayan.amro.buildlogic`.

`.gitignore` currently holds four lines. Add the standard Android and Gradle entries: `build/`,
`.gradle/`, `.idea/`, `*.iml`, `.kotlin/`, `captures/`.

**Done when** the files above are in place, no convention plugin carries a `chatbot`
identifier, and no `kmp` plugin remains. The `./gradlew help` gate moves to Chunk 3, which
adds the version catalog these build files read.

---

## Chunk 3: Version catalog — done

Start from the source `gradle/libs.versions.toml`.

Keep as-is, including the comments that explain them: `kotlin`, `ksp`, `agp`, `composeBom`,
`coreKtx`, `activityCompose`, `lifecycle`, `navigation3`, `adaptiveNavigation3`, `androidxHilt`,
`kotlinxSerialization`, `kotlinxCoroutines`, `kotlinxDatetime`, `room`, `sqlite`, `hilt`, `ktor`,
`okhttp`, `junit`, `robolectric`, `androidxTestEspresso`, `androidxTestExtJunit`,
`androidxTestRunner`, `spotless`, `ktlint`, `screenshot`, `kover`.

The `okhttp` and `androidxTestEspresso` entries carry comments explaining why they are pinned ahead
of what resolves on its own. Both reasons still apply here. Carry the comments with the versions.

Remove: `tink`, `datastore`, `coreSplashscreen`, and the two multiplatform plugin aliases.

Add: Coil 3 with its Ktor network fetcher, `ktor-client-content-negotiation`,
`ktor-serialization-kotlinx-json`, `ktor-client-logging`. Resolve their current versions with the
`android` CLI rather than guessing.

Rename every `chatbot-*` convention plugin alias to `amro-*`, and drop `chatbot-kmp-library` and
`chatbot-android-library-compose` (Chunk 2 folds that plugin into the screenshot one).

**Done when** `./gradlew help` succeeds with the configuration cache enabled. This is the first
chunk whose build files can configure, since it adds the catalog they read. No catalog entry is
unreferenced by the end of Chunk 6.

---

## Chunk 4: Module skeleton — done

Create the modules `docs/architecture.md` names, each with a `build.gradle.kts` and a namespace
under `com.shayan.amro`, and nothing else in them yet:

| Module | Convention plugin | Notes |
|---|---|---|
| `:app` | `amro.android.application` | Plus serialization, ksp, hilt. Model the build file on the source `app/build.gradle.kts` |
| `:core:model` | `amro.android.library` | Pure Kotlin types. No Compose, no Android imports in sources |
| `:core:network` | `amro.android.library` | Ktor, serialization |
| `:core:database` | `amro.android.library` | Room, ksp, the `androidx.room` plugin, `room { schemaDirectory(…) }` |
| `:core:data` | `amro.android.library` | Depends on `:core:network`, `:core:database`, `:core:model` |
| `:core:ui` | `amro.android.library.screenshot` | Chunk 5 |
| `:core:testing` | `amro.android.library` | Chunk 5 |
| `:feature:trending` | `amro.android.library.screenshot` | Empty for now |
| `:feature:detail` | `amro.android.library.screenshot` | Empty for now |

The dependency rules in `docs/architecture.md` are hard. A build file that declares a dependency
those rules forbid is wrong even when it compiles.

`:app` reads the TMDB v4 read access token from `local.properties` into a `buildConfigField`,
defaulting to an empty string. A clone without a token builds and runs. This is not in the source
and is written fresh.

**Done when** `./gradlew assembleDebug` succeeds.

---

## Chunk 5: Design system and test helpers — done

`:core:testing`. Copy from the source `core/testing/`:

- `TestResources.kt` (`string`, `quantityString`), unchanged beyond the package rename
- `preview/MultiPreviews.kt` (`FormFactorPreviews`, `FontScalePreviews`, `ThemePreviews`), unchanged
- the `build.gradle.kts`, with its dependencies exposed as `api`

Fakes and fixtures land here later, as the data layer is written.

`:core:ui`. Copy from the source `core/ui/`, renaming the package and the theme:

- `theme/`: `Theme.kt`, `Color.kt`, `ExtendedColors.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`,
  `Elevation.kt`, `Motion.kt`
- `component/`: `DsButton`, `DsCard`, `DsChip`, `DsBadge`, `DsTextField`, `DsSwitch`, `DsDialog`,
  `DsSnackbar`, `DsIconButton`
- `icon/`: `DsIcon.kt`, `Glyphs.kt`, and `res/font/material_symbols_rounded.ttf`
- `modifier/BottomBarSafeDrawingPadding.kt`
- `viewmodel/StateProduction.kt`
- `src/test/`: the theme, color scheme, typography, design token and component accessibility tests
- `src/test/resources/robolectric.properties`
- `src/screenshotTest/`: the preview galleries, with two changes to the source's shape. The source
  declares a dark and a light preview function per gallery; here one function carries
  `@ThemePreviews` from `:core:testing` instead, which is what puts that module on the
  `screenshotTest` classpath. And the source spells each gallery's content out twice, once in the
  component's colocated `@Preview` and again in the screenshot gallery; here the states live once,
  as `internal` composables in `designsystem/preview/DsGalleries.kt` under `src/main/`, which both
  sides render. This is the shape the source's feature modules use for a screen's preview data, and
  it holds for a component catalog too

Both projects render against the same design system, so the values come across unchanged: palette,
type scale, spacing, elevation, motion and shapes are copied as they are, not re-derived. The rename
is the only edit, with one exception: drop the tokens that exist only for the source's chat
surfaces and that nothing here consumes: `Glyphs.BRAND`, `MonoTextStyle` (and `DsTextField`'s
`mono` parameter), `ComponentShapes.bubbleUser` / `bubbleAssistant`, and `Motion.caretBlinkMillis`,
along with their test assertions and preview uses.

Also copy the source `specs/002-design-system.md` to `docs/design-system.md`, beside
`architecture.md` and `testing.md`. It is the translation of that design system into Compose terms
and holds here too. Rename throughout, and cut what describes screens this app does not have.

Do not copy the source's goldens. A golden's path carries the package that declared its preview, so
the rename orphans every one of them. Record fresh with `./gradlew :core:ui:updateDebugScreenshotTest`.

Finish the `design-system` skill against what landed here.

**Done when** `./gradlew :core:ui:check :core:testing:assembleDebug` succeeds, running the
`:core:ui` unit tests and validating the recorded goldens. The full `./scripts/test.sh` gate waits
for Chunk 7, which brings that script across.

---

## Chunk 6: App shell — done

Copy from the source `app/src/main/`, renaming throughout:

| Source file | Change |
|---|---|
| `ChatbotApplication.kt` | Rename. `@HiltAndroidApp` and nothing else |
| `MainActivity.kt` | Keep the edge-to-edge setup. Drop the splash screen: the app opens on the trending list, so there is no destination decision to hold one for |
| `navigation/NavKeys.kt` | Keys become `TrendingKey` and `MovieDetailKey(id)` |
| `navigation/ChatbotNavigator.kt` | Keep the shape: one class owning every back stack mutation. The operations become `openMovie` and `back`. Drop the deep link and the gate transition |
| `navigation/ChatbotNavDisplay.kt` | Keep the `NavDisplay` host and the adaptive two-pane scene. Panes split at expanded width only |
| `navigation/ChatbotApp.kt` | Rename |
| `di/TimeModule.kt` | Unchanged. It provides the injected `Clock` the freshness rule is asserted against |
| `res/`, `AndroidManifest.xml` | Take the manifest and the launcher icon shape. Copy is written fresh |

Do not copy `MainUiState.kt`, `MainViewModel.kt`, `di/ApiKeyModule.kt` or `di/ClaudeModule.kt`. They
answer a first-launch gate this app does not have. `di/CoroutinesModule.kt` and
`di/ApplicationScope.kt` do not come across either: every write here is request and response and
safely re-issuable, so nothing needs to outlive the screen that started it.

`di/DataModule.kt` waits for the data layer. There are no repositories to register until
`:core:data` holds some.

`:core:network` declares `TmdbConfig`, the address and credential a caller supplies, and `:app`
binds it. A distinct type needs no qualifier, and it carries the token alongside the address, which
`:app` already reads from `local.properties`. A device test replaces that binding with
`server.url("/")`, which is what removes the source's proxy and certificate machinery.

`:feature:trending` and `:feature:detail` each get their route composable, an empty surface the
graph can resolve. The screens themselves are written later.

**Done when** the app launches to an empty trending screen and `./gradlew assembleDebug` succeeds.

---

## Chunk 7: Test harness, scripts and CI — done

Copy from the source `app/src/androidTest/`:

- `HiltTestRunner.kt`
- `di/DependencyGraphTest.kt`, which turns an unresolvable graph into a test failure
- `flow/AppLauncher.kt`, `flow/FlowWaits.kt`
- `flow/A11yFlowTest.kt` as the model for the accessibility checks, retargeted at the two screens

Do not copy `wire/LocalAnthropic.kt`, `wire/LocalAnthropicTest.kt`,
`app/src/androidTest/assets/test_ca_with_key.pem`, or `app/src/debug/res/raw/test_ca.pem`. The source
hardcodes its base URL, so its tests intercept at the device's global proxy and impersonate the API
over TLS. Here the base URL is an injected value, so a device test binds it to `server.url("/")` and
MockWebServer answers over plain HTTP on loopback. Take the enqueue-per-scenario shape of
`LocalAnthropic` (a status code, a socket dropped mid-response) and leave the transport behind.

Network security config:

- `app/src/release/res/xml/network_security_config.xml`: copy unchanged
- `app/src/debug/res/xml/network_security_config.xml`: replace the source's trust anchor override
  with a `domain-config` permitting cleartext to `127.0.0.1` only. MockWebServer serves plain HTTP
  and the platform blocks that by default. No CA, no PEM, no raw resource

Scripts, copied from the source `scripts/`:

| Script | Change |
|---|---|
| `format.sh`, `coverage.sh`, `screenshotTest.sh`, `screenshotUpdate.sh` | Unchanged |
| `test.sh` | Lands as `unittest.sh`. Keep `./gradlew check`. Rewrite the header comment: the multiplatform `allTests` caveat does not apply |
| `instrumented.sh` | Keep the filter and module routing. Drop the proxy setup, the port constant and the cleanup trap that resets it. Module list becomes `:app` alone until another module carries device tests |

Do not copy `record-sse-fixture.sh` or `journeys.py`. The E2E runner waits on how this project
evaluates a journey, which is undecided.

`.github/workflows/ci.yml`: copy, then replace the `ANTHROPIC_API_KEY` secret with the TMDB read
access token, and rename the step comment that explains it. The KVM setup, the emulator level and
the artifact uploads are unchanged.

**Done when** `./scripts/unittest.sh` and `./scripts/instrumented.sh` both pass.

---

## Not carried across

| Left behind | Because |
|---|---|
| The `shared` multiplatform module and its source set layout | This project is Android only |
| Tink, the Keystore cipher, the API key store and its onboarding gate | The TMDB token is a build config value, not a user secret |
| DataStore | Filter and sort reset on cold start, so nothing survives one |
| `core-splashscreen` | The app opens on the trending list |
| The application-scoped `CoroutineScope` | Every write is re-issuable, so none needs to outlive its caller |
| Server-sent events, the streaming parser, the SSE fixtures | TMDB is request and response |
| The Chatbot feature modules and `docs/images/` | Another product |

## What the source does not answer

Written fresh, not ported: the Coil 3 setup on the Ktor engine, the `buildConfigField` that reads
the token from `local.properties`, the Room many-to-many schema with the genre alias table, and the
paged fetch with deduplication.
