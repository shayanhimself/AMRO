# Domain Model and the TMDB Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill `:core:model` with the types every module speaks in, and `:core:network` with the source contract, its TMDB implementation, and the recorded fixtures every layer above asserts against.

**Architecture:** `:core:model` is pure Kotlin types with one dependency. `:core:network` holds a source-neutral `MovieRemoteDataSource` returning `NetworkResult`, implemented by a Ktor client whose engine is injected so a JVM test passes `MockEngine` and production passes OkHttp. Wire shapes, normalisation, the genre table, the image width vocabulary and the failure mapping all stay inside `:core:network`; nothing above it sees a Ktor type or TMDB's name.

**Tech Stack:** Kotlin 2.4, kotlinx-datetime, kotlinx-serialization, Ktor 3 on OkHttp, Hilt, JUnit4 with `kotlin-test` assertions, Ktor `MockEngine`, Python 3 standard library for the recording script.

**Spec:** `docs/superpowers/specs/003-domain-model-and-tmdb-source.md`

---

## Global Constraints

- **Never commit or push.** `CLAUDE.md` overrides the commit step every superpowers task normally ends with. Each task ends with a verification step instead, and the work stays in the working tree for the user to review.
- **Work in the current branch. No worktree.** This plan is not executed in an isolated workspace, so the superpowers step that creates one is skipped.
- **Tick this plan's checkboxes as the work lands.** A task's steps are marked `- [x]` in this file once that task's verification step has passed, so the file states what is actually done.
- **Run the suite with `./scripts/unittest.sh`.** It runs `./gradlew check`, which covers unit tests, Robolectric screen tests, screenshot goldens and the ktlint gate. `./gradlew test` is not equivalent. Run `./scripts/format.sh` before it so ktlint does not fail the gate on formatting.
- **`const val` names are `SCREAMING_SNAKE_CASE`.** Non-const `val`s are `camelCase`.
- **No magic numbers.** A literal whose value carries meaning gets a named `const val`. Exempt: `0` and `1`, counts and sizes a test asserts on, and numbers that are already the domain's own vocabulary. TMDB's genre ids and its image widths are that domain vocabulary: the table and the width list name them.
- **No trailing comments.** A comment goes on its own line above the code it describes.
- **A comment says what the code does, or why it does it that way.** Not what it does not do.
- **KDoc where it earns its place, and interfaces and contracts always get it.** A KDoc block opens with what the thing is or does, then the reasoning. Reasoning about one property or branch goes above that property or branch.
- **Never name a document in code.** No spec number, plan task or design frame id in a comment or KDoc.
- **Test function names use backtick spaced form.** Device tests in `app/src/androidTest` are the exception and use `camelCase`, because a space in a method name needs DEX 040 and `minSdk` is 26.
- **A test's own fixture strings are named.** Every value a test builds a fixture from gets a file-level `private const val`.
- **No mocking library.** Fakes, real objects, and `MockEngine` standing in for the provider.
- **A module never sets `compileSdk`, `minSdk`, `targetSdk`, `compileOptions`, `jvmToolchain` or `compose = true`.** The convention plugin owns them. A module script declares `namespace`, its own plugins, and its dependencies.
- **Ktor types never leave `:core:network`.** No provider's name appears above it, except `TmdbConfig` built in `:app`.
- **No user-facing copy below the UI layer.** `Genre` and `ReleaseStatus` are identifiers the UI resolves to string resources.
- **Package root is `com.shayan.amro`.** Source files live under `src/main/kotlin/com/shayan/amro/…`.

## One deviation from the spec

**`error/500.json` is checked in rather than recorded.** TMDB cannot be asked for a 500, so `scripts/record-fixtures.py` records the 401 by sending a deliberately invalid credential and leaves the 500 body alone. The 500 fixture is TMDB's own error envelope, written once by hand and documented as un-recordable.

## One open item this plan does not close

`docs/testing.md` says the image base URL is a client configuration value a device test rebinds at a local server. The spec puts the host and the width vocabulary inside `:core:network` and gives `TmdbRemoteDataSource` a constructor taking the client alone, so this plan keeps the host as a constant in the image mapper. Nothing loads an image yet, so nothing needs the rebinding. Raise it in the spec that introduces the image loader: threading it through means an `imageBaseUrl` on `TmdbConfig` and a mapper that takes it as a parameter.

---

## File structure

**`:core:model`** — types only, no Android import, one dependency.

| File | Holds |
|---|---|
| `MovieId.kt` | `SourceId`, `MovieId` |
| `Genre.kt` | `Genre` |
| `ReleaseStatus.kt` | `ReleaseStatus` |
| `ImageRef.kt` | `ImageVariant`, `ImageRef`, `bestFor` |
| `Rating.kt` | `Rating` |
| `Movie.kt` | `Movie` |
| `MovieDetail.kt` | `MovieDetail` |
| `DataError.kt` | `DataError` |

**`:core:network`**

| File | Holds |
|---|---|
| `TmdbConfig.kt` | The address and the credential. Unchanged by this plan |
| `TmdbHttpClient.kt` | The `HttpClient` built from a `TmdbConfig` and an injected engine |
| `MovieRemoteDataSource.kt` | The source contract |
| `MoviePage.kt` | `MoviePage` |
| `NetworkResult.kt` | `NetworkResult` |
| `ErrorMapping.kt` | The only place a failure becomes a `DataError` |
| `TmdbGenres.kt` | TMDB's genre ids mapped onto `Genre` |
| `TmdbFacts.kt` | The page size, and that ranking shifts between requests |
| `TmdbRemoteDataSource.kt` | The two endpoints, the only place TMDB's paths are named |
| `dto/TrendingPageDto.kt` | The trending wire shape |
| `dto/MovieSummaryDto.kt` | A trending row |
| `dto/MovieDetailDto.kt` | The detail wire shape, with its nested genre |
| `mapper/ImageMapper.kt` | A path to an `ImageRef`, and the image host |
| `mapper/MovieMapper.kt` | Wire shape to model, the normalisation, and `TMDB_SOURCE` |
| `di/TmdbClientModule.kt` | The engine and the client |
| `di/MovieRemoteDataSourceModule.kt` | The contract bound to its TMDB implementation |

**`:core:testing`**

| File | Holds |
|---|---|
| `Fixtures.kt` | `fixture(path)`, beside the existing `string(id)` helper |
| `src/main/resources/fixtures/tmdb/` | The recorded bodies |

**Root**

| File | Holds |
|---|---|
| `scripts/record-fixtures.py` | Re-records everything except the assembled pages |

---

### Task 1: The domain model

**Files:**
- Modify: `core/model/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/MovieId.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/Genre.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/ReleaseStatus.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/ImageRef.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/Rating.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/Movie.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/MovieDetail.kt`
- Create: `core/model/src/main/kotlin/com/shayan/amro/core/model/DataError.kt`
- Test: `core/model/src/test/kotlin/com/shayan/amro/core/model/ImageRefTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `SourceId(value: String)`, `MovieId(source: SourceId, value: String)`, `enum Genre`, `enum ReleaseStatus`, `ImageVariant(widthPx: Int, url: String)`, `ImageRef(variants: List<ImageVariant>)`, `fun ImageRef.bestFor(widthPx: Int): String`, `Rating(average: Double, count: Int)`, `Movie(id, title, genres, popularity, releaseDate, poster)`, `MovieDetail(movie, overview, tagline, backdrop, runtime, status, budget, revenue, rating, imdbId)`, `sealed interface DataError` with `NoConnectivity`, `Server`, `EmptyResponse`.

- [ ] **Step 1: Give `:core:model` its one dependency**

`kotlinx-datetime` is `api` rather than `implementation`: `Movie.releaseDate` is a `LocalDate`, so every consumer needs the type on its own compile classpath.

`core/model/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.model"
}

dependencies {
    api(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
```

- [ ] **Step 2: Write the failing `bestFor` test**

`core/model/src/test/kotlin/com/shayan/amro/core/model/ImageRefTest.kt`:

```kotlin
package com.shayan.amro.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

private const val NARROW_URL = "https://images.example/w92/poster.jpg"
private const val MIDDLE_URL = "https://images.example/w185/poster.jpg"
private const val WIDE_URL = "https://images.example/w500/poster.jpg"

private const val NARROW_WIDTH = 92
private const val MIDDLE_WIDTH = 185
private const val WIDE_WIDTH = 500

private val POSTER =
    ImageRef(
        listOf(
            ImageVariant(NARROW_WIDTH, NARROW_URL),
            ImageVariant(MIDDLE_WIDTH, MIDDLE_URL),
            ImageVariant(WIDE_WIDTH, WIDE_URL),
        ),
    )

class ImageRefTest {
    @Test
    fun `a slot narrower than every variant takes the narrowest`() {
        assertEquals(NARROW_URL, POSTER.bestFor(NARROW_WIDTH - 1))
    }

    @Test
    fun `a slot exactly a variant wide takes that variant`() {
        assertEquals(MIDDLE_URL, POSTER.bestFor(MIDDLE_WIDTH))
    }

    @Test
    fun `a slot between two variants takes the wider one`() {
        assertEquals(MIDDLE_URL, POSTER.bestFor(NARROW_WIDTH + 1))
    }

    @Test
    fun `a slot wider than every variant takes the widest`() {
        assertEquals(WIDE_URL, POSTER.bestFor(WIDE_WIDTH + 1))
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew :core:model:testDebugUnitTest --tests "*ImageRefTest*"`
Expected: FAIL, compilation error, `ImageRef` and `bestFor` unresolved.

- [ ] **Step 4: Write the identity types**

`MovieId.kt`:

```kotlin
package com.shayan.amro.core.model

/** Which source issued a movie's id. */
@JvmInline
value class SourceId(val value: String)

/**
 * A movie's identity: the source that issued the id, and the id as that source writes it.
 *
 * The id space belongs to whoever issued it, so a second source reusing an integer does not
 * collide with the first.
 *
 * @property source the source the id came from.
 * @property value the id, in the source's own form.
 */
data class MovieId(
    val source: SourceId,
    val value: String,
)
```

`Genre.kt`:

```kotlin
package com.shayan.amro.core.model

/**
 * The app's own genre vocabulary. Each source maps its genres onto this, and a genre a source
 * offers that this does not carry is dropped.
 *
 * These are identifiers, not copy: the UI resolves each constant to a string resource.
 */
enum class Genre {
    ACTION,
    ADVENTURE,
    ANIMATION,
    COMEDY,
    CRIME,
    DOCUMENTARY,
    DRAMA,
    FAMILY,
    FANTASY,
    HISTORY,
    HORROR,
    MUSIC,
    MYSTERY,
    ROMANCE,
    SCIENCE_FICTION,
    TV_MOVIE,
    THRILLER,
    WAR,
    WESTERN,
}
```

`ReleaseStatus.kt`:

```kotlin
package com.shayan.amro.core.model

/**
 * Where a movie is in its release.
 *
 * An identifier, not copy: the UI resolves each constant to a string resource.
 *
 * [UNKNOWN] is what a value the app does not carry becomes, so a status added by a source later
 * lands somewhere legible instead of printing a raw token.
 */
enum class ReleaseStatus {
    RUMORED,
    PLANNED,
    IN_PRODUCTION,
    POST_PRODUCTION,
    RELEASED,
    CANCELED,
    UNKNOWN,
}
```

- [ ] **Step 5: Write the image types and `bestFor`**

`ImageRef.kt`:

```kotlin
package com.shayan.amro.core.model

/**
 * One rendition of an image.
 *
 * @property widthPx the width the rendition is served at.
 * @property url where it is served from.
 */
data class ImageVariant(
    val widthPx: Int,
    val url: String,
)

/**
 * Every rendition a source offers for one image, ordered ascending by width.
 *
 * A source offering no image yields no [ImageRef], so [variants] is never empty.
 *
 * @property variants the renditions, narrowest first.
 */
data class ImageRef(
    val variants: List<ImageVariant>,
)

/**
 * The URL to load for a slot [widthPx] pixels wide.
 *
 * The narrowest variant at least as wide as the slot, so nothing is upscaled, and the widest
 * variant when every one of them is narrower than the slot.
 */
fun ImageRef.bestFor(widthPx: Int): String = variants.firstOrNull { it.widthPx >= widthPx }?.url ?: variants.last().url
```

- [ ] **Step 6: Run the test and confirm it passes**

Run: `./gradlew :core:model:testDebugUnitTest --tests "*ImageRefTest*"`
Expected: PASS, four tests.

- [ ] **Step 7: Write the record types**

Nullability is the missing-data rule made structural: a field measured empty in a live top 100 is nullable, and absence is the only signal of it. No screen compares against zero or an empty string.

`Rating.kt`:

```kotlin
package com.shayan.amro.core.model

/**
 * A rating and the sample size behind it. A rating without its count misleads, so the two travel
 * together or not at all.
 *
 * @property average the mean score.
 * @property count how many votes it was drawn from.
 */
data class Rating(
    val average: Double,
    val count: Int,
)
```

`Movie.kt`:

```kotlin
package com.shayan.amro.core.model

import kotlinx.datetime.LocalDate

/**
 * The record the trending list renders.
 *
 * Every field has a consumer on that list: title, genres and poster are rendered, and popularity,
 * title and release date are the three sort keys.
 *
 * @property id the movie's identity, carrying the source that issued it.
 * @property title the movie's title.
 * @property genres the app's genres for it, which is every genre the source offered that the app
 *   carries.
 * @property popularity the source's own popularity score, the default sort key.
 * @property releaseDate null where the source published none.
 * @property poster null where the source offers no poster.
 */
data class Movie(
    val id: MovieId,
    val title: String,
    val genres: List<Genre>,
    val popularity: Double,
    val releaseDate: LocalDate?,
    val poster: ImageRef?,
)
```

`MovieDetail.kt`:

```kotlin
package com.shayan.amro.core.model

import kotlin.time.Duration

/**
 * The fuller record behind one [Movie].
 *
 * The movie is embedded rather than repeated, so one declaration covers the shared fields and the
 * two records cannot disagree.
 *
 * @property movie the fields the trending list also renders.
 * @property overview the description, null where the source published none.
 * @property tagline null where the source published none, which omits the line rather than
 *   blanking it.
 * @property backdrop null where the source offers no backdrop.
 * @property runtime null where the source published none.
 * @property status where the movie is in its release.
 * @property budget null where the source disclosed none, which is what reads "Not disclosed".
 * @property revenue null where the source disclosed none.
 * @property rating null where nothing has voted, which omits the rating block entirely.
 * @property imdbId null where the source carries none, which omits the row.
 */
data class MovieDetail(
    val movie: Movie,
    val overview: String?,
    val tagline: String?,
    val backdrop: ImageRef?,
    val runtime: Duration?,
    val status: ReleaseStatus,
    val budget: Long?,
    val revenue: Long?,
    val rating: Rating?,
    val imdbId: String?,
)
```

`DataError.kt`:

```kotlin
package com.shayan.amro.core.model

/**
 * Why a read failed, in the three causes the product renders one message each for.
 *
 * No exception text, status code or stack trace travels with it: a feature maps each cause to a
 * string resource it owns.
 */
sealed interface DataError {
    /** The request never reached a server. */
    data object NoConnectivity : DataError

    /** A server answered, and the answer was not usable. */
    data object Server : DataError

    /** A server answered successfully with nothing in it. */
    data object EmptyResponse : DataError
}
```

- [ ] **Step 8: Verify the module**

Run: `./scripts/format.sh && ./gradlew :core:model:testDebugUnitTest`
Expected: PASS. Leave the changes in the working tree.

---

### Task 2: The recorded fixtures and the way to re-record them

**Files:**
- Create: `scripts/record-fixtures.py`
- Create: `core/testing/src/main/kotlin/com/shayan/amro/core/testing/Fixtures.kt`
- Create (recorded): `core/testing/src/main/resources/fixtures/tmdb/trending/page-1.json` through `page-6.json`, `genres.json`, `movie/687163.json`, `movie/1294189.json`, `movie/1003596.json`, `error/401.json`
- Create (by hand): `core/testing/src/main/resources/fixtures/tmdb/error/500.json`
- Create (assembled): `core/testing/src/main/resources/fixtures/tmdb/trending/boundary-duplicates/page-1.json`, `page-2.json`, `README.md`
- Modify: `core/testing/build.gradle.kts`
- Test: `core/testing/src/test/kotlin/com/shayan/amro/core/testing/FixturesTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `fun fixture(path: String): String`, reading a recorded body from below `fixtures/` by a path such as `tmdb/genres.json`.

- [ ] **Step 1: Write the recording script**

Python 3 standard library only. The token is read from `local.properties`, the script refuses to run without one, every body is written exactly as TMDB sent it, and each target is reported as new, changed or unchanged.

`scripts/record-fixtures.py`:

```python
#!/usr/bin/env python3
"""Re-record the TMDB fixtures the test suite asserts against.

Every body is written exactly as TMDB sent it.

Two things here are not recorded. The pages under trending/boundary-duplicates/ are assembled
from recorded rows and checked in, because a recording only reproduces duplicate ids on a day
TMDB happens to re-rank mid-sequence. error/500.json is written by hand, because TMDB cannot be
asked for a 500.

Usage: python3 scripts/record-fixtures.py
"""

import sys
import urllib.error
import urllib.request
from pathlib import Path

API_ROOT = "https://api.themoviedb.org/3"
TOKEN_KEY = "tmdb.readAccessToken"
TRENDING_PAGES = 6

COMPLETE_MOVIE_ID = "687163"
MISSING_DATA_MOVIE_ID = "1294189"
POST_PRODUCTION_MOVIE_ID = "1003596"
MOVIE_IDS = (COMPLETE_MOVIE_ID, MISSING_DATA_MOVIE_ID, POST_PRODUCTION_MOVIE_ID)

# Sent in place of the real credential, to record what TMDB answers an unauthorised caller with.
INVALID_TOKEN = "not-a-token"

OK = 200
UNAUTHORISED = 401

REPO_ROOT = Path(__file__).resolve().parent.parent
FIXTURE_ROOT = REPO_ROOT / "core/testing/src/main/resources/fixtures/tmdb"


def read_token():
    """The read access token from local.properties, or None where the file or the key is absent."""
    properties = REPO_ROOT / "local.properties"
    if not properties.exists():
        return None
    for line in properties.read_text().splitlines():
        name, separator, value = line.partition("=")
        if separator and name.strip() == TOKEN_KEY:
            return value.strip() or None
    return None


def fetch(path, token):
    """The status and the raw body TMDB answers a GET of path with."""
    request = urllib.request.Request(
        f"{API_ROOT}/{path}",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()


def write(relative, body):
    """Writes body to the fixture at relative, and reports what changed."""
    target = FIXTURE_ROOT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    previous = target.read_bytes() if target.exists() else None
    target.write_bytes(body)
    if previous is None:
        return "new"
    return "changed" if previous != body else "unchanged"


def recordings(token):
    """Every fixture this script records, as a path, a request, and the credential to send."""
    targets = [
        (f"trending/page-{page}.json", f"trending/movie/week?page={page}", token, OK)
        for page in range(1, TRENDING_PAGES + 1)
    ]
    targets.append(("genres.json", "genre/movie/list", token, OK))
    targets += [(f"movie/{movie}.json", f"movie/{movie}", token, OK) for movie in MOVIE_IDS]
    targets.append(("error/401.json", f"movie/{COMPLETE_MOVIE_ID}", INVALID_TOKEN, UNAUTHORISED))
    return targets


def main():
    token = read_token()
    if token is None:
        sys.exit(f"no {TOKEN_KEY} in local.properties; nothing recorded")

    failed = False
    for relative, path, credential, expected in recordings(token):
        status, body = fetch(path, credential)
        if status != expected:
            print(f"{relative}: expected {expected}, got {status}; left alone")
            failed = True
            continue
        print(f"{relative}: {write(relative, body)}")

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Record the fixtures**

Run:

```bash
chmod +x scripts/record-fixtures.py && python3 scripts/record-fixtures.py
```

Expected: eleven lines reading `new`, and exit 0. If it exits with `no tmdb.readAccessToken in local.properties`, ask the user to add one before continuing; nothing below this step can be verified without the recordings.

- [ ] **Step 3: Write the 500 fixture by hand**

TMDB cannot be asked for a 500, so the body is its own error envelope written once.

`core/testing/src/main/resources/fixtures/tmdb/error/500.json`:

```json
{"success":false,"status_code":11,"status_message":"Internal error: Something went wrong, contact TMDB."}
```

- [ ] **Step 4: Assemble the boundary-duplicate pages**

Every row is a real recorded row. The second page opens with the last three rows of the first, which is the shape unstable ranking produces: a title on a page boundary served twice.

Run:

```bash
python3 - <<'PY'
import json
from pathlib import Path

root = Path("core/testing/src/main/resources/fixtures/tmdb/trending")
target = root / "boundary-duplicates"
target.mkdir(parents=True, exist_ok=True)

first = json.loads((root / "page-1.json").read_text())
second = json.loads((root / "page-2.json").read_text())

overlap = 3
assembled_first = dict(first)
assembled_second = dict(second)
assembled_second["results"] = first["results"][-overlap:] + second["results"][:-overlap]

(target / "page-1.json").write_text(json.dumps(assembled_first, ensure_ascii=False))
(target / "page-2.json").write_text(json.dumps(assembled_second, ensure_ascii=False))
PY
```

Expected: two files, each holding twenty rows, with three ids appearing in both.

- [ ] **Step 5: State how the assembled pages were built**

`core/testing/src/main/resources/fixtures/tmdb/trending/boundary-duplicates/README.md`:

```markdown
# Two pages that repeat rows across their boundary

`page-1.json` is `../page-1.json` unchanged. `page-2.json` is `../page-2.json` with its first
three rows replaced by the last three rows of page 1, which is what a ranking that shifts between
requests produces: a title on a page boundary served twice.

Every row is a real recorded row. These two files are checked in and never re-recorded. A
recording reproduces duplicate ids only on a day TMDB happens to re-rank mid-sequence, so a
re-record would disarm the deduplication test on every other day.
```

- [ ] **Step 6: Write the failing fixture-helper test**

`core/testing/src/test/kotlin/com/shayan/amro/core/testing/FixturesTest.kt`:

```kotlin
package com.shayan.amro.core.testing

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val GENRES_FIXTURE = "tmdb/genres.json"
private const val GENRES_WIRE_KEY = "\"genres\""
private const val ABSENT_FIXTURE = "tmdb/nothing-was-recorded-here.json"

class FixturesTest {
    @Test
    fun `a recorded body is read from the module's own resources`() {
        assertTrue(fixture(GENRES_FIXTURE).contains(GENRES_WIRE_KEY))
    }

    @Test
    fun `a path with nothing recorded at it fails loudly`() {
        assertFailsWith<IllegalStateException> { fixture(ABSENT_FIXTURE) }
    }
}
```

- [ ] **Step 7: Run it and confirm it fails**

First add the test dependencies. `core/testing/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.amro.android.library)
}

android {
    namespace = "com.shayan.amro.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.test.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
```

Run: `./gradlew :core:testing:testDebugUnitTest --tests "*FixturesTest*"`
Expected: FAIL, compilation error, `fixture` unresolved.

- [ ] **Step 8: Write the fixture helper**

The classloader rather than a context, so a plain JVM test reading a fixture needs no Robolectric.

`core/testing/src/main/kotlin/com/shayan/amro/core/testing/Fixtures.kt`:

```kotlin
package com.shayan.amro.core.testing

private const val FIXTURE_ROOT = "fixtures/"

/** Gives the helper below a class to resolve resources against. */
private object FixtureResources

/**
 * A recorded provider response, read from the resources this module ships.
 *
 * A recorded body carries the shapes the provider actually sends, including the ones the product
 * treats as normal, which a hand-built object does not.
 *
 * @param path the fixture's path below `fixtures/`, for example `tmdb/genres.json`.
 * @return the recorded body, verbatim.
 */
fun fixture(path: String): String {
    val resource = FIXTURE_ROOT + path
    val stream =
        FixtureResources::class.java.classLoader?.getResourceAsStream(resource)
            ?: error("no fixture recorded at $resource")
    return stream.use { it.readBytes().decodeToString() }
}
```

- [ ] **Step 9: Run it and confirm it passes**

Run: `./gradlew :core:testing:testDebugUnitTest --tests "*FixturesTest*"`
Expected: PASS, two tests.

- [ ] **Step 10: Verify**

Run: `./scripts/format.sh && ./gradlew :core:testing:testDebugUnitTest`
Expected: PASS. Leave the changes in the working tree.

---

### Task 3: The client and its credential

**Files:**
- Modify: `core/network/build.gradle.kts`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbHttpClient.kt`
- Test: `core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbHttpClientTest.kt`

**Interfaces:**
- Consumes: `fixture(path)` from Task 2, and the existing `TmdbConfig(baseUrl: String, readAccessToken: String)`.
- Produces: `fun tmdbHttpClient(engine: HttpClientEngine, config: TmdbConfig): HttpClient`.

- [ ] **Step 1: Give `:core:network` its test dependencies**

`core/network/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shayan.amro.core.network"
}

dependencies {
    // `api` rather than `implementation`: the source contract returns model types, so every
    // consumer of this module needs them on its own compile classpath.
    api(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Write the failing credential test**

`core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbHttpClientTest.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

private const val TRENDING_PAGE_1_FIXTURE = "tmdb/trending/page-1.json"
private const val TEST_BASE_URL = "https://api.example/3/"
private const val TEST_READ_ACCESS_TOKEN = "a-read-access-token"
private const val TRENDING_PATH = "trending/movie/week"
private const val JSON_CONTENT_TYPE = "application/json"

private val TEST_CONFIG =
    TmdbConfig(
        baseUrl = TEST_BASE_URL,
        readAccessToken = TEST_READ_ACCESS_TOKEN,
    )

class TmdbHttpClientTest {
    @Test
    fun `every request carries the credential as a bearer header`() =
        runTest {
            var sent: HttpRequestData? = null
            val client =
                tmdbHttpClient(
                    engine =
                        MockEngine { request ->
                            sent = request
                            respond(
                                content = fixture(TRENDING_PAGE_1_FIXTURE),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, JSON_CONTENT_TYPE),
                            )
                        },
                    config = TEST_CONFIG,
                )

            client.get(TRENDING_PATH)

            val request = assertNotNull(sent)
            assertEquals("Bearer $TEST_READ_ACCESS_TOKEN", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun `no request puts the credential in its url`() =
        runTest {
            var sent: HttpRequestData? = null
            val client =
                tmdbHttpClient(
                    engine =
                        MockEngine { request ->
                            sent = request
                            respond(
                                content = fixture(TRENDING_PAGE_1_FIXTURE),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, JSON_CONTENT_TYPE),
                            )
                        },
                    config = TEST_CONFIG,
                )

            client.get(TRENDING_PATH)

            val request = assertNotNull(sent)
            assertFalse(request.url.toString().contains(TEST_READ_ACCESS_TOKEN))
        }
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbHttpClientTest*"`
Expected: FAIL, compilation error, `tmdbHttpClient` unresolved.

- [ ] **Step 4: Write the client**

`core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbHttpClient.kt`:

```kotlin
package com.shayan.amro.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val SOCKET_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 20_000L

/**
 * The client every TMDB call is made through.
 *
 * The engine is a parameter, which is what lets a test pass a mock engine and production pass
 * OkHttp with nothing else about the client differing between the two.
 *
 * @param engine what actually sends the request.
 * @param config the address and the credential.
 */
fun tmdbHttpClient(
    engine: HttpClientEngine,
    config: TmdbConfig,
): HttpClient =
    HttpClient(engine) {
        // A non-2xx becomes a thrown response exception, which the error mapping folds into one
        // cause rather than every call site checking a status.
        expectSuccess = true

        defaultRequest {
            url(config.baseUrl)
            header(HttpHeaders.Authorization, "Bearer ${config.readAccessToken}")
        }

        install(ContentNegotiation) {
            // TMDB sends fields the app does not read, and adds more over time.
            json(Json { ignoreUnknownKeys = true })
        }

        install(Logging) {
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }
```

- [ ] **Step 5: Run the test and confirm it passes**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbHttpClientTest*"`
Expected: PASS, two tests.

---

### Task 4: TMDB's genres mapped onto the app's

**Files:**
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbGenres.kt`
- Test: `core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbGenresTest.kt`

**Interfaces:**
- Consumes: `Genre` from Task 1, `fixture(path)` from Task 2.
- Produces: `internal fun tmdbGenre(id: Int): Genre?` and `internal fun tmdbGenres(ids: List<Int>): List<Genre>`.

- [ ] **Step 1: Write the failing totality test**

The recorded genre list and every genre id in the recorded pages are the evidence. This proves the table is total over what has been recorded; it does not detect TMDB adding a genre, which surfaces when the fixture is next recorded.

`core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbGenresTest.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.testing.fixture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val GENRES_FIXTURE = "tmdb/genres.json"
private const val TRENDING_PAGE_FIXTURE_FORMAT = "tmdb/trending/page-%d.json"
private const val RECORDED_TRENDING_PAGES = 6

private const val GENRES_KEY = "genres"
private const val RESULTS_KEY = "results"
private const val GENRE_IDS_KEY = "genre_ids"
private const val ID_KEY = "id"

private const val UNRECOGNISED_GENRE_ID = -1

private fun recordedGenreIds(): Set<Int> =
    Json
        .parseToJsonElement(fixture(GENRES_FIXTURE))
        .jsonObject[GENRES_KEY]!!
        .jsonArray
        .map { it.jsonObject[ID_KEY]!!.jsonPrimitive.int }
        .toSet()

private fun genreIdsInRecordedPages(): Set<Int> =
    (1..RECORDED_TRENDING_PAGES)
        .flatMap { page ->
            Json
                .parseToJsonElement(fixture(TRENDING_PAGE_FIXTURE_FORMAT.format(page)))
                .jsonObject[RESULTS_KEY]!!
                .jsonArray
                .flatMap { row -> row.jsonObject[GENRE_IDS_KEY]!!.jsonArray.map { it.jsonPrimitive.int } }
        }.toSet()

class TmdbGenresTest {
    @Test
    fun `every genre in the recorded list maps to a genre`() {
        val recorded = recordedGenreIds()

        assertTrue(recorded.isNotEmpty())
        recorded.forEach { id -> assertNotNull(tmdbGenre(id), "no genre for recorded id $id") }
    }

    @Test
    fun `every genre id in the recorded pages maps to a genre`() {
        val recorded = genreIdsInRecordedPages()

        assertTrue(recorded.isNotEmpty())
        recorded.forEach { id -> assertNotNull(tmdbGenre(id), "no genre for id $id sent on a page") }
    }

    @Test
    fun `an unrecognised id is dropped`() {
        val recognised = recordedGenreIds().first()

        assertEquals(
            listOf(tmdbGenre(recognised)),
            tmdbGenres(listOf(recognised, UNRECOGNISED_GENRE_ID)),
        )
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbGenresTest*"`
Expected: FAIL, compilation error, `tmdbGenre` and `tmdbGenres` unresolved.

- [ ] **Step 3: Write the table**

The ids are TMDB's own vocabulary, and the table is what names them.

`core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbGenres.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.Genre

/**
 * TMDB's genre ids paired with the app's own genre.
 *
 * Keyed on the id rather than the name, because an id survives a rename and a name does not.
 */
private val TMDB_GENRES: Map<Int, Genre> =
    mapOf(
        28 to Genre.ACTION,
        12 to Genre.ADVENTURE,
        16 to Genre.ANIMATION,
        35 to Genre.COMEDY,
        80 to Genre.CRIME,
        99 to Genre.DOCUMENTARY,
        18 to Genre.DRAMA,
        10751 to Genre.FAMILY,
        14 to Genre.FANTASY,
        36 to Genre.HISTORY,
        27 to Genre.HORROR,
        10402 to Genre.MUSIC,
        9648 to Genre.MYSTERY,
        10749 to Genre.ROMANCE,
        878 to Genre.SCIENCE_FICTION,
        10770 to Genre.TV_MOVIE,
        53 to Genre.THRILLER,
        10752 to Genre.WAR,
        37 to Genre.WESTERN,
    )

/**
 * The app's genre for TMDB's [id], or null where the table does not carry it.
 */
internal fun tmdbGenre(id: Int): Genre? = TMDB_GENRES[id]

/**
 * The app's genres for [ids], in the order given.
 *
 * An id the table does not carry is dropped, so every genre a screen shows is one the filter sheet
 * also offers.
 */
internal fun tmdbGenres(ids: List<Int>): List<Genre> = ids.mapNotNull(::tmdbGenre)
```

- [ ] **Step 4: Run the test and confirm it passes**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbGenresTest*"`
Expected: PASS, three tests. A failure naming an id means TMDB sent a genre the table does not carry: add the constant to `Genre` in `:core:model` and the row here.

---

### Task 5: A path to an `ImageRef`

**Files:**
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/mapper/ImageMapper.kt`
- Test: `core/network/src/test/kotlin/com/shayan/amro/core/network/mapper/ImageMapperTest.kt`

**Interfaces:**
- Consumes: `ImageRef`, `ImageVariant` from Task 1.
- Produces: `internal fun posterRef(path: String?): ImageRef?` and `internal fun backdropRef(path: String?): ImageRef?`, plus the module's image host.

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/shayan/amro/core/network/mapper/ImageMapperTest.kt`:

```kotlin
package com.shayan.amro.core.network.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val POSTER_PATH = "/a-poster.jpg"
private const val BACKDROP_PATH = "/a-backdrop.jpg"
private const val BLANK_PATH = ""

private const val POSTER_VARIANTS = 6
private const val BACKDROP_VARIANTS = 3

private const val NARROWEST_POSTER_WIDTH = 92
private const val WIDEST_POSTER_WIDTH = 780
private const val NARROWEST_BACKDROP_WIDTH = 300
private const val WIDEST_BACKDROP_WIDTH = 1280

private const val ORIGINAL_SEGMENT = "original"

class ImageMapperTest {
    @Test
    fun `a poster path yields one variant per poster width`() {
        val poster = assertNotNull(posterRef(POSTER_PATH))

        assertEquals(POSTER_VARIANTS, poster.variants.size)
        assertEquals(NARROWEST_POSTER_WIDTH, poster.variants.first().widthPx)
        assertEquals(WIDEST_POSTER_WIDTH, poster.variants.last().widthPx)
    }

    @Test
    fun `a backdrop path yields one variant per backdrop width`() {
        val backdrop = assertNotNull(backdropRef(BACKDROP_PATH))

        assertEquals(BACKDROP_VARIANTS, backdrop.variants.size)
        assertEquals(NARROWEST_BACKDROP_WIDTH, backdrop.variants.first().widthPx)
        assertEquals(WIDEST_BACKDROP_WIDTH, backdrop.variants.last().widthPx)
    }

    @Test
    fun `a variant url is the host, the width and the path`() {
        val poster = assertNotNull(posterRef(POSTER_PATH))

        assertEquals(
            "${TMDB_IMAGE_BASE_URL}w$NARROWEST_POSTER_WIDTH$POSTER_PATH",
            poster.variants.first().url,
        )
    }

    @Test
    fun `variants are ordered ascending by width`() {
        val poster = assertNotNull(posterRef(POSTER_PATH))

        assertEquals(poster.variants.sortedBy { it.widthPx }, poster.variants)
    }

    @Test
    fun `no variant is the width-less original`() {
        val poster = assertNotNull(posterRef(POSTER_PATH))

        assertTrue(poster.variants.none { it.url.contains(ORIGINAL_SEGMENT) })
    }

    @Test
    fun `an absent path yields no image`() {
        assertNull(posterRef(null))
        assertNull(backdropRef(null))
    }

    @Test
    fun `a blank path yields no image`() {
        assertNull(posterRef(BLANK_PATH))
        assertNull(backdropRef(BLANK_PATH))
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*ImageMapperTest*"`
Expected: FAIL, compilation error, `posterRef` and `backdropRef` unresolved.

- [ ] **Step 3: Write the mapper**

`core/network/src/main/kotlin/com/shayan/amro/core/network/mapper/ImageMapper.kt`:

```kotlin
package com.shayan.amro.core.network.mapper

import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.ImageVariant

/** Where TMDB serves image files, which is a different host from the API. */
internal const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

/**
 * The poster widths TMDB serves, ascending.
 *
 * `original` is left out: it publishes no width, so nothing can pick it from a layout constraint,
 * and nothing should request it.
 */
private val POSTER_WIDTHS = listOf(92, 154, 185, 342, 500, 780)

/** The backdrop widths TMDB serves, ascending. `original` is left out for the same reason. */
private val BACKDROP_WIDTHS = listOf(300, 780, 1280)

/** The renditions of the poster at [path], or null where the source offers none. */
internal fun posterRef(path: String?): ImageRef? = imageRef(path, POSTER_WIDTHS)

/** The renditions of the backdrop at [path], or null where the source offers none. */
internal fun backdropRef(path: String?): ImageRef? = imageRef(path, BACKDROP_WIDTHS)

/**
 * One URL per width for the image at [path].
 *
 * The host and the width vocabulary stay here, so a layer that knows its layout constraints picks
 * a width without knowing which provider produced the URLs.
 */
private fun imageRef(
    path: String?,
    widths: List<Int>,
): ImageRef? {
    if (path.isNullOrBlank()) return null
    return ImageRef(widths.map { width -> ImageVariant(width, "${TMDB_IMAGE_BASE_URL}w$width$path") })
}
```

- [ ] **Step 4: Run the test and confirm it passes**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*ImageMapperTest*"`
Expected: PASS, seven tests.

---

### Task 6: The wire shapes and the normalisation

**Files:**
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/dto/MovieSummaryDto.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/dto/MovieDetailDto.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/dto/TrendingPageDto.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/mapper/MovieMapper.kt`
- Test: `core/network/src/test/kotlin/com/shayan/amro/core/network/mapper/MovieMapperTest.kt`

**Interfaces:**
- Consumes: `Movie`, `MovieDetail`, `MovieId`, `SourceId`, `Rating`, `ReleaseStatus` from Task 1; `fixture(path)` from Task 2; `tmdbGenre`, `tmdbGenres` from Task 4; `posterRef`, `backdropRef` from Task 5.
- Produces: `internal val TMDB_SOURCE: SourceId`, `internal fun MovieSummaryDto.toMovie(): Movie`, `internal fun MovieDetailDto.toMovieDetail(): MovieDetail`, and the three `@Serializable` DTOs `TrendingPageDto(page, totalPages, results)`, `MovieSummaryDto`, `MovieDetailDto`.

`TMDB_SOURCE` is declared beside the mapper rather than beside the data source, because the mapper is the only thing that builds a `MovieId`. The data source imports it for its `sourceId` and for the check on the id it is handed.

- [ ] **Step 1: Write the wire shapes**

Every field defaults, so a response missing one parses rather than throwing, and the mapper is the only thing deciding what an absent field means.

`core/network/src/main/kotlin/com/shayan/amro/core/network/dto/MovieSummaryDto.kt`:

```kotlin
package com.shayan.amro.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row of a trending page, as TMDB sends it. */
@Serializable
internal data class MovieSummaryDto(
    val id: Int,
    val title: String = "",
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)
```

`core/network/src/main/kotlin/com/shayan/amro/core/network/dto/MovieDetailDto.kt`:

```kotlin
package com.shayan.amro.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A genre on a detail response, which TMDB sends as an object rather than a bare id. */
@Serializable
internal data class GenreDto(
    val id: Int,
    val name: String = "",
)

/** One movie's full record, as TMDB sends it. */
@Serializable
internal data class MovieDetailDto(
    val id: Int,
    val title: String = "",
    val genres: List<GenreDto> = emptyList(),
    val popularity: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    val runtime: Int? = null,
    val status: String? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
)
```

`core/network/src/main/kotlin/com/shayan/amro/core/network/dto/TrendingPageDto.kt`:

```kotlin
package com.shayan.amro.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One page of trending movies, as TMDB sends it. */
@Serializable
internal data class TrendingPageDto(
    val page: Int,
    @SerialName("total_pages") val totalPages: Int? = null,
    val results: List<MovieSummaryDto> = emptyList(),
)
```

- [ ] **Step 2: Write the failing mapper test**

`core/network/src/test/kotlin/com/shayan/amro/core/network/mapper/MovieMapperTest.kt`:

```kotlin
package com.shayan.amro.core.network.mapper

import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.network.dto.MovieDetailDto
import com.shayan.amro.core.testing.fixture
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private const val COMPLETE_MOVIE_FIXTURE = "tmdb/movie/687163.json"
private const val MISSING_DATA_MOVIE_FIXTURE = "tmdb/movie/1294189.json"
private const val POST_PRODUCTION_MOVIE_FIXTURE = "tmdb/movie/1003596.json"

private const val EXPECTED_SOURCE = "tmdb"

private const val NOTHING_MISSING = """{"id":1,"tagline":"a tagline","budget":9,"revenue":8,"runtime":7,"vote_average":6.0,"vote_count":5,"imdb_id":"tt1","overview":"an overview","release_date":"2026-01-02"}"""
private const val EVERY_FIELD_ZERO = """{"id":1,"tagline":"a tagline","budget":0,"revenue":0,"runtime":0,"vote_average":0.0,"vote_count":0,"imdb_id":"tt1","overview":"an overview","release_date":"2026-01-02"}"""
private const val EVERY_FIELD_EMPTY = """{"id":1,"tagline":"","budget":0,"revenue":0,"runtime":0,"vote_average":0.0,"vote_count":0,"imdb_id":"","overview":"","release_date":""}"""
private const val EVERY_FIELD_ABSENT = """{"id":1}"""

private const val EXPECTED_RUNTIME_MINUTES = 7
private const val EXPECTED_BUDGET = 9L
private const val EXPECTED_REVENUE = 8L
private const val EXPECTED_VOTE_COUNT = 5

private val json = Json { ignoreUnknownKeys = true }

private fun detail(body: String) = json.decodeFromString<MovieDetailDto>(body).toMovieDetail()

class MovieMapperTest {
    @Test
    fun `the complete record maps with every field present`() {
        val movie = detail(fixture(COMPLETE_MOVIE_FIXTURE))

        assertEquals(EXPECTED_SOURCE, movie.movie.id.source.value)
        assertTrue(movie.movie.title.isNotBlank())
        assertTrue(movie.movie.genres.isNotEmpty())
        assertNotNull(movie.movie.releaseDate)
        assertNotNull(movie.movie.poster)
        assertNotNull(movie.backdrop)
        assertNotNull(movie.overview)
        assertNotNull(movie.tagline)
        assertNotNull(movie.runtime)
        assertNotNull(movie.budget)
        assertNotNull(movie.revenue)
        assertNotNull(movie.rating)
        assertNotNull(movie.imdbId)
        assertEquals(ReleaseStatus.RELEASED, movie.status)
    }

    @Test
    fun `the missing-data record maps with tagline, budget, revenue and rating all null`() {
        val movie = detail(fixture(MISSING_DATA_MOVIE_FIXTURE))

        assertNull(movie.tagline)
        assertNull(movie.budget)
        assertNull(movie.revenue)
        assertNull(movie.rating)
    }

    @Test
    fun `the post production record maps with no rating and its own status`() {
        val movie = detail(fixture(POST_PRODUCTION_MOVIE_FIXTURE))

        assertNull(movie.rating)
        assertEquals(ReleaseStatus.POST_PRODUCTION, movie.status)
    }

    @Test
    fun `a populated value survives the mapping`() {
        val movie = detail(NOTHING_MISSING)

        assertEquals(EXPECTED_RUNTIME_MINUTES.minutes, movie.runtime)
        assertEquals(EXPECTED_BUDGET, movie.budget)
        assertEquals(EXPECTED_REVENUE, movie.revenue)
        assertEquals(EXPECTED_VOTE_COUNT, assertNotNull(movie.rating).count)
    }

    @Test
    fun `a zero says nothing`() {
        val movie = detail(EVERY_FIELD_ZERO)

        assertNull(movie.budget)
        assertNull(movie.revenue)
        assertNull(movie.runtime)
        assertNull(movie.rating)
    }

    @Test
    fun `an empty string says nothing`() {
        val movie = detail(EVERY_FIELD_EMPTY)

        assertNull(movie.tagline)
        assertNull(movie.overview)
        assertNull(movie.imdbId)
        assertNull(movie.movie.releaseDate)
    }

    @Test
    fun `an absent field says nothing`() {
        val movie = detail(EVERY_FIELD_ABSENT)

        assertNull(movie.tagline)
        assertNull(movie.overview)
        assertNull(movie.imdbId)
        assertNull(movie.movie.releaseDate)
        assertNull(movie.budget)
        assertNull(movie.revenue)
        assertNull(movie.runtime)
        assertNull(movie.rating)
        assertNull(movie.movie.poster)
        assertNull(movie.backdrop)
    }

    @Test
    fun `a status the app does not carry becomes unknown`() {
        assertEquals(ReleaseStatus.UNKNOWN, detail(EVERY_FIELD_ABSENT).status)
    }
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*MovieMapperTest*"`
Expected: FAIL, compilation error, `toMovieDetail` unresolved.

- [ ] **Step 4: Write the mapper**

`core/network/src/main/kotlin/com/shayan/amro/core/network/mapper/MovieMapper.kt`:

```kotlin
package com.shayan.amro.core.network.mapper

import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.model.SourceId
import com.shayan.amro.core.network.dto.MovieDetailDto
import com.shayan.amro.core.network.dto.MovieSummaryDto
import com.shayan.amro.core.network.tmdbGenre
import com.shayan.amro.core.network.tmdbGenres
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

/** The source every movie this module produces is issued by. */
internal val TMDB_SOURCE = SourceId("tmdb")

/** TMDB's release status words paired with the app's own status. */
private val TMDB_STATUSES: Map<String, ReleaseStatus> =
    mapOf(
        "Rumored" to ReleaseStatus.RUMORED,
        "Planned" to ReleaseStatus.PLANNED,
        "In Production" to ReleaseStatus.IN_PRODUCTION,
        "Post Production" to ReleaseStatus.POST_PRODUCTION,
        "Released" to ReleaseStatus.RELEASED,
        "Canceled" to ReleaseStatus.CANCELED,
    )

/**
 * A trending row as the app's own record.
 *
 * Genres resolve here, so no caller receives a half-populated movie. The id is paired with
 * [TMDB_SOURCE], which is what stops a second source's ids colliding with these.
 */
internal fun MovieSummaryDto.toMovie(): Movie =
    Movie(
        id = MovieId(TMDB_SOURCE, id.toString()),
        title = title,
        genres = tmdbGenres(genreIds),
        popularity = popularity,
        releaseDate = releaseDate.toLocalDateOrNull(),
        poster = posterRef(posterPath),
    )

/**
 * A detail response as the app's own record.
 *
 * TMDB says "no value" three ways, a zero, an empty string and an absent field, and all three
 * become null here, so nothing above this compares against a sentinel.
 */
internal fun MovieDetailDto.toMovieDetail(): MovieDetail =
    MovieDetail(
        movie =
            Movie(
                id = MovieId(TMDB_SOURCE, id.toString()),
                title = title,
                genres = genres.mapNotNull { tmdbGenre(it.id) },
                popularity = popularity,
                releaseDate = releaseDate.toLocalDateOrNull(),
                poster = posterRef(posterPath),
            ),
        overview = overview?.ifBlank { null },
        tagline = tagline?.ifBlank { null },
        backdrop = backdropRef(backdropPath),
        runtime = runtime?.takeIf { it > 0 }?.minutes,
        status = TMDB_STATUSES[status] ?: ReleaseStatus.UNKNOWN,
        budget = budget?.takeIf { it > 0 },
        revenue = revenue?.takeIf { it > 0 },
        rating = rating(),
        imdbId = imdbId?.ifBlank { null },
    )

/**
 * The rating, or null where nothing has voted.
 *
 * No rating at all rather than a zero, which is what keeps the detail screen's omission rule out
 * of the detail screen.
 */
private fun MovieDetailDto.rating(): Rating? {
    val count = voteCount ?: return null
    if (count <= 0) return null
    val average = voteAverage ?: return null
    return Rating(average, count)
}

/** TMDB sends an absent date as an empty string, which is not a date. */
private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() }
```

- [ ] **Step 5: Run the test and confirm it passes**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*MovieMapperTest*"`
Expected: PASS, eight tests.

- [ ] **Step 6: Verify the module so far**

Run: `./scripts/format.sh && ./gradlew :core:network:testDebugUnitTest`
Expected: PASS. Leave the changes in the working tree.

---

### Task 7: The source contract and its TMDB implementation

**Files:**
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/NetworkResult.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/MoviePage.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/MovieRemoteDataSource.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/ErrorMapping.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbFacts.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbRemoteDataSource.kt`
- Test: `core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbRemoteDataSourceTest.kt`

**Interfaces:**
- Consumes: `DataError`, `Movie`, `MovieDetail`, `MovieId`, `SourceId` from Task 1; `fixture(path)` from Task 2; `tmdbHttpClient`, `TmdbConfig` from Task 3; `TMDB_SOURCE`, `toMovie`, `toMovieDetail` and the DTOs from Task 6.
- Produces: `sealed interface NetworkResult<out T>` with `Success<T>(value)` and `Failure(error)`; `MoviePage(movies, page, totalPages)`; `interface MovieRemoteDataSource` with `sourceId`, `trendingPage(page)`, `movieDetail(id)`; `internal class TmdbRemoteDataSource @Inject constructor(client: HttpClient)`; `const val TMDB_PAGE_SIZE`.

- [ ] **Step 1: Write the contract**

`core/network/src/main/kotlin/com/shayan/amro/core/network/NetworkResult.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError

/**
 * What one call to a source ends in.
 *
 * A failure is a returned value rather than a thrown exception, which puts the failure in the
 * signature and gives the caller an exhaustive `when`. That is what lets a repository fetching
 * several pages keep the ones that already succeeded.
 *
 * It lives beside the source rather than on the model, because it describes a call rather than a
 * movie.
 */
sealed interface NetworkResult<out T> {
    /** The call answered with [value]. */
    data class Success<T>(val value: T) : NetworkResult<T>

    /** The call failed, for one of the three causes the product renders. */
    data class Failure(val error: DataError) : NetworkResult<Nothing>
}
```

`core/network/src/main/kotlin/com/shayan/amro/core/network/MoviePage.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.Movie

/**
 * One page of movies as a source returned it.
 *
 * @property movies the page's rows, in the order the source served them.
 * @property page which page this is.
 * @property totalPages null where the source publishes no total. It is not universal: one source
 *   reports a total, another ends its paging with an empty page, so a caller stops on enough
 *   distinct movies, an empty page, or its own cap, never on a total alone.
 */
data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int?,
)
```

`core/network/src/main/kotlin/com/shayan/amro/core/network/MovieRemoteDataSource.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.SourceId

/**
 * One remote source of movies.
 *
 * The source knows how one page is requested and how large it is; a caller decides how many pages
 * to ask for and when to stop. That split is what keeps a fetch policy free of any provider's
 * pagination.
 */
interface MovieRemoteDataSource {
    /** Which source this is, and therefore which id space its movies belong to. */
    val sourceId: SourceId

    /**
     * The trending movies on page [page], counted from one.
     *
     * @return the page, or the cause it failed for.
     */
    suspend fun trendingPage(page: Int): NetworkResult<MoviePage>

    /**
     * The full record behind [id].
     *
     * @param id an id this source issued. An id from another source is a programming error, not a
     *   failure to render.
     * @return the record, or the cause it failed for.
     */
    suspend fun movieDetail(id: MovieId): NetworkResult<MovieDetail>
}
```

- [ ] **Step 2: Write the failing data source test**

`core/network/src/test/kotlin/com/shayan/amro/core/network/TmdbRemoteDataSourceTest.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.SourceId
import com.shayan.amro.core.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val TRENDING_PAGE_1_FIXTURE = "tmdb/trending/page-1.json"
private const val COMPLETE_MOVIE_FIXTURE = "tmdb/movie/687163.json"
private const val UNAUTHORISED_FIXTURE = "tmdb/error/401.json"
private const val SERVER_ERROR_FIXTURE = "tmdb/error/500.json"

private const val TEST_BASE_URL = "https://api.example/3/"
private const val TEST_READ_ACCESS_TOKEN = "a-read-access-token"
private const val JSON_CONTENT_TYPE = "application/json"

private const val COMPLETE_MOVIE_ID = "687163"
private const val EXPECTED_SOURCE = "tmdb"
private const val ANOTHER_SOURCE = "another-source"

private const val FIRST_PAGE = 1
private const val TOTAL_PAGES_KEY = "total_pages"

private const val EMPTY_RESULTS = """{"page":1,"total_pages":500,"results":[]}"""
private const val MALFORMED_BODY = """{"page":"not a number"}"""

private const val TRENDING_PATH_SEGMENT = "trending/movie/week"
private const val PAGE_QUERY = "page=1"

private val TEST_CONFIG =
    TmdbConfig(
        baseUrl = TEST_BASE_URL,
        readAccessToken = TEST_READ_ACCESS_TOKEN,
    )

private val COMPLETE_MOVIE = MovieId(SourceId(EXPECTED_SOURCE), COMPLETE_MOVIE_ID)
private val FOREIGN_MOVIE = MovieId(SourceId(ANOTHER_SOURCE), COMPLETE_MOVIE_ID)

private fun sourceRespondingWith(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    record: (HttpRequestData) -> Unit = {},
): TmdbRemoteDataSource {
    val engine =
        MockEngine { request ->
            record(request)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, JSON_CONTENT_TYPE),
            )
        }
    return TmdbRemoteDataSource(tmdbHttpClient(engine, TEST_CONFIG))
}

private fun sourceFailingWith(failure: Throwable): TmdbRemoteDataSource {
    val engine = MockEngine { throw failure }
    return TmdbRemoteDataSource(tmdbHttpClient(engine, TEST_CONFIG))
}

class TmdbRemoteDataSourceTest {
    @Test
    fun `a page maps its movies, its page number and its total`() =
        runTest {
            val body = fixture(TRENDING_PAGE_1_FIXTURE)
            val recordedTotal = Json.parseToJsonElement(body).jsonObject[TOTAL_PAGES_KEY]!!.jsonPrimitive.int

            val result = sourceRespondingWith(body).trendingPage(FIRST_PAGE)

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(TMDB_PAGE_SIZE, page.movies.size)
            assertEquals(FIRST_PAGE, page.page)
            assertEquals(recordedTotal, page.totalPages)
        }

    @Test
    fun `the trending request names the page it wants`() =
        runTest {
            var sent: HttpRequestData? = null

            sourceRespondingWith(fixture(TRENDING_PAGE_1_FIXTURE)) { sent = it }
                .trendingPage(FIRST_PAGE)

            val url = sent!!.url.toString()
            assertTrue(url.contains(TRENDING_PATH_SEGMENT))
            assertTrue(url.contains(PAGE_QUERY))
        }

    @Test
    fun `a detail request names the movie it wants`() =
        runTest {
            var sent: HttpRequestData? = null

            sourceRespondingWith(fixture(COMPLETE_MOVIE_FIXTURE)) { sent = it }
                .movieDetail(COMPLETE_MOVIE)

            assertTrue(sent!!.url.toString().contains(COMPLETE_MOVIE_ID))
        }

    @Test
    fun `a detail response maps to the app's record`() =
        runTest {
            val result = sourceRespondingWith(fixture(COMPLETE_MOVIE_FIXTURE)).movieDetail(COMPLETE_MOVIE)

            val detail = assertIs<NetworkResult.Success<MovieDetail>>(result).value
            assertEquals(COMPLETE_MOVIE, detail.movie.id)
        }

    @Test
    fun `the source names itself`() {
        assertEquals(SourceId(EXPECTED_SOURCE), sourceRespondingWith(EMPTY_RESULTS).sourceId)
    }

    @Test
    fun `an id from another source is a programming error, not a failure to render`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                sourceRespondingWith(fixture(COMPLETE_MOVIE_FIXTURE)).movieDetail(FOREIGN_MOVIE)
            }
        }

    @Test
    fun `a dropped connection is no connectivity`() =
        runTest {
            val result = sourceFailingWith(IOException()).trendingPage(FIRST_PAGE)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a server error is a server failure`() =
        runTest {
            val result =
                sourceRespondingWith(fixture(SERVER_ERROR_FIXTURE), HttpStatusCode.InternalServerError)
                    .trendingPage(FIRST_PAGE)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a rejected credential is a server failure`() =
        runTest {
            val result =
                sourceRespondingWith(fixture(UNAUTHORISED_FIXTURE), HttpStatusCode.Unauthorized)
                    .trendingPage(FIRST_PAGE)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a movie that is not there is a server failure`() =
        runTest {
            val result =
                sourceRespondingWith(fixture(SERVER_ERROR_FIXTURE), HttpStatusCode.NotFound)
                    .movieDetail(COMPLETE_MOVIE)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a body that will not parse is a server failure`() =
        runTest {
            val result = sourceRespondingWith(MALFORMED_BODY).trendingPage(FIRST_PAGE)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a successful response with no results is an empty response`() =
        runTest {
            val result = sourceRespondingWith(EMPTY_RESULTS).trendingPage(FIRST_PAGE)

            assertEquals(DataError.EmptyResponse, assertIs<NetworkResult.Failure>(result).error)
        }
}
```

- [ ] **Step 3: Run it and confirm it fails**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbRemoteDataSourceTest*"`
Expected: FAIL, compilation error, `TmdbRemoteDataSource` and `TMDB_PAGE_SIZE` unresolved.

- [ ] **Step 4: Write the error mapping**

This is the only place a failure becomes a `DataError`.

`core/network/src/main/kotlin/com/shayan/amro/core/network/ErrorMapping.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

/**
 * A 2xx whose results array was empty.
 *
 * It belongs to the trending call: a detail response carries no such array, so that call fails
 * only on transport and status.
 */
internal class EmptyResponseException : Exception()

/**
 * The cause a failure is rendered as.
 *
 * A request that never reached a server is no connectivity. Everything else, a non-2xx status and
 * a body that will not parse alike, is the API failing to answer. A movie that is not there is
 * that same failure rather than a fourth thing to render.
 */
internal fun Throwable.toDataError(): DataError =
    when (this) {
        is EmptyResponseException -> DataError.EmptyResponse
        is UnresolvedAddressException, is HttpRequestTimeoutException, is IOException -> DataError.NoConnectivity
        else -> DataError.Server
    }

/**
 * Runs [call] and folds every way it can fail into a returned value.
 *
 * @return what [call] produced, or the cause it failed for.
 */
internal suspend fun <T> networkResult(call: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(call())
    } catch (cancellation: CancellationException) {
        // Cancellation is the caller leaving, not a failure to render.
        throw cancellation
    } catch (failure: Exception) {
        NetworkResult.Failure(failure.toDataError())
    }
```

- [ ] **Step 5: Write TMDB's own facts**

`core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbFacts.kt`:

```kotlin
package com.shayan.amro.core.network

/**
 * What TMDB does that a caller has to plan around.
 *
 * Its ranking shifts between requests, so a title on a page boundary is served twice and pages
 * must resolve in order: a caller stops on enough distinct movies rather than on a row count, and
 * concurrent requests would multiply the duplicates rather than avoid them.
 */

/**
 * How many movies one page carries.
 *
 * Not configurable. Seven undocumented size parameters were probed against the live endpoint and
 * every one was ignored.
 */
const val TMDB_PAGE_SIZE = 20
```

- [ ] **Step 6: Write the data source**

`core/network/src/main/kotlin/com/shayan/amro/core/network/TmdbRemoteDataSource.kt`:

```kotlin
package com.shayan.amro.core.network

import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.SourceId
import com.shayan.amro.core.network.dto.MovieDetailDto
import com.shayan.amro.core.network.dto.TrendingPageDto
import com.shayan.amro.core.network.mapper.TMDB_SOURCE
import com.shayan.amro.core.network.mapper.toMovie
import com.shayan.amro.core.network.mapper.toMovieDetail
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

private const val TRENDING_PATH = "trending/movie/week"
private const val MOVIE_PATH = "movie"
private const val PAGE_PARAMETER = "page"

/**
 * TMDB as a source of movies, and the only place its endpoints are named.
 *
 * The client is injected rather than built here, so the engine underneath it is a test's choice,
 * and it already carries the address and the credential.
 *
 * @param client the configured client every call goes through.
 */
internal class TmdbRemoteDataSource
    @Inject
    constructor(
        private val client: HttpClient,
    ) : MovieRemoteDataSource {
        override val sourceId: SourceId = TMDB_SOURCE

        override suspend fun trendingPage(page: Int): NetworkResult<MoviePage> =
            networkResult {
                val response: TrendingPageDto =
                    client
                        .get(TRENDING_PATH) { parameter(PAGE_PARAMETER, page) }
                        .body()
                if (response.results.isEmpty()) throw EmptyResponseException()
                MoviePage(
                    movies = response.results.map { it.toMovie() },
                    page = response.page,
                    totalPages = response.totalPages,
                )
            }

        override suspend fun movieDetail(id: MovieId): NetworkResult<MovieDetail> {
            // Outside the folding below, so it surfaces as the programming error it is instead of
            // being rendered as one of the three causes.
            require(id.source == TMDB_SOURCE) { "$id was not issued by $TMDB_SOURCE" }
            return networkResult {
                val response: MovieDetailDto = client.get("$MOVIE_PATH/${id.value}").body()
                response.toMovieDetail()
            }
        }
    }
```

`TmdbRemoteDataSource` is `internal`, so the test in the same module constructs it directly and no module above sees the name.

The trending call is the only one that can end in `EmptyResponse`: its response carries a results array, and a detail response does not.

- [ ] **Step 7: Run the test and confirm it passes**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*TmdbRemoteDataSourceTest*"`
Expected: PASS, twelve tests.

- [ ] **Step 8: Verify the module**

Run: `./scripts/format.sh && ./gradlew :core:network:testDebugUnitTest`
Expected: PASS. Leave the changes in the working tree.

---

### Task 8: Wiring the source into the graph

**Files:**
- Modify: `core/network/build.gradle.kts`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/di/TmdbClientModule.kt`
- Create: `core/network/src/main/kotlin/com/shayan/amro/core/network/di/MovieRemoteDataSourceModule.kt`
- Modify: `app/src/androidTest/kotlin/com/shayan/amro/di/DependencyGraphTest.kt`
- Modify: `.claude/skills/build-logic/SKILL.md`

**Interfaces:**
- Consumes: `tmdbHttpClient`, `TmdbConfig` from Task 3; `MovieRemoteDataSource`, `TmdbRemoteDataSource` from Task 7.
- Produces: a singleton `HttpClient` and a `MovieRemoteDataSource` binding in the Hilt graph.

`TmdbConfig` and the `:app` module that provides it are unchanged, so nothing in `:app` or in the device test's local server needs touching.

- [ ] **Step 1: Give `:core:network` Hilt**

Add the two plugins and the two dependencies. `core/network/build.gradle.kts` keeps everything it already has, with these additions:

```kotlin
plugins {
    alias(libs.plugins.amro.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
```

```kotlin
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
```

- [ ] **Step 2: Provide the client**

`core/network/src/main/kotlin/com/shayan/amro/core/network/di/TmdbClientModule.kt`:

```kotlin
package com.shayan.amro.core.network.di

import com.shayan.amro.core.network.TmdbConfig
import com.shayan.amro.core.network.tmdbHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/**
 * The client every TMDB call is made through, and the engine underneath it.
 *
 * The engine is a binding of its own so the image loader can later share one connection pool with
 * the API calls.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TmdbClientModule {
    @Provides
    @Singleton
    fun provideEngine(): HttpClientEngine = OkHttp.create()

    @Provides
    @Singleton
    fun provideHttpClient(
        engine: HttpClientEngine,
        config: TmdbConfig,
    ): HttpClient = tmdbHttpClient(engine, config)
}
```

- [ ] **Step 3: Bind the contract**

`core/network/src/main/kotlin/com/shayan/amro/core/network/di/MovieRemoteDataSourceModule.kt`:

```kotlin
package com.shayan.amro.core.network.di

import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.TmdbRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The source contract bound to its TMDB implementation.
 *
 * The binding is what keeps the provider's name out of every module above this one: a repository
 * asks for the contract.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MovieRemoteDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindMovieRemoteDataSource(source: TmdbRemoteDataSource): MovieRemoteDataSource
}
```

- [ ] **Step 4: Assert the new binding resolves**

`app/src/androidTest/kotlin/com/shayan/amro/di/DependencyGraphTest.kt`, adding one injection and one assertion. Device test method names are `camelCase`.

Add to the imports:

```kotlin
import com.shayan.amro.core.network.MovieRemoteDataSource
```

Add the field:

```kotlin
    @Inject
    lateinit var movieRemoteDataSource: MovieRemoteDataSource
```

Add two assertions inside `everyStartupBindingResolves`, the second proving the contract resolved to the source that issues these ids:

```kotlin
        assertNotNull(movieRemoteDataSource)
        assertEquals(TMDB_SOURCE_NAME, movieRemoteDataSource.sourceId.value)
```

with the constant beside `BASE_URL_SUFFIX`, and `assertEquals` imported from `kotlin.test`:

```kotlin
private const val TMDB_SOURCE_NAME = "tmdb"
```

- [ ] **Step 5: Record that `:core:network` now carries Hilt**

`.claude/skills/build-logic/SKILL.md`, in the module table, change the `:core:network` row's "Also declares" cell from `serialization, Ktor` to `serialization, Ktor, KSP, Hilt`.

- [ ] **Step 6: Verify the whole JVM gate**

Run: `./scripts/format.sh && ./scripts/unittest.sh`
Expected: PASS across every module, including the existing `:core:ui` tests and screenshot goldens.

- [ ] **Step 7: Verify the graph on a device**

Run: `./scripts/instrumented.sh`
Expected: PASS. If no device is attached, say so rather than reporting the step as done, and leave it for the user.

---

### Task 9: Bring the documents in line

The spec's own closing section names what this changes elsewhere. Leaving the documents describing a fetched genre list would leave the next spec brainstormed against a design that no longer exists.

**Files:**
- Modify: `docs/architecture.md`
- Modify: `docs/superpowers/specs/001-tech-stack.md`
- Modify: `docs/superpowers/specs/000-product-brief.md`

**Interfaces:**
- Consumes: everything Tasks 1 through 8 built.
- Produces: nothing code depends on.

- [ ] **Step 1: Rewrite the architecture doc's stale sections**

In `docs/architecture.md`:

- The module table's `:core:model` row: replace `MovieFilter`, `SortKey`, `SortDirection` mention only if they are still owned elsewhere; they are, so leave them, and add `MovieDetail`, `ImageRef`, `Rating`, `ReleaseStatus` and `SourceId` to the list of types.
- The `:core:database` row: drop "the genre alias table, and the relation query that returns a movie with its genres". The genres a movie carries are the app's own by the time they leave `:core:network`, so no table translates them.
- Delete the `## What the mapper normalises (TODO: remove this)` section. Its content is owned by the spec.
- Delete the `## Genres (TODO: remove this)` section. Replace it with a short section stating that `Genre` is a hardcoded enum the app owns, that each source maps its own vocabulary onto it inside that source's module, that an id the table does not carry is dropped, and that the genre endpoint is never called in production.
- In `## Loading the trending list (TODO: remove this)`, delete the paragraph beginning "The genre list is fetched once and stored." Leave the rest for the spec that owns the fetch policy.

- [ ] **Step 2: Correct the tech stack spec**

In `docs/superpowers/specs/001-tech-stack.md`, the Data table:

- The **Schema** row: drop the genre tables and the cross reference. It reads as movie summary and movie detail tables only.
- The **Genre resolution** row: replace it. The genre list is never fetched. `Genre` is an enum the app owns, and each source maps its own ids onto it in that source's module.

- [ ] **Step 3: Answer the brief's open question**

In `docs/superpowers/specs/000-product-brief.md`, section 9.3, **Q1** is answered: the join happens in the source's mapper, the 19-entry list is never fetched, and nothing about it is cached. Mark it answered in place rather than deleting it, so the question and its answer stay together.

- [ ] **Step 4: Review the prose**

Run the `prose-review` skill over the three edited Markdown files. Fix what it names.

- [ ] **Step 5: Verify**

Run: `./scripts/format.sh && ./scripts/unittest.sh`
Expected: PASS. Report what is ready and leave everything in the working tree for the user to commit.

---

## What this plan does not build

- `MovieFilter`, `SortKey` and `SortDirection`, which belong to the spec that owns filter and sort.
- Any repository, any Room entity, any screen. `MovieRemoteDataSource` has no consumer until the data layer exists, which is why Task 8's graph test injects it directly to prove the binding resolves.
