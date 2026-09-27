# 001: Tech Stack

This spec is the canonical record of the project's technology choices.
Library versions are not recorded here; the version catalog owns them.

## Platform & build

| Decision | Choice | Rationale |
|---|---|---|
| Language | **Kotlin 2.4.x** | Current stable line. Sets the AGP and KSP floors the catalog has to satisfy |
| Build | **AGP 9**, **Gradle 9**, version catalog, `build-logic` convention plugins for shared module configuration | Every module's Android and Kotlin setup lives in one place, so a ten-module project has one copy of it. AGP 9 has built-in Kotlin: omit the `kotlin-android` plugin, set `compileOptions.targetCompatibility` once, and `jvmTarget` inherits it (never set separately) |
| Build performance | Configuration cache ON (`org.gradle.configuration-cache=true`), Isolated Projects OFF | Config cache is Gradle 9's preferred mode. Isolated Projects is still experimental |
| Annotation processing | **KSP2** | Only actively developed processor, and required by Room and Hilt on Kotlin 2+. KSP2 releases are plain-versioned, so the KSP version does not track the Kotlin version: read its release notes for the supported Kotlin range rather than inferring one |
| Compose compiler | `org.jetbrains.kotlin.plugin.compose`, version = Kotlin version | Ships with Kotlin since 2.0. Strong skipping is the default, so the `composeCompiler {}` block is omitted and the `StrongSkipping` and `IntrinsicRemember` flags are not set |
| JDK | 17 toolchain plus the foojay resolver plugin in settings. Daemon JVM pinned by criteria in `gradle/gradle-daemon-jvm.properties` | AGP 9's default bytecode target is 17, and foojay auto-provisions the toolchain. Daemon criteria select the daemon JVM independently of the launcher, so `./gradlew` needs no per-machine `JAVA_HOME`. JDK 21 is needed only to *run* Robolectric |
| compileSdk | 37 | Current stable platform, and the floor the androidx artifacts hard-require through AAR metadata. AGP fails the build rather than warning, so the floor rises whenever those artifacts do |
| targetSdk | 37 | Matches compileSdk and clears the Play Store mandate |
| minSdk | 26 | The lowest floor that costs no legacy code path in this app: Compose, Ktor, Room and Coil all support it, and neither screen reaches for a newer API. Reach is bought for nothing |
| Release shrinking | **R8**, with resource shrinking, over `proguard-android-optimize.txt` | The release build ships the code and resources it reaches and nothing else. Every library in the stack publishes its own consumer rules and no app class is reached by reflection or by name, so the app's own rules file holds no keep rule and R8 reports no missing classes |
| API credential | The TMDB v4 read access token is read from `local.properties` into a `buildConfigField`, defaulting to empty | The token never enters version control, and a clone without one still builds and runs: the app opens on a state naming the two-line fix rather than failing at the Gradle stage. Rejected: committing the token (a live credential in a public repository), and failing the build (the first run is a stack trace rather than an app) |


## Project structure

| Decision | Choice | Rationale |
|---|---|---|
| Modularization | Feature-modularized, following Google's modularization guidance. The module map, the dependency rules, and the layer conventions are owned by the architecture spec | Enforced boundaries and parallel builds. A feature module never depends on another feature, so a new feature team adds a module instead of editing one, and swapping a data source is a change behind a contract rather than a change above it |

## UI

| Decision | Choice | Rationale |
|---|---|---|
| UI toolkit | Jetpack Compose | Standard for new apps |
| Design system | Material 3, stable APIs only | M3 Expressive components are still alpha; adopt selectively once stable |
| Navigation | **Jetpack Navigation 3** | The back stack is ours: typed keys in a `SnapshotStateList`, an `entryProvider` DSL, and `NavDisplay`. Rejected: Navigation Compose paired with `NavigableListDetailPaneScaffold`, where two libraries own the back stack between them, which is where list-detail back-button bugs come from |
| Adaptive layout | `androidx.compose.material3.adaptive:adaptive-navigation3` two-pane scene, keyed to window size class | Makes "expanded width shows both panes" a navigation concern rather than a per-screen branch, and selecting an item at expanded width updates the pane without a navigation event. Nothing keys off device type or orientation |
| Insets | Edge-to-edge from day one | Platform requirement, and the poster art reaches the screen edges by design |
| Images | **Coil 3** with the Ktor 3 network fetcher | Compose-native and coroutine-first. Routing it through the Ktor engine means one HTTP stack and one connection pool for images and API calls, and offline poster rendering comes from Coil's own disk cache. Rejected: Glide, which needs KSP codegen and a Compose bridge to reach the same place |

## Architecture & DI

| Decision | Choice | Rationale |
|---|---|---|
| Architecture | Official guidance: a UI layer over a data layer, with a domain layer only where one earns its place. Unidirectional data flow, single source of truth, view models as state holders | The architecture spec owns how the layers map onto modules |
| DI | **Hilt** | Google's recommendation, and its instrumented testing support is what lets a device test swap one binding and keep the rest of the real graph. Rejected: manual constructor injection, which is honest at this size but leaves the multi-module wiring question unanswered |
| Async | kotlinx.coroutines and Flow | Every repository read is a `Flow`, every network call a suspend function. Cancellation follows the coroutine, so a screen leaving takes its requests with it |
| Serialization | kotlinx.serialization | Wire payloads and navigation keys. Reflection-free, so R8 keeps nothing on its account |

## Networking

| Decision | Choice | Rationale |
|---|---|---|
| HTTP client | **Ktor client** on the **OkHttp engine** | Authentication, retry, timeouts, logging and serialization are plugins in one client configuration rather than split between interceptors and a converter factory, and `MockEngine` makes the whole network layer testable in-process. OkHttp underneath supplies connection pooling and HTTP/2. Rejected: Retrofit, whose declarative interface earns less across three endpoints, and which would need MockWebServer for tests that `MockEngine` answers |
| Authentication | The v4 read access token in an `Authorization: Bearer` header, injected by client configuration | Same access as the v3 key, but the credential never enters a URL, so it cannot leak through logs, crash reports, or proxy history. Rejected: the v3 `api_key` query parameter |
| Error model | Transport and status failures map to a sealed type carrying exactly three causes: no connectivity, server or API failure, unexpected empty response | The UI renders one message per cause, and no exception text, status code, or stack trace reaches a screen. The mapping is one function with a unit test per branch |

## Data

| Decision | Choice | Rationale |
|---|---|---|
| Persistence | **Room** as the single source of truth | Screens observe Room; only the data layer writes. A cached list therefore renders before any request completes, and a refresh replaces the trending table in one transaction, so a subscriber sees the new set and never an empty list between the two. Rejected: an OkHttp disk cache, which cannot be queried and makes staleness a matter of negotiating cache headers rather than our own rule; and a serialized JSON snapshot, where one detail lookup means scanning the whole blob |
| Schema | Two tables, movie summary and movie detail, each keyed on the movie's id, and the detail table carrying its own copy of the summary columns | A cached detail has to outlive the trending set, which a key into it would not survive. Nothing queries by genre in SQL, so there is no genre table and no cross reference: genres store as a delimited list of the app's own constants |
| Genre resolution | `Genre` is an enum the app owns, and each source maps its own vocabulary onto it at its own mapper | No genre endpoint is called and no genre list is stored. The filter sheet lists every genre with no fetch and no empty-state race, and the names resolve to string resources a translator can reach |
| Paged fetch | Sequential requests, capped, inside the remote source | Ranking shifts between requests, so pages must resolve in order to know when enough distinct movies are held, and concurrent requests would multiply the duplicates rather than avoid them. A failure part way through fails the whole call, because a caller cannot tell a list cut short from one that is simply short |
| Deduplication | A pure function in the remote source, taking pages and returning distinct movies | A fixture reproducing the observed boundary duplicates drives it directly, which it could not do buried in a UI layer |
| Freshness | None. Both screens render whatever is cached and issue their request regardless, and an answer replaces what is on screen | There is no moment to store and nothing to compare it against, so the app holds no clock and no test has to move time. What separates the two screens is what a failure does, not how old a row is. Rejected: a `cachedAt` column against an injected `Clock`, which saves one request on a screen the user explicitly asked for and costs a column, a clock and a rule to explain |
| Filter and sort | Pure Kotlin over the already-fetched set, not SQL | Locale-aware case-insensitive title ordering and nulls-last date ordering are correct in Kotlin and dubious in SQLite, and the ordering rules are asserted against a fixed fixture rather than against a database |
| Key-value storage | None | Filter and sort reset on cold start, so there is nothing to persist |

## Testing

Fakes over mocks: there is no mocking library in the build. A mock returns the value the test wrote
and asserts the call rather than the result, so it stays green after the real collaborator stops
behaving that way. Fakes live in a shared testing module, which is what makes them visible to every
other module's tests. `MockEngine` is not an exception to this: it stands in for the server, not
for a class we own.

Whether a test runs on the JVM or on a device is decided by its source set, never by an annotation.
`@RunWith(AndroidJUnit4::class)` appears on both a Robolectric test and a device test, and only
`test` versus `androidTest` separates them.

| Tool | Where | Why |
|---|---|---|
| **JUnit4** | every JVM and instrumented source set | Android's default. JUnit5 is still third-party-plugin territory |
| **kotlin-test** assertions | JVM tests | Reads better than JUnit's assertion API and matches the Kotlin the code is written in |
| **kotlinx-coroutines-test** | view model and repository tests | `runTest` with a test dispatcher gives virtual time, so a timeout or a retry delay is asserted rather than waited out |
| **Ktor `MockEngine`** | network and repository tests | Answers requests in-process with fixture JSON. Every failure branch, including the three error causes and a partial page failure, is a value the engine returns rather than a condition anyone has to arrange |
| **Turbine** | tests asserting on what a `Flow` emits over time | `awaitItem()` names one emission and `expectNoEvents()` names the absence of the next, so a sequence reads as a sequence instead of a channel and two timeouts |
| **Room in-memory database** | DAO tests, under Robolectric | Exercises the real query compiler and the real schema against a database that dies with the test |
| **Robolectric** | all JVM tests needing the Android framework | Runs Android APIs on the JVM at unit-test speed |
| **Compose testing (v2)** | screen tests on Robolectric, and device tests | `createComposeRule` with semantics matchers covers behaviour inside one screen: conditional visibility, which callback fires, and the accessibility contracts for touch target size, role and state. Import the rule from `androidx.compose.ui.test.junit4.v2`; the same name in the parent package is the deprecated v1 rule |
| **Compose Preview Screenshot Testing** | golden tests beside each feature, and beside the shared composables | Rendering is what assertions are worst at: a test can confirm a label exists while the layout behind it is broken. Every distinct visual state gets a `@Preview` and a golden, in light and dark. Fall back to Roborazzi if the preview-channel tooling blocks us |
| **Hilt instrumented testing** | device tests | Swaps the binding that chooses the remote source and keeps the rest of the real graph, so a flow test integrates the real Room database and the real navigation while arranging its data as movies rather than as one provider's JSON. `DependencyGraphTest` injects the unswapped graph, which makes a graph that fails to resolve a test failure rather than a launch crash |
| **androidx.test** runner, ext-junit, espresso-core | device tests | Instrumentation plumbing. Espresso is pinned ahead of the version Compose resolves on its own, whose input injector is built from a platform method newer API levels drop |
| **Journey files** | end-to-end, against the live API on an emulator | The one layer that substitutes nothing: the build a user installs, reaching the server they reach. One journey per acceptance criterion. Never gates `check`, because it needs a token and a network |
| **Kover** | JVM test tasks | Coverage reporting, to find untested branches rather than to enforce a number |

## Code quality tooling

| Decision | Choice | Rationale |
|---|---|---|
| Formatting | **Spotless** running **ktlint**, style in a root `.editorconfig` | Formatting is enforced at the same gate as tests, and the IDE and the build read the same file |
| Test entry point | `scripts/unittest.sh` | One command runs everything that gates a change, so the suite is not a list of Gradle tasks a reader has to reassemble |

## Deliberately not in the stack

| Left out | Because |
|---|---|
| Paging 3 | The set is a fixed hundred items held in memory. Paging solves a problem this screen does not have |
| DataStore | Nothing survives a cold start |
| `ConnectivityManager` | Offline is detected reactively, by a request failing. A system flag that reports "connected" through a captive portal turns a false reading into a refusal to try |
| WorkManager | No background work in the MVP |
| Kotlin Multiplatform | The product's stated future names feature teams, more API sources and deeper offline, and never a second platform |
| `androidx.sqlite:sqlite-bundled` | The platform's own SQLite runs every statement this app writes: no window function, no `ON CONFLICT DO UPDATE`, no FTS, and ordering is Kotlin's rather than the database's. Bundling would buy one SQLite version across the whole `minSdk` spread and a JVM test running the engine the app ships, and cost 1.3 MB of native library per ABI |
| A mocking library | See the testing section |
| `core-splashscreen` | The app opens on the trending list, so there is no destination decision to hold a splash for |

## Open items

1. M3 Expressive adoption, to re-evaluate once the components reach stable.
