# 010: New sources by detail enrichment

`:core:model`, `:core:network`, `:core:data` and `:core:database`. A second kind of source: one
that adds fields to a movie another source already issued, keyed on the movie's external id, rather
than issuing movies of its own. The first such field is a movie's ratings from more than one
authority.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

---

## 1. Two kinds of second source

003 §6 and the architecture doc own the source that rivals TMDB: it issues its own movie ids, serves
its own trending list, and resolves its own details. Composing more than one of those is a set of
`MovieRemoteDataSource`, a routing method that reads the source out of a qualified id, and a product
decision about combining trending lists. None of it is built.

This spec owns the other kind, and it is cheaper because it shares almost nothing with that one.

| | Rival primary source | Enrichment source |
|---|---|---|
| Issues movie ids | Yes, its own space (`trakt:5678`) | No. It never mints an id |
| Serves trending | Yes | No |
| Resolves a whole detail | Yes | No. It fills a slice of one |
| Keyed on | The qualified id, read back to its own id | `imdbId`, the external id every detail already carries |
| Touches `MovieId` and id routing | Yes | Never |
| Contract | `MovieRemoteDataSource` | `RatingsRemoteDataSource`, this spec |

**An enrichment source adds fields to a movie that already exists.** OMDb returns a film's IMDb,
Rotten Tomatoes and Metacritic ratings in one call, addressed by `imdb_id`. It has no trending list
to walk and no movie of its own to identify, so the movie-source contract, which is a trending walk
and a detail resolve over ids it issues, is the wrong shape for it. Forcing it behind that contract
would make it implement a trending call it cannot answer and join an id-routing scheme it never
takes part in.

**Enrichment keys on the external id, not on the source that issued the movie.** The qualified id
says TMDB minted this movie. The ratings source does not care: it asks OMDb about `tt0111161`, which
is the same film wherever the row came from. So enrichment never reads inside a `MovieId`, and adding
one changes nothing about how movies are identified or routed.

---

## 2. The contract

`:core:network` publishes a second interface at its root, beside `MovieRemoteDataSource`, in the
app's own types:

```kotlin
/**
 * Supplementary ratings for a movie identified across sources by its external id.
 *
 * A ratings source adds fields to a detail another source already resolved. It issues no movie and
 * serves no list, so it is addressed by [imdbId] rather than by a movie's qualified id.
 *
 * @param imdbId the film's IMDb id, which a detail carries where its issuing source publishes one.
 * @return the ratings this source holds for that film, empty when it has none.
 */
interface RatingsRemoteDataSource {
    suspend fun getRatings(imdbId: String): NetworkResult<List<Rating>>
}
```

**A ratings source returns the slice it fills, not a whole detail and not a wrapper around one.** Ratings
is the one field a second source earns today, and `List<Rating>` is already a type, so no
`Enrichment` record is minted to hold one list: a wrapper over one field names nothing the field
does not, the way a filter selection is a plain set rather than a type of its own. A second kind of
enrichment, streaming links or awards, is a second contract decided on the day it lands, not a
general partial built now for a field nobody has asked for.

**The ratings sources inject as a set, empty by default.**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RatingsSourceModule {
    @Multibinds
    abstract fun ratingSources(): Set<RatingsRemoteDataSource>

    @Binds
    @IntoSet
    internal abstract fun bindOmdb(source: OmdbRatingsRemoteDataSource): RatingsRemoteDataSource

    @Binds
    @IntoSet
    internal abstract fun bindMdbList(source: MdbListRatingsRemoteDataSource): RatingsRemoteDataSource
}
```

`@Multibinds` is what lets `:core:data` inject `Set<RatingsRemoteDataSource>` in a build with no source
at all: the set is empty rather than an unsatisfied dependency. A source is added by its `@IntoSet`
binding and removed by deleting it, the same way the movie sources compose. Two ratings sources ship,
OMDb and MDBList, both addressed by the same `imdbId`. `@JvmSuppressWildcards` is required on the injection
site.

---

## 3. What the app owns

A rating stops being one number and becomes one an authority published on its own scale.

```kotlin
enum class RatingAuthority {
    IMDB, ROTTEN_TOMATOES_CRITIC, ROTTEN_TOMATOES_AUDIENCE,
    METACRITIC, LETTERBOXD, ROGER_EBERT, TMDB, UNKNOWN,
}

enum class RatingScale { PERCENT, OUT_OF_HUNDRED, OUT_OF_TEN, OUT_OF_FIVE }

data class Rating(
    val authority: RatingAuthority,
    val score: Double,
    val scale: RatingScale,
    val count: Int?,
)
```

`MovieDetail.rating: Rating?` becomes `ratings: List<Rating>`.

**A rating names who gave it and on what scale, because a source returns several at once.** OMDb hands
back IMDb out of ten, a Rotten Tomatoes percentage and a Metacritic score out of a hundred in a
single response, and no screen can render 8.1, 91% and 80 as one number on one axis. The scale is the
app's own vocabulary a source maps onto, the way `Genre` and `ReleaseStatus` are, so nothing above a
source reads a raw provider axis.

**`score` is not `average`, because a rating is not always a mean.** A Tomatometer is the share of
reviews that were positive, so the field carries whatever the authority publishes and the name stops
asserting a mean the value is not.

**The ratings are a list, not a map keyed by authority.** The screen walks them and orders them for
display, and nothing in the app looks one up by its authority, so a list carries what a map would
without keying a lookup nobody makes. A map's one real gain, at most one rating per authority, is an
invariant the merge secures at its single write instead (see §5), so the stored list holds no
duplicate authority and an array stays the plainer shape for a value the UI only iterates.

**No rating at all is an empty list.** The missing-data rule is unchanged: where a source reports
nothing, the list is empty and the screen omits the block, the way a null `Rating` did before.

**A count is nullable, because a rating is not always counted.** IMDb reports a vote count where
Rotten Tomatoes and Metacritic do not, and across the surveyed APIs a third publish a score with no
count at all, so requiring one would discard ratings those sources do report. `count == null` is a
rating the authority does not count, not one counted zero times.

**A comparable value is derived, not stored.** Ordering two ratings needs them on one axis, but the
detail screen only displays them, each on the scale it arrived on, because a reader who knows a film
is 87% on Rotten Tomatoes does not recognise 8.7. A normalisation is a pure function of `score` and
`scale` the day a screen sorts ratings, computed then rather than carried as a field now.

`popularity`, the model's other cross-source ordering, is untouched. It stays the coupling 003 §6
names and does not solve.

---

## 4. The rating sources

Two ratings sources ship, each a directory under `:core:network`, `internal`, the same shape as `tmdb/`,
and each addressed by `imdbId`, the join key every detail already carries. Each key joins
`API_TOKENS`, so `:core:network` and `:app` gain one `BuildConfig` field per source and no source
adds a plugin of its own. An absent key yields an empty string, so a clone without it still builds:
that source's requests fail and the base detail renders without its ratings.

OMDb is the first ratings source, because its shape is knowable and its call is one request. MDBList is the
second, and it reaches the ratings OMDb cannot.

### OMDb

```
:core:network/sources/omdb/
    OmdbConfig.kt
    OmdbHttpClient.kt
    OmdbRatingsRemoteDataSource.kt
    dto/OmdbResponseDto.kt
    mapper/OmdbRatingMapper.kt
    di/OmdbModule.kt
```

| File | Holds |
|---|---|
| `OmdbConfig.kt` | The API key, read from the build the way TMDB's token is |
| `OmdbHttpClient.kt` | The `HttpClient` and `OMDB_BASE_URL`, built from the key and an injected engine |
| `OmdbRatingsRemoteDataSource.kt` | `GET ?i={imdbId}`, the one implementation of the contract |
| `dto/OmdbResponseDto.kt` | The `@Serializable` wire shape: the `Ratings` array and the response flag |
| `mapper/OmdbRatingMapper.kt` | `Ratings` entries to `List<Rating>` |
| `di/OmdbModule.kt` | Provides the config and the client, `internal` |

OMDb returns IMDb, Rotten Tomatoes critic and Metacritic in one call. Rotten Tomatoes publishes no
API of its own, so OMDb is one of the few routes to a Tomatometer number.

**Every field OMDb sends is a string, and a missing one is the literal `"N/A"`.** The mapper parses
rather than reads. A `Ratings` entry is a `{Source, Value}` pair whose value carries its own notation,
`8.8/10`, `87%` or `80/100`, so the source name maps onto a `RatingAuthority` and the notation onto a
`score` and a `RatingScale`. A vote count arrives for IMDb alone, as `imdbVotes`, and it carries
thousands separators, so its commas are stripped before it parses; Rotten Tomatoes and Metacritic
carry no count, which is the null the model already allows. A value of `"N/A"`, a source string the
table does not carry, or a notation that does not parse is dropped and never turned into a number: a
parse over `"N/A"` yields a silent `NaN`, and a rating that reaches a screen as `NaN` is worse than
one that is absent.

**A film OMDb does not hold is an empty list, not a failure.** OMDb answers a miss with a `200`
carrying `{"Response":"False"}` and no `Ratings`, so the failure is in the body rather than the
status. The ratings source reads that flag and returns `Success(emptyList())`, and reserves `Failure` for a
dropped connection or a non-2xx status, the same three-cause split `apiCall` draws for every source.

**OMDb content is licensed non-commercially.** Its terms are CC BY-NC 4.0, so it enriches a
non-commercial build and is a ceiling on a commercial one, the same class of constraint TMDB's
own commercial terms impose. Whether a licence permits the product is a driver for adopting a source,
recorded here and decided by the brief.

### MDBList

```
:core:network/sources/mdblist/
    MdbListConfig.kt
    MdbListHttpClient.kt
    MdbListRatingsRemoteDataSource.kt
    dto/MdbListResponseDto.kt
    mapper/MdbListRatingMapper.kt
    di/MdbListModule.kt
```

MDBList aggregates the ratings no other surveyed source reaches: Rotten Tomatoes audience alongside
critic, Letterboxd, and Roger Ebert, on top of the IMDb and Metacritic scores OMDb already carries.
It is addressed by `imdb_id`, `GET /imdb/movie/{imdbId}`, and its ratings arrive only under
`append_to_response=ratings`, so the ratings source sends that parameter.

**Its rating shape is learned from a recorded response, not from its contract.** MDBList's published
schema declares `ratings` an untyped array of untyped objects, so the wire DTO cannot be written from
the document the way OMDb's and TMDB's are. One live response is recorded, the DTO and the mapper are
written against it, and that recording is what every test then runs on. This is the one source here
whose shape is evidence rather than contract, called out so a reader does not trust a field the vendor
never promised.

**Its scale vocabulary may outgrow the enum.** Roger Ebert rated on four stars, a scale `RatingScale`
does not carry, and whether MDBList exposes that raw or normalised to its own hundred is not knowable
until the recording is read. The enum gains a constant if the recording shows one is needed, the way a
genre or a release status is added when a source first sends one. Until then a notation the mapper
cannot place is dropped, so an unplaceable Ebert score is absent rather than wrong.

---

## 5. The merge

The detail repository gains the ratings-source set and one step. `:core:data` still imports no provider and
neither library.

```kotlin
internal class DefaultMovieDetailRepository @Inject constructor(
    private val local: MovieLocalDataSource,
    private val remote: MovieRemoteDataSource,
    private val ratingSources: Set<@JvmSuppressWildcards RatingsRemoteDataSource>,
) : MovieDetailRepository {

    override suspend fun refresh(id: String): DataError? {
        val base = when (val result = remote.getMovieDetail(id)) {
            is NetworkResult.Success -> result.value
            is NetworkResult.Failure -> return result.error
        }
        val ratings = (base.ratings + gather(base.imdbId)).dedupedByAuthority()
        local.writeMovieDetail(base.copy(ratings = ratings))
        return null
    }

    private suspend fun gather(imdbId: String?): List<Rating> {
        if (imdbId == null || ratingSources.isEmpty()) return emptyList()
        return coroutineScope {
            ratingSources.map { async { it.getRatings(imdbId) } }
                .awaitAll()
                .filterIsInstance<NetworkResult.Success<List<Rating>>>()
                .flatMap { it.value }
        }
    }

    private fun List<Rating>.dedupedByAuthority(): List<Rating> =
        groupBy { it.authority }
            .map { (_, sameAuthority) -> sameAuthority.maxWith(compareBy({ it.count ?: -1 }, { it.score })) }
            .sortedBy { it.authority.ordinal }
}
```

**Enrichment runs at detail time and only there.** A trending row has no `imdbId`, so it cannot be
enriched, and the trending repository is untouched. The detail fetch is what first holds one, so the
ratings sources run after it and against what it returned.

**A ratings-source failure never fails the refresh.** The base detail already succeeded, and a rating is
additive and re-fetchable, so a failed enrichment is dropped and the refresh returns null with the
base detail written. Trading a whole detail the user opened for a supplementary rating is the outcome
this rules out. `gather` keeps the successes and discards the failures, so nothing above sees a
source's cause.

**The ratings sources run at once, not one after another.** They are independent calls keyed on the same id,
so `coroutineScope` fans them out and `awaitAll` collects them, and N sources cost one round trip
rather than N deep. No dispatcher is injected: Ktor is main-safe on its engine, the same reason the
repositories wrap nothing in `withContext`.

**The merge concatenates, then collapses by authority.** The base detail's own rating and every
ratings source's are joined into one list. With both sources bound the overlap is not hypothetical:
IMDb, Rotten Tomatoes critic and Metacritic each arrive from OMDb and again from MDBList. Those are not two opinions, they are one score delivered twice, because `RatingAuthority`
names who scored a film and not who relayed the number. So the joined list is collapsed to one rating
per authority, and a duplicate is dropped rather than shown.

**The kept copy is the one with the larger sample, and the pick does not depend on set order.** For
an authority reported twice, `dedupedByAuthority` keeps the copy with the greater vote count and then
the higher score. A null count sorts below any real one, so a source that reports votes wins over one
that does not, and where both report the same number the result is identical whichever survives. The
rule reads nothing about which source delivered a rating, so it lands the same on every build no
matter how the injected set iterated, without a source-priority list to keep. Rotten Tomatoes critic
and audience are separate authorities, so both survive: only a true duplicate is removed. The result
is ordered by `RatingAuthority` so the stored list and its tests are deterministic, and the detail
screen renders that order or imposes its own.

A rule that one source's number is more trustworthy than another's regardless of sample would be a
source priority the merge ranks on instead. The value-based rule here does not need one, so none is
carried until a product decision asks for it.

---

## 6. Persistence

The two rating columns on `movie_details` become one.

```kotlin
val ratings: String
```

A JSON array of rating rows, `"[]"` when the film has none. `movies`, the trending table, gains
nothing: it never held a rating and does not now.

**Ratings store as JSON, where a genre list stores as delimited names.** A genre is one uppercase
identifier and a rating is a record of four fields, so the delimiter that carries the first cannot
carry the second, and kotlinx-serialization is already on the stack. Nothing queries a rating in SQL,
because the detail screen reads a whole row and renders it, so a rating table would buy a join and a
relation query for a query nobody writes, the same reasoning that kept genres out of a table of their
own. A column per authority is a column set that grows with the enum and stands empty on every film
but the few an authority scored.

**One mapper owns the JSON, and a read never fails on it.** `MovieEntityMapper` encodes the list on
the way in and decodes it on the way out, so the translation stays in the one place the module's other
conversions live and no type converter splits it. A stored authority or scale a later build has
retired decodes to `UNKNOWN` or is dropped, never thrown: a cache read that failed on an old row would
lose a movie the user opened offline.

**The schema bump drops the database, and the MVP writes no migration.** Ratings are re-fetchable, so
version two with the destructive fallback drops the old table and the next detail open refetches and
re-enriches, which is what D41 already decided for every schema change here. A hand-written
`Migration` that recreates `movie_details` with the new column is the shape this would take the day a
detail row stops being a pure cache, and on the platform SQLite the minimum device ships, which
predates `ALTER TABLE DROP COLUMN`, that migration is a create-copy-swap rather than a column drop. It
is not written now, because it preserves what one request restores. The exported schema regenerates
at version two.

---

## 7. Tests

All on the JVM against `MockEngine`, recorded fixtures and fakes. No network, clock or device.

Recorded OMDb and MDBList responses live under `core/network/src/test/resources/fixtures/omdb/` and
`.../mdblist/`, each browsed through its own fixture enum the way the TMDB recordings are. The MDBList
recording is also the source of its wire shape, because the vendor's schema declares its ratings array
untyped.

| Test | Fails when |
|---|---|
| The OMDb full response maps to IMDb out of ten, Rotten Tomatoes as a percent and Metacritic out of a hundred | a scale or an authority is read wrong |
| An OMDb value of `"N/A"` maps to no rating, and an `imdbVotes` with thousands separators parses to its count | a `"N/A"` reaches a screen as `NaN`, or a comma defeats the count parse |
| An unknown source string maps to `UNKNOWN` and an unparseable notation is dropped | an authority this build does not know takes down the read |
| An OMDb response flagged not found maps to an empty list, not a failure | a film OMDb lacks reads as a broken call |
| The MDBList recording maps Rotten Tomatoes audience, Letterboxd and every authority it carries | the untyped shape is read wrong, or an authority is lost |
| A MDBList rating whose scale the enum cannot place is dropped, not coerced | an Ebert score lands on the wrong scale |
| A dropped connection and a non-2xx status each map to their cause, for either source | a ratings-source failure gets a wrong shape |
| A detail refresh writes the base ratings joined with every source's | the merge drops a source it should keep |
| Two sources reporting one authority collapse to the copy with the larger vote count, while Rotten Tomatoes critic and audience both survive | a duplicate authority reaches the screen, or a distinct one is dropped |
| The collapse picks the same copy whatever order the injected set iterates | the dedup depends on set order and differs across builds |
| A refresh with a failing ratings source writes the base detail and returns null | a supplementary rating failure destroys a detail |
| A refresh for a movie with no `imdbId` calls no ratings source and writes the base detail | enrichment runs without the key it needs |
| A refresh whose base detail fetch fails calls no ratings source and returns the cause | a failed base still hits the network again |
| A build with no ratings source bound injects an empty set and writes the base detail | the multibinding stops being optional |
| A rating list with several authorities, and an empty one, each round-trip through the JSON column | a stored rating comes back wrong or a read throws |
| A stored authority or scale a build no longer knows decodes without throwing | an old cached row takes down a read |

`:core:testing` gains `FakeRatingsRemoteDataSource`, a scripted `NetworkResult<List<Rating>>` per id
whose own test asserts it returns what it was scripted, and a `rating()` builder with a default for
every field.

---

## 8. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D118 | An enrichment source is a separate `RatingsRemoteDataSource` contract keyed on `imdbId`, not a second `MovieRemoteDataSource` | It issues no id, serves no list and resolves no whole detail, so the movie-source contract, which is a trending walk and a detail resolve over ids it mints, is a shape it cannot fill. Keying on the external id is what lets it ask about a film without reading the qualified id, so it never joins the id-routing the rival-primary source needs | Adding it to the `MovieRemoteDataSource` set with a routing method, which makes a source that only adds fields implement a trending call it has no answer for and a `doesIssue` over ids it never issues |
| D119 | `getRatings` returns `List<Rating>` directly, and there is one ratings-source contract while ratings is the only slice | The list is already a type, so a record wrapping one field names nothing the field does not, the way a filter selection is a plain set. A second kind of enrichment is a second contract decided when it lands, not a general partial with a nullable field per future enrichment | An `Enrichment` record carrying nullable ratings, streaming links and awards, which is a type built for enrichments nobody has asked for and read half-empty on every real one |
| D120 | `Rating` carries `authority`, `score`, `scale` and a nullable `count`, and `MovieDetail` carries `ratings: List<Rating>` | A source returns several ratings at once, each on its own axis, so one number with one implicit scale cannot render them and one field cannot hold them. The scale is the app's vocabulary a source maps onto, like a genre. The count is nullable because IMDb publishes one where Rotten Tomatoes and Metacritic do not, and a third of the surveyed APIs score with none. A list keeps merge order and needs no rule for two sources reporting one authority, which a map would. This supersedes 003 D27 and resolves the singular-rating coupling 003 §6 names | Keeping a single `Rating?`, which discards every rating past the first; a `Map<RatingAuthority, Rating>`, whose one gain, at most one rating per authority, the merge already secures at its single write, and which in exchange loses the display order and stores as a keyed object where an array suffices; a required count, which discards a rating from every source that publishes none; `average`, which asserts a mean a Tomatometer is not; a stored normalised value, which is a pure function of score and scale computed the day a screen sorts ratings |
| D121 | A ratings-source failure never fails the refresh, the sources run concurrently, and the base detail is written regardless | The base detail already succeeded and a rating is additive and re-fetchable, so failing the refresh would trade a whole detail for a supplementary field. The calls are independent and keyed on one id, so a concurrent fan-out costs one round trip rather than N. Ktor is main-safe on its engine, so nothing wraps a dispatcher | Failing the refresh on any source failure, which loses a detail the user opened; running them in sequence, which stacks their latencies; surfacing a source's cause, which is a failure no screen has a state for |
| D122 | Ratings persist as one JSON column, one mapper owns the translation, and a schema bump drops the database rather than migrating | A rating is a four-field record a delimiter cannot carry, nothing queries it in SQL, and the serialization library is already present, so a rating table buys a join for no query and a column per authority grows with the enum. Ratings are re-fetchable, so the destructive fallback and a version bump restore them on the next open, which D41 already decided | A rating table with a relation query, for a query nobody writes; a column per authority, unbounded and mostly empty; a type converter, which splits one translation across two mechanisms; a hand-written migration, which preserves what one request restores |
| D123 | Ratings sharing an authority are collapsed at the merge, keeping the copy with the larger vote count then the higher score, ordered by authority | Two sources reporting one authority re-export one score, not two opinions, because the authority names who scored the film and not who relayed it, so showing both is a defect and dropping one loses nothing. Deciding on count then score makes the pick independent of the injected set's iteration order, so it is the same on every build with no source-priority list to maintain. Rotten Tomatoes critic and audience are distinct authorities, so the real opinions survive | Keeping both and deduplicating at display, which stores a duplicate every consumer must resolve; a fixed source priority, a policy to maintain for a value that is the same number either way, deferred until a product rule needs it; last-write-wins over the set, which is nondeterministic across builds |

---

## 9. What this changes elsewhere

- 003's `Rating` is no longer an average on one scale: it carries an authority and a scale, and
  `MovieDetail` carries a list of them. D27 is superseded, and the singular-rating coupling in 003 §6
  is resolved as a list rather than left named. The TMDB mapper now builds a one-element list, tagged
  `TMDB` on the out-of-ten scale, and its zero-votes sentinel becomes an empty list rather than a null
  `Rating`.
- 004's detail table loses `ratingAverage` and `ratingCount` for one `ratings` JSON column, the schema
  goes to version two, and the mapper's rating round trip changes shape. The destructive fallback and
  the re-fetch are unchanged, so D41 already covers the bump.
- 005's detail repository gains the ratings-source set and the merge step, and a best-effort rule where a
  failed enrichment leaves the base detail. The trending repository and the read paths are unchanged.
- 007 renders one rating in the header badge slot, exclusive with the status by D74. A list of ratings
  is more than that slot holds, so how a film's several ratings are shown, and whether the status stays
  exclusive with a primary one, is a presentation decision that spec owns. This spec changes what the
  detail carries, not how it is drawn.
- The brief's multi-source ambition gains a concrete, cheap first move: two enrichment sources, OMDb
  and MDBList, are each a directory, a merge step and a shared column behind a contract already
  published, with no id space, no trending path and no navigation touched. The rival-primary source
  and its trending-merge product decision stay where 003 §6 and the architecture doc leave them.
- OMDb's non-commercial licence and MDBList's unstated commercial terms are inputs to when each earns
  a place, not to how it is built. They sit with the brief's multi-source decision, beside TMDB's own
  commercial ceiling.
