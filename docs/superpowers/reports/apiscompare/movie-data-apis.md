# Movie data APIs: a comparison for AMRO

**Date:** 2026-09-02
**Purpose:** AMRO's stated post-MVP plan is to collect data from multiple API sources. This document surveys what is available besides TMDB, what each returns, in what shape, and what it costs in money, licence terms and integration work.

---

## 0. Method, and what "verified" means here

Every claim below is tagged:

- **LIVE** means a request was issued from this machine on 2026-09-02 and the response is quoted or summarised from the actual bytes.
- **SPEC** means the source's own machine-readable contract (OpenAPI document, API Blueprint) was downloaded and read.
- **DOC** means the vendor's prose documentation was read, but no request was made because the endpoint needs a key this project does not hold.

Sources needing a key that AMRO does not have (OMDb, Trakt, TheTVDB, Watchmode, Streaming Availability, Simkl, MDBList, Fanart.tv) were probed unauthenticated to confirm the auth model, then read from spec or doc. Where a number is unverified, the text says so.

TMDB was queried with the project's own v4 read access token.

---

## 1. The field

Eleven sources on top of TMDB, plus one artwork adjunct.

| # | Source | What it is really for | Auth | Free tier | Commercial use |
|---|---|---|---|---|---|
| 0 | **TMDB** | General movie metadata, images, discovery | v4 bearer token | Unmetered in practice | Written agreement required |
| 1 | **OMDb** | IMDb-shaped metadata plus critic scores, by IMDb id | `apikey` param | 1,000/day | Yes, via Patreon tiers |
| 2 | **IMDb** (datasets + Essential Metadata) | The authoritative title graph and ratings | None (bulk) / AWS Data Exchange | Bulk TSV, non-commercial only | Paid licence only |
| 3 | **Trakt** | Trending, popularity, social signal, user lists | `trakt-api-key` header | 1,000 GET / 5 min | Yes |
| 4 | **TheTVDB v4** | TV-first catalogue that also carries movies | Bearer from `/login` | Free below $50k revenue | Revenue-tiered, $1k to $10k/yr |
| 5 | **Watchmode** | Where to watch, 200+ services, 54 countries | `X-API-Key` header | 2,500/month, non-commercial | $349/mo and up |
| 6 | **Streaming Availability** (Movie of the Night) | Where to watch, 65 countries | RapidAPI key | 1,000/month | Yes, on every tier including free |
| 7 | **Simkl** | Tracking, scrobbling, multi-rating metadata | `client_id` param | Free, 10 GET/sec | Licence required |
| 8 | **MDBList** | Rating aggregation across nine providers, plus lists | `apikey` param or bearer | 1,000/day | Not stated |
| 9 | **JustWatch** | The underlying where-to-watch graph | None | Undocumented, unauthenticated | **Not licensed. Do not ship.** |
| 10 | **Cinemeta** (Stremio) | IMDb-derived metadata for Stremio addons | None | Unmetered | No terms published |
| 11 | **Wikidata** | Structured facts, awards, entity links | None | SPARQL, 60s query cap | CC0, unrestricted |
| A | **Fanart.tv** | Logos, clearart, disc art, banners | `api-key` header | Personal key | Project key required |

---

## 2. Per-source detail

### 2.0 TMDB (baseline) — LIVE

**Auth:** `Authorization: Bearer <v4 read access token>`. The v3 `api_key` query parameter also works and is what most tutorials show. The project already took the bearer route (brief D13).

**Endpoints that matter:**

```
GET /3/trending/movie/{day|week}      20 per page, page param undocumented but works
GET /3/movie/{id}                     full detail
GET /3/genre/movie/list               19 genres
GET /3/discover/movie                 server-side filter and sort
GET /3/search/movie
GET /3/movie/{id}/credits|images|videos|watch/providers|recommendations
```

**List item shape** (verified, trending page 1):

```json
{
  "adult": false, "backdrop_path": "/ziXF8...jpg", "id": 1386315,
  "title": "The Runner", "original_title": "The Runner",
  "overview": "Maia Marten, a brilliant London lawyer...",
  "poster_path": "/uxCaB...jpg", "media_type": "movie",
  "original_language": "en", "genre_ids": [53],
  "popularity": 8.2741, "release_date": "2026-09-17",
  "softcore": false, "video": false,
  "vote_average": 0.0, "vote_count": 0
}
```

**Detail shape** adds: `belongs_to_collection`, `budget`, `genres` (objects, not ids), `homepage`, `imdb_id`, `origin_country`, `production_companies`, `production_countries`, `revenue`, `runtime`, `spoken_languages`, `status`, `tagline`.

**Genres:** 19, integer ids, stable. `28 Action, 12 Adventure, 16 Animation, 35 Comedy, 80 Crime, 99 Documentary, 18 Drama, 10751 Family, 14 Fantasy, 36 History, 27 Horror, 10402 Music, 9648 Mystery, 10749 Romance, 878 Science Fiction, 10770 TV Movie, 53 Thriller, 10752 War, 37 Western`. Ids are not contiguous and are not sorted by name, so the app must treat them as opaque keys.

**The asymmetry that costs work:** list items carry `genre_ids` only, detail carries `genres` objects. Any list rendering that shows genre names needs the separate genre list fetched and joined. This is the brief's open question Q1.

**Licence, and why it matters more than the shape:** the API terms forbid commercial use without a written agreement, require the TMDB logo plus a non-endorsement notice, and **cap caching at six months**. AMRO's stated ambition to offer features offline runs straight into that ceiling: an offline-first store that keeps a title forever is a terms breach, not just a design choice.

---

### 2.1 OMDb — DOC (401 confirmed live without a key)

**Auth:** `?apikey=`. No header option. The key travels in the URL, which is exactly the hygiene problem D13 avoided for TMDB.

**Endpoints:** there is effectively one, plus a poster service.

```
GET /?i={imdb_id}|t={title}   &type=&y=&plot=short|full&r=json|xml
GET /?s={title}&page=1..100   search, 10 results per page
GET http://img.omdbapi.com/?apikey=&i=   posters, patrons only
```

**Shape:** flat, all values are strings, missing values are the literal `"N/A"`. Fields: `Title, Year, Rated, Released, Runtime, Genre, Director, Writer, Actors, Plot, Language, Country, Awards, Poster, Ratings[], Metascore, imdbRating, imdbVotes, imdbID, Type, DVD, BoxOffice, Production, Website, Response`.

`Ratings` is the reason to use OMDb at all: an array of `{Source, Value}` carrying Internet Movie Database, Rotten Tomatoes and Metacritic in one call. Rotten Tomatoes has no public API of its own, and OMDb is one of the few legitimate routes to a Tomatometer number.

**Genres:** a single comma-joined string of IMDb genre names, `"Action, Adventure, Sci-Fi"`. No ids, no array. Parsing is on the client.

**Free tier:** 1,000 requests per day, stated at key signup rather than in the public docs, so treat the number as unconfirmed. The Poster API is patron-only.

**Verdict for AMRO:** the cheapest possible second source. Keyed by `imdb_id`, which TMDB detail already returns for 100% of the sampled trending set, so the join needs no fuzzy matching. Everything is a string, including numbers, so the data layer needs real parsing and a strict `"N/A"` policy.

---

### 2.2 IMDb — LIVE (bulk datasets), DOC (commercial)

Two products with nothing in common but the name.

**Bulk datasets**, `https://datasets.imdbws.com/`, no key, gzipped TSV, refreshed daily. A ranged request confirmed the headers:

```
title.basics.tsv.gz
tconst  titleType  primaryTitle  originalTitle  isAdult  startYear  endYear  runtimeMinutes  genres
tt0000001  short  Carmencita  Carmencita  0  1894  \N  1  Documentary,Short

title.ratings.tsv.gz
tconst  averageRating  numVotes
tt0000001  5.7  2229
```

Companions: `title.akas`, `title.crew`, `title.principals`, `title.episode`, `name.basics`. Missing values are `\N`.

**Genres, measured rather than recalled:** 268,084 rows were decompressed and the genre column tallied. Exactly **28 distinct genres**, at most three per title:

`Drama, Comedy, Short, Documentary, Romance, Crime, Adult, Action, Animation, Adventure, Family, Thriller, Western, Horror, Mystery, Music, Fantasy, Musical, War, History, Sci-Fi, Biography, Sport, Film-Noir, Game-Show, Talk-Show, News, Reality-TV`

This is the vocabulary that OMDb, Cinemeta and much of the ecosystem re-export.

**Licence:** personal and non-commercial only. There is no free commercial path.

**Commercial:** IMDb Essential Metadata and IMDb Ratings ship through AWS Data Exchange behind a GraphQL API, with add-ons for IMDb Meters, user reviews, parents guide, trivia and Box Office Mojo grosses. Pricing is on request.

**Verdict for AMRO:** unusable in an app. Useful as a fixture generator: the datasets are the honest way to build a large, real test corpus without hammering anybody's API.

---

### 2.3 Trakt — SPEC (full API Blueprint downloaded, 854 KB)

**Auth:** `trakt-api-key: {client_id}` plus `trakt-api-version: 2` on every call. OAuth only for user-scoped writes. An unauthenticated request is dropped by Cloudflare at the edge, confirmed live with a 403 challenge page rather than a JSON error.

**Movie surface, and it is wide:**

```
/movies/trending  /popular  /favorited  /played  /watched  /collected  /anticipated
/movies/updates/{start_date}   /movies/updates/id/{start_date}
/movies/{id}      /aliases /releases /translations /comments /lists /people
/movies/{id}/ratings /related /stats /studios
/recommendations/movies        /genres/movies
```

**Shape, `?extended=full`:**

```json
{
  "title": "TRON: Legacy", "year": 2010,
  "ids": { "trakt": 343, "slug": "tron-legacy-2010", "imdb": "tt1104001", "tmdb": 20526 },
  "tagline": "The Game Has Changed.", "overview": "...",
  "released": "2010-12-16", "runtime": 125, "country": "us",
  "updated_at": "2014-07-23T03:21:46.000Z", "trailer": null,
  "homepage": "http://disney.go.com/tron/", "status": "released",
  "rating": 8.0, "votes": 111, "comment_count": 92,
  "languages": ["en"], "available_translations": ["en"],
  "genres": ["action"], "certification": "PG-13", "original_title": "TRON: Legacy"
}
```

The `ids` block is the single best cross-source join key in this whole survey: Trakt, slug, IMDb and TMDB in one object on every entity.

**Genres:** 34 top-level slugs, and uniquely in this field, a **second tier**. `?extended=subgenres` returns subgenres per genre; `action` alone expands to `action-comedy, heist, revenge, martial-arts, buddy-cop, spy-thriller, vigilante, assassin, chase, manhunt, vigilante-justice, bounty-hunter, gunfight, one-man-army, undercover-agent, heroic-bloodshed, sword-and-sandal`.

Top level: `action, adventure, animation, anime, comedy, crime, disaster, documentary, donghua, drama, eastern, family, fan-film, fantasy, film-noir, history, holiday, horror, indie, music, musical, mystery, none, road, romance, science-fiction, short, sports, sporting-event, suspense, thriller, tv-movie, war, western`.

Note `none` as an explicit genre. Trakt models "we know it has no genre" rather than leaving the array empty.

**Paging:** `?page=&limit=`, default 10 per page. Response carries `X-Pagination-Page`, `X-Pagination-Limit`, `X-Pagination-Page-Count`, `X-Pagination-Item-Count`. Contrast TMDB, where 20 per page is not configurable at all.

**Limits:** 1,000 GET per 5 minutes, authenticated or not. Writes are 1 per second. A 429 carries `Retry-After` and an `X-Ratelimit` JSON blob.

**Verdict for AMRO:** the closest thing to a drop-in second opinion on "what is trending". Configurable page size would have eliminated the brief's D11 paging problem outright. Some endpoints are VIP-gated.

---

### 2.4 TheTVDB v4 — SPEC (OpenAPI 3.0.0, version 4.7.10, 115 KB)

**Auth:** POST `/login` with `{apikey, pin?}`, receive a bearer token valid one month. The PIN applies to user-supported keys.

**Movie surface:**

```
GET /movies                       paged list
GET /movies/{id}                  base record
GET /movies/{id}/extended         everything
GET /movies/{id}/translations/{language}
GET /movies/filter                by country, language, company, genre, sort, year
GET /movies/slug/{slug}
GET /movies/statuses
GET /genres  /genres/{id}
GET /search  /search/remoteid/{remoteId}
```

`/search/remoteid/{remoteId}` resolves an IMDb or TMDB id straight to a TVDB record, which makes it cheap to bolt onto an existing TMDB-keyed store.

**`MovieExtendedRecord` fields:** `aliases, artworks, audioLanguages, awards, boxOffice, boxOfficeUS, budget, characters, companies, contentRatings, first_release, genres, id, image, inspirations, lastUpdated, lists, name, nameTranslations, originalCountry, originalLanguage, overviewTranslations, ...` plus releases, remoteIds, runtime, score, slug, spoken_languages, status, studios, subtitleLanguages, trailers, year.

Three things TMDB does not give: **`boxOfficeUS` split out separately**, **`awards` as structured records**, and **`inspirations`**, which links a film to the book or event it came from. `contentRatings` is per-country and structured, where TMDB needs a separate release-dates call.

Note the spec's own warning: `score` is generated differently per entity type and carries no defined meaning beyond relative ordering.

**Genres:** `GenreBaseRecord` is `{id, name, slug}`. Count not verified, the endpoint needs a key.

**Money:** free below $50k annual revenue with attribution and a direct link to TheTVDB.com. $1,000/yr for $50k to $250k, $10,000/yr for $250k to $1M, custom above. Self-assessed, adjusted annually.

**Verdict for AMRO:** strongest on artwork, awards and per-country certification. TV-shaped, so movie coverage is thinner than TMDB's. The revenue tiering is the friendliest commercial model in this list for a startup that actually starts earning.

---

### 2.5 Watchmode — DOC (full API Explorer read, v1.1.7)

**Auth:** `X-API-Key` header preferred, `Authorization: Bearer` accepted, `?apiKey=` kept for backwards compatibility. The docs explicitly recommend the header so the key stays out of URLs, logs and referrers. Same reasoning as brief D13.

**Endpoints:**

```
GET /v1/sources     /regions     /networks     /genres          configuration
GET /v1/search      /autocomplete-search                        lookup
GET /v1/list-titles                                             filter and page
GET /v1/title/{id}/details  /sources  /seasons  /episodes  /cast-crew
GET /v1/person/{id}
GET /v1/releases    /title-release-dates
GET /v1/changes/new_titles  /new_people  /titles_sources_changed
    /titles_details_changed /titles_episodes_changed            paid only
GET /v1/status                                                  quota
```

**Title detail fields:** `id, title, original_title, plot_overview, will_you_like_this, review_summary, type, runtime_minutes, year, end_year, release_date, imdb_id, tmdb_id, tmdb_type, genres[], genre_names[], user_rating, critic_score, us_rating, content_ratings{}, poster, posterMedium, posterLarge, backdrop, original_language, similar_titles[], networks[], network_names[], relevance_percentile, popularity_percentile, trailer, trailer_thumbnail`.

Two fields worth naming: `will_you_like_this` and `review_summary` are AI-generated prose. They are the only generated-text fields in this entire survey, and any product using them inherits the accuracy question.

`popularity_percentile` is normalised 0 to 100, which is easier to reason about than TMDB's unbounded `popularity` float.

**Sources shape:** `{source_id, name, type, region, ios_url, android_url, web_url, tvos_url, android_tv_url, roku_url, format, price, seasons, episodes}` where `type` is one of `sub, rent, buy, free, tve`. On the free plan the deep link fields literally return the string `"Deeplinks available for paid plans only."` rather than null, which will trip a naive parser.

**Genres:** integer ids with an explicit `tmdb_id` mapping field on every row, `0` where no TMDB equivalent exists. `{"id": 4, "name": "Comedy", "tmdb_id": 35}`, and `{"id": 33, "name": "Anime", "tmdb_id": 0}`. This is the only source in the survey that ships a TMDB genre mapping as data rather than leaving you to write it.

**Credit accounting, which is the real cost model:** a Watchmode id lookup costs 1 credit, an IMDb or TMDB id lookup costs 2, each `append_to_response` value costs 1 more, and each extra region costs 1. A single "details plus sources plus cast" call keyed by TMDB id is therefore 4 credits, not 1. The free 2,500 monthly requests are closer to 600 useful screens.

**Plans:** Developer free, 2,500/month, non-commercial, attribution required, up to 3 countries, no deeplinks, no premium endpoints. Startup $349/mo for 40,000. Business $599/mo for 100,000. Enterprise custom with S3 or sFTP dataset access.

**Caching terms, which matter for offline:** paid subscribers may store data indefinitely while subscribed. Free users must refresh or delete cached data within 30 days. All accounts delete on cancellation.

**Coverage:** 367 providers listed, 54 countries.

---

### 2.6 Streaming Availability, Movie of the Night — DOC

**Auth:** RapidAPI key.

**Endpoints:** `GET /shows/{id}`, `/shows/search/filters`, `/shows/search/title`, `/shows/top`, `/countries`, `/genres`, `/changes`.

`/shows/{id}` accepts its own id, an IMDb id (`tt…`), or a TMDB id in `movie/278` form, so no separate mapping step is needed.

**Show fields:** `itemType, showType, id, imdbId, tmdbId, title, overview, releaseYear, firstAirYear, lastAirYear, originalTitle, genres[], directors[], creators[], cast[], rating (0-100), seasonCount, episodeCount, runtime, imageSet, streamingOptions, seasons`.

`streamingOptions` is a map keyed by country code, each value an array carrying service, deep link, quality, audio and subtitle languages, price and addon information.

**Genres:** 21, `{id: string, name: string}`, with an `output_language` parameter for localised names.

**Paging:** cursor-based, `hasMore` plus `nextCursor`. The only cursor pager in this survey; everything else is offset or page-number.

**Plans:** Free $0 for 1,000/month, Starter $49 for 25,000, Growth $99 for 100,000, Scale $299 for 1,000,000. Quota is a hard stop with no overage billing.

**The differentiator:** commercial use is permitted on every tier including free. Against Watchmode's $349 entry point for any commercial use at all, this is the pragmatic choice for a startup shipping a where-to-watch feature. Watchmode wins on breadth, 200+ services and 54 countries against 65 countries with a narrower service list, and on episode-level links.

---

### 2.7 Simkl — SPEC (blueprint) + DOC (current docs at api.simkl.org)

**Auth:** `client_id` on every request, plus `app-name` and `app-version` parameters and a real `User-Agent`. OAuth 2.0, PKCE or PIN flow for user data.

**Endpoints:** `/movies/{id}` where id is a Simkl or IMDb id, `/movies/trending`, `/movies/genres/{genre}/{year}/…`, `/search/…`, `/sync/…`, plus `/search/random` with filters on type, genre, year range, rating, popularity rank and streaming service.

**Shape, `?extended=full`:**

```json
{
  "title": "Harry Potter and the Deathly Hallows: Part 2", "year": 2011, "type": "movie",
  "ids": { "simkl": 67144, "slug": "...", "tmdb": "12445", "imdb": "tt1201607", "offen": "http://www.harrypotter.com" },
  "rank": 641, "poster": "96/967506e32f7921d4", "fanart": "96/967624d7a281067c",
  "released": "2011-07-15", "runtime": 130, "director": "David Yates",
  "certification": "PG", "budget": 125000000, "revenue": 1342000000,
  "overview": "...", "genres": ["Adventure", "Family", "Fantasy"], "country": "US",
  "ratings": { "simkl": {"rating": 8.1, "votes": 427}, "imdb": {"rating": 8.1, "votes": 607002} },
  "trailers": [{ "name": "Official Trailer 1", "youtube": "mObK5XD8udk", "size": 1080 }]
}
```

Images are path fragments, not URLs. The client composes the CDN prefix and a size suffix, the same pattern as TMDB's `image.tmdb.org/t/p/{size}`.

`ratings` carries Simkl and IMDb side by side in the base response, with no extra call.

**Genres:** title-cased strings in the response, lowercase slugs in the browse path. The browse vocabulary mirrors the website's own filter URLs, which means it is defined by a web page rather than by an endpoint. Observed movie set: `action, adventure, animation, anime, comedy, crime, documentary, drama, family, fantasy, history, horror, music, mystery, romance, science-fiction, short, thriller, war, western`.

**Limits:** 10 GET/sec and 1 POST/sec per `client_id` and per user token. The docs are unusually blunt: sustained overage suspends the `client_id` "without warning, no appeal".

**Licence:** free for non-commercial, commercial licence required.

---

### 2.8 MDBList — SPEC (OpenAPI 3.0.3, 360 KB, fetched from `/schema/`)

**Auth:** `?apikey=` or `Authorization: Bearer`. A keyless call returns a clean JSON 401 naming both options, confirmed live.

**Endpoints:**

```
GET /{media_provider}/{media_type}/{media_id}/        media_provider ∈ imdb, mal, mdblist, tmdb, trakt, tvdb
GET /{media_provider}/{media_type}/                   batch lookup
GET /{media_provider}/{media_type}/{media_id}/watchprovider_links/
GET /catalog/movie   /catalog/show                    filter, sort, page
GET /genres                                           ?anime=0|1
GET /justwatch/streaming-charts/{media_type}
GET /lists/{listid}/items   /external/lists/{listid}/items
GET /calendar/events
```

**Response:** `{id, imdb_id, title, year, type, score, ratings[], streams[], recommendations[]}` with `append_to_response` accepting `episode_ratings, extra, keyword, recommendations, review`.

**The reason this source exists:** its rating vocabulary. The catalogue sort enum names every provider it aggregates: `imdbrating, imdbvotes, imdbpopular, tmdbpopular, letterrating, lettervotes, metacritic, myanimelist, rogerebert, rtomatoes, rtaudience, score, score_average`. Nine rating providers, including **Letterboxd and Roger Ebert**, which appear nowhere else in this survey, and both Rotten Tomatoes critic and audience scores as separate values.

Also sortable on `budget, revenue, runtime, released, releasedigital, last_air_date, rank, random, sort_title, title, added, usort`.

**Genres:** `GET /genres` returns `[{id, name}]`, with `?anime=1` switching to an anime-specific visibility set. Two vocabularies behind one endpoint.

**Limits:** free 1,000 requests/day. Patreon tiers at €1, €2, €3 and €5 raise it to 10,000, 25,000, 100,000 and 250,000 per day. The cheapest scaling in this survey by an order of magnitude.

**Caveat:** the `/justwatch/streaming-charts/` endpoint re-exports JustWatch data. Whether that is licensed is not stated anywhere in the spec.

---

### 2.9 JustWatch — LIVE, and excluded

There is no public JustWatch API. There is an unauthenticated GraphQL endpoint at `https://apis.justwatch.com/graphql` that the website itself uses, and it answers anyone who asks. A live query returned, in one round trip:

```json
{ "title": "Inception", "originalReleaseYear": 2010,
  "genres": [ {"shortName": "act", "translation": "Action & Adventure"},
              {"shortName": "trl", "translation": "Mystery & Thriller"},
              {"shortName": "scf", "translation": "Science-Fiction"} ],
  "externalIds": { "imdbId": "tt1375666", "tmdbId": "27205" },
  "scoring": { "imdbScore": 8.8, "tmdbScore": 8.373, "tomatoMeter": 86 },
  "offers": [ {"monetizationType": "FLATRATE", "presentationType": "_4K", "package": {"clearName": "HBO Max"}}, ... ] }
```

IMDb score, TMDB score and Tomatometer in one response, with per-country offers and quality tiers. It is the best-shaped data in the entire survey.

**It is also the one source here that must not ship.** The endpoint is undocumented, unversioned, unauthenticated and not offered for third-party use. Building on it means building on something that can change or start blocking without notice, and doing so against the terms of a company that sells this exact data commercially. Watchmode and Streaming Availability exist to be the licensed version of this.

Its **genre vocabulary is still worth studying**, because it is the only one in the survey designed for a filter UI rather than for cataloguing. 19 genres, three-character codes, and deliberately merged buckets:

`act Action & Adventure, cmy Comedy, crm Crime, doc Documentary, drm Drama, eur Made in Europe, fml Kids & Family, fnt Fantasy, hrr Horror, hst History, msc Music & Musical, rly Reality TV, rma Romance, scf Science-Fiction, spt Sport, trl Mystery & Thriller, war War & Military, wsn Western, ani Animation`

Note what merging does. TMDB's separate Action and Adventure, and separate Mystery and Thriller, each collapse into one chip. And `eur Made in Europe` is not a genre at all, it is a provenance facet wearing a genre's clothes. That is a product decision showing through a data model, and it is directly relevant to the brief's D1 multi-select filter sheet.

---

### 2.10 Cinemeta (Stremio) — LIVE

**Auth:** none. `https://v3-cinemeta.strem.io/`.

```
GET /manifest.json                        catalogue and genre declarations
GET /meta/movie/{imdb_id}.json
GET /catalog/movie/{top|year}.json        supports /genre=X.json and /skip=N.json
```

**Shape**, live for `tt1375666`:

```json
{ "name": "Inception", "year": "2010", "imdb_id": "tt1375666", "moviedb_id": 27205,
  "genres": ["Action", "Adventure", "Sci-Fi"], "imdbRating": "8.8",
  "runtime": "148 min", "released": "2010-07-16T00:00:00.000Z",
  "country": "United Kingdom, United States",
  "director": ["Christopher Nolan"], "writer": ["Christopher Nolan"],
  "cast": ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"],
  "awards": "Won 4 Oscars. 160 wins & 220 nominations total",
  "popularities": { "moviedb": 55.6277, "stremio": 1.854, "trakt": ... },
  "trailerStreams": [{ "title": "Inception", "ytId": "..." }],
  "poster": "https://images.metahub.space/poster/small/tt1375666/img",
  "background": "...", "logo": "..." }
```

`runtime` is the string `"148 min"`, `imdbRating` is the string `"8.8"`, `year` is a string. Everything is IMDb-derived and IMDb-shaped.

Two things nothing else here offers for free: **`awards` as a human-readable summary**, and **`popularities`**, a side-by-side popularity reading from TMDB, Trakt and Stremio in one object. Images come from metahub with `small`, `medium` and `large` variants, and a `logo` per title.

**Genres, and a trap worth naming:** the manifest declares **19** filterable movie genres (the IMDb 28 minus Adult, Film-Noir, Game-Show, Music, Musical, News, Reality-TV, Short, Talk-Show). But meta objects return the **full 28**: a live fetch of Big Buck Bunny came back `["Animation", "Short", "Comedy"]`, and `Short` is not in the filter list. **The filter vocabulary and the item vocabulary are different sets.** Any UI built from the manifest will show items carrying genres it cannot filter on.

**Terms:** none published. This is Stremio's internal addon service, not a product. No SLA, no support, and no promise it stays open. Fine for prototyping and for fixtures, not something to build a company on.

---

### 2.11 Wikidata — LIVE

**Auth:** none. SPARQL at `https://query.wikidata.org/sparql`, needs a real `User-Agent`.

A live query resolving IMDb id to genres, duration and box office:

```sparql
SELECT ?g ?gLabel ?dur ?boxoffice WHERE {
  ?film wdt:P345 "tt1375666".
  OPTIONAL { ?film wdt:P136 ?g. }
  OPTIONAL { ?film wdt:P2047 ?dur. }
  OPTIONAL { ?film wdt:P2142 ?boxoffice. }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
}
```

Returned `drama film (Q130232)`, `action film (Q188473)` and others, duration `148`, box office `292587330`.

Useful properties: `P345` IMDb id, `P4947` TMDB id, `P136` genre, `P57` director, `P161` cast, `P2047` duration, `P2130` cost, `P2142` box office, `P166` award received, `P444` review score, `P495` country of origin.

**Genres:** no fixed vocabulary. A live count of items that are instances of film genre or its subclasses returned **693**. Genres are Q-items in an open ontology, so `?g wdt:P279* wd:Q130232` finds every subgenre of drama transitively. That is expressive far beyond any flat list, and completely unsuited to a filter chip row.

**Practical limits:** a 60 second query timeout, enforced hard. A broad aggregate over all films returned a 502 in this session before finishing. Any production use needs narrow, indexed queries, not scans.

**Licence:** CC0. No restrictions at all, which makes it the only source here with no legal friction whatsoever.

**Verdict for AMRO:** not a primary source. The right tool for a specific gap, such as awards, or linking a film to the novel it adapts.

---

### 2.A Fanart.tv — SPEC (blueprint), demo key expired

Artwork only, no metadata. `GET /v3/movies/{tmdb_id|imdb_id}` returns arrays keyed by artwork type: `hdmovielogo, movielogo, moviedisc, movieposter, moviebackground, moviebanner, moviethumb, hdmovieclearart, movieart`. Each entry is `{id, url, lang, likes}`, and disc art adds `{disc, disc_type}`.

The per-language `lang` field is the point: a localised title logo per language, which TMDB's image API does not organise as cleanly. Requires both a project `api-key` and a personal `client-key` for the newest uploads. Relevant only if AMRO wants logo-over-backdrop treatments in the detail header.

---

## 3. Genre systems, compared head to head

This is where the sources differ most, and where a second source costs the most work.

| Source | Count | Key type | On list items | Shape |
|---|---|---|---|---|
| **TMDB** | 19 | Non-contiguous integer | `genre_ids: [53]` | `{id, name}` on detail only |
| **IMDb** | 28 | Name string | n/a | `"Documentary,Short"`, max 3 |
| **OMDb** | 28 (IMDb's) | Name string | n/a | `"Action, Adventure, Sci-Fi"`, one field |
| **Cinemeta** | 28 on items, **19 filterable** | Name string | yes | `["Action", "Adventure", "Sci-Fi"]` |
| **Trakt** | 34 + subgenre tier | Lowercase slug | yes | `["action"]`, `{name, slug}` on `/genres` |
| **JustWatch** | 19, merged buckets | 3-char code | yes | `{shortName, translation}` |
| **Simkl** | ~20 | Slug in path, title-case in body | yes | `["Adventure", "Family"]` |
| **Watchmode** | integer, unverified count | Integer + **`tmdb_id` mapping** | ids and names both | `{id, name, tmdb_id}` |
| **Streaming Availability** | 21 | String id | yes | `{id, name}`, localisable |
| **TheTVDB** | unverified | Integer + slug | yes | `{id, name, slug}` |
| **MDBList** | two sets, film and anime | Integer | yes | `{id, name}`, `?anime=0\|1` |
| **Wikidata** | **693** | Q-item | n/a | open ontology, transitive subclasses |

### What actually breaks when you add a second source

**No two vocabularies agree, and the disagreements are not cosmetic.**

- TMDB has `Science Fiction`. IMDb, OMDb and Cinemeta have `Sci-Fi`. Trakt has `science-fiction`. Three spellings of one concept across four sources.
- TMDB has `TV Movie` as genre id 10770. IMDb has no such genre, it is a `titleType` instead. Trakt has both `tv-movie` as a genre and its own type system. **The same distinction is a genre in one model and a type in another.**
- IMDb carries `Short`, `News`, `Talk-Show`, `Game-Show` and `Reality-TV`. TMDB carries none of them. Trakt adds `anime`, `donghua`, `holiday`, `indie`, `road`, `eastern`, `disaster`, `fan-film` and `none`, nine concepts TMDB has no slot for.
- JustWatch merges pairs TMDB keeps apart: `Action & Adventure` and `Mystery & Thriller`. A round trip through JustWatch loses the Action/Adventure distinction permanently.
- Trakt is the only two-level system. Nothing else can express that `heist` is a kind of `action`.
- Wikidata is not a list at all. It is a 693-item graph with transitive subclassing, which no chip row can render.

**The consequences for AMRO are concrete.** The MVP filters client-side over TMDB `genre_ids` (D6, D1). That works precisely because there is one source with one vocabulary. Add a second, and one of three things has to happen:

1. **Own the vocabulary.** Define an AMRO genre enum and map every source into it. Merges lose information, gaps need a fallback, and the mapping is code that needs its own tests. This is the only option that scales past two sources.
2. **Keep TMDB genres canonical** and treat other sources as enrichment only, never as a genre authority. Cheapest, and the honest MVP answer.
3. **Union the vocabularies** and let the filter sheet grow. Guarantees near-duplicate chips (`Sci-Fi` next to `Science Fiction`) and a filter that returns different results depending on which chip a user picks.

Only Watchmode ships the mapping as data, via `tmdb_id` on each genre row, `0` where none exists. Everyone else leaves it to you.

**Where this lands against the brief:** open question Q1 asks where the genre-id-to-name join happens and when the 19-entry list is fetched. The answer should not be "join TMDB ids to TMDB names in the repository", it should be "resolve source genres into the domain's own genre type at the source boundary". Those are the same amount of work today and completely different amounts of work when a second source arrives. That is what being ready for multiple sources without having built them looks like.

---

## 4. Release status: one word, three meanings

Only four of the twelve sources carry a title's release or production state at all, and no two represent it the same way.

### The word is overloaded

**Title lifecycle** is what most callers want: has this film come out, or is it still in post production. TMDB, Trakt, Simkl and TheTVDB all expose it.

**Envelope status** is the other thing. Every TheTVDB response is wrapped as `{"data": ..., "status": "..."}`, where that outer `status` reports whether the call succeeded. OMDb's `Response` field, `"True"` or `"False"`, is the same idea under a different name. A lookup that reaches for `status` on a TheTVDB response finds the envelope, not the movie.

### Per source

| Source | Field | Where it lives | Values |
|---|---|---|---|
| **TMDB** | `status`, string | detail only, **absent from list items** | `Rumored`, `Planned`, `In Production`, `Post Production`, `Released`, `Canceled` |
| **Trakt** | `status`, string | `?extended=full` | `released`, `in production`, `post production`, `planned`, `rumored`, `canceled` |
| **Simkl** | `status`, string | movie objects, including list rows | `released` for movies |
| **TheTVDB** | `status`, **object** | `MovieBaseRecord` and `MovieExtendedRecord` | a `Status` record, `{id, name, keepUpdated, recordType}`. Enumerate the set with `GET /movies/statuses`. |
| **Watchmode** | none | | `type`, `year` and `end_year` only |
| **Streaming Availability** | none | | `showType` is `movie` or `series`, plus `releaseYear` |
| **MDBList** | none for release state | | `status` appears in its schema only on user-collection sync payloads, `added` and `removed` |
| **OMDb** | none | | `Response` is call outcome, not title state |
| **IMDb datasets** | none | | `titleType`, `startYear`, `endYear` |
| **Cinemeta** | movies none, series yes | verified live: a movie meta object has no `status` key | |
| **JustWatch** | none observed | | `objectType` is `MOVIE` or `SHOW` |
| **Wikidata** | none | | modelled as `P31` plus `P577` publication date |

Where both movies and TV exist, the vocabularies diverge again. Trakt's show statuses are `returning series`, `continuing`, `in production`, `planned`, `upcoming`, `pilot`, `canceled` and `ended`. Simkl reuses `ended` and `airing` for shows, and a separate set for a user's own list: `watching`, `plantowatch`, `completed`, `hold`, `dropped`.

### Measured

All 20 details behind TMDB trending page 1 were fetched on 2026-09-02: **19 `Released`, 1 `Post Production`**. That matches the brief's larger sample of 92 distinct titles, which came back Released 83, Post Production 6, In Production 2, Planned 1.

The distribution is the point. Roughly one trending film in ten has not been released, which is why `vote_count` is zero for 11% of them and why budget and revenue are empty for over 40%. `status` is the field that explains the missing data, and it is the field the list endpoint does not send.

### What it costs

TMDB gives title-case strings, Trakt and Simkl give lowercase strings, TheTVDB gives a nested object with an id. A shared `ReleaseStatus` in the domain needs per-source parsing, exactly like the genre vocabularies one layer up. And because TMDB carries `status` on detail but not on list items, any list-level decision that depends on it, such as suppressing a rating block for an unreleased film, either waits for the detail fetch or infers the state from `vote_count` and `release_date`.

---

## 5. Field coverage

`Y` present, `.` absent, `~` present but degraded or string-typed.

| Field | TMDB | OMDb | IMDb bulk | Trakt | TVDB | Watchmode | Str.Avail | Simkl | MDBList | Cinemeta | Wikidata |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Title, overview | Y | Y | ~ | Y | Y | Y | Y | Y | Y | Y | Y |
| Poster, backdrop | Y | ~ | . | . | Y | Y | Y | Y | . | Y | . |
| Logo, clearart | . | . | . | . | Y | . | . | . | . | Y | . |
| Runtime | Y | ~ | Y | Y | Y | Y | Y | Y | . | ~ | Y |
| Release date | Y | Y | ~ | Y | Y | Y | ~ | Y | Y | Y | Y |
| Genres | Y | ~ | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| Budget, revenue | Y | ~ | . | . | Y | . | . | Y | Y | . | Y |
| Own rating | Y | Y | Y | Y | ~ | Y | Y | Y | Y | Y | . |
| Rotten Tomatoes | . | Y | . | . | . | ~ | . | . | Y | . | ~ |
| Metacritic | . | Y | . | . | . | ~ | . | . | Y | . | ~ |
| Letterboxd, Ebert | . | . | . | . | . | . | . | . | Y | . | . |
| Certification | ~ | Y | . | Y | Y | Y | . | Y | . | . | . |
| Cast, crew | ~ | Y | Y | ~ | Y | ~ | Y | ~ | . | Y | Y |
| Awards | . | Y | . | . | Y | . | . | . | . | Y | Y |
| Trailers | ~ | . | . | Y | Y | Y | . | Y | . | Y | . |
| Streaming availability | ~ | . | . | . | . | Y | Y | ~ | Y | . | . |
| Deep links | . | . | . | . | . | Y | Y | . | ~ | . | . |
| IMDb id | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| TMDB id | Y | . | . | Y | Y | Y | Y | Y | Y | Y | Y |
| Change feed | Y | . | Y | Y | Y | Y | Y | . | . | . | . |

`~` notes: TMDB cast, images, videos and watch providers each need a separate call or `append_to_response`. OMDb numbers are strings. IMDb bulk splits across files. Watchmode's Rotten Tomatoes and Metacritic arrive folded into one `critic_score`, not attributed.

---

## 6. Joining across sources

Every source in this survey except IMDb bulk speaks IMDb id, and every one except OMDb, IMDb bulk and Fanart.tv speaks TMDB id.

```
TMDB id ──(TMDB /movie/{id}.imdb_id)──> IMDb id ──> OMDb, Cinemeta, Simkl, MDBList
        └──(Trakt .ids{trakt,slug,imdb,tmdb})──> everything Trakt touches
        └──(TVDB /search/remoteid/{id})────────> TVDB id
        └──(Watchmode /search?search_field=tmdb_movie_id, or the daily title_id_map.csv)
        └──(Streaming Availability /shows/movie/{tmdb_id} directly)
        └──(Wikidata P4947, or P345 for IMDb)
```

The path of least resistance is unchanged from the MVP: hold TMDB id as the primary key, carry `imdb_id` alongside it, and every other source becomes reachable in one hop. The brief already records `imdb_id` present on 100% of the sampled detail responses, which makes this join safe in practice while still nullable in the model.

Watchmode's `title_id_map.csv`, refreshed daily and carrying Watchmode, IMDb and TMDB ids for every title, is the only bulk mapping offered anywhere in this list.

---

## 7. What to do about it

**Keep TMDB as the primary source.** Nothing here displaces it for a general movie catalogue: it is the only source combining broad coverage, good images, a clean discovery endpoint and no per-request cost. The survey confirms the MVP choice rather than unsettling it.

**Two facts about TMDB deserve to be on the record**, because neither is a shape problem:

- **The six-month cache ceiling.** AMRO's stated ambition is offline features. TMDB's terms allow caching for at most six months. An unbounded offline store is a licence breach, not merely a design trade-off. The MVP's 24-hour detail freshness window (D17) sits comfortably inside it. A future offline mode needs an eviction policy for legal reasons, not only for storage.
- **Commercial use needs a written agreement.** A non-commercial MVP is fine. The AMRO business is not, without signing something.

**If a second source is added, it should be chosen by what it adds, not by overlap:**

| Goal | Source | Why it, and not the alternative |
|---|---|---|
| Critic scores, cheapest possible | **OMDb** | Rotten Tomatoes and Metacritic in one call, keyed by an `imdb_id` already held. Everything is a string. |
| Many ratings at once | **MDBList** | Nine providers including Letterboxd and Roger Ebert. 1,000/day free, 250,000/day for €5/month. |
| Where to watch, shipping commercially | **Streaming Availability** | Commercial use on the free tier. Watchmode is broader but starts at $349/mo for any commercial use. |
| Where to watch, maximum coverage | **Watchmode** | 367 providers, 54 countries, episode-level deep links, and the only genre table with a TMDB mapping. Budget for the credit multipliers. |
| Trending, better paging | **Trakt** | Configurable page size would have removed D11's paging problem. `ids` block is the best join key here. |
| Artwork, awards, per-country ratings | **TheTVDB** | Free below $50k revenue, and the only revenue-tiered pricing in the field. |
| Test fixtures at scale | **IMDb datasets** | Real data, no key, no rate limit. Non-commercial, so fixtures only. |

**Excluded on purpose:** JustWatch's GraphQL endpoint, because it is not offered for third-party use. Cinemeta, because it publishes no terms and carries no SLA, though it is a legitimate prototyping and fixture source.

**The one architectural change this survey argues for**, and it costs nothing today: resolve each source's genre representation into an AMRO-owned genre type at the source boundary, rather than letting TMDB's integer ids reach the domain. Twelve vocabularies, no two of which agree, is what "collect data from multiple API sources" actually means in this field.

---

## Appendix: reproducing the live findings

```bash
# TMDB, 19 genres
curl -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/genre/movie/list?language=en-US"

# IMDb, 28 genres measured from 268,084 real rows
curl -s -r 0-6000000 "https://datasets.imdbws.com/title.basics.tsv.gz" \
  | gzip -dc 2>/dev/null \
  | awk -F'\t' 'NR>1{n=split($9,a,","); for(i=1;i<=n;i++) if(a[i]!="\\N") g[a[i]]++} \
                END{for(k in g) print g[k]"\t"k}' | sort -rn

# TMDB, status across every detail behind trending page 1
for id in $(curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
    "https://api.themoviedb.org/3/trending/movie/week" \
    | python3 -c 'import sys,json;print(" ".join(str(r["id"]) for r in json.load(sys.stdin)["results"]))'); do
  curl -s -H "Authorization: Bearer $TMDB_TOKEN" "https://api.themoviedb.org/3/movie/$id" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])'
done | sort | uniq -c | sort -rn

# JustWatch, 19 merged genres
curl -s -X POST https://apis.justwatch.com/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{genres{shortName translation(language:\"en\")}}"}'

# Cinemeta, filter vocabulary (19) against item vocabulary (28)
curl -s https://v3-cinemeta.strem.io/manifest.json
curl -s https://v3-cinemeta.strem.io/meta/movie/tt1254207.json   # returns "Short"

# Wikidata, 693 film-genre items
curl -s -G https://query.wikidata.org/sparql -H 'Accept: application/sparql-results+json' \
  -H 'User-Agent: AMRO-research/1.0' \
  --data-urlencode 'query=SELECT (COUNT(DISTINCT ?g) AS ?c) WHERE { ?g wdt:P31/wdt:P279* wd:Q201658 . }'

# Machine-readable contracts
curl -s https://thetvdb.github.io/v4-api/swagger.yml      # OpenAPI 3.0.0, v4.7.10
curl -s https://api.mdblist.com/schema/                   # OpenAPI 3.0.3
curl -s https://jsapi.apiary.io/apis/trakt.apib           # API Blueprint
curl -s https://jsapi.apiary.io/apis/simkl.apib
curl -s https://jsapi.apiary.io/apis/fanarttv.apib
curl -s https://api.simkl.org/llms.txt                    # current Simkl doc index

# Auth models confirmed by unauthenticated probe
curl -s "https://www.omdbapi.com/?i=tt1375666"            # 401 {"Error":"No API key provided."}
curl -s "https://api.watchmode.com/v1/genres/"            # 401 "Please enter a valid API key."
curl -s "https://api.mdblist.com/tmdb/movie/27205"        # 401, names both apikey and bearer
curl -s "https://api.trakt.tv/movies/inception-2010"      # 403 Cloudflare, no trakt-api-key
```
