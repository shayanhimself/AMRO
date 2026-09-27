# 003: Models and network

`:core:model` and `:core:network`. The types every other module speaks in, the source contract the
data layer depends on, its TMDB implementation, and the recorded responses every layer above
asserts against.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

"Movie" here is the record the trending list renders. "Movie detail" is the fuller record behind
one. Neither is the trending screen's summary row.

---

## 1. `:core:model`

Pure Kotlin. One dependency, kotlinx-datetime for `LocalDate`, exposed as `api` because it is on
`Movie` and every consumer needs the type on its own classpath. No `android.*`.

```kotlin
enum class Genre {
    ACTION, ADVENTURE, ANIMATION, COMEDY, CRIME, DOCUMENTARY, DRAMA, FAMILY, FANTASY, HISTORY,
    HORROR, MUSIC, MYSTERY, ROMANCE, SCIENCE_FICTION, TV_MOVIE, THRILLER, WAR, WESTERN,
}

enum class ReleaseStatus {
    RUMORED, PLANNED, IN_PRODUCTION, POST_PRODUCTION, RELEASED, CANCELED, UNKNOWN,
}

data class ImageRef(
    val small: String,
    val large: String,
)

data class Rating(val average: Double, val count: Int?)

data class Movie(
    val id: String,
    val title: String,
    val genres: List<Genre>,
    val popularity: Double,
    val releaseDate: LocalDate?,
    val poster: ImageRef?,
)

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

sealed interface DataError {
    data object NoConnectivity : DataError
    data object Server : DataError
    data object EmptyResponse : DataError
}
```

**A movie's id is one opaque string.** It identifies the movie across every source, and how a
source is written into it is `:core:network`'s business alone. Every layer above passes an id back
down the way it came up.

**Every field on `Movie` has a consumer on the trending list**: title, genres and poster are
rendered, popularity, title and release date are the three sort keys. Anything the detail screen
alone renders sits on `MovieDetail`.

**A `Rating` is an average on a nought to ten scale, and the number of votes behind it where the
source publishes one.** A source scoring out of a hundred is normalised at its own mapper, the way
its genres and its image sizes are, so nothing above the source compares two scales. Four of the
twelve movie APIs surveyed publish a score with no vote count at all, so a null count means the
source does not report one, and the screen shows the average alone.

**No rating at all is a null `Rating`, and that is what a sentinel becomes.** TMDB says "unrated"
with an average of nought and a count of nought rather than with an absent field, so its mapper
turns that pair into null. A nought is otherwise a real score, so the decision belongs to the source
that knows its own conventions.

**`popularity` is an ordering, not a measurement.** Greater is more popular, and that is the whole
contract: the number has no unit and no scale, it is never displayed, and two sources' values are not
comparable. Each source maps whatever it publishes onto it, a score, a watcher count, or a position
in the list it returned. This is what the product asks of a trending source, and it is deliberately
the weakest thing that still delivers the default sort.

**Nullability is the missing-data rule made structural.** A field measured empty in a live top 100
is nullable, and absence is the only signal of it. `rating == null` is what omits the rating block,
`budget == null` is what reads "Not disclosed". No screen compares against zero or an empty string.

**An `ImageRef` names two sizes rather than pixel widths**, because a size is the most any source
agrees on. A size is relative to the image it belongs to, so a small poster and a small backdrop are
different widths. One type serves both: what differs between a portrait poster and a landscape
backdrop is the box the UI puts it in, not what it takes to load it. Several publish only named buckets and no dimensions, so a width would have to be
invented for them. Every field is a usable URL: a source offering fewer sizes repeats the one it
has, which keeps the fallback in the mapper instead of at every call site. A movie with no image
yields no `ImageRef` at all.

`SortKey`, `SortDirection` and `MovieSort` belong to the spec that owns filter and sort.

---

## 2. `:core:network`

### The contract

```kotlin
interface MovieRemoteDataSource {
    suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>>

    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>
    data class Failure(val error: DataError) : NetworkResult<Nothing>
}
```

A failure is a returned value, never a thrown exception. The caller gets an exhaustive `when` and
the failure is visible in the signature.

**A caller asks for a number of movies and gets distinct ones.** How many requests that took, how
large a page is, whether the provider repeats a title across a page boundary, and whether it pages
by number or by cursor are the implementation's business. The product wants exactly one hundred and
rules out paging beyond them, so a page number the caller chose would be ceremony, and with
deduplication underneath it would be worse: the raw pages that hold deduplicated rows 101 to 200
are not knowable in advance. A provider that serves the whole list in one call writes no loop; one
that does not absorbs its own quirk here rather than exporting it to every caller.

**A success is a whole answer, never a part of one.** The list never exceeds `count`. Fewer means
the source has no more to give or spent its own request budget, both of which are complete answers:
the count is honest rather than padded, and nothing appears on screen about it. A read that fails
is a `Failure` carrying no movies, even when earlier pages succeeded. A caller cannot tell a short
list from a truncated one, so a partial list returned as a success would be shown as though it were
everything the source had.

`NetworkResult` lives in this module rather than in `:core:model`, because it describes a call
rather than a movie.

### Inside the module

```
core/network/src/main/kotlin/com/shayan/amro/core/network/
    MovieRemoteDataSource.kt
    NetworkResult.kt
    ApiExceptionHandler.kt
    sources/
        MovieId.kt
        tmdb/
            TmdbConfig.kt
            TmdbHttpClient.kt
            TmdbRemoteDataSource.kt
            TmdbFacts.kt
            di/TmdbModule.kt
            dto/
            mapper/
    di/RemoteSourceModule.kt
```

The module root holds what every source shares: the contract, the types it returns, and the one
wrapper that turns a thrown transport failure into a returned one. Everything a single provider
knows sits under `sources/<name>`, its Hilt module included, and what every source needs to name
its own movies sits at the root of `sources/`.

| File | Holds |
|---|---|
| `MovieRemoteDataSource.kt` | The contract, with `NetworkResult.kt` the type it returns |
| `ApiExceptionHandler.kt` | `apiCall { }`, the only place a thrown failure becomes a `DataError`. It names no provider, because every source reached over HTTP reads a dropped connection the same way |
| `sources/MovieId.kt` | The grammar that writes a source and that source's id into one string and reads them back out. `internal`, so no layer above this module can take an id apart |
| `sources/tmdb/TmdbConfig.kt` | The read access token, the one thing about reaching TMDB that enters from the build rather than from the source |
| `sources/tmdb/TmdbHttpClient.kt` | The `HttpClient` built from that token and an injected engine, and `TMDB_BASE_URL`, the root it resolves every endpoint against |
| `sources/tmdb/TmdbRemoteDataSource.kt` | `GET trending/movie/week?page=n`, `GET movie/{id}`, and the name TMDB stamps its ids with |
| `sources/tmdb/TmdbFacts.kt` | The page size and the page cap, both `internal`: nothing outside the source counts pages |
| `sources/tmdb/dto/` | The `@Serializable` wire shapes, one file each. `TmdbGenreListDto` is among them but `@VisibleForTesting`: production never calls the genre endpoint |
| `sources/tmdb/mapper/` | `TmdbMovieMapper.kt` and `TmdbImageMapper.kt`, wire shape to model, and `TmdbGenres.kt`, TMDB's genre ids mapped onto `Genre` |
| `sources/tmdb/di/TmdbModule.kt` | Provides the config and the client. `internal`, like the rest of the source |
| `di/RemoteSourceModule.kt` | Binds which source answers `MovieRemoteDataSource`. The one public module, so a device test can uninstall this binding alone and put a fake behind the contract |

**A source is a directory.** It is added by creating one and removed by deleting one, and every file
in it is read without first deciding which of the files at the root belong to it. Nothing outside
`sources/tmdb` names TMDB.

Everything under `sources/` is `internal` but `TmdbConfig`, which the module's own Hilt module
provides. Nothing above this module names a provider: which source answers is decided by
`di/RemoteSourceModule.kt`, and a second source is composed behind that binding rather than
chosen above it. No feature, repository, or entity names one.

**The address is a constant, not a binding.** It sits beside the client that sends it, because
nothing varies it: no build type, no flavour, and no test. A test that wants a different server
hands the client a different engine, and a test that wants different movies binds a fake behind
`MovieRemoteDataSource`.

**The credential reaches a module through the `amro.api.tokens` convention plugin.** It carries every
source's credential, not only TMDB's: a module that applies it gets one `BuildConfig` field per
source, and a source adds itself by adding one entry to the plugin's list rather than by a plugin of
its own. A module that needs the TMDB credential applies it and reads
`BuildConfig.TMDB_READ_ACCESS_TOKEN`; nothing else names `local.properties` or an environment
variable. Two modules apply it: `:app`, because production needs the token at runtime on a device,
and `:core:network`, because the live check needs it.

The plugin reads `local.properties` first, so a developer sets it once, and each source's own
environment variable second, because that file is not in the repository and CI has only its
secrets. Either absent yields an empty string, so a clone without a token still builds: the app runs
and fails its requests, and the live check skips.

A plugin rather than a shared function, because a plugin id in a `plugins` block is a line a reader
can follow to the file that implements it. A function on the build classpath appears from nowhere
and navigates to nothing.

Whether the live check runs is a separate switch: without `-PliveApi` the build excludes it from
every test task by name, and with the flag it runs and the task is never up to date, because a live
check answered from a cached result is not one. Excluding it rather than letting it decide for
itself is what keeps a developer who has a token from making a network call on every `check`.

This puts the credential in a second compiled artifact, `:core:network`'s own `BuildConfig`, in the
module whose production code otherwise never sees one. It is the same secret already shipped in the
app, so nothing new is exposed, and one way of reading a build input beats two.

The client takes its engine as a parameter, which is what lets a test pass `MockEngine` and
production pass OkHttp. Ktor's calls are main-safe on that engine and parsing happens off the
caller's thread. It is configured with `expectSuccess = false`, because the status is read rather
than thrown; with `ignoreUnknownKeys`, so everything TMDB sends beyond the declared shapes is
dropped rather than failing the parse; with separate connect, socket and request timeouts, so a
server that accepts a connection and then stalls still fails; and with the `Authorization` header
redacted in the logging plugin, because it is on every request and logging is the one place that
would otherwise write it out. The production level logs no header at all, so the logger and the
level are parameters with production defaults: a test raises the level and reads what was written,
which is the only way the redaction can be observed.

`apiCall` rethrows `CancellationException` instead of mapping it. A screen that navigated away
cancels its scope, and a cancelled read is not a failure to render.

### Wire shapes

Only fields with a consumer are declared. The client ignores unknown keys, so everything else TMDB
sends (`adult`, `video`, `original_language`, production companies and the rest) never enters the
build.

```kotlin
@Serializable
internal data class TmdbTrendingPageDto(
    val results: List<TmdbMovieDto>,
)

@Serializable
internal data class TmdbMovieDto(
    val id: Long,
    val title: String,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
internal data class TmdbMovieDetailDto(
    val id: Long,
    val title: String,
    val overview: String? = null,
    val tagline: String? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val runtime: Int? = null,
    val status: String? = null,
    val budget: Long = 0,
    val revenue: Long = 0,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
internal data class TmdbGenreDto(
    val id: Int,
    val name: String,
)
```

`results`, `id` and `title` are the only fields with no default, because a response missing one of
them cannot become movies at all. It fails to parse and surfaces as `Server` rather than reaching a
screen as a blank row. Everything else has a default, so a sparse record parses and the mapper
decides what its absence means. The page number and total TMDB also sends have no consumer: the walk
stops on distinct movies held or on an empty page, which a total cannot tell it.

There is no error DTO. A non-2xx status is `Server` whatever body came with it, so nothing parses
one. The recorded error bodies exist to prove exactly that: the status decides, and an error body
never parses into a movie.

`release_date` is a `String` on the wire because TMDB sends an empty one for a film without a date.
The mapper parses it, and anything unparseable becomes a null `LocalDate` rather than a failure.

`runtime`, `budget` and `revenue` are integers TMDB defaults to zero rather than omitting, so zero is
how it says "unknown" for all three. The normalisation turns each of them into null, which is what
removes the runtime row and reads "Not disclosed" for the money.

`TmdbGenreListDto` is the one wire shape production never decodes, because production never calls
the genre endpoint. It sits with the others, marked `@VisibleForTesting`, and only the two tests that
check the genre table read it: one against the recording, one against the live list.

### The TMDB source

`TmdbRemoteDataSource` is the contract's only implementation, and the only place TMDB's endpoints
are named.

```kotlin
internal const val TMDB_SOURCE = "tmdb"

internal class TmdbRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) : MovieRemoteDataSource {
    override suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>>

    override suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}
```

`trending` walks `trending/movie/week` from page one, with `page` as the only query parameter, until
it holds `count` distinct movies. No `language` is sent, so TMDB answers in its `en-US` default,
which is the only language the product ships. Every documented and undocumented page size parameter
is ignored by TMDB, so twenty per page is a fact the walk works with rather than a default it can
override, and it stays `internal`.

**The walk keeps going on distinct movies held, not on pages fetched.** TMDB re-ranks between
requests, so a movie can arrive on two sequential pages while another is missed. A repeat is dropped
where it lands, by id. The walk ends on the count, on an empty page, or on the page cap of ten, and
returns what it holds trimmed to the count. Spending the cap is not a failure.

A page that fails fails the whole walk, discarding the pages read before it. An empty
first page is `EmptyResponse`; an empty later page is TMDB reaching the end of what it will serve,
and ends the walk with what it holds.

`movieDetail` reads TMDB's own id back out of the id it is handed, and resolves `movie/{id}` from
that. Every movie either call returns carries an id built from `TMDB_SOURCE` and the id in the
response, which is what stops a second source's ids colliding with these. Nothing routes on that
source yet, so no source checks the ids it is handed: the contract exposes no identity and a source
reads the id it is given. The check belongs with whatever first holds more than one source.

`EmptyResponse` belongs to the trending call, whose response carries a results array that can come
back empty, and only to its first page. A detail response has no such array, so that call fails only
on transport and status.

### Mapping

Two files, both a set of `internal` extension functions on the wire shapes rather than a class.
Mapping needs nothing injected, so a test decodes a fixture and calls one directly.

```kotlin
internal fun TmdbMovieDto.toMovie(): Movie
internal fun TmdbMovieDetailDto.toMovieDetail(): MovieDetail

internal fun posterRef(path: String?): ImageRef?
internal fun backdropRef(path: String?): ImageRef?
```

`TmdbMovieMapper.kt` owns everything that turns one of TMDB's records into one of the app's: the genre
lookup, the date parse, the release status table, the rating rule, and the normalisation below. Both
mappers that build a `Movie` build the same id from `TMDB_SOURCE` and the id in the response.

`TmdbImageMapper.kt` owns the host, the size vocabulary, and the width each size resolves to for a poster
and for a backdrop. It is the only file that knows an image is a path rather than a URL. A poster and
a backdrop differ only in their width table, so one private builder serves both and an absent or
blank path returns null from either.

The direction is one way. The app never writes to a provider, so no model type has a mapping back to
a wire shape, and a DTO is never constructed outside a test.

TMDB's release statuses are matched by their exact strings, `Released` and `Post Production` among
them. A status the table does not carry becomes `UNKNOWN`, which omits the row rather than printing
a raw token.

### Genres

TMDB's ids map onto `Genre` through a table in this module. An id the table does not carry is
dropped, so every chip on a screen is one the filter sheet also offers.

**The genre endpoint is not called in production.** Its response is recorded as a fixture, as
evidence for the test that proves the table is total over what TMDB sends.

`Genre` and `ReleaseStatus` are identifiers, not copy. The UI resolves each constant to a string
resource, so no provider's English reaches a screen and a translator can reach both. The status a
screen shows still reads as the provider words it, "Released" and "Post Production".

### Images

| Size | Poster | Backdrop | Rendered by |
|---|---|---|---|
| `small` | `w342` | `w300` | a list row, and the detail header's poster |
| `large` | `w500` | `w1280` | the backdrop behind the detail header |

Two sizes because the design has exactly three image placements: a poster in a list row, a poster
card on the detail screen, and one full-width backdrop behind it. A third bucket would have no
caller.

The two poster placements share one size. They are 76dp and 92dp, so the width that covers the
wider of them covers both, and `w342` is the step above what a 92dp tile asks for at the densities
phones ship at. Sharing it is what puts the tile the user tapped on the detail screen without a
request, on the one state that has nothing else to show, and what stops the tile reloading when the
record lands. A poster's `large` is what an image placement wider than the header's would read.

`original` is excluded: it publishes no width, and nothing should request it. TMDB rejects a width
outside its published set with a 400, so the sizes are named here rather than computed.

The host and the size vocabulary stay in this module, so `:core:ui` asks for the size its layout
calls for without knowing which provider produced the URL.

TMDB's poster path carries no aspect ratio, and its posters are not uniformly 2:3. Layouts fix the
ratio and crop, which is the only option available from a path alone.

A backdrop is nullable because a third of the movie APIs surveyed have no such image at all, and
those that do often mark it optional. TMDB supplied one for every movie in the recorded sample, so
the branch is one the detail screen has to design for rather than one it will meet in testing.

### Normalisation

TMDB says "no value" three ways: a zero, an empty string, and an absent field. The mapper turns all
three into `null`. A `vote_count` of zero yields no `Rating` at all, which is what keeps the detail
screen's omission rule out of the detail screen.

### Ordering

TMDB publishes `popularity` on both its list rows and its detail records, and the mapper passes it
through. A row that omits it sorts last, and is still shown.

**A row's position is not a substitute for its score.** The two order the list differently: across
the recorded pages the `popularity` field breaks descending order 64 times in 120 rows, and the row
TMDB ranks first sits around tenth by score. Standing a position in for a missing score would
therefore mix two orderings in one list and place a row wherever the wrong one put it. A source that
publishes no score at all maps its position onto `popularity` for every row, which is one ordering
uniformly applied and is what the product requires of it. Doing that for some rows and not others is
what this rules out.

**Sorting last is chosen over failing the page.** `popularity` is never rendered, it only picks a
position, and a page that fails takes its whole walk with it. Refusing a row over a field nobody
sees would trade a hundred movies for one absent number.

### The three causes

| Condition | `DataError` |
|---|---|
| `IOException`, unresolved host, connect or socket timeout | `NoConnectivity` |
| Any non-2xx status, and a body that fails to parse | `Server` |
| A 2xx whose results array is empty, on the first page | `EmptyResponse` |

A 404 on a movie detail is `Server`. Three causes are what the product allows, and a missing movie
is the API failing to answer rather than a fourth thing to render.

An unresolved host is named separately from `IOException`, because the JDK does not report it as
one: it surfaces as `UnresolvedAddressException`, which extends `IllegalArgumentException`. Without
that branch a device with no DNS reads as a server fault rather than as offline.

---

## 3. Fixtures

Recorded provider responses, under `core/network/src/test/resources/fixtures/tmdb/`. They stay in
the module that talks to the provider: nothing above it arranges its data as a provider's JSON.

A test never names a path. `:core:network` publishes a `TmdbFixture` enum to its own tests, one
entry per recording with a line saying what shape it holds, and `TmdbFixture.SPARSE_MOVIE.json` is
the body. The set is what a reader browses to pick a shape, a renamed recording breaks one file
rather than every test that read it, and no test can name a recording that was never made. A test
beside it asserts every entry resolves, which is what makes that last claim true rather than hoped
for.

| Path | Is |
|---|---|
| `trending/page-1.json` to `page-6.json` | Six sequential pages, recorded verbatim |
| `trending/overlap/` | Two pages assembled from recorded rows, repeating three across the boundary: 37 distinct in 40 rows |
| `genres.json` | The 19 genres, evidence for the totality test |
| `movie/released.json` | Every field populated |
| `movie/in-production.json` | Empty tagline, zero budget, zero revenue, zero votes |
| `movie/post-production.json` | Post Production: no financials, no votes |
| `error/401.json`, `error/404.json` | TMDB's own error bodies, from an invalid token and an id it does not issue |

`scripts/record-fixtures.py` re-records everything except the assembled pages. Python 3 standard
library only, token read from `local.properties`, refuses to run without one, writes each body
verbatim, and reports what changed.

The assembled pages are checked in and never re-recorded, because a recording only reproduces
duplicate ids on a day TMDB happens to re-rank mid-sequence. Every row in them is a real recorded
row. `README_TMDB_FIXTURES.md` beside the recordings says what each file is and how the assembled pages were
built, so the answer lives with the files rather than in this spec alone.

There is no recorded 500. A healthy API cannot be asked for one, so that status is a value the test
engine returns, with no body to parse. That is the point the fixtures make about every error status:
the status decides, and an error body never becomes a movie.

---

## 4. Tests

All of them run on the JVM against `MockEngine` and the recorded fixtures. None needs a network, a
clock or a device.

| Test | Fails when |
|---|---|
| The complete record maps with every field present | a mapping is wrong on the easy case |
| The missing-data record maps with tagline, budget, revenue and rating all null | the omission rule breaks on the record the brief names |
| The Post Production record maps with no rating and `POST_PRODUCTION` | zero votes reach a screen as a rating |
| Each nullable field maps to null from a zero, an empty string and an absent field | one of the three ways of saying nothing gets through |
| A response with votes maps to a `Rating` carrying its count, and one with zero votes maps to no `Rating` | an unrated film renders as though it scored nought |
| A walk over the recorded pages stops once it holds the hundred that were asked for | the source returns more or fewer than it was asked for |
| The assembled boundary pages yield 37 movies, each id once | a movie served on two pages reaches a screen twice |
| A walk to a hundred asks for pages one to six, in order | the walk skips, repeats or reorders a request |
| A source that repeats one page forever is stopped by the cap, with one page of movies | a pathological response loops, or spending the budget reads as an error |
| A page that fails after the first fails the walk | a part of a list is returned as the whole of one |
| An unparseable or empty release date maps to no date rather than a failed response | a film with no date takes a whole page down with it |
| A row with no `popularity` is kept and sorts below every scored row | an absent score sinks the page that carried it, or takes a position that reads as measured |
| Every genre id in the recorded pages and the recorded genre list maps to a `Genre` | the table has a gap against what TMDB sends |
| An unrecognised genre id is dropped | a chip appears that the filter sheet cannot offer |
| A poster and a backdrop each fill both sizes with their documented widths, and an absent path yields no `ImageRef` | a URL is built wrong, or an absent image becomes a broken frame |
| Two sources issuing one id give two different movie ids | one provider's movie overwrites another's |
| An id survives a round trip through its qualified form | a movie cannot be read back from the id it was stored under |
| A source's own id keeps a separator it carries | a provider whose ids carry the separator loses half of one |
| A detail read for an id carrying its source requests TMDB's own id | a source name reaches a provider's URL |
| A dropped connection, a 500, the recorded 401, the recorded 404, a malformed body and an empty first page each map to their stated cause | a screen shows the wrong message for a failure |
| An unresolved host maps to `NoConnectivity` | a device with no DNS reads as a server fault |
| Every request carries the bearer header and no credential appears in a URL | the credential decision quietly stops being true |
| A logged request names the `Authorization` header and does not carry its value | the credential reaches the log |
| A cancelled call rethrows rather than returning a failure | a screen that navigated away renders an error it never asked for |

The totality test proves the table is total over what has been recorded. It cannot notice TMDB
publishing a new genre, because the recording still holds the old list.

One test does, and it is the only one that reaches the real API:

| Test | Fails when |
|---|---|
| The live genre list has an entry for every genre it publishes, and the table carries no id it has stopped publishing | the app's idea of TMDB's genres has drifted from TMDB's |

It needs a token and a network, so it never gates `check`: the build excludes it there by name, and
`scripts/livecheck.sh` is what runs it. It reads the token from this module's `BuildConfig`, and
skips itself when there is none, so a clone without a credential is not a broken build. It goes through the production client, so a change to the base address, the credential
header or the JSON configuration fails here too. A failure means TMDB changed, not that the app
broke, and the fix is to re-record and update whatever the recording now disagrees with.

---

## 5. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D19 | A failure is a returned `NetworkResult`, never a thrown exception | The failure is in the signature and the caller's `when` is exhaustive, so a caller decides what one means rather than a `catch` deciding for it | `kotlin.Result`, which erases the failure type and needs `DataError` wrapped in a `Throwable`; a typed exception, whose control flow is invisible and whose missed catch reaches the user as a crash |
| D20 | `Genre` is a hardcoded enum the app owns, and each source maps its own vocabulary onto it | The filter sheet lists every genre with no fetch, no cache dependency and no empty-state race, a genre matching zero movies still appears, and the names become string resources a translator can reach. Verified against three other movie APIs: two of them ship no genre endpoint at all, and none ships TMDB's ids | Fetching and storing the provider's genre list, which buys nothing once the app owns its own genre type, since an id outside the table is dropped either way |
| D21 | The genre endpoint is never called in production | One request fewer per launch, no second failure branch inside a page fetch, and a request cap that still means what it says | Calling it inside the trending fetch, which makes a method named "fetch one page" issue two requests |
| D22 | The source returns fully formed `Movie` values | Genres resolve in the mapper, so no type is ever half-populated and no caller has to know which half it holds | A source-shaped type alongside the app's own, which was only needed while genre names arrived separately |
| D23 | `MovieDetail` embeds `Movie` | One declaration per field, and the two records cannot disagree | Repeating the fields flat, which means making every mapping change twice; splitting the detail-only fields out, which gives one render two loaded values and two failure paths |
| D24 | `ImageRef` holds a resolved URL for each of two app-owned sizes | Across twelve movie APIs surveyed, several publish only named buckets and no dimensions, so keying on pixel width would make their mappers invent numbers. Two is what the design actually places and what every source can fill honestly, and a source offering fewer repeats one rather than pushing a fallback into the UI. A third size is a mapper change and a column, not a UI change, so it costs nothing to defer | Pixel widths with a nearest-width rule, which is precise arithmetic over invented inputs for most sources; one URL chosen in the data layer, where nothing knows the layout |
| D28 | `popularity` is a non-null `Double` carrying an ordering rather than a measurement, and each source maps its own signal onto it | Of thirteen movie APIs surveyed, eleven can order a list and only six publish a number, no two of those six on the same scale: a percentile, a decayed lifetime score, watcher counts two orders of magnitude apart, and one whose own specification says no meaning should be inferred. An ordering is the weakest contract that still delivers the default sort, and the only one every list-serving source can meet. Each source maps one signal onto it for every row it returns, so the field never needs a null and the sort never needs a nulls-last branch | Modelling the shape as well as the value, which is right for a screen comparing two sources' popularity and is a feature nobody has asked for; a nullable field, which puts a branch in the sort for a case the mapper can resolve; keeping the wire default of zero, which is the sentinel this spec removes everywhere else |
| D25 | `ReleaseStatus` is an enum with `UNKNOWN` | Consistent with the genre decision: the app owns the type, the UI owns the copy, and a value added later lands somewhere legible rather than printing a raw token | Passing the provider's string to the screen, which is untranslatable and cannot be branched on |
| D26 | The duplicate-page fixture is assembled from recorded rows | The deduplication test fails for the right reason on every run, rather than only on days a recording caught the provider re-ranking | Using whatever a recording caught, where a re-record can silently disarm the test; a script that retries until it observes duplicates, which is unbounded and not reproducible |
| D27 | A `Rating` carries an average on one app-owned scale and an optional vote count. No rating at all is a null `Rating`, and each source maps its own way of saying so | Four of twelve movie APIs surveyed publish a score with no vote count, so requiring one would discard a rating those sources do report. One scale is what lets a screen render any source without asking which axis a number sits on. TMDB's nought-with-nought-votes is a sentinel its own mapper resolves, so an unreleased film still shows nothing rather than a score of nought | Requiring the count, which throws away real ratings from a third of the sources surveyed; treating every returned number as a score, which renders nought for the 12% of trending films nobody has voted on; carrying scale and provenance on the type, which is the right answer for a comparison feature nobody has asked for |
| D29 | A source is a package under `sources/`, holding its client configuration, wire shapes, mappers, genre map and Hilt module. The module root holds only what every source shares | A source is added by creating a directory and removed by deleting one, and the boundary is visible in the tree rather than in a naming convention. The root then contains nothing a reader has to attribute to a provider, which is what keeps the contract readable as a contract | A flat module with a `Tmdb` prefix, where the boundary exists only as long as everyone keeps typing it, and a second source doubles the files at the root; a module per source, which buys the same separation for a settings file, a build script and a published surface each |
| D31 | The source owns the walk: a caller asks for a count and gets distinct movies, and paging, deduplication and the request cap are each source's own | The product wants exactly a hundred and rules out paging past them, so a page number the caller chose carries no information. Unstable ranking is a TMDB quirk, and the contract should not make a provider that serves a hundred in one call write a loop to satisfy it. The duplicate-page fixture and the walk it exercises land in the same module | The repository walking pages and deduplicating, which puts one provider's pagination into a layer that must not know any provider; a page number plus a page size, which with deduplication underneath cannot say which raw pages hold a given deduplicated page without re-walking from the first |
| D32 | A read that fails part way through a walk fails the whole call, and no partial list is returned | A caller cannot tell a list cut short by a failure from one that is simply short, so a partial success is shown as though it were everything the source had. A hundred asked for and forty rendered with no sign of the difference is the worse of the two outcomes | Returning what was held with the cause beside it, which puts the burden on every caller to check a nullable field the compiler does not force it to read; a third result case for a partial, which spreads a TMDB paging quirk into the shared contract |
| D33 | A row with no `popularity` sorts last, and no stand-in is computed from its position | Position and score order the list differently: the score breaks the served order 64 times in 120 recorded rows, and the top-ranked row sits around tenth by score. A position stand-in would place a row by the wrong ordering while looking like a measured value. Sorting last says only that the source did not say, which is what happened | Falling back to the row's position, which mixes two orderings inside one list; refusing the row, which after D32 costs the whole walk over a field that is never rendered |
| D86 | A poster's `small` resolves to the width the widest placement reading it needs, which is `w342` | Both poster placements read it, and the wider is 92dp, so a width chosen for the 76dp row is upscaled in the header on every density above xhdpi. One width for both is what lets the two screens share an address, and the step that covers the larger tile covers the smaller one exactly | A width per placement, which needs a third bucket the type has no slot for and puts the header on an address no other screen has loaded; `w185` for both, which is soft in the header, and was already short of what the row asks for above xhdpi |
| D71 | A movie's id is one opaque `String` on the model, carrying the source and that source's id, and the grammar that joins the two is `internal` to `:core:network` | Only a source both mints an id and resolves one against a provider, so the pair is exactly as wide as the module that uses it. Above it an id is passed back down the way it came up, which keeps a source out of every DAO signature, table key, navigation key and row state. The source travels inside the id rather than beside it, because retrofitting it is a schema migration and a navigation key format change, where writing it from the first day costs one concatenation. The split takes the first separator, so only a source name has to avoid one and a provider whose ids carry separators keeps them whole | A `MovieId(source, value)` pair on the model, which puts a network concern in every layer that only ever hands the value back; a bare id with no source in it, which is free today and a migration on the day a second source issues an id of its own; a value class over the string, which names the invariant and still costs every consumer an import for a type it only forwards |
| D30 | Mapping is `internal` extension functions on the wire shapes, in one direction only | Nothing is injected, so a test decodes a fixture and calls the function. One direction because the app never writes to a provider, so a model type has no wire form and a DTO is constructed only by a test | A mapper class, which needs a binding and a fake for something that is a pure function; a two-way mapper, whose other half has no caller |

---

## 6. A second source

A second source is another package under `sources/`, holding its own client configuration, wire
shapes, mappers and genre map, and binding itself through its own Hilt module. No new Gradle module,
no existing file edited, and nothing at the module root grows. How the sources are then bound and what the repository does with more than one is owned by
the architecture doc.

What the types here make free, measured against eleven other movie APIs:

| Where sources usually diverge | Why it costs nothing |
|---|---|
| Genre representation: non-contiguous integers, comma-joined strings, lowercase slugs, three-character codes, `{id, name, slug}` objects | Each source maps its own representation onto `Genre` at its own boundary. Nothing above sees a provider's vocabulary |
| Genre names: localised by three providers, differently cased by another between its body and its own URLs | A name is never a key. `Genre` is the key and the display string is a resource |
| Merged genre buckets, where one provider's single genre covers two of ours | A source's map is its own, so one key expanding to two `Genre` values is that source's business |
| Release status: title case, lower case, a nested object with an id, or absent entirely | Per-source parse onto `ReleaseStatus`. `UNKNOWN` absorbs both a word we do not know and a provider with no such concept, and both omit the row |
| Images: a path plus a width template, named finished URLs, or a single URL | All three are variant sets. A source emitting one width and a source emitting six are the same type |
| Identity | An id carries the source that issued it, so two providers' integer `1` are two different movies. Only this module reads inside one |
| External identity | `imdbId` is the one-hop route to most of the field, which is why it sits on the model rather than only in TMDB's DTO |

Two couplings remain, and they are named rather than solved. A third dissolved:

- **`popularity` is on the source's own scale.** One provider's runs past 1000, another caps at 100,
  another reports a percentile. Sound as a sort key within one source's set, undefined across a
  merged one.
- **`rating` is singular.** Providers exist that carry three or nine ratings from different
  authorities. A second opinion on a film is a `Map` of them, which is a field change rather than a
  redesign.
- **`trending` asks for a count.** A provider that pages by cursor walks its own cursors inside the
  call and exposes none of it, and a provider that serves a hundred in one request makes one. Neither
  shape leaks, so this is not a coupling so much as the place a provider's paging goes to be
  forgotten.

Fields other providers carry and this model does not, such as certifications, awards, streaming links
and cast, are each a nullable field on `MovieDetail` that one source fills and the others leave
absent. The missing-data rule already renders that correctly.

---

## 7. What this changes elsewhere

- The tech stack spec's genre resolution and schema rows describe a fetched and stored genre list.
  Both are obsolete.
- The architecture doc's `:core:model` and `:core:network` entries, and its sections on
  normalisation and on genres, are rewritten. The alias table and the genre relation query are gone.
- The architecture doc's second-source section says a source sits alongside the contract. It now
  sits under `sources/`, and the contract is what the module root holds.
- The architecture doc gives the repository the page walk, the deduplication and the request cap.
  All three are the source's. The repository asks once for a hundred and writes what comes back,
  which is what the repository spec inherits.
- The persistence spec loses the genre table, the per-source alias table and the relation query from
  its decision surface. Nothing queries by genre in SQL, because filtering is pure Kotlin. An
  `ImageRef` stores as two columns per image, so it needs no type converter either.
- The brief's open question about when the genre list is fetched is answered: never.
- The brief now requires a trending source to be able to order its own catalogue, which is what
  `popularity` carries.
- The detail screen gains a third rating shape, an average with no vote line, which no TMDB response
  can produce. Whether it earns a designed treatment or a `TODO` is the detail screen spec's call.
- The trending list loads a wider poster than the row alone would ask for, which is what the detail
  screen reads back from the cache.
