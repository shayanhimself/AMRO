# 005: Repositories, filter and sort

`:core:data`. Two repositories, one per table, and the filter and sort rules the trending list
applies over what they serve. This is the only module a feature depends on for data, and it names no
provider and imports neither Ktor nor Room. The rules themselves sit with the one screen that
applies them.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

---

## 1. What the specs below already settled

Most of what this spec was scheduled to decide is decided.

- **The page walk, the deduplication and the request cap are the remote source's.** A repository asks
  once for a count of distinct movies and writes what comes back.
- **There is no partial answer.** A walk that fails part way returns a failure carrying no movies, so
  no repository ever holds part of a list, and nothing in the app can produce the brief's partial page
  failure state.
- **The trim to the count happens in the source**, before dedupe leaves it. Nothing here trims.
- **There is no freshness window, no timestamp and no clock.** A read serves what is cached and a
  refresh runs regardless.

What is left is the shape of the two contracts, what a failure returns, and the filter and sort rules.

---

## 2. The contracts

```kotlin
interface TrendingMoviesRepository {
    fun getTrendingMoviesFlow(): Flow<List<Movie>>

    suspend fun refresh(): DataError?
}

interface MovieDetailRepository {
    fun getMovieFlow(id: String): Flow<Movie?>

    fun getMovieDetailFlow(id: String): Flow<MovieDetail?>

    suspend fun refresh(id: String): DataError?
}
```

**Two repositories, split on which table writes.** `movies` has one writer and `movie_details` has
another, which is what lets a cached detail outlive the trending set. Two interfaces make that a fact
the compiler holds rather than a convention inside one class body, and a feature then depends on the
surface it uses: the trending view model cannot reach detail fetching, and its fake implements two
members instead of five. The split costs one interface file and one binding, because the two share no
state and no collaborator that only one of them needs.

**The reads name the local source's own methods.** A repository read is a pass-through, and it reads
as one. The writes deliberately do not match: the source has `replaceTrending` and `writeMovieDetail`,
the repository has `refresh`. Different verbs for different acts, because the source stores what it is
handed and the repository fetches first and stores only on success.

```
core/data/src/main/kotlin/com/shayan/amro/core/data/
    TrendingMoviesRepository.kt
    MovieDetailRepository.kt
    DefaultTrendingMoviesRepository.kt
    DefaultMovieDetailRepository.kt
    di/DataModule.kt
```

Both implementations are `internal` and bound in `DataModule`, so the module publishes two interfaces
and nothing else.

`:core:model` becomes an `api` dependency, because `Movie`, `MovieDetail` and `DataError` are all on
those contracts. `:core:network` and `:core:database` stay `implementation`: a feature sees
`DataError`, which is a model type, and never `NetworkResult`.

---

## 3. The policy

One policy, written twice because it is three lines.

```kotlin
internal const val TRENDING_COUNT = 100

internal class DefaultTrendingMoviesRepository @Inject constructor(
    private val local: MovieLocalDataSource,
    private val remote: MovieRemoteDataSource,
) : TrendingMoviesRepository {

    override fun getTrendingMoviesFlow(): Flow<List<Movie>> = local.getTrendingMoviesFlow()

    override suspend fun refresh(): DataError? =
        when (val result = remote.getTrendingMovies(TRENDING_COUNT)) {
            is NetworkResult.Success -> {
                local.replaceTrending(result.value)
                null
            }
            is NetworkResult.Failure -> result.error
        }
}
```

The detail repository is the same shape one row wide: two reads straight off the local source, and a
refresh that writes on success and returns the error otherwise.

**A read has no failure channel.** A `Flow` off Room emits rows or emits nothing, and nothing about
reading a local table can fail in a way a screen renders. Only `refresh` can fail, so only `refresh`
carries a failure, and no read is wrapped in a result type it would never populate.

**A failure returns as `DataError?`, where null means the write happened.** There is nothing to carry
on success, so a two-case sealed type would have an empty case. `DataError` is already sealed, so a
caller that branches on the cause still gets an exhaustive `when` inside its null check.

**A failed refresh writes nothing.** The source returns no movies with a failure, so there is no
partial set to merge and what is cached is left exactly as it was.

**The count is this module's.** "Exactly one hundred" is a product rule and the source takes a count,
so `TRENDING_COUNT` sits with the policy that asks for it and no layer above names a number. Fewer
than a hundred comes back when the source spent its request budget, which is a complete answer, and
the repository writes it unremarked.

**No dispatcher is injected and nothing calls `withContext`.** Room runs its queries on
`Dispatchers.IO` by driver configuration and Ktor's calls are main-safe on the OkHttp engine, so both
collaborators are already main-safe and a dispatcher qualifier here would wrap a suspend function that
does not need wrapping.

**Neither implementation is scoped.** They hold no state between calls. The database and the HTTP
client they delegate to are singletons already, which is where the scoping that matters lives.

---

## 4. Who decides that a failure is shown

The repository returns the error. It does not decide whether anyone sees it.

The rule that separates the two screens is that a failure is surfaced when the content left on screen
would otherwise assert something false. That is a fact about what is rendered, and a repository cannot
see what is rendered. So the view model folds the returned error into its state: the trending screen
turns it into a bar over a cached list or into a full-screen error over nothing, and the detail screen
drops it when content is on screen and raises it when there is none.

**There is one `refresh(id)`, not a cache-miss fetch and a background fetch.** The call is identical
in both cases: fetch, write on success, return the cause otherwise. What differs is whether the caller
had something on screen when it made the call, which the caller is the only thing that knows. A second
method would be the same body under two names, and it would put the screen's rule in the layer
furthest from the screen.

---

## 5. Filter and sort

The sort types live in `:feature:trending`, with the rules that read them. Nothing below the UI
layer orders a list, so they are `internal` to the module that does, and `MovieSort` is `Parcelable`
because that module saves it.

```kotlin
internal enum class SortKey { POPULARITY, TITLE, RELEASE_DATE }

internal enum class SortDirection { ASCENDING, DESCENDING }

@Parcelize
internal data class MovieSort(val key: SortKey, val direction: SortDirection) : Parcelable {
    companion object {
        val DEFAULT = MovieSort(key = SortKey.POPULARITY, direction = SortDirection.DESCENDING)
    }
}
```

A selection is a `Set<Genre>` and not a type of its own. It carries one facet, an empty set already
means everything, and a wrapper over one set is a name for a thing the set already says. A second
facet is what earns one. `MovieSort` pairs the key with the direction because they always travel
together: one thing is saved, restored and passed.

The rules are `internal` extension functions in `:feature:trending`, in `viewmodel/MovieFiltering.kt`
and `viewmodel/MovieSorting.kt`.

```kotlin
internal fun List<Movie>.applyFilter(genres: Set<Genre>): List<Movie>

internal fun List<Movie>.applySort(sort: MovieSort): List<Movie>
```

Pure functions rather than a repository method, because they take a list and a selection and return a
list. Nothing is injected, nothing is suspended, and a test calls one directly against a fixture. They
are not a third repository, and the roadmap's count of three surfaces is two repositories and this.

**They sit with the screen because the screen is what has a selection.** The genres and the sort are
state the trending view model owns and a cold start throws away, and no other consumer of
`TrendingMoviesRepository` has one to pass. Keeping them in `:core:data` would publish two functions to a
module whose one caller is a screen, and put a screen's selection in the layer that answers where
data comes from. `internal` is what makes that a fact.

### Filtering

An empty selection matches everything. Otherwise a movie matches when it carries any selected genre,
which is the brief's OR semantics. It is a predicate over the list, so a movie carrying two selected
genres is kept once and no row can repeat.

### Sorting

| Key | Ordering |
|---|---|
| `POPULARITY` | Natural order on a non-null `Double`. No null branch, because a source maps one signal onto it for every row |
| `TITLE` | `String.CASE_INSENSITIVE_ORDER`: case does not distinguish two titles |
| `RELEASE_DATE` | Natural order on `LocalDate`, absent dates last in both directions |

**The direction reverses the comparison of two present values, and nothing else.** Nulls-last sits
outside it, so a `nullsLast` wrapper takes an already-directed comparator rather than the whole
comparator being reversed. Reversing the whole thing is what puts absent dates first in one direction,
which is the bug this shape removes.

```kotlin
private fun comparatorFor(sort: MovieSort, locale: Locale): Comparator<Movie> {
    val byKey = when (sort.key) {
        SortKey.POPULARITY ->
            compareBy(sort.direction.applyTo(naturalOrder<Double>())) { it.popularity }
        SortKey.TITLE ->
            compareBy(sort.direction.applyTo(String.CASE_INSENSITIVE_ORDER)) { it.title }
        SortKey.RELEASE_DATE ->
            compareBy(nullsLast(sort.direction.applyTo(naturalOrder<LocalDate>()))) { it.releaseDate }
    }
    return byKey.thenBy { it.id }
}
```

`applyTo` is a private extension on `SortDirection` returning the comparator it was given, or its
reverse.

**Case folding is what the recorded data demands.** Ordering the 113 distinct recorded titles by
`compareTo` and by case-insensitive order gives two different lists: `LEGO Star Wars: The
Mandalorian` sorts ahead of `La bola negra`, because `E` is 69 and `a` is 97.
`String.CASE_INSENSITIVE_ORDER` is the JDK's answer to exactly that, and it needs no locale, so the
sort is the same on every machine and a test asserts an ordering that is its own.

A title opening on an accent still sorts after every ASCII letter, which a `Collator` would file
under its base letter instead. No recorded title carries a diacritic at all, so that is a case the
MVP does not have and a locale parameter threaded through every caller and test is not worth
carrying for it. It is one comparator swap on the day a source serves such titles.

**The tiebreak is the movie's id, and it does not reverse.** Two films sharing a title or a release
date need a total order, or a re-sort reorders rows that did not change and the list visibly shuffles
under a filter change. The tiebreak stays ascending in both directions because its job is identity rather
than ordering.

**Filter and sort compose in either order.** Sorting is a total order and filtering preserves relative
order, so filtering first and sorting first give the same list. Filtering first is a cost decision, and
the acceptance criterion asking for both orders is satisfied by the shape of the two functions rather
than by a rule anyone has to follow.

**Where these run is the trending spec's decision.** This spec owns what they mean.

---

## 6. What `:core:testing` gains

Four fakes, each carrying its own test, because a fake that lies is worse than no fake.

| Fake | Is | Its own test asserts |
|---|---|---|
| `FakeMovieLocalDataSource` | Maps behind `MutableStateFlow`s | A replace emits once carrying the new set, a detail write leaves the trending flow untouched, and an absent row emits null |
| `FakeMovieRemoteDataSource` | A scripted `NetworkResult` per call | Results are returned in the order they were scripted, and the count each `trending` call asked for is recorded |
| `FakeTrendingMoviesRepository` | A seeded flow plus a scripted refresh outcome | A successful refresh replaces what the flow emits, and a failed one returns the cause and changes no emission |
| `FakeMovieDetailRepository` | The same, per id | The same, and a movie with nothing seeded emits null from both reads |

`FakeMovieLocalDataSource` exists because the Room implementation is `internal` to `:core:database`,
so `:core:data` cannot reach it. Its own test makes the same claims the real one's tests make, which
is what makes a repository test that passes against it mean something.

The filter and sort fixture is hand-built rather than recorded. `:core:testing` publishes a `movie()`
builder with a default for every field, so a test names only the fields it asserts on, and one
`SORT_FIXTURE` list covering the shapes the rules turn on: an absent release date, an accented title,
two titles differing only in case, a tied popularity, and movies sharing genres.

---

## 7. Tests

All on the JVM. No Robolectric, no database and no engine: both collaborators are fakes.

| Test | Fails when |
|---|---|
| A trending refresh writes the returned list and returns null | a successful fetch does not reach the table |
| A trending refresh asks for one hundred | the count stops being the product's |
| A failed trending refresh returns the cause and writes nothing | a failure blanks the cache it should have left alone |
| The trending read emits the cache before any refresh, and again after one succeeds | the screen stops being fed by the database |
| A refresh runs whatever the cache holds | a freshness rule reappears |
| A detail refresh writes that detail and returns null | the detail write breaks |
| A failed detail refresh returns the cause and leaves the cached detail | a failed background refetch destroys what was correct |
| A detail refresh leaves the trending flow untouched | the two writers stop being independent |
| `getMovieFlow` emits null for a movie the trending set does not hold | the detail screen's fallback is never exercised |
| An empty filter returns every movie in the order it was given | "nothing selected means everything" breaks |
| One genre returns exactly the movies carrying it | the predicate is wrong on the simple case |
| Two genres return their union, each movie once | OR semantics repeats a row, or becomes AND |
| A genre no movie carries returns an empty list | a zero-result filter fails instead of emptying |
| Each key in each direction orders the fixture correctly | a comparator is wrong or a direction is not applied |
| Absent release dates sort last ascending and last descending | reversing the comparator drags nulls to the front |
| Titles sort case-insensitively | an all-caps title sorts ahead of one a reader would put first |
| Movies with an equal sort key keep the same relative order across repeated sorts | the order is not total and the list shuffles on a filter change |
| Filtering then sorting equals sorting then filtering | the two stop commuting |

---

## 8. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D45 | Two repositories, split on which table each one writes | 004 gave each table exactly one writer, and two interfaces make that a compile-time fact rather than a convention inside one class. A feature depends on the surface it uses, so a trending view model cannot reach detail fetching and its fake implements two members instead of five, and two feature teams do not edit one file. The split costs an interface file and a binding, because the two share no state and no collaborator only one of them needs | One `MovieRepository` over both tables, where the single-writer rule survives only as long as everyone keeps honouring it and every feature's fake carries members it never calls; splitting further, per read, which is five interfaces over two tables |
| D46 | A read returns a plain `Flow` and only `refresh` can fail, returning `DataError?` with null for success | Reading a local table has no failure a screen renders, so a result type on a read would have a case nothing populates. There is nothing to carry on success, so a sealed result would have an empty case, and `DataError` is already sealed, so a caller branching on the cause still gets an exhaustive `when` | `Flow<Result<List<Movie>>>`, which puts a failure channel on the one path that has none; a sealed `RefreshResult`, whose success case holds nothing; throwing, which 003 D19 already ruled out below this layer |
| D47 | The repository returns the error and never decides whether it is shown | The rule is that a failure surfaces when the content left on screen would assert something false, which is a fact about what is rendered. A repository cannot see what is rendered, so the view model folds the error into its state and the same call serves a bar on one screen and silence on the other | A repository that swallows a background failure, which needs to be told what is on screen and gives the same call two meanings; a repository returning a UI-shaped state, which puts presentation below the data layer |
| D48 | One `refresh(id)`, not a cache-miss fetch and a background fetch | The body is identical: fetch, write on success, return the cause otherwise. What differs is whether the caller had content on screen, which only the caller knows | Two methods, which is one body under two names and puts the screen's rule furthest from the screen |
| D49 | Filter and sort are `internal` pure extension functions on `List<Movie>` in `:feature:trending`, alongside `SortKey`, `SortDirection` and `MovieSort`, and a filter selection is a plain `Set<Genre>` | They take a list and a selection and return a list, so nothing is injected and a test calls one against a fixture, next to the screen whose state the selection is. They are not a third repository. The genre set needs no wrapper while it is the only facet: an empty set already means everything, and a second facet is what would earn a type | The same functions published from `:core:data`, whose only caller is one screen, which puts a selection a cold start discards in the layer that answers where data comes from; a `SelectionRepository` or a use case, which is a binding and a fake around a pure function; the rules inside the view model itself, where they need a view model to test; a `MovieFilter` holding one set, which names nothing the set does not; the sort types in `:core:model`, which puts an operation over movies beside the facts about one, in a module nothing below the UI layer needs it from |
| D50 | Title ordering uses `String.CASE_INSENSITIVE_ORDER`, with no locale | Case folding is measured, not assumed: across the 113 distinct recorded titles, `compareTo` and case-insensitive order disagree, putting `LEGO Star Wars: The Mandalorian` ahead of `La bola negra`. Needing no locale is what keeps the ordering identical on every machine, so a test asserts an ordering that is its own rather than the one its hardware is set to | A `Collator` at `SECONDARY` strength, which additionally files a title opening on an accent under its base letter rather than after Z: no recorded title carries a diacritic, so it buys a case the MVP does not have in exchange for a locale parameter on every caller and every test, and it is one comparator swap on the day a source serves those titles; raw `compareTo`, which is the defect above; stripping leading articles, which is a per-language word list the MVP does not need |
| D51 | Nulls-last sits outside the direction, and the movie's id is a fixed ascending tiebreak | Reversing the whole comparator is what puts absent dates first in one direction, so the direction applies to the comparison of two present values and the null placement is not part of it. Two films sharing a title need a total order, or a re-sort reorders rows that did not change and the list shuffles under a filter change. The tiebreak does not reverse, because its job is identity rather than ordering | `compareBy(nullsLast()).reversed()`, which is the bug; a stable sort with no tiebreak, which is stable only against the order it was handed, and the order off Room is unspecified |
| D52 | No dispatcher is injected and no method calls `withContext` | Room runs its queries on `Dispatchers.IO` by driver configuration and Ktor is main-safe on the OkHttp engine, so both collaborators are already main-safe | A `@Dispatcher` qualifier and a `withContext` in every method, which wraps two libraries that do it themselves and adds a constructor parameter every fake has to satisfy |
| D53 | `TRENDING_COUNT` lives in `:core:data`, and neither implementation is scoped | "Exactly one hundred" is a product rule and the source takes a count, so the number sits with the policy that asks for it and no layer above names one. The repositories hold no state between calls, and the database and client they delegate to are singletons already | The count in `:core:network`, which makes the source's parameter decorative; `@Singleton` on a stateless class, which is a scope with nothing to scope |

---

## 9. What this changes elsewhere

- The roadmap's 005 row lists deduplication, the request cap, partial failure and the trim to a
  hundred. All four belong to 003, and its non-negotiable dedupe test lives there. What is left is two
  contracts and the filter and sort rules, which is well under the 2:30 budgeted. The slack goes to
  006.
- The roadmap's "three repository surfaces" is two repositories, plus two files of pure functions
  that belong to `:feature:trending` rather than to this module.
- The brief's partial page failure state in 5.1 is unreachable, because no layer can produce a partial
  list. It is deleted rather than cut, and cut list item 5 has nothing to cut.
- The architecture doc gives `:core:data` the fetch policy, deduplication, and filter and sort.
  Deduplication is the source's, and the rules and the sort types are `:feature:trending`'s.
  `:core:model` gains neither `MovieSort` nor a `MovieFilter`.
- `:core:data` promotes `:core:model` to an `api` dependency, because `Movie` and `DataError` are on
  the published contracts.
- `:core:testing` gains two source fakes, two repository fakes, the `movie()` builder and
  `SORT_FIXTURE`.
- 006 inherits two suspend refreshes returning a nullable cause, two flows off Room, and the two
  functions its own module already holds. It owns where they run and how the cause becomes a bar or
  a screen.
- 007 inherits one refresh whose failure it discards while content is on screen, and the summary read
  that fills its loading state.
