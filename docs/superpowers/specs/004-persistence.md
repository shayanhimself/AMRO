# 004: Persistence

`:core:database`. Room is the single source of truth, so this module holds what every screen
observes: two tables, the contract the data layer reads them through, and the translation between a
row and a model type.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

---

## 1. The contract

```kotlin
interface MovieLocalDataSource {
    fun getTrendingMoviesFlow(): Flow<List<Movie>>

    suspend fun replaceTrending(movies: List<Movie>)

    fun getMovieFlow(id: String): Flow<Movie?>

    fun getMovieDetailFlow(id: String): Flow<MovieDetail?>

    suspend fun writeMovieDetail(detail: MovieDetail)
}
```

This mirrors `:core:network`: the module root holds a contract in model types, and everything that
knows Room sits behind it and is `internal`. A repository then depends on two interfaces and imports
neither library, which is what makes "Room types never leave this module" a compile-time fact rather
than a convention. Room can be an `implementation` dependency, and that it can is the proof.

**Nothing here is timestamped, and no layer holds a clock on its account.** A read serves what is
cached and a fetch replaces it, on both tables, so no rule anywhere compares a stored moment against
now. Every type on this contract is a model type, and no test in the project has to move time.

`getTrendingMoviesFlow()` emits in an unspecified order. Every consumer sorts, and no column here carries the
provider's ranking.

```
core/database/src/main/kotlin/com/shayan/amro/core/database/
    MovieLocalDataSource.kt
    RoomMovieLocalDataSource.kt
    AmroDatabase.kt
    entity/MovieEntity.kt
    entity/MovieDetailEntity.kt
    dao/MovieDao.kt
    dao/MovieDetailDao.kt
    mapper/MovieEntityMapper.kt
    di/DatabaseModule.kt
```

`:core:model` and the coroutines artifact become `api` dependencies, because `Movie` and `Flow` are
both on that contract.

---

## 2. Schema

Two tables, each keyed on the movie's id, and each with exactly one writer. That id already carries
the source that issued it, so this module holds no source of its own.

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

@Entity(tableName = "movie_details")
internal data class MovieDetailEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
    val overview: String?,
    val tagline: String?,
    val backdropSmall: String?,
    val backdropLarge: String?,
    val runtimeMinutes: Long?,
    val status: String,
    val budget: Long?,
    val revenue: Long?,
    val ratingAverage: Double?,
    val ratingCount: Int?,
    val imdbId: String?,
)
```

**The detail table carries its own copy of the movie columns rather than a key into the other
table.** A movie the user has opened keeps its detail long after the trending set has moved on, and
opening it with no network renders from that cache. A join onto `movies` returns nothing once a
refresh has dropped the row, and repairing that by having the detail write also fill `movies` puts
non-trending rows in the trending table, which then needs a membership marker and makes the trending
read a join too. Two problems bought to avoid duplicating six columns on the tens of rows a user
actually opens.

**Those shared columns are declared twice, flat, rather than shared through an `@Embedded` class.**
Each entity then reads as the table it is, and the primary key is a column the reader can see. The
wire shapes a layer down already declare the same fields twice for the same reason, because that is
what TMDB sends. What is duplicated is eight declarations and the few lines that build a `Movie` from
them, all of it mechanical, and both tables carry a round trip test, so a drift between them fails
rather than ships.

**Neither table carries a timestamp.** Both screens work the same way: the cache renders at once and
a request goes out regardless, so what is on screen is replaced when an answer arrives. Nothing asks
how old a row is, so nothing stores it, and the difference between the two screens is what a failure
does rather than when a fetch happens.

**Every column is a primitive, and one mapper owns the translation.** `ImageRef` is two columns, a
`Rating` is two more, and a runtime is a whole number of minutes. None of those is a type converter's
one-to-one exchange, so half the translation is mapper work whichever way the rest goes. Putting the
other half in converters would split one translation across two mechanisms and two test files.

| Model | Columns | Reads back as |
|---|---|---|
| `Movie.id` | `movieId` | The whole id, which carries its source, so two providers' identical ids are two rows |
| `List<Genre>` | `genres`, constant names joined on a comma | An unrecognised name is dropped |
| `LocalDate?` | `releaseDateEpochDay`, a `Long` | A total round trip with no parse that can fail |
| `Duration?` | `runtimeMinutes`, a `Long` | The granularity TMDB publishes and the screen renders |
| `ReleaseStatus` | `status`, the constant name | An unrecognised name reads as `UNKNOWN` |
| `ImageRef?` | two URL columns per image | Null when both are absent |
| `Rating?` | `ratingAverage`, `ratingCount` | Null when the average is absent, and an average with no count when only the count is |

**A column takes whatever type the conversion hands it.** SQLite stores every integer in one
`INTEGER` column whatever the Kotlin type says, so a narrower type buys no storage and costs a
conversion: `toEpochDays` and `inWholeMinutes` both give a `Long`, and a vote count is already an
`Int` on the model and stays one. Nothing in the mapper widens or narrows a number.

Genres store the constant name and not the ordinal, because alphabetising the enum later would
silently change what every cached row means. Dropping an unrecognised name is the rule the source's
own genre table already follows, and it is why the status column is mapped by hand as well: Room's
built-in enum handling throws on a name it does not know, and a read of a cache must never fail.

There is no genre table and no movie-to-genre cross reference. Nothing queries by genre in SQL,
because filtering is pure Kotlin over the fetched set, so a cross reference would buy a table, three
hundred rows per refresh and a relation query in exchange for a query nobody writes.

---

## 3. Writes

```kotlin
@Transaction
suspend fun replaceAll(movies: List<MovieEntity>) {
    deleteAll()
    insertAll(movies)
}
```

**A refresh replaces the trending table outright.** The table is the last successful response and
holds nothing else, so there is no row to preserve and no half-updated row to explain. The walk
already succeeds or fails whole, so there is never a partial set to write.

The transaction is what makes the write atomic. A crash between the delete and the insert rolls back,
so the table holds the set it held before rather than nothing.

A subscriber sees one emission carrying the new hundred and never an empty list between the two.
Room coalesces the invalidation, so that holds whether or not the write is transactional, and the
test asserting it guards the emission rather than the transaction.

Rejected: upserting the new set and deleting what it does not contain, which binds a hundred ids to
answer a question the table's own contents already answer.

**Writing a detail replaces that one row and touches nothing else.** A fetch is issued every time a
movie is opened, so the write is an upsert whose job is to replace what the screen is already
rendering, the same shape as the trending replace one row wide.

A detail row is never pruned. Not by trending membership, which is what would break a cached movie
opening offline, and not by age, because a row exists only for a movie the user opened and growth is
bounded by their own behaviour. Storage budgets are out of scope for the MVP, and eviction later is a
column and a query rather than a redesign.

---

## 4. Reads

`getTrendingMoviesFlow()` and `getMovieFlow(id)` read the trending table, `getMovieDetailFlow(id)` reads
the detail table, and nothing joins them.

**`getMovieFlow(id)` is what the detail screen renders before its own fetch returns.** The title, the
poster and the genres are already held, so a screen reached from the list opens with them rather than
with a bare skeleton. It emits null when the movie is not in the current trending set, which is the shape
the screen falls back to, and it changes nothing about the three states that screen has.

This is a second read rather than a navigation key carrying a whole `Movie`, so the screen draws
from the single source of truth like every other screen and does not depend on where its caller was
standing.

---

## 5. The database

```kotlin
@Database(
    entities = [MovieEntity::class, MovieDetailEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class AmroDatabase : RoomDatabase()
```

Built with `AndroidSQLiteDriver`, the platform's own SQLite, and a query coroutine context of
`Dispatchers.IO`, which the driver requires.

Nothing here needs a SQLite newer than the oldest supported device ships. Every statement is a
`SELECT`, an `INSERT`, an `UPDATE` or a `DELETE`, ordering is Kotlin's rather than the database's, and
the upsert compiles to an insert with an update behind it rather than to `ON CONFLICT DO UPDATE`. A
bundled SQLite would buy one version across every device and a test running the engine the app ships,
and cost 1.3 MB of native library per ABI.

**A schema change drops the database.** `fallbackToDestructiveMigration(dropAllTables = true)`, and
no `Migration` in the MVP. Everything stored here is re-fetchable, so a migration would be code
written to preserve what one request restores. The exported schema is still checked in, because it
is what makes a real migration possible on the day a table stops being a pure cache.

`DatabaseModule` provides the database and the two DAOs, and binds `RoomMovieLocalDataSource`.

---

## 6. Tests

Two clusters. The mapper's are plain JVM tests over values, and the rest run against an in-memory
database and the real query compiler.

Whether those need Robolectric is checked rather than assumed, and they do: Room's in-memory builder
takes a `Context` on Android, so the `amro.robolectric` plugin is applied.

| Test | Fails when |
|---|---|
| Every `Genre` survives a round trip | a constant is unstorable |
| An unrecognised genre name is dropped, and an empty list round-trips | a cached row from another build takes down a read |
| An unrecognised status name reads as `UNKNOWN` | the same, for the column Room would have thrown on |
| A rating with a count, a rating without one, and no rating each round-trip | an unrated film comes back scoring nought |
| A movie with no poster, no date and no genres round-trips with those absent | a null becomes a zero or an empty string on the way through |
| A detail with no tagline, no financials, no runtime and no imdb id round-trips with all of them null | the omission rule breaks below the screen that applies it |
| A replace emits once, with the new set, and never an empty list | a refresh blanks the screen mid-write |
| A replace removes the rows the new set does not contain | a movie that stopped trending stays on the list |
| A detail written for a movie no longer in the trending table still reads back | a cached movie stops opening offline |
| Writing a detail leaves the trending table untouched | the two writers stop being independent |
| A detail read with nothing cached emits null | a cache miss reads as a failure |
| A summary read for a movie a refresh dropped emits null | the detail screen's fallback is never exercised |
| Writing a detail twice leaves one row, carrying the second | a refetch appends instead of replacing |

---

## 7. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D34 | Two tables, and the detail table carries its own copy of the movie columns rather than a key into the trending table | A cached detail has to outlive the trending set, which a join does not survive. Each table then has exactly one writer, and a write to one means nothing to the other | A foreign key into `movies`, which returns nothing once a refresh drops the row, and whose repair puts non-trending rows in the trending table and needs a membership marker; one table with nullable detail columns, where `@Upsert` from the trending write would null the detail half and "has a detail" becomes a check across eight columns |
| D35 | Genres store as their constant names joined on a comma, with no genre table and no cross reference | Filtering is pure Kotlin over the fetched set, so nothing queries by genre in SQL and a cross reference costs a table, three hundred rows a refresh and a relation query for no query. Names rather than ordinals, because alphabetising the enum would otherwise change what every cached row means | A cross-reference table, which is the right shape for a genre query that does not exist; JSON, which adds a serialization dependency and escaping to a list of uppercase identifiers; a nineteen-bit mask, which is compact, unreadable in a database inspector, and a second genre vocabulary to keep in step |
| D36 | A refresh deletes the trending table and inserts the new set in one transaction | The table is the last successful response and holds no app-owned column, so replacing it needs no diff. The transaction makes the write atomic, so a crash between the two statements leaves the previous set rather than an empty table, and a subscriber sees one emission carrying the new set | Upsert plus a delete of what the new set does not contain, which binds a hundred ids to answer what the table already answers, and leaves "what is a half-updated row" open |
| D37 | A detail row is never pruned, by membership or by age | Pruning by membership is what stops a cached movie opening offline, and a row exists only for a movie the user opened, so growth is bounded by their own behaviour. Eviction later is a column and a query on a database that is dropped on any schema change anyway | Cascading from the trending table, which breaks the offline read the product requires; an age bound, which is a storage budget the MVP does not have |
| D38 | Neither table is timestamped, and there is no freshness window anywhere | Both screens already render the cache at once and fetch regardless, so a stored moment would be compared against now only to decide something that has already been decided. It removes a column, a parameter on every write, a clock injected into the layer above, and the tests that would have moved time. What separates the two screens is what a failure does, which is a rule about the claim on screen and not about an age | A `cachedAt` on the detail table with a day-long window, which is one request saved on a screen the user explicitly asked for, paid for with a column, a clock and a rule; a window on both tables for symmetry, which is the same cost twice |
| D39 | Entities hold primitives and one mapper owns the translation, with no type converters | An image is two columns, a rating is two more, and a runtime is minutes: none is a converter's one-to-one exchange, so the mapper exists either way. One mechanism and one test file beats splitting one translation across two | Type converters for the fields that fit one, which puts half the translation somewhere else; Room's built-in enum handling, which throws on a name it does not know, and a read of a cache must never fail |
| D40 | The module publishes `MovieLocalDataSource` in model types, and everything Room touches is `internal` | It makes "Room types never leave this module" hold at compile time rather than by convention, lets Room be an `implementation` dependency, and leaves `:core:data` importing neither library. Symmetric with the remote contract, so a repository reads as one policy over two sources | DAOs returning entities, where the annotated types are the module's public surface and every caller maps them; a repository holding the DAO directly, which puts Room in the layer that must not name a library |
| D41 | A schema change drops the database, and the MVP writes no migration | Everything here is re-fetchable, so a migration is code preserving what one request restores. The exported schema is checked in anyway, because it is what a real migration would be written against | Hand-written migrations from version one, which is ceremony over a cache |
| D42 | The detail screen's pre-fetch content comes from a read of the trending table, not from the navigation key | The screen draws from the single source of truth like every other screen, so what it renders does not depend on where its caller was standing, and the key stays one id. A movie absent from the current set reads as null, which is the fallback | A navigation key carrying a whole `Movie`, which routes app data around the database and serialises a record on every save of the back stack |
| D43 | The columns both tables share are declared twice, flat, rather than once in an `@Embedded` class | Each entity reads as the table it is, the primary key is a column the reader can see, and the module holds no type that is neither an entity nor a model. The duplication is eight declarations and the lines that build a `Movie` from them, all mechanical, and a round trip test on each table fails if the two drift. The wire shapes below already declare the same fields twice, because that is the shape the provider sends | An `@Embedded` shared class, which declares the shape once and costs a third kind of type in the module and a key naming a column that appears nowhere in the entity |
| D44 | A column takes the type its conversion produces, so an epoch day and a runtime are both `Long` | SQLite holds every integer in one `INTEGER` column whatever the declaration says, so a narrower type buys no storage and costs a conversion at the boundary. `toEpochDays` and `inWholeMinutes` produce a `Long`, a vote count is an `Int` on the model and stays one, and nothing in the mapper widens or narrows a number | `Int` where the range obviously allows it, which is true and irrelevant: it reads as a storage decision that SQLite does not implement |

---

## 8. What this changes elsewhere

- The tech stack spec's schema row describes movie, detail and genre tables with a cross reference.
  There are two tables and no genre table.
- The architecture doc's `:core:database` entry gains the contract, and its rule about Room types is
  now enforced by that contract rather than by convention.
- The repository spec inherits two interfaces of the same shape and one policy for both screens:
  serve the cache, fetch anyway, replace on success. It inherits no clock and no freshness rule,
  which is most of what its brainstorm was going to spend its time on.
- The brief's detail freshness rule is gone. Its line saying a fresh cached detail makes no request
  is contradicted, and D17 with it. What survives unchanged is that a cached detail renders
  immediately, that a refetch behind it fails silently, and that the failure reaches nobody while
  the content on screen stays true.
- The detail screen keeps its three states, and its loading state may carry a title, a poster and
  genres from the summary read. It renders a bare skeleton only for a movie absent from the current
  trending set.
