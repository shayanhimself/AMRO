# 009: Pagination, and filter and sort in the API

The alternative to 008. The same problem, solved by asking the provider for the list the screen wants
rather than by assembling it. The trending idea is given up: the screen discovers a catalogue instead
of reporting a week.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

008 and this are two answers to one requirement, and only one is built. Section 12 states what each
costs, because that comparison is the deliverable whichever is chosen.

---

## 1. What the endpoint gives

`/discover/movie`, probed live on 2026-09-09 with the project's token.

| Finding | Measured |
|---|---|
| Paging is stable inside one walk | Page one twice returned an identical list, and page one and page two share no movie |
| The catalogue re-ranks between walks | A movie read near the end of one fetch arrives again in the next fetch under the same query |
| Genre composition is two-way | `with_genres=35\|27` returned 237,243 results, `35,27` returned 11,378. The pipe is a union, the comma an intersection |
| Every sort key the screen offers exists | `popularity`, `original_title` and `primary_release_date`, each ascending and descending |
| The page cap is 500 | `page=501` answers 400. `total_pages` reports 58,827 and is not reachable |
| A date sort drops undated movies | `primary_release_date.asc` opens on 1874 rather than on absent dates |

**One walk is internally stable, and two walks are not.** While a page is assembled no movie arrives
twice, so nothing is lost to repeats inside it. Between one fetch and the next the provider re-ranks,
and a movie the query already holds arrives again. The database resolves that, in section 6.

**The reachable set is still 10,000 movies**, now per query rather than in total. Switching endpoints
does not lift the ceiling, and `total_pages` must never be read as the end of pagination: it counts
what exists, not what is served.

**A date sort cannot reach an undated movie.** They are not ordered first, and anything ordered last
sits past page 500. The old rule placed absent dates last in both directions; here they are absent
from the ordering entirely. This is a behaviour change the product accepts, not one it can configure.

`include_adult` defaults to false and is not sent.

---

## 2. What this deletes

Every one of these exists in 008 and is not carried here.

| Gone | Because |
|---|---|
| Deduplication in the fetch policy | A repeat is one unique index, not code that walks a page against what is stored |
| The rule that a page's yield is not its size | Inside one walk a page yields what it holds |
| `titleSortKey` and its index | The provider orders, and its collation is the one on screen |
| Six `PagingSource` queries | One query, ordered by a stored position |
| The `movie_genres` cross-reference table | The provider filters |
| Nulls-last written by hand, and the id tiebreak | The provider's order is total and arrives as a position |
| Filtering a prefix, and the empty state that resolved by scrolling | The filter reaches the whole catalogue |

What survives from 008 unchanged: Paging 3 over a `RemoteMediator`, `PagingData` published from
`:core:data`, `MovieSelection` in `:core:model`, Room as the single source of truth, no launch-refresh
guard, no result count on screen, and the rule that a failure is surfaced when the content left on
screen would otherwise assert something false.

---

## 3. The remote contract

```kotlin
interface MovieRemoteDataSource {
    suspend fun getMovies(
        selection: MovieSelection,
        count: Int,
        after: Cursor?,
    ): NetworkResult<MoviePage>

    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}

data class MoviePage(
    val movies: List<Movie>,
    val next: Cursor?,
)

@JvmInline
value class Cursor(val value: String)
```

**The selection reaches the source.** This is the whole difference from 008, where the source knew
only how many movies to return. A source now narrows and orders its own catalogue, and what it is
asked for is the app's own genre type and the app's own sort, never a provider's vocabulary.

**`count` is a floor, and the walk never trims.** The source concatenates whole provider pages until
it holds at least the count. Cutting the last page would discard movies already fetched and leave the
cursor pointing past them, so the next fetch would skip what was cut. The repository's page size is a
multiple of TMDB's twenty, so this source meets the count exactly without a rule saying it must.

**A page that fails fails the call.** The walk discards what it read, because a caller cannot tell a
list cut short from one that is simply short.

**The cursor stays opaque.** TMDB's pages are stable and numbered, so a page number would be honest
here, and the cursor still earns its place: a provider paging by keyset writes its own token, and
`next == null` is the one end-of-pagination signal every source can give. Ending on the cursor is what
keeps `total_pages` from being believed.

---

## 4. What a source must now be able to do

008 asked the weakest thing that still delivers a default sort: a source need only order its own
catalogue. This asks for more, and the requirement is what selects the next source rather than the
other way round.

**A source backing this screen must filter by genre and sort by all three keys in both directions.**

| Source | Meets it | On what |
|---|---|---|
| **TMDB** | Yes | `/discover/movie`, seven sort fields with direction, genre with AND and OR |
| **Streaming Availability** | Yes | `/shows/search/filters`, `original_title`, `release_date` and four popularity windows, `order_direction`, `genres_relation`. Requires a country |
| **MDBList** | Yes | `/catalog/movie`, thirteen sort keys with `sort_order`, repeatable genre with `genre_mode`. Its schema changed shape within a week, so it needs a re-fetch step rather than a hard-coded enum |
| **Watchmode** | Nearly | `/list-titles` sorts on four fields with direction, but its genre list documents no boolean, so OR semantics is unconfirmed |
| **TheTVDB** | No | `/movies/filter` sorts on three values with no direction control, takes one genre with no boolean, requires a country and a language, and documents no paging |
| **Trakt** | No | Rich filters, and no sort at all. Its endpoints each carry a fixed ordering |

So four sources can back this screen and two cannot, against eleven of twelve that could satisfy 008.
Trakt is the sharpest loss: it is the one source in the survey whose trending list is itself
filterable, and it cannot serve a screen that also chooses the order.

**A source that cannot do both cannot back this screen**, and no client-side fallback is built.
Shipping one would mean two filter implementations and two orderings behind one list, and no way for
a reader to tell which produced what is on screen. A source that filters but cannot sort remains
useful for movie details.

---

## 5. Schema

Three tables. The movie facts are held once, the orderings are held per query.

```kotlin
@Entity(tableName = "movies")
internal data class MovieEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
)

@Entity(
    tableName = "query_results",
    primaryKeys = ["queryKey", "position"],
    indices = [Index(value = ["queryKey", "movieId"], unique = true)],
)
internal data class QueryResultEntity(
    val queryKey: String,
    val position: Int,
    val movieId: String,
)

@Entity(tableName = "query_keys")
internal data class QueryKeyEntity(
    @PrimaryKey val queryKey: String,
    val nextCursor: String?,
)
```

**`position` is the provider's ordering, made durable.** The app cannot reproduce it: TMDB's
collation is its own, its popularity is unpublished, and its date ordering omits rows the app holds.
So the order is data rather than a comparator, and it is the column every read sorts on. This is the
rank column 008 rejected, and here it is the mechanism rather than a spare.

It counts up from zero within a query and is never rewritten.

**A movie holds one place per query, enforced by the unique index on the key and the id.** The
provider re-ranks between fetches, so a movie stored near the end of one arrives again at the start of
the next, and two rows for one movie is a duplicate key the list refuses. A repeat is dropped, and the
place the movie first took is the one it keeps: that is where the user is already looking at it.

**`MovieEntity` loses `titleSortKey` and every sort index**, and keeps `genres` as the delimited
string a row renders. Nothing queries it.

**A query key is the app's own, not a provider's.** It is built from the selection alone, so the same
narrowing and ordering is one cache whichever source answered it:

```
"ACTION,COMEDY:POPULARITY:DESCENDING"
```

Genre names sorted, so a selection made in either order is one key. An empty selection leaves the
first segment empty. Names rather than ordinals, for the reason the stored genres already use them.

**No foreign key from `query_results` to `movies`.** One writer owns both and writes them in one
transaction. The join is what a read does, not what a constraint enforces.

---

## 6. Writes and reads

```kotlin
interface MovieLocalDataSource {
    fun moviesPagingSource(queryKey: String): PagingSource<Int, Movie>

    suspend fun replaceQuery(queryKey: String, page: MoviePage)

    suspend fun appendQuery(queryKey: String, page: MoviePage)

    suspend fun queryCursor(queryKey: String): Cursor?

    // the detail reads and write are unchanged
}
```

**`replaceQuery` clears that query's results and cursor, then writes the first page.** One
transaction. It clears one query's rows and touches no other query and no movie row: a refresh under
one selection must not discard a list cached under another.

**`appendQuery` upserts the movies and inserts result rows after the last position stored.** It reads
that position inside its own transaction rather than taking it from the caller: a repeat is dropped
rather than stored, so how many places a fetch takes is known only once it is written. One
transaction, so the rows and the resume point cannot disagree.

**Movie rows are upserted and never deleted.** A movie held under one query is the same movie under
another, and the detail screen reads this table by key.

**A write tells the reads it handed out that they are stale.** The database registers its own table
observer while the first read of that table is running, so a write landing inside that window reaches
a reader that is already showing an empty list and never hears there is more, which is the shape of
every cold start. The local source holds the paged reads open under each key and invalidates them
after it writes.

```kotlin
@Query("""
    SELECT movies.* FROM query_results
    JOIN movies ON movies.movieId = query_results.movieId
    WHERE query_results.queryKey = :queryKey
    ORDER BY query_results.position
""")
fun moviesPagingSource(queryKey: String): PagingSource<Int, MovieEntity>
```

**One query, and it takes no selection.** The selection was resolved into a key before anything
reached the database, so nothing here branches on a sort or a genre. The six methods 008 needed are
one, and the ordering is a column rather than six `ORDER BY` clauses.

---

## 7. Nothing is evicted

Every query's results are kept. A selection the user has run before renders from cache with no
network, which is the only offline story this design has, and each row is a key, an integer and an
id.

Growth is bounded by what the user actually opened, which is the argument the detail rows already
make. A schema change drops the database, and everything here is re-fetchable.

**Movie rows outlive every query that referenced them.** Pruning them by membership is what would
break a cached detail opening offline.

---

## 8. Offline

**A query with no cached rows and a failed fetch is the full-screen error state**, even while the
database holds ten thousand movies under a different key.

This follows the rule the product already has rather than bending it: the failure is surfaced because
there is nothing on screen, and what is cached elsewhere is an answer to a question the user did not
ask. Filtering the other query's rows in Kotlin to fill the gap would be a second filter
implementation, ordered by a comparator the provider does not use, producing a list that differs from
the one the same selection shows online.

So the offline surface is exactly the set of selections the user has already run, and it degrades to
an error outside it. This is the sharpest cost of the approach and it is not mitigated.

| Situation | Screen |
|---|---|
| Cached rows for this key, refresh failed | The list, plus the notice bar |
| No rows for this key, refresh failed | Full-screen error with Retry |
| No rows for this key, refresh returned nothing | `EmptyFromFilter` |

`EmptyFromFilter` is close to unreachable in production: every one of the nineteen genres returns six
figures of results. It is kept because it costs one branch and it is the honest state when a provider
does answer with nothing.

---

## 9. Mapping the selection onto the provider

`TmdbDiscoverMapper.kt`, beside the existing mappers, and the only file that knows what TMDB calls
these things.

| App | TMDB |
|---|---|
| `genres`, empty | `with_genres` omitted |
| `genres`, one or more | `with_genres`, TMDB ids joined with `\|` |
| `POPULARITY` | `popularity.desc` or `.asc` |
| `TITLE` | `original_title.asc` or `.desc` |
| `RELEASE_DATE` | `primary_release_date.desc` or `.asc` |

**The genre table gains a direction.** Today it reads a provider id into a `Genre`; now it also writes
a `Genre` back out as an id. One table, read both ways, and a test asserts the round trip so the two
directions cannot disagree.

**Title ordering is the original title, not the localised one.** `title.asc` exists and sorts what the
row displays, and the app does not use it: the app sends no language, so a localised sort would order
by a string the provider chose while the app holds another. Ordering by the original title is one
rule that holds whatever a row renders.

The pipe is what makes the genre filter a union, which is the OR semantics the product asks for.

---

## 10. The screen

Renamed. The app bar reads Discover, and nothing on it claims a week.

The state, the content cases, the badge, the sheet, pull to refresh and the notice bar are all as 008
left them. What changes is behind them:

- The mapper reads one `Movie` into one row and narrows and orders nothing.
- A changed selection is a different query key, so `flatMapLatest` builds a new `Pager` and the list
  starts at the top on its own.
- `cachedIn` sits after `flatMapLatest`, so a chip tap re-queries Room rather than the network when
  that selection is already cached.
- The result count stays off the screen. `total_results` is available here and is not shown: it
  counts a catalogue the list cannot reach past ten thousand.
- Refreshing is the pager's own. Pull to refresh and Retry both restart the load already defined, and
  a repository method that also wrote would race the mediator for the same rows. A failure reaches the
  screen through the load state, carrying the `DataError` behind it.
- The screen takes what to render rather than what to page: a body derived from the refresh load state
  and the row count, and the rows behind a small interface. Paged items present only inside a running
  app, so a preview and a screen test both draw the rows as a plain list.

---

## 11. Tests

| Test | Fails when |
|---|---|
| A selection and its reverse produce one query key | the same narrowing caches twice |
| An empty selection produces a key with an empty genre segment | the unfiltered list collides with a filtered one |
| Every `Genre` maps to a TMDB id and back | the two directions of the table disagree |
| Two genres map to a pipe-joined parameter | the filter becomes an intersection |
| Each sort key and direction maps to its `sort_by` token | an ordering silently becomes the provider's default |
| A walk concatenates whole provider pages in order until it holds the count | the source under-delivers, trims or reorders |
| A page that fails after the first fails the call | a part of a list is returned as the whole |
| Positions are ascending across an append | a page lands out of order or overwrites another |
| A movie an append repeats keeps the place it first took | one movie holds two places in a list and the keys collide |
| A write invalidates a read already open over that query | a list that was empty when the fetch began stays empty |
| `replaceQuery` clears one key and leaves another intact | a refresh under one selection discards another's cache |
| An append leaves movie rows for queries that no longer reference them | a movie the user opened stops opening offline |
| A read joins and orders by position, not by any column on the movie | an ordering the provider chose is re-derived locally |
| A cursor past the page cap ends pagination | `total_pages` is believed and the list walks into a 400 |
| A key with no rows and a failed refresh is the error state | the offline rule bends toward another query's cache |
| A key with cached rows and a failed refresh is the list plus a notice | a failure destroys content the user can still use |

The live check gains two cases: `page=501` answers 400, and `with_genres` with a pipe returns more
results than either genre alone. Both fix as measured facts the two things this design rests on.

Screen tests feed a plain list of rows and a body into a stateless screen. Which body a load state
and a row count produce is a unit test of its own.

---

## 12. What it costs against 008

Neither is dominant, and the choice is a product one.

| | 008, in the app | 009, in the API |
|---|---|---|
| Deduplication | Required inside a walk, an upsert | Required between walks, a unique index |
| Ordering | Six SQL queries, an index each, a sort key column | One query, a stored position |
| Genre filter | A cross-reference table | A request parameter |
| Filter honesty | Narrows what is loaded, resolves by scrolling | Narrows the catalogue |
| Undated movies | Sort last, in both directions | Unreachable under a date sort |
| Reach | 10,000 movies | 10,000 per query |
| Offline, a selection already used | Works | Works |
| Offline, a new selection | Filters the cache | Full-screen error |
| Offline, first launch | The cached list | The cached list for that key only |
| Source requirement | Order its own catalogue. Eleven of twelve surveyed can | Filter and sort our way. Four of twelve can |
| Trakt as a second source | Usable | Excluded, having no sort |
| The screen | Trending, a claim about a week | Discover, a claim about a catalogue |

**008 keeps the product's identity and its offline behaviour, and pays for them with machinery.**
**009 deletes the machinery and pays with the week, the offline filter, and two thirds of the sources
that could ever replace TMDB.**

The deciding question is not which is cleaner. It is whether the list is a claim about now, which is
what the offline rule and the second-source contract were both shaped around.

---

## 13. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D104 | The screen reads `/discover/movie` and stops being a trending list | The provider applies no filter and no sort to its trending list, measured across sixteen parameters, so a filterable ordered list and a trending list are two different endpoints. Choosing the filterable one is what puts the ordering and the narrowing where the whole catalogue is | Keeping `/trending` and filtering what is loaded, which is 008; running both and switching on whether a chip is tapped, which is two endpoints, two cursors and two orderings behind one list, and a screen whose meaning changes under the user |
| D105 | The selection reaches the remote source, and a source must filter by genre and sort by all three keys in both directions | The requirement is what selects the next source. Four of the twelve surveyed can meet it, against eleven that could order their own catalogue, and naming that plainly is worth more than a contract that pretends otherwise | A capability flag per source with a client-side fallback, which ships two filter implementations and two orderings and leaves no way to tell which produced the list on screen |
| D106 | A movie holds one place per query, enforced by a unique index on the key and the id, and a repeat keeps the place it first took | Paging is stable inside one walk and the catalogue re-ranks between them, so a movie already stored under a query arrives again in the next fetch under it. Two rows for one movie is a duplicate key the list refuses, and the first place is the one the user is already looking at it in | Comparing each fetched movie against what is stored in Kotlin, which is a read before every write for a condition the database states once; keeping both rows and making the read distinct, which charges every page of every read for a boundary case |
| D107 | The provider's ordering is stored as a `position` and every read sorts on it | The ordering cannot be recomputed: the collation is the provider's, the popularity is unpublished, and the date ordering omits rows the app holds. So it is data. One column replaces a sort key column, three indices and six queries | Re-deriving the order in SQL, which produces a different list from the one the provider paged and makes page boundaries disagree; storing the order implicitly as insertion order, which no `ORDER BY` can name |
| D108 | A query key built from the selection alone, with genre names sorted | The key is the app's own, so one cache serves a selection whichever source answered it and whichever order the user picked the chips in. Names rather than ordinals, for the reason the stored genres already use them | A key carrying the provider's parameters, which caches the same selection twice if a mapping changes; a hash, which is opaque in a database inspector for no gain |
| D109 | Every query's results are kept, and movie rows are never pruned | A selection the user has run before is the only thing this design can serve offline, so discarding it removes the last of that. Each result row is a key, an integer and an id, and growth is bounded by what the user opened | Keeping only the active query, which makes returning to the unfiltered list a network round trip and leaves nothing offline; an age bound, which is a storage budget the MVP does not have |
| D110 | A query with no cached rows and a failed fetch is the full-screen error, with no fallback to another query's cache | The rule is already that a failure surfaces when there is nothing on screen. Filtering another query's rows in Kotlin would be a second filter implementation ordered by a comparator the provider does not use, producing a list that differs from what the same selection shows online | Falling back to a client-side filter over whatever is cached, which is 008's machinery kept alongside this one and a list whose ordering depends on whether the network answered |
| D111 | End of pagination is read from the cursor, and `total_pages` is never read | The provider reports 58,827 pages and refuses page 501. A list that believed the count would walk into a 400 | Trusting `total_pages`, which is a fact about the catalogue and not about what is served; counting rows against `total_results`, which is the same error one layer down |
| D112 | Title ordering is `original_title`, not the localised title | The app sends no language, so it holds original titles and a localised sort would order by strings it does not have. One rule that holds whatever a row renders | `title.asc`, which sorts what the row displays and orders a list the app cannot reproduce or test against its own data |
| D113 | The genre table is read in both directions, with a round trip test | One table already maps a provider's ids onto `Genre`, and the filter needs the reverse. Two tables could disagree; one read both ways cannot | A second table for the outbound direction, which is the same facts twice and a drift waiting to happen |
| D114 | An append derives its own start position inside the transaction that writes | A fetch takes fewer places than it carries whenever the provider repeats a movie, so no caller can state where the next one begins. Reading the last position where the write happens also closes the gap between choosing a position and taking it | Passing the start in from the mediator, which counts what was requested rather than what was stored and drifts by one place per repeat |
| D115 | The walk returns whole provider pages, so `count` is a floor | Trimming would discard movies already fetched and leave the cursor pointing past them, so the next fetch would skip what was cut. A page size that is a multiple of the provider's meets the count exactly without a rule for it | Trimming to the count and resuming mid-page, which needs a cursor addressing an offset inside a page that the provider cannot express |
| D116 | The local source invalidates the paged reads it handed out, after it writes | The database registers a table observer while the first read of that table runs, so a write inside that window is never delivered and the screen holds an empty list against a full table. That window is every cold start. The writer knows the key it wrote and the reads open over it | Waiting on the database's own invalidation, which loses exactly the first write after launch |
| D117 | Refreshing is the pager's, and no repository method starts one | The mediator is the one writer of a query's rows, and a refresh that also wrote would race it for them. The cause of a failed load reaches the screen through the load state instead | A repository `refresh()`, which is a second path into the same tables and a second answer to whether the list is loading |
