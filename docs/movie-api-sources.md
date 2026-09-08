# Movie API sources

This is how we use movie API sources. TMDB is the first source, it might be swapped in the future, or we might add more APIs. So it is kept behind a boundary.

## The contract

`:core:network` publishes one interface, in the app's own types:

```kotlin
interface MovieRemoteDataSource {
    suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>>

    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}
```

A caller (Repository) asks for a number of movies and gets distinct ones. How many requests that took and how large
a page, are the implementation's business.

The response contains app's domain models (Movie, MovieDetail), not the APIs DTO.

Repositories in `:core:data` depend on that interface. **No name of a provider appears above
`:core:network`**, in a repository, an entity, a navigation key or a screen.

## Where TMDB lives

Everything TMDB-specific is one directory:

```
:core:network/sources/tmdb/
```

A source is a directory: added by creating one, removed by deleting one. Everything in `sources/` is
`internal`.

`RemoteSourceModule` is the one public Hilt module, which is what lets a device test uninstall that
binding alone and put a fake behind the contract while keeping the rest of the real graph.

## What the app owns

The app's idea of a movie is not TMDB's JSON. Each of these is a type in `:core:model` that a
source maps its own vocabulary onto, at its own mapper.

| The app owns | Which means |
|---|---|
| `Movie.id` | One opaque string carrying the source and that source's id, joined by `MovieId.kt` (ex: `tmdb:1234`). Two providers' identical ids are two distinct movies. Only `:core:network` reads inside one; every layer above passes it back down the way it came up |
| `Genre` | An enum of nineteen constants. TMDB's ids map onto it in `mapper/TmdbGenres.kt`, and an id the table does not carry is dropped. The UI resolves each constant to a string resource, so the copy is ours. How to keep the genre list in sync is explained below. |
| `ImageRef` | Two resolved URLs, `small` and `large`, for a poster or a backdrop. The host and the width each size resolves to stay in `mapper/TmdbImageMapper.kt`; the UI asks for a size, never a width. A source with fewer sizes repeats the one it has |
| `poster` and `backdrop` | Two image roles the app defines, not two fields a provider happens to publish. A poster is the portrait tile a list row renders, a backdrop is the landscape image, and each source decides which of its images fills which role. A source publishing one image can fill both roles with it, the way a source with one size fills both sizes with it. Only a source with no image at all yields no `ImageRef`, which is what the placeholder tile renders |
| `ReleaseStatus` | Where a film sits in its lifecycle, as an enum: (ex: in production, released, etc.). Each source parses its own wording onto it. `UNKNOWN` absorbs both a word we do not know and a provider with no such concept. The UI resolves each constant to a string resource, so the copy is ours |
| `Rating` | An average on a 0 to 10 scale, plus a vote count where the source publishes one. That scale is the app's, so a source scoring out of 100 divides by ten at its own mapper. No rating at all is a null `Rating` |
| `popularity` | An ordering, not a measurement. Greater is more popular, and that is the whole contract: no unit, no scale, never displayed, not comparable between sources. A source with no score maps its list position onto it |

These types are owned by the app's domail model `Movie` and `MovieDetail`.

Nullability carries the missing-data rule. The mappers should turn: 0, empty string, absent field -> `null`. UI treats: `rating == null` omits the rating block, `budget == null` reads "Not disclosed".

## The paging workaround

TMDB serves trending twenty movies per page. Worse, it re-ranks between requests
([1](https://www.themoviedb.org/talk/5ee3abd1590086001f50b3c1),
[2](https://www.themoviedb.org/talk/5bbabe890e0a2616d7005bd6)), so a movie on a page boundary is
served twice while another is missed. Five pages measured 100 rows and only 92 distinct movies!

`TmdbRemoteDataSource.getTrendingMovies` is where that is absorbed. It walks pages from one, drops a
repeated movie, and keeps going on **distinct movies held**. A page that fails takes down the whole walk with
it, because showing less than 100 movies is confusing, we show error & retry button.

## Keeping the genre list in sync

The genre endpoint is never called in production. `Genre` is the app's own enum, and
`mapper/TmdbGenres.kt` maps TMDB's ids onto it, so the filter sheet lists every genre with no fetch
and no empty-state race.

Two tests guard that table, both in `core/network/src/test`:

| Test | Fails when |
|---|---|
| `TmdbGenresTest` | The table has a gap against the recorded genre list or the recorded trending pages. It reads a recording, so it cannot notice TMDB publishing a new genre |
| `TmdbGenreListLiveTest` | **This is the one that breaks when we drift.** It calls the live genre endpoint through the production client and fails when TMDB publishes a genre the table has no entry for, or the table carries an id TMDB has retired |

The live test needs a token and a network, so it skips itself when there is none. A failure means
TMDB changed: add or remove the entry, then re-record with `scripts/record-fixtures.py`.

## Adding a second source

1. Create `sources/<name>/` beside `tmdb/`: a config, a client, an api, wire shapes, mappers and a Hilt module. Everything `internal`.
2. Add its credential to `API_TOKENS` in `build-logic/convention/src/main/kotlin/ApiTokens.kt`. Every
   module applying `amro.api.tokens` then gets one `BuildConfig` field per source.
3. Bind it in `di/RemoteSourceModule.kt`.

Nothing in the UI, the database schema or the navigation changes.

Three things do change, and the steps below were built and compiled against the current graph before
being written down.

### Binding two sources

While there is one source the binding is a plain `@Binds` and the repositories take a single
`MovieRemoteDataSource`. A second source turns that into a set: each source contributes itself
through a Hilt multibinding.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteSourceModule {
    @Binds
    @IntoSet
    internal abstract fun bindTmdbSource(source: TmdbRemoteDataSource): MovieRemoteDataSource

    @Binds
    @IntoSet
    internal abstract fun bindOtherSource(source: OtherRemoteDataSource): MovieRemoteDataSource
}
```

`@JvmSuppressWildcards` on the injection site is required.

### The source contract gains one function

Only a source can say which movie ids are its own, so the contract asks it:

```kotlin
interface MovieRemoteDataSource {
    suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>>

    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>

    fun doesIssue(movieId: String): Boolean
}
```

Each source answers it by reading the source out of the qualified id, which is the one thing
`MovieId` is for and stays `internal` to this module:

```kotlin
override fun doesIssue(movieId: String): Boolean = MovieId.of(movieId).source == TMDB_SOURCE
```


### The repositories hold a set

`MovieDetailRepository` finds the source that issued the movie, then asks that one:

```kotlin
override suspend fun refresh(movieId: String): DataError? {
    val source = sources.firstOrNull { it.doesIssue(movieId) } ?: return DataError.Server
    return when (val result = source.getMovieDetail(movieId)) {
        // handle result
    }
}
```

An id no source claims is a cached movie from a source that has since been removed. It cannot be
fetched by anyone, so it answers like a failed read and the cached record keeps rendering.


### Open product decision
`TrendingMoviesRepository` asks every source and writes what comes back. Whether to **combine trending lists of multiple sources** is a product decision that needs to be made.
- **What a second source means.** Merging two trending lists, preferring one, or showing them apart
  is a product decision. The data layer can express any of them.
- **Cross-source identity.** One film served by two providers is two rows. Matching them needs a rule
  of its own, on `imdbId` if both carry it, and otherwise on title and release date.
- **Comparable popularity.** Each provider scores on its own scale, so a merged list cannot be ordered
  by popularity without a normalisation. Title and release date order fine across sources.
