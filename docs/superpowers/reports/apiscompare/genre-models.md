# Genre models across movie APIs

**Date:** 2026-09-02
**Companion to:** [`movie-data-apis.md`](movie-data-apis.md), which surveys the same twelve sources across every field. This document covers one field in depth: how a genre appears inside a movie object, whether there is a dedicated genre endpoint, what that endpoint returns, and what multi-genre filtering means on each service.

Evidence tags match the companion report. **LIVE** means a request was issued from this machine on 2026-09-02 and the response is quoted. **SPEC** means the source's OpenAPI document or API Blueprint was downloaded and read. **DOC** means vendor prose, because the endpoint needs a key this project does not hold.

---

## 1. The short answer

| Source | Genre inside a movie object | Dedicated endpoint | Endpoint returns | Evidence |
|---|---|---|---|---|
| **TMDB** | `genre_ids: [53]` on list items, `genres: [{id, name}]` on detail | `GET /3/genre/movie/list` | `{genres: [{id, name}]}`, 19 rows | LIVE |
| **OMDb** | `"Genre": "Action, Adventure, Sci-Fi"`, one comma-joined string | none | | DOC |
| **IMDb datasets** | `genres` column, `"Documentary,Short"`, max 3 | none | vocabulary is implicit in the data | LIVE |
| **Trakt** | `genres: ["action"]`, slugs | `GET /genres/movies` | `[{name, slug}]`, 34 rows, `?extended=subgenres` nests a second tier | SPEC |
| **TheTVDB** | `genres: [GenreBaseRecord]` | `GET /genres`, `GET /genres/{id}` | `{data: [{id, name, slug}], status}` | SPEC |
| **Watchmode** | `genres: [7]` **and** `genre_names: ["Drama"]`, both on the same object | `GET /v1/genres` | `[{id, name, tmdb_id}]` | DOC |
| **Streaming Availability** | `genres: [{id, name}]` | `GET /genres` | `[{id, name}]`, 21 rows, `?output_language=` | DOC |
| **Simkl** | `genres: ["Adventure", "Family"]`, title case | none | vocabulary is documented as path values, 20 slugs | SPEC |
| **MDBList** | inside the media record | `GET /genres?anime=0\|1` | `[{id, name}]`, two vocabularies behind one path | SPEC |
| **JustWatch** | `genres: [{shortName, translation}]` | root `genres` field on the GraphQL schema | 19 rows of `{shortName, translation}` | LIVE |
| **Cinemeta** | `genres: ["Action", "Adventure", "Sci-Fi"]`, and a duplicate `genre` key | none, declared in `manifest.json` | catalog `extra.options`, 19 filterable strings | LIVE |
| **Wikidata** | `P136` statements pointing at Q-items | none, it is a SPARQL query | 693 items are instances of film genre | LIVE |

Three sources carry no genre vocabulary of their own at all: OMDb and Cinemeta re-export IMDb's, and IMDb publishes no endpoint because the vocabulary only exists as values in a TSV column.

---

## 2. Inside the movie object

### TMDB — LIVE

The asymmetry is the single most consequential fact in this document.

```jsonc
// GET /3/trending/movie/week  ->  results[0]
{ "id": 1386315, "title": "The Runner", "genre_ids": [53] }

// GET /3/movie/27205
{ "id": 27205, "title": "Inception",
  "genres": [ {"id": 28, "name": "Action"},
              {"id": 878, "name": "Science Fiction"},
              {"id": 12,  "name": "Adventure"} ] }
```

Different key, different type, same concept. A list screen that shows genre names cannot get them from the list response. That is what forces the separate `/genre/movie/list` fetch and the client-side join.

### OMDb — DOC

```json
{ "Genre": "Action, Adventure, Sci-Fi" }
```

One string, comma and space delimited, singular key name. No ids, no array, and the delimiter appears inside no genre name, so a naive `split(", ")` is safe. When absent the value is the literal `"N/A"`, not an empty string and not `null`.

### IMDb datasets — LIVE

```
tconst      titleType  primaryTitle  ...  genres
tt0000001   short      Carmencita    ...  Documentary,Short
```

Comma delimited with no space, capped at three per title, `\N` when empty.

### Trakt — SPEC

```json
{ "title": "TRON: Legacy", "genres": ["action"], "certification": "PG-13" }
```

Lowercase slugs, present only with `?extended=full`. The base response carries no genres at all.

### TheTVDB — SPEC

`genres` is an array of `GenreBaseRecord`, the same object the `/genres` endpoint returns:

```yaml
GenreBaseRecord:
  properties:
    id:   { type: integer, format: int64 }
    name: { type: string }
    slug: { type: string }
```

The only source here that gives you id, display name and a URL-safe slug in one object, on both the movie and the vocabulary endpoint.

### Watchmode — DOC

```json
{ "genres": [7], "genre_names": ["Drama"] }
```

Both representations, pre-joined, on the same object. No second call, no client-side lookup table. Nothing else in this survey does that.

### Streaming Availability — SPEC

```ts
export interface Genre {
  id: string;    // note: string, not integer
  name: string;
}
```

Confirmed from the generated TypeScript client for OpenAPI document 4.1.0. The id being a string matters: `"18"` will not match TMDB's `18` without a cast.

### Simkl — SPEC

```json
{ "title": "Harry Potter and the Deathly Hallows: Part 2",
  "genres": ["Adventure", "Family", "Fantasy"] }
```

Title case in the response body, lowercase hyphenated in the browse path. `Science Fiction` in a movie object is `science-fiction` in a URL. One source, two casings of the same vocabulary.

### JustWatch — LIVE

```json
{ "genres": [ {"shortName": "act", "translation": "Action & Adventure"},
              {"shortName": "trl", "translation": "Mystery & Thriller"},
              {"shortName": "scf", "translation": "Science-Fiction"} ] }
```

Three-character codes with a localised display string alongside. The key is called `translation` rather than `name`, which is honest: the value changes with the requested language and the `shortName` does not.

### Cinemeta — LIVE

```json
{ "name": "Inception",
  "genre":  ["Action", "Adventure", "Sci-Fi"],
  "genres": ["Action", "Adventure", "Sci-Fi"] }
```

Both keys, same values, on every response. `genre` is the legacy key; `genres` is the current one. A consumer reading the wrong one still works today, which is exactly how such duplication survives.

### Wikidata — LIVE

Not a field. `P136` statements point at genre items, and each is a Q-id with a label resolved separately:

```
Q130232  drama film
Q188473  action film
```

---

## 3. The dedicated endpoints, and what they return

### TMDB — LIVE

```
GET /3/genre/movie/list?language=en-US
```

```json
{"genres":[{"id":28,"name":"Action"},{"id":12,"name":"Adventure"},
           {"id":16,"name":"Animation"},{"id":35,"name":"Comedy"},
           {"id":80,"name":"Crime"},{"id":99,"name":"Documentary"},
           {"id":18,"name":"Drama"},{"id":10751,"name":"Family"},
           {"id":14,"name":"Fantasy"},{"id":36,"name":"History"},
           {"id":27,"name":"Horror"},{"id":10402,"name":"Music"},
           {"id":9648,"name":"Mystery"},{"id":10749,"name":"Romance"},
           {"id":878,"name":"Science Fiction"},{"id":10770,"name":"TV Movie"},
           {"id":53,"name":"Thriller"},{"id":10752,"name":"War"},
           {"id":37,"name":"Western"}]}
```

Wrapped in a `genres` object rather than returned as a bare array. Nineteen rows, ids non-contiguous and unsorted, so they are opaque keys and nothing may be inferred from their order or range.

### Trakt — SPEC

```
GET /genres/movies
GET /genres/movies?extended=subgenres
```

```json
[ {"name": "Action", "slug": "action"},
  {"name": "Adventure", "slug": "adventure"} ]
```

With `?extended=subgenres` each row gains a nested array:

```json
[ { "name": "Action", "slug": "action",
    "subgenres": [ {"name": "Action Comedy", "slug": "action-comedy"},
                   {"name": "Heist", "slug": "heist"},
                   {"name": "Revenge", "slug": "revenge"},
                   {"name": "Martial Arts", "slug": "martial-arts"},
                   {"name": "Buddy Cop", "slug": "buddy-cop"},
                   {"name": "Spy Thriller", "slug": "spy-thriller"},
                   {"name": "Vigilante", "slug": "vigilante"},
                   {"name": "Assassin", "slug": "assassin"},
                   {"name": "Chase", "slug": "chase"} ] } ]
```

The only hierarchical genre model in this survey, and the blueprint marks subgenres as a technical preview. `/genres/{type}` also serves `shows`, whose vocabulary differs.

### TheTVDB — SPEC

```
GET /genres        ->  { "data": [ {id, name, slug} ], "status": "..." }
GET /genres/{id}   ->  { "data": {id, name, slug}, "status": "..." }
```

Note the envelope. The outer `status` reports call success, not anything about the genre. Row count needs a key to read.

### Watchmode — DOC

```
GET /v1/genres
```

```json
[ {"id": 4,  "name": "Comedy",      "tmdb_id": 35},
  {"id": 6,  "name": "Documentary", "tmdb_id": 99},
  {"id": 33, "name": "Anime",       "tmdb_id": 0} ]
```

The `tmdb_id` field is the only cross-source genre mapping published as data anywhere in this survey. `0` marks a genre TMDB has no equivalent for, which is how `Anime` is represented. Every other pairing of sources requires a mapping table you write and maintain yourself.

### Streaming Availability — DOC

```
GET /genres?output_language=en
```

Returns 21 rows of `{id: string, name: string}`. The values were not read, because the endpoint needs a RapidAPI key.

### MDBList — SPEC

```
GET /genres?anime=0   ->  [ {id, name} ]
GET /genres?anime=1   ->  the anime-specific visibility set
```

One path, two vocabularies, selected by a query flag. The catalogue filter then takes genre **names**, not the ids this endpoint returns.

### Cinemeta — LIVE

No endpoint. The vocabulary is declared inside the addon manifest, as the options of a catalogue's `genre` extra:

```
GET /manifest.json
catalogs[0].extra[0] = { "name": "genre", "options": [
  "Action","Adventure","Animation","Biography","Comedy","Crime","Documentary",
  "Drama","Family","Fantasy","History","Horror","Mystery","Romance","Sci-Fi",
  "Sport","Thriller","War","Western" ] }
```

Nineteen options. Meta objects return the full IMDb 28, so `Short`, `Music`, `Musical`, `Film-Noir`, `News`, `Reality-TV`, `Talk-Show`, `Game-Show` and `Adult` appear on items that no catalogue filter can select. A live fetch of `tt1254207` returns `["Animation", "Short", "Comedy"]`, and `Short` is not in the list above.

### JustWatch — LIVE

Root field on the GraphQL schema rather than a REST path:

```graphql
{ genres { shortName translation(language: "en") } }
```

```
war:War & Military      eur:Made in Europe     cmy:Comedy
fml:Kids & Family       spt:Sport              ani:Animation
crm:Crime               fnt:Fantasy            msc:Music & Musical
trl:Mystery & Thriller  act:Action & Adventure doc:Documentary
drm:Drama               hst:History            hrr:Horror
scf:Science-Fiction     wsn:Western            rly:Reality TV
rma:Romance
```

Nineteen rows. `language` is a required argument on `translation`, so there is no untranslated form of a genre name; the `shortName` is the only stable key.

### Wikidata — LIVE

No endpoint. The equivalent is a query:

```sparql
SELECT (COUNT(DISTINCT ?g) AS ?c) WHERE { ?g wdt:P31/wdt:P279* wd:Q201658 . }
```

which returns **693**. Genres form an open ontology, so `?g wdt:P279* wd:Q130232` walks every subgenre of drama transitively. Expressive far past any flat list, and unusable as a chip row.

---

## 4. TMDB has two genre vocabularies, not one

`/genre/movie/list` and `/genre/tv/list` are different sets, and they share an id space. Both were fetched live.

- Movie: **19** genres. TV: **16**. Union across both: **27** distinct ids.
- **8 ids appear in both**, with identical names: `16 Animation`, `18 Drama`, `35 Comedy`, `37 Western`, `80 Crime`, `99 Documentary`, `9648 Mystery`, `10751 Family`. No id carries a different name in the two lists.
- **11 are movie-only:** `12 Adventure`, `14 Fantasy`, `27 Horror`, `28 Action`, `36 History`, `53 Thriller`, `878 Science Fiction`, `10402 Music`, `10749 Romance`, `10752 War`, `10770 TV Movie`.
- **8 are TV-only:** `10759 Action & Adventure`, `10762 Kids`, `10763 News`, `10764 Reality`, `10765 Sci-Fi & Fantasy`, `10766 Soap`, `10767 Talk`, `10768 War & Politics`.

The TV list merges what the movie list splits. `10759 Action & Adventure` covers movie ids `28` and `12`; `10765 Sci-Fi & Fantasy` covers `878` and `14`; `10768 War & Politics` covers `10752`. TMDB's TV vocabulary is shaped like JustWatch's, in merged buckets, while its movie vocabulary is not.

Caching one genre table keyed by id and using it for both media types silently produces wrong names for TV, because eleven movie ids are simply absent from the TV list. The join table has to be per media type.

---

## 5. Localisation

`/genre/movie/list?language=nl-NL`, live:

```
28:Actie, 12:Avontuur, 16:Animatie, 35:Komedie,
80:Misdaad, 99:Documentaire, 18:Drama, 10751:Familie
```

Ids stable, names translated. The same holds for Streaming Availability via `output_language`, and for JustWatch via the required `language` argument on `translation`. TheTVDB carries `nameTranslations` on records rather than on the genre list.

The rule that follows: **never persist a genre name as a key.** Names are display strings that change with locale and with the vendor's own editorial decisions. The id or slug is the key. For OMDb, IMDb, Cinemeta and Simkl, where the name *is* the key, there is no localised form at all, so the app owns any translation.

---

## 6. Multi-genre filtering, where the real trap lives

Four sources let you filter by several genres at once, and they disagree about what that means. The delimiter that means AND on one is the delimiter that means OR on another.

### TMDB — LIVE, measured

`/3/discover/movie`, `total_results` for each query, taken within one minute of each other:

| Query | `total_results` |
|---|---|
| `with_genres=28` (Action) | 49,673 |
| `with_genres=878` (Science Fiction) | 27,022 |
| `with_genres=28,878` (comma) | 5,615 |
| `with_genres=28%7C878` (pipe) | 71,087 |

Comma is AND, pipe is OR. The union check holds: 49,673 + 27,022 − 5,615 = 71,080, against a measured 71,087, a drift of 7 titles between calls.

### Trakt — SPEC

From the blueprint's filter documentation: `?genres=action,adventure` matches action **OR** adventure. Comma is OR, the opposite of TMDB. There is no AND form.

### Streaming Availability — SPEC

An explicit parameter rather than a delimiter convention:

```ts
genres?: Array<string>;
genresRelation?: 'and' | 'or';
```

Confirmed from the generated client. The caller states the semantics instead of inferring them.

### MDBList — SPEC

```
?genre=action&genre=comedy&genre_mode=or
```

Repeated parameters rather than a delimited list, with `genre_mode` accepting `and` or `or`, defaulting to **or**. Note the filter takes genre *names* while `/genres` hands back `{id, name}`.

### Cinemeta — LIVE

One genre per request. The catalogue path is `/catalog/movie/top/genre=Action.json`, and there is no multi-genre form.

### Watchmode — DOC

`genres=4,7`, comma delimited by ids. The documentation does not state whether that is AND or OR, and it was not measured because the endpoint needs a key.

**Why this matters for AMRO.** The brief's D1 chose multi-select with OR semantics, applied client-side over the fetched hundred, and defended it on the grounds that a median of three genres per film makes AND return almost nothing. That reasoning is confirmed here from the other direction: TMDB's own server-side AND cuts Action and Science Fiction from 49,673 and 27,022 down to 5,615, a 92% reduction against the smaller of the two sets. The decision is right. What changes if filtering ever moves server-side is that the semantics become a per-source parameter, and on Trakt the naive comma form silently gives OR where TMDB gives AND.

---

## 7. Vocabularies side by side

Counts are measured where the source is LIVE, read from the contract where SPEC, and left blank where a key is needed.

| Source | Count | Key | Case | Hierarchy | Localised names |
|---|---|---|---|---|---|
| TMDB movie | 19 | int | Title Case | flat | yes |
| TMDB TV | 16 | int, shared id space | Title Case | flat | yes |
| IMDb / OMDb | 28 | name | `Sci-Fi`, `Film-Noir` | flat | no |
| Cinemeta items | 28 | name | as IMDb | flat | no |
| Cinemeta filter | 19 | name | as IMDb | flat | no |
| Trakt movie | 34 | slug | lowercase hyphen | **two levels** | no |
| Simkl movie | 20 | name in body, slug in path | both | flat | no |
| JustWatch | 19 | 3-char code | code plus translation | flat | yes, mandatory |
| Streaming Availability | 21 | string id | id plus name | flat | yes |
| Watchmode | | int, plus `tmdb_id` | id plus name | flat | no |
| TheTVDB | | int plus slug | id, name, slug | flat | via record translations |
| MDBList | two sets | int, filter by name | | flat | no |
| Wikidata | 693 | Q-item | label | **transitive subclassing** | yes, any language |

**Simkl movie vocabulary, in full**, from its documented path values: `action, adventure, animation, comedy, crime, documentary, drama, erotica, family, fantasy, history, horror, music, mystery, romance, science-fiction, thriller, tv-movie, war, western`. It is the only vocabulary here carrying `erotica`, and one of two carrying `tv-movie` as a genre rather than a type.

---

## 8. Gotchas, collected

- **TMDB list items carry ids, detail carries objects.** Two shapes for one field on one API.
- **TMDB movie and TV genre lists share an id space but not their contents.** One cached table cannot serve both.
- **Comma means AND on TMDB and OR on Trakt.** Measured on one, documented on the other.
- **Cinemeta's filter vocabulary is a subset of its item vocabulary.** Items carry genres no catalogue query can select.
- **Cinemeta returns both `genre` and `genres` with identical values.** Legacy and current key, side by side.
- **Simkl casing differs between response body and request path.** `Science Fiction` in the object, `science-fiction` in the URL.
- **Simkl fails silently on a bad genre slug:** the top-level response is `null`, not `[]`, and not a 404.
- **Streaming Availability genre ids are strings.** They will not compare equal to TMDB's integers without a cast.
- **MDBList's `/genres` returns ids, but its catalogue filter takes names.**
- **TheTVDB's response envelope has its own `status` field.** Unrelated to the genre payload.
- **OMDb writes `"N/A"`, not null, when a genre is missing.**
- **Trakt genres appear only with `?extended=full`.** The base response omits them entirely.
- **JustWatch's `translation` is required to be localised.** There is no neutral English name, only the `shortName` code.

---

## 9. What this means for AMRO

The MVP filters client-side over TMDB `genre_ids` (D6), multi-select with OR semantics (D1). Nothing in this survey argues against either. Two things do follow from it.

**The genre join belongs at the source boundary, not in the repository.** Open question Q1 asks where the id-to-name join happens. Framed as "join TMDB ids to TMDB names" it looks like a repository detail. Framed as "resolve a source's genre representation into the app's own genre type" it is a boundary responsibility, and it is the same amount of code today. The difference shows up the moment a second source arrives, because the twelve representations catalogued above are integers, strings, slugs, three-character codes, objects with three fields, and Q-items in an open ontology. Only Watchmode ships a mapping to TMDB; every other pairing is a table someone writes.

**Genre names are display strings, not keys.** They are localised on TMDB, Streaming Availability and JustWatch, and they differ in case between Simkl's body and its own URLs. A `Genre` type keyed by an app-owned identifier, with the display name resolved for the current locale, costs nothing now and is the only version that survives a Dutch build or a second source.

The genre list itself is small, stable and cacheable: 19 rows, ids that have not moved, and a payload measured in hundreds of bytes. It is a good candidate for a long-lived cache with a much lazier refresh than the 24-hour detail window in D17, and for a bundled fallback so the filter sheet is never empty on a cold, offline start.

---

## Appendix: reproducing this

```bash
# TMDB, both vocabularies and the id overlap
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/genre/movie/list?language=en-US"
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/genre/tv/list?language=en-US"

# TMDB, names localise and ids do not
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/genre/movie/list?language=nl-NL"

# TMDB, comma is AND and pipe is OR
for q in 28 878 "28,878" "28%7C878"; do
  curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
    "https://api.themoviedb.org/3/discover/movie?with_genres=$q" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["total_results"])'
done

# JustWatch, 19 merged buckets
curl -s -X POST https://apis.justwatch.com/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{genres{shortName translation(language:\"en\")}}"}'

# Cinemeta, filter vocabulary against item vocabulary
curl -s https://v3-cinemeta.strem.io/manifest.json
curl -s https://v3-cinemeta.strem.io/meta/movie/tt1254207.json   # returns "Short"

# IMDb, the 28-value vocabulary measured from the data itself
curl -s -r 0-6000000 "https://datasets.imdbws.com/title.basics.tsv.gz" \
  | gzip -dc 2>/dev/null \
  | awk -F'\t' 'NR>1{n=split($9,a,","); for(i=1;i<=n;i++) if(a[i]!="\\N") g[a[i]]++} \
                END{for(k in g) print g[k]"\t"k}' | sort -rn

# Wikidata, 693 film-genre items
curl -s -G https://query.wikidata.org/sparql -H 'Accept: application/sparql-results+json' \
  -H 'User-Agent: AMRO-research/1.0' --data-urlencode \
  'query=SELECT (COUNT(DISTINCT ?g) AS ?c) WHERE { ?g wdt:P31/wdt:P279* wd:Q201658 . }'

# Contracts carrying the genre schemas and filter semantics
curl -s https://thetvdb.github.io/v4-api/swagger.yml            # GenreBaseRecord, /genres
curl -s https://api.mdblist.com/schema/                         # /genres, genre_mode
curl -s https://jsapi.apiary.io/apis/trakt.apib                 # /genres/movies, subgenres, filters
curl -s https://api.simkl.org/api-reference/simkl/get-movies-genres.md
curl -s https://raw.githubusercontent.com/movieofthenight/ts-streaming-availability/main/src/models/Genre.ts
curl -s https://raw.githubusercontent.com/movieofthenight/ts-streaming-availability/main/src/apis/ShowsApi.ts
```
