# Testing

## The test suite principle

A test is worth having only if it can fail for the right reason. Every check goes to the cheapest
layer that can still fail for that reason, and to exactly one layer.

Cheapness is not only wall-clock time. A JVM test that fails names one class; an end-to-end test
that fails names the whole app and the server behind it. Pushing a check down makes the failure more
legible as well as faster.

## Doubles

Real object first, hand-written fake second, mocking library never. There is no mocking dependency in
the build and no case for adding one: a mock returns the value the test wrote and asserts the call
rather than the result, so it stays green after the real collaborator stops behaving that way. A fake
asserts what the call produced, so a refactor that preserves the behaviour preserves the test.

Fakes and fixtures live in `:core:testing`, which is what makes them
visible to other modules' tests.

## The layers

### 1. Unit

**Where:** `core/*/src/test` and the view model tests in `feature/*/src/test`.

**Tools:** JUnit4, `kotlin.test`, `kotlinx-coroutines-test`, Ktor `MockEngine`, Room in-memory under
Robolectric.

**What goes here:** one class against its collaborators' fakes. The rules a mapper or a repository
owns, ordering and selection over a fixture, anything decided against an injected clock, every
failure branch a call can take, and the state a view model produces for each combination of its
inputs.

**Why here:** a timeout or a malformed response is a value `MockEngine` returns, not a condition
anyone has to arrange. The fakes in `:core:testing` carry their own tests, because a fake that lies
is worse than no fake.

### 2. Screenshot

**Where:** `core/ui/src/screenshotTest` and `feature/*/src/screenshotTest`, with goldens checked in
under `src/screenshotTestDebug/reference`.

**Tools:** Compose Preview Screenshot Testing, `@PreviewTest` on a `@Preview`. Gated by `check`, so
`scripts/unittest.sh` runs it. `scripts/screenshotTest.sh` validates the goldens on their own, which
is the fast loop while a component is being changed, and `scripts/screenshotUpdate.sh` re-records
them.

**What goes here:** rendering. Every component in light and dark, every distinct visual state of a
screen, the form factors including the two-pane layout, and the large-font setting.

This layer carries more weight here than the count of tests suggests, because a record with fields
missing is the normal case rather than the edge one. Omitting a field is a rendering rule, and a
screen that drops a row correctly still has to look composed afterwards.

Posters never come from the network. Previews supply a fake image loader over a local drawable, so a
golden captures layout and never a download.

**Why here:** assertions are worst at exactly this. A test can confirm a label exists and the layout
can still be broken behind it.

### 3. Screen

**Where:** `feature/*/src/test` and `core/ui/src/test`.

**Tools:** Compose testing on Robolectric. No emulator.

**What goes here:** behaviour inside a single screen or component. Conditional visibility, which
callback fires, how a control's state follows the state it reflects, and the accessibility
contracts: minimum touch target, state and role semantics, and live regions.

**Why here:** it is logic that happens to live in the UI layer, so it is tested in the UI layer, at
JVM speed.

### 4. Flow

**Where:** `app/src/androidTest`, in `com.shayan.amro.flow`. On a device.

**Tools:** Hilt instrumented testing, Compose testing, and a fake remote source. Run by
`scripts/flowTests.sh`.

**What goes here:** multi-screen flows, integrating every real layer of the app with only the remote
source replaced. Navigation and the arguments that travel with it, state surviving a configuration
change and process death, the adaptive layout resolving at each width, and content served from the
database when the next refresh fails.

Room running on the device's own SQLite is covered here. The Hilt graph is not, since a flow test
puts a fake remote source into it.

**How the source is replaced:** A flow test uninstalls the Hilt module that chooses which source
backs `MovieRemoteDataSource` and binds `FakeMovieRemoteDataSource` in its place. That interface is
the app's own, so a flow test arranges its data as movies rather than as a provider's JSON, and
stays source-agnostic.

A flow test therefore cannot observe anything that only latency produces (ex: a loading placeholder),
because a fake answers within the frame.

**Every test starts as a fresh install.** A rule empties the app's databases before each one. A
cached list would otherwise let the test skip the network path.

**Reading a failure:** a flow test failing should mean two layers are wired together wrong. If the
cause turns out to sit inside one layer, that is also a gap in the cheaper test that should have
caught it, and both get fixed.

### 5. E2E

**Where:** `app/src/androidTest`, in `com.shayan.amro.e2e`, marked `@E2eTest`. On a device.

**Tools:** Hilt instrumented testing and Compose testing over the app's own graph, against the live
API with a real token. Run by `scripts/e2e.sh`, which selects on the annotation.
`scripts/flowTests.sh` excludes it, which is what keeps the two device layers apart.

**What makes it end to end:** The app resolves APIs real endpoints with the credential the build
carries, responses that come back to the device's SQLite, and pulls its posters from the image host.

**What goes here:** Test cases that need to exercise the real API, such as a search that returns a
result, or a movie that has a poster.

**Every test starts as a fresh install.** A rule empties the app's databases before each one. A
cached list would otherwise let the test skip the network path.

**Why here:** this layer replaces nothing. Everywhere else the app draws its data from responses we
recorded. Running the real build against the real server is what makes this the most expensive and
least repeatable layer, so it stays small and a failure is read as signal rather than re-run. It
also needs a token and a network, so it never gates `check`.

## Where a test goes

| Layer | Runs on | Command                         |
|---|---|---------------------------------|
| Unit | JVM | `scripts/unittest.sh`           |
| Screen | JVM | `scripts/unittest.sh`           |
| Screenshot | JVM | `scripts/screenshotTest.sh` or `scripts/unittest.sh` |
| Flow | device | `scripts/flowTests.sh`          |
| E2E | device, real API | `scripts/e2e.sh`                |

Whether a test runs on the JVM or on a device is decided by its source set.
`@RunWith(AndroidJUnit4::class)` appears on both a Robolectric test and a device test, and only
`test` versus `androidTest` separates them. `@E2eTest` divides the two device layers within
`androidTest`, and says nothing about where a test runs.

## The coverage bar

**80% line coverage per module.** Kover measures it, `koverVerify` runs as part of `check`, so
`scripts/unittest.sh` and CI both gate on it. `scripts/coverage.sh` prints the table, worst module
first, and writes browsable HTML per module.

The number is a floor, not a target. It sits below where every module already is.

Kover does NOT count the screenshot, flow or E2E layers. It only counts unit and screen tests.

## What is deliberately not tested

**The same assertion at two layers.** A scenario covered by a flow test is not also an E2E.
Duplication does not add confidence, it adds a second place to diagnose the same failure and a second
thing to update.

**Framework and library behaviour.** We test our adapter, not the library under it. Mapping a page of
a provider's JSON onto our types is ours and is tested; whether Ktor's `MockEngine` reports a dropped
connection the same way OkHttp does is theirs and is not.

**Failure branches above layer 1.** Cheap as a value the engine returns, awful to provoke on a device
or against a live API.

**Every state crossed with every theme and form factor as a golden.** Combinatorial. Representative
screen states, plus component goldens for the rest.

**Anything non-deterministic in a screenshot.** Skeleton placeholders animate and a network image
arrives when it arrives, so a golden capturing either would fail on its own schedule. Freeze the
animation and serve the image locally.

## Golden maintenance

Baselines are re-recorded only in the change that intentionally alters the design, and the image diff
is part of that review. A design system change is one deliberate re-record, not a hundred goldens
quietly updated alongside unrelated work. A screenshot suite that goes noisy gets ignored, which is
worse than not having it.

## When a layer gets too expensive

Cut in this order:

1. E2E, to a run before a milestone rather than on demand while working.
2. Flow, to merge into `main` rather than every push.
3. Nothing else. Unit, screenshot and screen tests stay on every change; they are the cheap layers,
   and cutting them is how the fast signal disappears.
