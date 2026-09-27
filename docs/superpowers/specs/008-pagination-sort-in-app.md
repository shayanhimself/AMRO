# 008: Pagination, and filter and sort in SQL

The trending list stops being a hundred movies held in memory. It becomes a window onto a table the
network fills a page at a time, narrowed and ordered by the database rather than by Kotlin.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

This spec supersedes the fixed hundred wherever the two meet. It does not rewrite the specs it
contradicts; each of those is corrected in its own pass, and section 11 lists what is owed.

---

## 1. What changes, and what does not

Unchanged: reactive-only offline, no timestamps and no clock, the three error causes, opaque
source-carrying ids, the detail table and both its reads, one policy of serve-the-cache and fetch
regardless.

Changed: the source returns a page and a cursor rather than a whole answer, the trending table grows
instead of being replaced, the screen reads `PagingData` rather than a list, and the ordering and the
genre predicate move into SQL.

### What the endpoint will serve is bounded

`/trending/movie/week` caps at page 500 and answers `page=501` with a 400. Twenty per page, so the
trending list tops out at **10,000 movies**, and `total_results` reads exactly that at every page.
This bounds the endpoint and not the provider: unfiltered `/discover/movie` reports 1,176,315
movies, and trending refuses to serve past its top ten thousand. The list is unbounded from the
user's side and finite from the table's, which is what bounds the deepest page a reader can reach
and gives the end of pagination a real trigger.

### The provider offers no filter and no sort

Every ordering and narrowing parameter was probed against the live endpoint and none is applied.
`sort_by`, `sort`, `order_by`, `order`, `with_genres`, `without_genres`, `genre`, `genres`, `filter`,
`query`, `year`, `primary_release_year`, `vote_average.gte`, `with_original_language`, `region` and
`include_adult` all return the trending order untouched. Two of them return byte-identical lists to
each other, and `with_original_language=ja` drops the only Japanese row, so what looks like a
response to a parameter is a cached variant rather than a query. `language` is honoured and localises
titles; the app sends none and takes `en-US`.

So filtering and ordering are the app's own work, over what the app holds. This is the same
conclusion the fixed hundred reached, arrived at for a different reason: not because the product
ruled out a server query, but because the provider has none to offer.

---

## 2. The remote contract

```kotlin
interface MovieRemoteDataSource {
    suspend fun getTrendingMovies(count: Int, after: Cursor?): NetworkResult<TrendingPage>

    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}

data class TrendingPage(
    val movies: List<Movie>,
    val next: Cursor?,
)

@JvmInline
value class Cursor(val value: String)
```

**A cursor, not a page number.** A caller says how many movies it wants and where it left off, and
never how the provider counts. TMDB writes its page number into the cursor, a provider that pages by
token writes its token, and one that serves everything in a single call returns a null `next` on the
first. `next == null` is the end of what the source will serve, and the only thing that ends
pagination.

A page number would carry no usable information here: with deduplication underneath, the raw pages
holding deduplicated rows 101 to 200 are not knowable without walking from the first.

**`count` is a request, not a promise.** The source returns what the provider gave it for roughly
that many. TMDB's page size is fixed at twenty and is not overridable, so a request for fifty is
three pages and sixty movies. The source does not trim, and the list it returns may hold repeats:
ranking shifts between requests, so a movie served on page 3 can arrive again on page 4.

This reverses the old contract's two promises. It no longer trims to the count, and a success is no
longer distinct movies. Both existed to make one call the whole answer, and a paged read is not one.

**A failed request fails that call, not the walk.** The mediator asked for one page and gets a
failure or a page. Nothing accumulates across calls inside the source, so there is no partial set to
discard and the request cap disappears: the mediator's own call rate is the budget.

---

## 3. Deduplication

**In the local source, by primary key.** The remote source hands over whatever the provider served,
repeats included, and the write upserts on `movieId`. Sixty movies containing three repeats become
fifty-seven rows, and a movie already held from an earlier page is overwritten with itself.

The remote source keeps no dedupe of its own and no pure function to test. A repeat inside one call
and a repeat across two calls are the same event, and the table already holds the key that resolves
both. Deduplicating in the source would resolve only the first kind and would still need the upsert
for the second.

The consequence is that **a page's yield is not its size**. A fetch of sixty can add fifty-seven
rows, or seven, or none, when the provider re-serves what is already held. Nothing may infer the end
of the list from a short yield: only `next == null` ends pagination.

---

## 4. The table

`MovieEntity` gains two columns and loses nothing.

```kotlin
@Entity(
    tableName = "movies",
    indices = [
        Index("popularity"),
        Index("titleSortKey"),
        Index("releaseDateEpochDay"),
    ],
)
internal data class MovieEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val titleSortKey: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
)
```

**`titleSortKey` is `title.lowercase()`, written by the mapper.** SQLite's `COLLATE NOCASE` folds
ASCII and nothing else, so an accented capital would sort apart from its lowercase form. Folding in
Kotlin and ordering in SQL keeps the behaviour `String.CASE_INSENSITIVE_ORDER` already gives while
letting an index serve the ordering. A title opening on an accent still sorts after every ASCII
letter, which is what the fixed hundred already accepted.

The column exists because the tail of the catalogue is not the head of it. No recorded title in the
top hundred carries a diacritic; by row ten thousand the list is largely foreign-language, and
TMDB's `en-US` title falls back to the original where no English title exists.

**Three indices, one per sort key.** Without them every page sorts the whole table.

**No rank column.** The provider's order is never rendered: the default is popularity descending, and
the three sort keys are the only orders the screen has. A column nothing reads would still have to be
written, migrated and reasoned about at every append.

### Genres

A movie's genres are stored twice, because they are read one way and queried another.

```kotlin
@Entity(
    tableName = "movie_genres",
    primaryKeys = ["movieId", "genre"],
    indices = [Index("genre")],
)
internal data class MovieGenreEntity(
    val movieId: String,
    val genre: String,
)
```

**The cross-reference table is what the filter queries.** One row per movie per genre, the genre held
as the constant's name, indexed on `genre` so a selection resolves through the index rather than by
testing every row. The composite primary key is what makes a repeated write idempotent.

**`MovieEntity.genres` stays, and is what a row renders.** A `@Relation` would be the tidy way to
read genres back, and it is the wrong one here: Room resolves a relation with a second query per
page, and a `PagingSource` returning one would issue it on every page load to build a line of text a
column already holds. The delimited column is read, the table is queried, and neither is derived from
the other at read time.

Both are written from the same `List<Genre>` in one transaction, so they cannot drift, and a round
trip test on each write asserts they did not.

The genre column keeps its own leading and trailing delimiter, `,ACTION,COMEDY,`, so the value stays
unambiguous to a reader of the database even though nothing now matches inside it.

### Remote keys

```kotlin
@Entity(tableName = "trending_remote_keys")
internal data class TrendingRemoteKeyEntity(
    @PrimaryKey val id: Int = TRENDING_KEY_ID,
    val nextCursor: String?,
)
```

One row. It holds where the next append resumes, and a null cursor on a written row means the
catalogue is exhausted. It is in the database rather than in the mediator because a mediator dies
with the process and an append after a restore would otherwise refetch from the first page.

This is the first row in the schema that is not re-fetchable in one request, and it changes nothing:
dropping the database drops the cursor, and the next read is a refresh from page one.

---

## 5. Writes

```kotlin
interface MovieLocalDataSource {
    fun trendingPagingSource(selection: MovieSelection): PagingSource<Int, Movie>

    suspend fun replaceTrending(page: TrendingPage)

    suspend fun appendTrending(page: TrendingPage)

    suspend fun trendingCursor(): Cursor?

    // the detail reads and write are unchanged
}
```

**`replaceTrending` clears both tables and the key, then inserts.** One transaction, so a crash
between the clear and the insert leaves the previous set rather than nothing. This is the old replace
with the genre rows and the cursor written beside it.

**`appendTrending` upserts the movies, replaces their genre rows, and overwrites the cursor.** One
transaction, so the rows and the resume point cannot disagree: a crash after the insert and before the
key write would otherwise re-fetch a page already held, which the upsert would absorb but which would
cost a request on every restart.

**The genre rows for an appended movie are deleted before they are inserted.** An upsert on `movies`
updates a row in place rather than replacing it, so nothing cascades, and a movie whose genres the
provider has changed would otherwise accumulate both sets. Deleting by the ids in the page and
inserting the new rows is one rule that holds for a movie arriving for the first time and for one
arriving again.

**No foreign key.** One writer owns both tables and writes them together, so the constraint would
enforce on every insert what the transaction already guarantees, and `ON DELETE CASCADE` would cover
the clear while leaving the upsert path to be written by hand anyway. One rule, applied in both
writes, beats a constraint that covers one of them.

Both take a `TrendingPage` rather than a list and a cursor, because the two always travel together
and neither write is meaningful with one of them missing.

---

## 6. Reads

The DAO publishes six `PagingSource` queries, one per sort key and direction, each taking the genre
predicate as a parameter.

```kotlin
@Query("""
    SELECT * FROM movies
    WHERE :unfiltered OR movieId IN (
        SELECT movieId FROM movie_genres WHERE genre IN (:genres)
    )
    ORDER BY popularity DESC, movieId ASC
""")
fun pagePopularityDesc(
    unfiltered: Boolean,
    genres: List<String>,
): PagingSource<Int, MovieEntity>
```

**Six methods, not one `@RawQuery`.** Room verifies each against the schema at compile time, and a
column renamed under one of them fails the build rather than a test. The genre predicate is one
parameterised clause shared by all six, so the six differ only in their `ORDER BY`.

**OR semantics falls out of the subquery.** `IN` is a membership test, so a movie carrying two
selected genres matches once and no row can repeat. Nothing has to say so.

**The selection is one bound list.** Room expands `IN (:genres)` for however many are selected, so the
six signatures do not grow a parameter per genre.

**An empty selection is a boolean, not a missing clause.** `:unfiltered` short-circuits the predicate,
which is what keeps six methods from becoming twelve.

**Nulls last is written by hand.** `ORDER BY releaseDateEpochDay IS NULL, releaseDateEpochDay ASC`
and the same with `DESC`. SQLite's `NULLS LAST` arrived in 3.30, which is above what `minSdk` 26
reaches. The `IS NULL` term sits outside the direction, so absent dates sort last in both, which is
the rule the Kotlin comparator already held.

**The tiebreak is `movieId ASC` on all six and never reverses.** A total order is what stops rows the
user has already read from moving when a page lands. Without it an offset-paged query can serve one
row twice and skip another.

**Nothing reads the whole table.** `getTrendingMoviesFlow(): Flow<List<Movie>>` is gone. The summary
read `getMovieFlow(id)` stays, because the detail screen reads one row by key and is not paged.

---

## 7. The selection moves down

```kotlin
data class MovieSelection(
    val genres: Set<Genre>,
    val sort: MovieSort,
)

enum class SortKey { POPULARITY, TITLE, RELEASE_DATE }

enum class SortDirection { ASCENDING, DESCENDING }

data class MovieSort(val key: SortKey, val direction: SortDirection) {
    companion object {
        val DEFAULT = MovieSort(SortKey.POPULARITY, SortDirection.DESCENDING)
    }
}
```

All four move from `:feature:trending` to `:core:model`, and stop being `internal`.

**Filter and sort stopped being presentation the moment the database started executing them.** The
old placement argued that a selection belongs beside the screen that holds it, which was true while
the rules were pure functions over a list the screen already had. A `PagingSource` cannot be chosen
without the selection, and the DAO cannot see a feature module.

`MovieFiltering.kt` and `MovieSorting.kt` are deleted with the functions in them. Their rules survive
as the six queries and the predicate, and the tests that asserted them move to the DAO.

`MovieSort` stops being `Parcelable`: `:core:model` has no Android dependency and never gains one.
What the screen saves is the enum names of the key and the direction, alongside the genre names it
already saved.

---

## 8. The repository

```kotlin
interface TrendingMoviesRepository {
    fun trendingPagingFlow(selection: MovieSelection): Flow<PagingData<Movie>>

    suspend fun refresh(): DataError?
}
```

**The `Pager` is built in `:core:data`.** `paging-common` is pure Kotlin with no Android dependency,
so `PagingData` crosses the boundary the way `Flow` already does, and the feature never holds a DAO.
`:core:data` takes `paging-common`; `:feature:trending` takes `paging-runtime` and `paging-compose`.

```kotlin
internal const val PAGE_SIZE = 50

override fun trendingPagingFlow(selection: MovieSelection): Flow<PagingData<Movie>> =
    Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = TrendingRemoteMediator(local, remote),
    ) {
        local.trendingPagingSource(selection)
    }.flow
```

**`PAGE_SIZE` is this module's, as `TRENDING_COUNT` was.** How many movies a page holds is the
policy's, and the provider's own page size is a fact the source absorbs.

**`refresh()` stays, returning `DataError?`.** It is what the pull gesture and the retry actions call,
and what the mediator's `REFRESH` shares a body with. A read still cannot fail.

### The mediator

| `LoadType` | Does |
|---|---|
| `REFRESH` | Fetches with a null cursor, calls `replaceTrending`, returns `endOfPaginationReached = page.next == null` |
| `PREPEND` | Returns `endOfPaginationReached = true` immediately. The list only grows downward |
| `APPEND` | Reads the stored cursor, returns end if it is null, otherwise fetches, calls `appendTrending`, and ends on a null `next` |

A failure returns `MediatorResult.Error` carrying the `DataError`. Nothing is written, so the cache is
left exactly as it was.

**End of pagination is read from the cursor alone.** Never from a page yielding fewer rows than
asked, because deduplication makes a short yield ordinary.

---

## 9. The screen

`TrendingContent.Movies` carries `LazyPagingItems<MovieRowUiState>` rather than an
`ImmutableList`, and the six states resolve from paging's own load states beside the item count.

| Condition | State |
|---|---|
| `refresh is Loading` and `itemCount == 0` | `Skeleton` |
| `refresh is Error` and `itemCount == 0` | `Error`, read from the cause |
| `refresh is Error` and `itemCount > 0` | `Movies` plus a notice |
| `itemCount == 0`, refresh settled, selection non-empty | `EmptyFromFilter` |
| `itemCount == 0`, refresh settled, selection empty | `Error`, read from `EmptyResponse` |
| otherwise | `Movies` |

**The mapper stops narrowing and ordering.** It reads one `Movie` into one `MovieRowUiState`, and the
`when` that picked a body reads load states instead of a list. Everything else it owns, a cause into a
glyph and a sentence, a genre into a label, is unchanged.

**`EmptyFromFilter` is honest about being partial.** The copy names the situation as what is loaded so
far rather than as a verdict on the catalogue: a rare genre matches nothing in the first pages and
matches something a few pages later. Scrolling to the end of a short filtered list is a boundary, so
the mediator appends, and the list fills itself while the append indicator is on screen. The state
resolves by scrolling, which is what the screen already invites.

**The count on the summary row is gone.** "32 of 100" named a total the list no longer has. Active
narrowing is stated by the badge, which is unchanged.

**`activeSelectionCount`, the badge, the sheet, pull to refresh, the notice bar, and the scroll to top
on a changed selection are unchanged.** Scroll to top is now free: a changed selection builds a new
`PagingSource`, which starts at the top on its own.

```kotlin
val pagingFlow = selection
    .flatMapLatest { repository.trendingPagingFlow(it) }
    .cachedIn(viewModelScope)
```

**`cachedIn` sits after `flatMapLatest`, never inside it.** Inside, every chip tap would drop the cache
and refetch from the network instead of re-querying Room.

**`onLaunch` is deleted.** Paging fetches on first collection, so the launch refresh has nothing to
guard and the route asks for nothing. The pull gesture and both retry actions still call the paths
that reach the network.

---

## 10. Tests

What the deleted functions asserted moves to the DAO, against an in-memory database under
Robolectric. What paging adds is asserted at the mediator.

| Test | Fails when |
|---|---|
| Each sort key in each direction orders a fixture correctly | a comparator became a wrong `ORDER BY` |
| Absent release dates sort last ascending and last descending | the `IS NULL` term drifted inside the direction |
| Titles sort case-insensitively, including a non-ASCII pair | `titleSortKey` stopped being written or stopped being read |
| An empty selection returns every row | the short-circuit broke |
| One genre returns exactly the rows carrying it | the predicate is wrong on the simple case |
| Two genres return their union, each row once | OR semantics repeats a row, or became AND |
| A movie's genre rows and its rendered genre column agree after both writes | the two stores of one fact drifted |
| Re-appending a movie whose genres changed leaves only the new rows | a stale genre keeps matching a filter |
| A replace leaves no genre row without a movie | the two tables stopped being cleared together |
| Rows with an equal sort key keep a stable order across repeated queries | the tiebreak is missing and a page repeats or skips a row |
| A page written with repeats yields one row per id | deduplication stopped happening at the key |
| A second page repeating the first adds no rows and still advances the cursor | a re-served page reads as the end of the list |
| `REFRESH` clears the table and the cursor before inserting | a refresh appends onto a stale set |
| `APPEND` resumes from the stored cursor | a restart refetches from page one |
| A null `next` ends pagination, and a short yield does not | the end is inferred from a count |
| A failed append returns `Error` and writes nothing | a failure blanks a cache it should have left alone |
| A cursor survives the process, read back from the table | the resume point lives only in memory |

Screen tests feed `PagingData.from(...)` into `TrendingScreen`, so the six states are values a test
constructs rather than conditions it arranges. The goldens are unchanged in number and in what they
capture.

The live check gains one case: `page=501` answers 400, which is what fixes that ceiling as a
measured fact rather than a remembered one.

---

## 11. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D88 | Paging 3, a `RemoteMediator` over a Room `PagingSource` | Room stays the single source of truth and the screen still reads only the database, so offline read, the cached-list-first render and the notice-over-content rule all survive unchanged. Load states carry the failure the screen already folds into its state | A `PagingSource` straight onto the network, which has no cache to render offline and puts filter and sort back out of reach; a growing `LIMIT` window, which re-emits the whole list on every append and every write, and hand-rolls invalidation, retry and load state into a worse Paging 3 |
| D89 | The source takes a count and an opaque `Cursor`, and returns a page with the next one | A caller says where it left off without knowing how the provider counts. TMDB writes a page number, a cursor-paged provider writes its token, and a provider serving everything at once returns a null `next` on the first call. `next == null` is the only end of pagination | A page number, which with deduplication underneath cannot say which raw pages hold a given page without walking from the first; an offset, which makes every append re-walk or forces the source to hold walk state |
| D90 | `count` is a request the source need not meet exactly, and the returned list may hold repeats | TMDB's page size is fixed at twenty and no parameter overrides it, so a request for fifty is three pages and sixty movies, and re-ranking between requests puts a movie on two of them. Trimming would discard movies already paid for, and deduplicating here would resolve repeats inside one call while leaving repeats across two | The old promise of exactly `count` distinct movies, which existed to make one call a whole answer and has no meaning once the answer is a page |
| D91 | Deduplication is the upsert on the primary key, in the local source | A repeat inside one call and a repeat across two are the same event, and the key resolves both. The remote source keeps no dedupe and no fixture test for one, because the write already does it | Deduplication in the remote source, which handles only repeats inside one call and still needs the upsert for the rest, so the cost is a second mechanism for a subset of one problem |
| D92 | End of pagination is read from a null cursor, never from a short page | Deduplication makes a short yield ordinary: sixty movies can become seven rows when the provider re-serves what is held. A count-based end would stop the list wherever the provider happened to repeat itself | Ending on a yield below the page size, which is the standard shape and is wrong the moment a write deduplicates |
| D93 | No rank column | The provider's order is never rendered. The default is popularity descending and the three sort keys are the only orders the screen offers, so a rank column would be written on every append and read by nothing | A `rank` preserving the served order, which is a column, a migration and a per-append write for an ordering the product does not show |
| D94 | Genres are queried through a `movie_genres` cross-reference table, indexed on the genre, while `MovieEntity.genres` stays as the column a row renders | A selection resolves through an index rather than by testing a string on every row, and `IN` gives OR semantics with each movie once without a rule anyone maintains. The two stores are written from one list in one transaction, so the fact is duplicated where a `@Relation` would instead issue a second query on every page load to rebuild a line of text a column already holds | A delimited `LIKE` over the column alone, which tests a string per row and cannot take a bound list, so the query grows a parameter per genre; a `@Relation` replacing the column, which is the tidy read and costs a query per page to serve the row's own text; a nineteen-bit mask, which no index serves and which is a second genre vocabulary to keep in step |
| D103 | No foreign key between the two tables, and appended genre rows are deleted before they are inserted | One writer owns both tables and writes them in one transaction, so a constraint would enforce on every insert what the transaction already guarantees. `ON DELETE CASCADE` would cover the clear and not the upsert, because an upsert updates a row in place and cascades nothing, so the delete-then-insert has to be written regardless. One rule that holds for a movie arriving once and for one arriving again | A foreign key with cascade, which covers one of the two writes and leaves a reader to know which; relying on the upsert to replace genre rows, which accumulates both sets when a provider changes a movie's genres |
| D95 | Title ordering is an indexed `titleSortKey` column holding `title.lowercase()` | `COLLATE NOCASE` folds ASCII only, and the tail of a ten-thousand-row catalogue is largely foreign-language, where TMDB's `en-US` title falls back to the original. Folding in Kotlin and ordering in SQL keeps the behaviour the comparator had while letting an index serve the sort | `COLLATE NOCASE`, which is correct on the hundred rows measured and wrong on the ten thousand available; ordering the title column raw, which is the defect the comparator was chosen to avoid |
| D96 | Six `@Query` methods returning `PagingSource`, sharing one parameterised genre predicate | Room verifies each against the schema, so a renamed column fails the build rather than a test. A boolean short-circuits the predicate on an empty selection, which is what keeps six from becoming twelve | One `@RawQuery` and a Kotlin query builder, which is one method and a builder needing its own tests, with nothing verified at compile time; a `CASE` in the `ORDER BY`, which no index can serve and which full-sorts the table on every page |
| D97 | `MovieSelection`, `MovieSort`, `SortKey` and `SortDirection` move to `:core:model` and stop being `internal` | A `PagingSource` cannot be chosen without the selection, and the DAO cannot see a feature module. Filter and sort stopped being presentation when the database started executing them, so the types sit where both layers can name them | Passing primitives down, which puts a column name and a direction string in the UI layer; the feature holding the DAO, which breaks repositories being the sole entry to the data layer |
| D98 | The `Pager` is built in `:core:data`, and the repository publishes `Flow<PagingData<Movie>>` | `paging-common` is pure Kotlin, so `PagingData` crosses the boundary the way `Flow` already does and no library type leaks upward. The feature never holds a DAO or a mediator | The `Pager` in the view model, which needs the DAO above the data layer; a repository returning a list, which is the thing paging exists to stop |
| D99 | A single-row `trending_remote_keys` table holds the cursor | A mediator dies with the process, so an append after a restore would refetch from the first page. One row, because the list only appends and no per-row key is read | The cursor in memory, which loses the resume point on every process death; a key per row, which is what a prepending list needs and this one never prepends |
| D100 | `EmptyFromFilter` names what is loaded rather than the catalogue, and the append resolves it | A filter over a paged table filters a prefix. Reaching the end of a short filtered list is a boundary, so the mediator appends and matches arrive while the append indicator is on screen. The state resolves by the gesture the screen already invites | Auto-fetching until a page fills, which is an unbounded loop behind one chip tap; switching to `/discover` when a chip is tapped, which does filter and sort the real catalogue and stops the screen being trending the moment it is touched, runs two endpoints with two cursors and two orderings behind one list, and puts a second title collation in the app |
| D101 | The result count leaves the UI, and the badge stays | "32 of 100" named a total the list no longer has, and "32 of 214 loaded" states an implementation detail. The badge already says that something is narrowed, and one tap says what | A count over loaded rows, which reads as a fact about the catalogue and is not one |
| D102 | `onLaunch` is deleted | Paging fetches on first collection, so there is nothing to hold to once. The paths the user drives still call the refresh that reaches the network | Keeping the guard, which now guards a fetch nobody issues |

---

## 12. What is owed elsewhere

Each of these is corrected in its own pass. None is edited by this spec.

- **The brief.** The hundred, the ten-request cap, `AC-L1`, `AC-L11`, the summary row's count, and
  D6's client-side-over-the-fetched-hundred. The catalogue ceiling and the probe that found no filter
  or sort parameter belong in section 9.2 beside the page-size finding.
- **001.** Paging 3 leaves the deliberately-not-in-the-stack table. The filter-and-sort row reverses:
  it is SQL now, and the row should say why the ordering rules survived the move.
- **003.** The contract, `TrendingPage`, the cursor, and the removal of the walk, the trim, the
  request cap and the dedupe function. D31, D32 and the dedupe fixture go with them.
- **004.** Two columns, three indices, the `movie_genres` and remote-keys tables, `appendTrending`,
  the paging reads, and the loss of `getTrendingMoviesFlow`. D35 keeps the column and gains the
  cross reference beside it, and D36 narrows to `REFRESH` alone.
- **005.** `TRENDING_COUNT` becomes `PAGE_SIZE`, the read becomes `PagingData`, and D49 reverses.
  `MovieFiltering.kt`, `MovieSorting.kt` and their tests are deleted.
- **006.** The content case, the state table, the mapper's narrowing and ordering, `onLaunch`, the
  count, and the empty-from-filter copy.
- **The architecture doc.** `:core:data` gains the `Pager`, `:core:model` gains the selection types,
  and the data-layer diagram gains the mediator.
