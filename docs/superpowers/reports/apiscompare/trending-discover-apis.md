# Trending and discover endpoints: a comparison for AMRO

**Date:** 2026-09-09
**Purpose:** AMRO's list screen needs a source of "movies right now" plus in-app sort and filter. This report asks one question of each source from [movie-data-apis.md](movie-data-apis.md): does it have a trending endpoint, does that endpoint take sort or filter parameters, and if not, what's the nearest endpoint that does. Good-to-know reference: [tmdb-api.md](../tmdb-api.md).

---

## 0. Method

Every claim is tagged:

- **SPEC** means the source's OpenAPI document or API Blueprint was downloaded on 2026-09-09 and read directly.
- **DOC** means vendor prose was read, no machine-readable contract available.
- **NONE** means the source has no endpoint that returns a movie list at all.

MDBList's schema was re-fetched live rather than trusted from the earlier survey: it had changed in the seven days since, see §4.

---

## 1. The field, one line each

| Source | Trending endpoint | Sort on it | Filter on it | Nearest list endpoint if not |
|---|---|---|---|---|
| **TMDB** | `/trending/movie/{time_window}` | No | No | `/discover/movie`: 14-way sort, ~25 filters |
| **Trakt** | `/movies/trending` | No (fixed: watchers desc) | Yes, same filters as `/movies/popular` | `/movies/popular` (rating-based), same filter set |
| **Simkl** | Static JSON files, not an API call | No | No | `/movies/genres/{genre}/{type}/{country}/{year}/{sort}`, path-segment filter and sort |
| **TheTVDB** | None | — | — | `/movies/filter`: 5 filters, 3-way sort |
| **Watchmode** | None | — | — | `/list-titles`: 10 filters, 8-way sort |
| **Streaming Availability** | `/shows/top` (per-service Top 10) | No, service's own ranking | Country, service, show type only | `/shows/search/filters`: 8 filters, 7-way sort |
| **MDBList** | None | — | — | `/catalog/movie`: 8 filters, 13-way sort |
| OMDb | None | — | — | `/?s=` search only, filters on `type`/`y`, no sort |
| IMDb bulk | None | — | — | Flat files, sort and filter are the client's problem |
| JustWatch | None documented for third parties | — | — | excluded, see [movie-data-apis.md §2.9](movie-data-apis.md) |
| Cinemeta | `/catalog/movie/top.json` | No | `?genre=` only, one at a time | same endpoint |
| Wikidata | None | — | — | SPARQL is itself the filter and sort language |

Six sources (TMDB, Trakt, TheTVDB, Watchmode, Streaming Availability, MDBList) have a real discover-shaped endpoint. Three of those six (Trakt, Streaming Availability, and TMDB's own trending) also expose a literal "trending" list, and only Trakt lets you filter the trending list itself. Everywhere else, "trending" and "filterable" are two different endpoints, and picking one costs the other.

---

## 2. Per-source detail

### 2.1 TMDB — SPEC (local reference)

`/trending/movie/{time_window}` takes exactly two parameters: `time_window` (`day` or `week`) and `language`. No `sort_by`, no genre, no year, no rating floor. Order is TMDB's own trending rank, which is a different score from `popularity` and shifts between requests (see [tmdb-api.md §1.11](../tmdb-api.md)).

`/discover/movie` is the filterable list, and it's the widest in the survey:

```
sort_by: original_title.asc|desc, popularity.asc|desc, revenue.asc|desc,
         primary_release_date.asc|desc, title.asc|desc,
         vote_average.asc|desc, vote_count.asc|desc
  (default popularity.desc)
```

Filters: `with_genres` / `without_genres`, `with_keywords` / `without_keywords`, `with_cast`, `with_crew`, `with_people`, `with_companies` / `without_companies`, `with_origin_country`, `with_original_language`, `with_release_type`, `with_runtime.gte` / `.lte`, `primary_release_year`, `primary_release_date.gte` / `.lte`, `release_date.gte` / `.lte`, `year`, `vote_average.gte` / `.lte`, `vote_count.gte` / `.lte`, `certification` / `.gte` / `.lte` (with `certification_country`), `with_watch_providers` / `without_watch_providers` / `with_watch_monetization_types` (with `watch_region`), `include_adult`, `include_video`, `region`.

Genre, keyword, cast, crew, people, company and release-type filters accept comma (`AND`) or pipe (`OR`) separated ids. This is the only source in the survey with two-way boolean composition on a filter value; everyone else's filters are single-value or a flat OR list.

**What it costs:** no trending-plus-filter combination exists. A "trending action movies" screen means either filtering trending results client-side against `genre_ids`, or dropping trending rank in favor of `discover` sorted by `popularity.desc`, which is a materially different ordering.

### 2.2 Trakt — SPEC (API Blueprint)

`/movies/trending` returns "most watched over the last 24 hours," ordered by `watchers` descending, fixed. `/movies/popular` is "rating percentage times rating count," also fixed order. Neither takes `sort_by`. Both are tagged 📄 Pagination, ✨ Extended Info, 🎬 Filters, and the filters are real: they narrow the candidate set before Trakt applies its own fixed ordering, they don't change the ordering.

**Filters** (shared across `movies`, `shows`, `calendars`, appended as query string):

Common: `query`, `years`, `genres`, `subgenres` (technical preview), `languages`, `countries`, `runtimes` (`30-90`), `studio_ids`.

Rating: `ratings` (Trakt 0-100), `votes` (Trakt 0-100000), `tmdb_ratings` (0.0-10.0), `tmdb_votes`, `imdb_ratings` (0.0-10.0), `imdb_votes` (0-3,000,000), `rt_meters` (Rotten Tomatoes critic, 0-100), `rt_user_meters` (Rotten Tomatoes audience, 0-100), `metascores` (0-100).

Movie-specific: `certifications` (US rating, e.g. `pg-13`).

This is the only source in the survey that filters on Rotten Tomatoes and Metacritic scores server-side; everywhere else that data, if present at all, has to be fetched from a second source and filtered client-side. Ranges are dash-separated strings (`75-100`), not paired `.gte`/`.lte` parameters like TMDB's.

Pagination is `?page=&limit=`, genuinely configurable, unlike TMDB's fixed 20.

**What it costs:** nothing. Trending and filterable are the same request shape here; the only trade is that filtering narrows candidates before Trakt's fixed watcher-count ordering, so there's still no way to sort trending results by, say, rating.

### 2.3 Simkl — SPEC (API Blueprint)

The blueprint's own words: "Simkl provides pre-built JSON files with trending data ranked by the number of watchers." `today_100.json`, `week_500.json`, and so on, served from `data.simkl.in`, not `api.simkl.com`. There's no query string at all, no key required, and the docs say outright: "you can use to filter and sort locally in your app." This is the only source in the survey where the trending endpoint is a static file, not a parameterized API call.

The filterable list is a different, path-segment endpoint:

```
GET /movies/genres/{genre}/{type}/{country}/{year}/{sort}?client_id=
```

Genre, country, year and sort are positional URL segments, not query parameters, and the endpoint's own doc says it "duplicates the URLs of the Genres on the website": build the filter on simkl.com/movies/all/ and copy the resulting path. `sort` values are undocumented strings like `rank`, `popular-this-month`, `popular-today`, taken from the website's own filter widget rather than an enumerated list. `/search/random` is a third option, filtering on `genre`, `rating_from`/`rating_to` (IMDb rating, 0-10), `year_from`/`year_to`, `rank_limit`, and `service` (restrict to one streaming service's catalog), but it returns one random pick, not a list.

**What it costs:** trending is unfilterable by construction, since it isn't a request, it's a file. Getting "trending action movies" means downloading the trending file and filtering locally, exactly as the docs recommend, or abandoning trending rank for the genre-browse endpoint's own `popular-today` sort, which is a different metric entirely and whose exact definition isn't documented.

### 2.4 TheTVDB — SPEC (OpenAPI, v4.7.10)

No trending endpoint of any kind. `/movies` is a plain paged list with one parameter, `page`, no ordering control at all. `/movies/filter` is the discover-shaped endpoint:

```
company (production company id), contentRating (id, per country),
country (required), genre (id, enum 1-36 with gaps),
lang (required), sort, status (enum 1/2/3), year
```

`sort` accepts exactly three values: `score`, `firstAired`, `name`. `firstAired` is a straight copy-paste from the TV-series filter schema; TheTVDB's own spec sorts movies by a field named for when a show first airs, and there's no movie-specific date-sort name. `/movies/filter` also has no `page` parameter in its own schema, unlike the plain `/movies` list, so pagination on a filtered query is undocumented.

Both `country` and `lang` are required, not optional narrowing, so this endpoint can't be called as "just sort by score, no filter."

**What it costs:** everything. There's no trending signal here at all, `score` is TheTVDB's own composite the spec admits "carries no defined meaning beyond relative ordering" (see [movie-data-apis.md §2.4](movie-data-apis.md)), and the one sort key most apps would want, release date, is named after a TV concept and unconfirmed to behave sanely for movies.

### 2.5 Watchmode — SPEC (OpenAPI, fetched live from `api.watchmode.com/swagger.json`)

No trending endpoint. `/list-titles` is the one list-and-filter endpoint, and it's wide:

```
sort_by: relevance_desc|asc, popularity_desc|asc,
         release_date_desc|asc, title_desc|asc
  (default relevance_desc)
```

Filters: `types` (`movie`, `tv_series`, `tv_movie`, `tv_special`, `tv_miniseries`, `short_film`), `regions`, `source_types` (`sub`, `rent`, `buy`, `free`, `tve`), `source_ids`, `genres`, `network_ids`, `languages`, `release_date_start`/`_end` (`YYYYMMDD` integers), `user_rating_low`/`_high` (0-10), `critic_score_low`/`_high` (0-100), `person_id`. Paging is `page` plus `limit` (1-250).

`popularity_desc` is the closest thing to "trending" here, and the docs are explicit that it's "Watchmode's normalized popularity percentile," not a live rank. There's no time-window version of it, no "today" versus "this week."

**What it costs:** the credit model bites here specifically. The endpoint description states each requested region costs 1 credit per page, and `source_ids`/`source_types` limit the call to one region. A "trending near me, filtered by service" query is cheap; a multi-region trending-plus-filter sweep is not.

### 2.6 Streaming Availability — SPEC (OpenAPI, fetched from the vendor's own GitHub repo)

`/shows/top` exists and is named for it, but it's the streaming service's own official Top 10 (Netflix, Prime, Disney+, Apple TV, Max, Crunchyroll only), not Movie of the Night's own trending signal. Parameters are just `country`, `service`, `show_type`, and `output_language`. No genre, no rating, no date range, no sort override, because there's nothing to sort: it's already a ranked top-10 the service published.

`/shows/search/filters` is the real discover endpoint:

```
order_by: original_title, release_date, rating,
          popularity_alltime, popularity_1year,
          popularity_1month, popularity_1week
  (default original_title, asc; rating/popularity default desc)
order_direction: asc|desc
```

Filters: `catalogs` (service plus streaming-option-type, e.g. `netflix`, `prime.subscription`, `peacock.free`, up to 32 comma-separated), `genres` plus `genres_relation` (`and`/`or`, default `and`), `show_original_language`, `year_min`/`_max`, `rating_min`/`_max` (0-100), `keyword` (matches title or overview), `show_type`, `country` (required).

`popularity_1week` is the closest analog to trending, and it's a real sortable field here, not a separate fixed-order endpoint. That makes this the second source, after Trakt, where a single request can be both time-windowed-popular and genre-filtered at once.

Pagination is cursor-based, the only cursor pager among the discover-shaped endpoints (MDBList's newer schema, §4, is cursor-based too).

**What it costs:** `country` is required on the filter endpoint, same shape as TheTVDB's requirement, so there's no country-agnostic global trending query.

### 2.7 MDBList — SPEC (OpenAPI, re-fetched live 2026-09-09, see §4 for what changed)

No trending endpoint. `/catalog/movie` is the filter-and-sort endpoint:

```
sort: imdbpopular, imdbrating, imdbvotes, letterrating, metacritic,
      released, releasedigital, rtaudience, rtomatoes, score,
      score_average, title, tmdbpopular
sort_order: asc|desc (default desc)
```

Filters: `genre` (repeatable name, e.g. `?genre=action&genre=comedy`) with `genre_mode` (`and`/`or`, default `or`), `country` (CSV, max 10), `language` (CSV, max 10), `score_min`/`_max` (MDBList's own 0-100), `released_from`/`_to` (date), `year_min`/`_max`, `runtime_min`/`_max`. `append_to_response` adds `poster`, `ratings`, `description`, `genres` to an otherwise minimal row. Paging is `limit` (max 100) plus `cursor`.

`imdbpopular` and `tmdbpopular` are the nearest thing to a trending sort, both borrowed rankings rather than an MDBList-native trending score.

**What it costs:** nothing structural, this is one of the two widest sort vocabularies in the survey (13 keys, next to TMDB's 7 base fields expressed as 14 directional options). The cost is elsewhere, see §4: this endpoint changed shape in the last week, and the aggregator sources (Letterboxd, Roger Ebert) that made MDBList worth a second look are gone from it.

### 2.8 The rest: no discover-shaped endpoint

- **OMDb**: `/?s=` searches by title, with `type` and `y` as the only filters. No genre, no rating, no sort, and no way to browse without a search term.
- **IMDb bulk**: flat TSV files, no query interface of any kind. Sort and filter are entirely client-side, over the whole 268k-plus row dataset.
- **JustWatch**: its GraphQL endpoint does accept rich filter arguments (genre, year, provider, offer type), but it's undocumented and not licensed for third-party use, see [movie-data-apis.md §2.9](movie-data-apis.md). Not usable regardless of shape.
- **Cinemeta**: `/catalog/movie/top.json` accepts one `?genre=` at a time (from the manifest's declared list) and `?skip=` for paging. No sort parameter; order is whatever Cinemeta's own `top` ranking is, undocumented.
- **Wikidata**: SPARQL is itself an arbitrarily expressive filter and sort language, `ORDER BY` and any `WHERE` clause the query wants, but there's no fixed "trending" concept to query for, and the 60-second timeout rules out an interactive filter UI backed directly by it.

---

## 3. Sort and filter vocabulary, compared

| Source | Sort keys | Genre filter | Rating filter | Date/year filter | Provider filter |
|---|---|---|---|---|---|
| **TMDB discover** | 7 fields × asc/desc | id, AND/OR | `vote_average`, `vote_count` | year, primary/general release date range | watch provider, monetization type |
| **Trakt** | none (fixed per endpoint) | slug, OR only | Trakt, TMDB, IMDb, RT critic, RT audience, Metacritic | `years`, no explicit range op | none |
| **Streaming Avail.** | 3 fields + 4 popularity windows | id, AND/OR | one aggregate 0-100 | `year_min`/`_max` | catalog + streaming-option-type |
| **MDBList** | 13 named provider scores | name, AND/OR | per-field, one at a time via `sort`/`score_min` | released and production year, both ranged | none |
| **Watchmode** | 4 fields × asc/desc | id, comma list (AND assumed) | user rating, critic score | release date range | source id, source type, network |
| **TheTVDB** | 3 fields, no direction control | id (1-36, gaps) | none | year (single value, no range) | none |
| **Simkl** (genre-browse) | undocumented string enum | slug, one at a time | none | year segment, coarse (`2010s`) or single year | one service via `/search/random` |

Three things stand out against the D6/D1 in-app-filter design AMRO already has for TMDB:

1. **Genre AND/OR composition is not universal.** TMDB, Streaming Availability and MDBList support it explicitly. Trakt is OR-only across multiple genre slugs. TheTVDB and Watchmode take a single id or comma list with no documented boolean. Simkl's genre-browse endpoint takes exactly one genre per call.
2. **Direction control is not universal either.** TheTVDB's `sort=score` has no `asc`/`desc` toggle in its schema at all, sort direction is presumably fixed. Simkl's sort values are pre-composed strings (`popular-this-month`) rather than a field-plus-direction pair, so "popularity ascending" may not exist as a request.
3. **"Trending" and "sortable/filterable" are almost always different endpoints**, and picking a trending list generally means giving up server-side filtering entirely (TMDB, Simkl, Streaming Availability's `/shows/top`) or giving up the trending signal for a different ranking that happens to be filterable (TheTVDB's `score`, Watchmode's `popularity_desc`, MDBList's `imdbpopular`/`tmdbpopular`). Trakt is the one source where the same request is both.

---

## 4. MDBList drifted in the last week

The 2026-09-02 survey's `/catalog/movie` sort enum was: `imdbrating, imdbvotes, imdbpopular, tmdbpopular, letterrating, lettervotes, metacritic, myanimelist, rogerebert, rtomatoes, rtaudience, score, score_average` plus separate sortable fields `budget, revenue, runtime, released, releasedigital, last_air_date, rank, random, sort_title, title, added, usort`. The live schema read on 2026-09-09 has `sort` as a single enum of 13 values (§2.7), and `lettervotes`, `myanimelist`, `rogerebert`, `budget`, `revenue`, `runtime`, `rank`, `random`, `sort_title`, `added`, `usort` are gone from it. `genre` also changed shape, from a comma-list of integer ids to a repeatable name parameter with an explicit `genre_mode`. Pagination changed from an id-based cursor to a `limit`/`cursor` pair the endpoint calls "keyset pagination."

None of this is documented as a breaking change anywhere in the schema itself, it's simply a different response the same URL now gives. If AMRO builds against MDBList's catalog endpoint, treat the schema as unstable and re-fetch it before relying on any specific sort key, rather than hard-coding the enum from either date.

---

## 5. What to do about it

**For a trending screen with in-app sort and filter, Trakt or Streaming Availability are the only sources where one request does both.** Trakt's `/movies/trending` plus its Common/Rating/Movie filters, or Streaming Availability's `/shows/search/filters` sorted by `popularity_1week`, are the two closest things in this survey to "TMDB discover, but time-windowed."

**TMDB stays primary** per [movie-data-apis.md §7](movie-data-apis.md): its `/discover/movie` has the widest filter set outright, 30-plus parameters against Trakt's roughly a dozen, and AND/OR composition on genre, cast, crew and company that nothing else in the survey matches. The trade AMRO already made, client-side filtering over a fixed trending list, is the same trade every other source with a real trending endpoint (Simkl, Streaming Availability's `/shows/top`) forces too. It isn't a TMDB-specific gap.

**No source offers a choice of sort field on its trending list.** Every trending-shaped endpoint in the survey (TMDB, Trakt, Simkl, Streaming Availability's `/shows/top`) has one fixed metric, watchers, popularity, or the service's own ranking, and no `sort_by`. Trakt is the closest thing to a middle ground: `/movies/trending` accepts the same filters as its discover-shaped siblings, so the fixed-order trending set can still be narrowed by genre, year, rating and certification server-side, just not re-ordered. That's worth weighing against the six-month TMDB cache ceiling and commercial-use terms already on record: Trakt's terms permit commercial use outright (see [movie-data-apis.md §1](movie-data-apis.md)).

**Do not build against MDBList's catalog shape without a re-fetch step.** Its sort enum, genre filter shape and pagination model all changed within a week of the first survey.

---

## 6. Matching AMRO's `MovieSort`

`feature/trending/.../MovieSort.kt` defines `SortKey` as `POPULARITY`, `TITLE`, `RELEASE_DATE`, each with an independent `SortDirection`. Checking every discover-shaped endpoint against that exact shape, genre filter plus all three keys in both directions:

| Endpoint | Genre filter | `POPULARITY` both directions | `TITLE` both directions | `RELEASE_DATE` both directions | Shape match |
|---|---|---|---|---|---|
| **TMDB `/discover/movie`** | `with_genres`, AND/OR | `popularity.asc`/`.desc` | `title.asc`/`.desc` | `primary_release_date.asc`/`.desc` | Exact, one param per key |
| **Watchmode `/list-titles`** | `genres` | `popularity_asc`/`_desc` | `title_asc`/`_desc` | `release_date_asc`/`_desc` | Exact, one param per key |
| **Streaming Avail. `/shows/search/filters`** | `genres`, AND/OR | `popularity_1week` (or `_1month`/`_1year`/`_alltime`) via `order_by`, direction via separate `order_direction` | `original_title` via `order_by` | `release_date` via `order_by` | Covers all three, but popularity is time-windowed, not a single field, and direction is a second param |
| **MDBList `/catalog/movie`** | `genre`, AND/OR | `tmdbpopular` or `imdbpopular` via `sort` | `title` via `sort` | `released` via `sort` | Covers all three, but one shared `sort_order` applies to whichever field is chosen, not independent per-field direction |
| **Trakt `/movies/popular`** | `genres`, OR only | fixed order, no `sort_by` | none | none | No match, not sortable at all |
| **TheTVDB `/movies/filter`** | `genre` id | `score` via `sort`, no direction toggle | `name` via `sort`, no direction toggle | none (`firstAired` is undated for movies) | No match, no direction control |
| **Simkl** (genre-browse) | one genre per call | undocumented composed strings (`popular-today`) | none | coarse year segment only | No match, not a field-plus-direction model |

**TMDB's `/discover/movie` and Watchmode's `/list-titles` are the only two endpoints that map onto `MovieSort` one field at a time**, each `SortKey` a distinct `sort_by` value already carrying its own direction suffix. Since TMDB is already primary, `SortKey.POPULARITY` / `.TITLE` / `.RELEASE_DATE` should read as `popularity` / `title` / `primary_release_date`, and `SortDirection` as the `.asc`/`.desc` suffix, with no translation layer needed. A second source added later for the same sort UI would need to be Watchmode; anything else in this survey needs either a fallback for one `SortKey` (Streaming Availability, MDBList) or doesn't sort server-side at all (Trakt, TheTVDB, Simkl).

---

## Appendix: reproducing the findings

```bash
# TMDB discover and trending params: read from the local reference
grep -n "Discover — Movie" -A 60 docs/superpowers/reports/tmdb-api.md

# Trakt: full API Blueprint
curl -s https://jsapi.apiary.io/apis/trakt.apib -o trakt.apib
grep -n "^# Filters" -A 60 trakt.apib
grep -n "## Trending \[/movies/trending\]" -A 30 trakt.apib

# Simkl: full API Blueprint
curl -s https://jsapi.apiary.io/apis/simkl.apib -o simkl.apib
grep -n "Group Trending" -A 20 simkl.apib
grep -n "## Genres \[/movies/genres" -A 25 simkl.apib

# TheTVDB: OpenAPI 3.0.0
curl -s https://thetvdb.github.io/v4-api/swagger.yml -o tvdb.yml
awk '/movies\/filter.:/,/tags:/' tvdb.yml

# Watchmode: OpenAPI, served directly from the API host
curl -s https://api.watchmode.com/swagger.json -o watchmode.json
python3 -c "import json; d=json.load(open('watchmode.json')); \
  print(d['paths']['/list-titles']['get']['parameters'])"

# Streaming Availability: OpenAPI, vendor's own GitHub repo
curl -s https://raw.githubusercontent.com/movieofthenight/streaming-availability-api/main/openapi.yaml -o streamavail.yml
awk '/\/shows\/search\/filters:/,/\/shows\/top:/' streamavail.yml

# MDBList: OpenAPI, served as YAML despite the endpoint name
curl -s https://api.mdblist.com/schema/ -o mdblist.yml
awk '/\/catalog\/movie:/,/\/catalog\/show:/' mdblist.yml
```
