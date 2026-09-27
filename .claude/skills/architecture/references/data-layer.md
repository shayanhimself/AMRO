# Data layer

Repositories in `:core:data`, Room in `:core:database`, the remote source in `:core:network`,
the types all three speak in `:core:model`.

## Repositories

One repository per data type, interface plus implementation:

```kotlin
interface MovieDetailRepository {
    fun getDetailFlow(id: MovieId): Flow<MovieDetail?>
    suspend fun refreshIfStale(id: MovieId): Result<Unit>
}

class DefaultMovieDetailRepository @Inject constructor(
    private val remote: MovieRemoteDataSource,
    private val detailDao: MovieDetailDao,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MovieDetailRepository { /* ... */ }
```

- Repositories are the **only** entry to the data layer. Nothing above them touches a DAO
  or a data source.
- Expose `Flow<T>` for data that changes over time, `suspend fun` for one-shot operations.
  Never expose a mutable type.
- The interface is what everything else depends on. A Hilt module in `:app` binds it to the
  implementation, so a device test rebinds one line and keeps the rest of the graph.
- A repository holds the policy: how many pages to ask for, when to stop, what counts as
  stale, what a partial failure keeps. The data source holds only how one request is made.
- A repository depends on `MovieRemoteDataSource`, never on `TmdbRemoteDataSource`.

## Single source of truth

Room is the SSOT. A repository's read path is a DAO `Flow`; its network path writes rows and
returns nothing to render. A cached list therefore renders before any request completes, and
a refresh that changes nothing changes nothing on screen because rows are keyed by movie id.

A `Flow` off Room and the call that refreshes it are separate members. The screen collects
the first and triggers the second, so failing to refresh is not failing to render.

## The remote source

`MovieRemoteDataSource` is the source-neutral contract. Its TMDB implementation owns the
Ktor client configuration, the endpoints, the wire DTOs, the DTO-to-model mappers, and
TMDB's own facts: its page size, its host, its image widths, the instability of its ranking.

- Wire DTOs are `@Serializable` and `internal`, and never leave the module.
- A mapper is a pure function over a DTO. It normalises the provider's ways of saying "no
  value" onto `null`, so nothing above the data layer checks for a sentinel.
- Transport and status failures map onto `DataError` in one function with a unit test per
  branch. No exception text, status code, or stack trace escapes the module.

## Storage

| Data | Store |
|---|---|
| Movie summaries, movie details, genres, the movie-to-genre cross reference | Room |
| Filter and sort selection | ViewModel state only. Nothing persists it, so it resets on cold start |

There is no key-value store in this app. Do not add one without changing the spec.

## Threading and scopes

- Every repository and data source is **main-safe**. Room and Ktor suspend APIs already
  are; wrap CPU-heavy or blocking work in `withContext(ioDispatcher)` inside the data layer,
  not in callers.
- Inject dispatchers — never hardcode `Dispatchers.IO` inline; tests replace them.
- Inject a `Clock` for anything compared against the current time. A day-long freshness
  window is then testable in milliseconds.
- Work that must outlive the calling screen takes the application-scoped `CoroutineScope`
  provided from `:app`.

## Errors

- A one-shot call returns its failure: `Result<T>` or a type carrying `DataError`. It does
  not throw for an expected outcome, and it never swallows `CancellationException`.
- A `Flow` off Room does not fail for a network reason. The refresh call carries that.
- The two calls behind one screen may carry different failures on purpose: a fetch the
  screen asked for is the screen's error state, a background refetch behind correct content
  reports nothing.

## Testing

- Mapper tests over recorded fixtures from `:core:testing`, one per normalisation rule and
  one per `DataError` branch.
- Repository tests with a fake data source and in-memory Room under Robolectric. Fixtures
  drive the interesting cases: duplicate ids across page boundaries, a page that fails after
  earlier ones succeeded, a record cached before its release.
- DAO tests against in-memory Room, including the relation query that returns a movie with
  its genres.
- Write `FakeXRepository` in `:core:testing` next to each repository interface. It is the
  standard dependency for ViewModel tests. No mocking libraries.
