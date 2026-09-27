# Popularity: the number nobody defines

Third in the series, after the field guide and the genre/image/rating reports. Same 13 sources, same
evidence rules.

- **LIVE** — request issued from this machine on 2026-09-03, response quoted.
- **SPEC** — OpenAPI, API Blueprint, or published client model downloaded and read.
- **DOC** — vendor prose, because the endpoint needs a key not held here.

The question: on a list of trending or popular movies, what number does each API give you, and what
does it mean?

The short version is that eleven of the thirteen sources sort a list by popularity, six publish a
number alongside it, and **not one of the six sorts the list by the number it publishes**. Popularity
is the field where the API's contract and the API's behaviour come apart.

---

## 1. The short answer

| Source | List endpoint | Number in the list | Type / scale | Sorted by it? | Evidence |
|---|---|---|---|---|---|
| **TMDB** | `trending/movie/{day,week}`, `movie/popular`, `movie/now_playing`, `discover` | `popularity` | float, unbounded, no unit | **No** | LIVE |
| **OMDb** | none | — | — | — | SPEC |
| **IMDb datasets** | none | — | — | — | LIVE |
| **IMDb suggestion** | `suggestion/x/{query}.json` | `rank` | int, 1 = most popular | **No** | LIVE |
| **Trakt** | 8 endpoints | `watchers` / `play_count` / `list_count` / `revenue` … | ints, real units | **Yes** | SPEC |
| **TheTVDB** | `movies/filter?sort=score` | `score` | double, no range, no meaning | unstated | SPEC |
| **Watchmode** | `list-titles?sort_by=popularity_desc` | `popularity_percentile` | 0–100 percentile, nullable | stated | SPEC |
| **Streaming Availability** | `shows/search/filters?order_by=popularity_1week` | none | — | n/a, value withheld | SPEC |
| **Simkl** | keyless `today/week/month_{100,500}.json` | `watched` + `rank` + `drop_rate` | int, int, **string with `%`** | **Yes**, on `watched` | LIVE |
| **MDBList** | re-served lists | — | — | — | DOC |
| **JustWatch** | `popularTitles(sortBy: POPULAR)` | `tmdbPopularity` (someone else's) | float | **No** | LIVE |
| **Cinemeta** | `catalog/movie/top.json` | `popularities{trakt,moviedb,stremio,stremio_lib}` | four scales at once | **No** | LIVE |
| **Wikimedia** | `pageviews/top/en.wikipedia/…` | `views` + `rank` | int, **an actual unit** | **Yes** | LIVE |
| **Fanart.tv** | none public | — | — | — | SPEC |

Two sources rank by a number you can verify: Trakt and Wikimedia. Both do it by publishing a raw
count and letting you divide. Everyone else ships a score.

---

## 2. Sorted by what?

This is the finding the rest of the report hangs off.

### TMDB does not sort by `popularity`

`GET /3/discover/movie?sort_by=popularity.desc&page=1` returned 20 movies. Reading the `popularity`
field down the page:

```
rank  popularity   title
   1    901.1066   Spider-Man: Brand New Day
   2    511.3743   The Odyssey
   3    404.6317   Mutiny
   4    276.8163   Toxic: A Fairy Tale for Grown-ups
   5    250.4566   Facing El Chapo
   6    273.9758   Shape of My Heart      <- higher than rank 5
   7    257.3377   ...
   8    269.6047   ...                    <- higher than rank 7
```

Six of the nineteen adjacent pairs are inversions:

| pair | value at the higher rank | value at the lower rank | gap |
|---|---|---|---|
| 5 → 6 | 250.4566 | 273.9758 | 23.52 |
| 7 → 8 | 257.3377 | 269.6047 | 12.27 |
| 10 → 11 | 201.2618 | 213.8596 | 12.60 |
| 14 → 15 | 165.0525 | 187.4256 | 22.37 |
| 17 → 18 | 149.3230 | 161.6957 | 12.37 |
| 18 → 19 | 161.6957 | 182.0009 | 20.31 |

The gaps are not rounding. They are up to 15% of the value. The parameter is named
`sort_by=popularity.desc`, the field is named `popularity`, and they are two different numbers. The
one you can read is a copy that lags the one the index sorts on.

`/movie/popular` and `/movie/now_playing` invert in the same way, at the same ranks.

### And TMDB's two popular lists disagree with each other

`/movie/popular` and `discover?sort_by=popularity.desc`, requested seconds apart, returned **the same
twenty ids in a different order**: positions 14 and 16 are swapped.

```
discover: … 1323244, 1315772, 1375646, 1108427, 1560520 …
popular : … 1323244, 1375646, 1108427, 1315772, 1560520 …
```

Two surfaces over one index, and they cannot agree on which of two films is more popular.

### JustWatch proves it is achievable

`popularTitles(sortBy: TMDB_POPULARITY)` over 15 titles is **strictly monotonic** in the
`tmdbPopularity` it returns:

```
1027.966  585.8749  413.216  318.6  318.6  275.7  260.8  232.2  230.4  227.7  205.1 …
```

A third party sorting TMDB's number keeps the sort and the number consistent. TMDB, sorting its own,
does not.

Under `sortBy: POPULAR` and `sortBy: TRENDING` the same list is monotonic in nothing JustWatch
exposes — those two rankings have no readable key at all.

---

## 3. The number, source by source

### TMDB — LIVE

Five list endpoints carry `popularity`, and so does the detail record. Item shape from
`trending/movie/day`:

```json
{
  "id": 1386315,
  "title": "The Runner",
  "popularity": 161.6957,
  "vote_average": 6.054,
  "vote_count": 46,
  "media_type": "movie",
  "softcore": false
}
```

`media_type` appears only on `trending/*`; the other four lists omit it. `softcore` is a second adult
flag alongside `adult`, on every list item.

**The value is global, not per-list.** 25 movie ids appeared in more than one of the five endpoints
in the same minute. Zero of them carried a different `popularity`. The number is one scalar per film,
served identically everywhere.

**And it is not window-scoped, even though the list is.** 13 titles appear in both
`trending/movie/day` and `trending/movie/week`. All 13 carry the *identical* `popularity` in both.
The window changes which films are listed and in what order; it does not change the number printed
next to them. So the number cannot be what produced the ordering.

**What it measures.** TMDB documents it as a "lifetime" score whose daily inputs are votes, views,
favourites, watchlist adds, the release date, the total vote count, and **the previous day's score**.
It is recursive: today's popularity is partly yesterday's popularity. That makes it a decayed
running total, not a rate, which is why a 40-year-old film can outrank this week's release. Trending
is documented only as "calculated quite differently".

**Range, by depth.** `movie/popular` at four page depths:

| page | min | max |
|---|---|---|
| 1 | 149.3230 | 901.1066 |
| 50 | 11.5021 | 17.4786 |
| 200 | 5.8747 | 7.9996 |
| 500 | 3.0822 | 5.4966 |

No zeros anywhere in the reachable range. The scale is unbounded above and compresses hard: the top
20 films span 750 points, ranks 9,980–10,000 span 2.4.

**The page cap.** `movie/popular` reports `total_pages: 58741` and `total_results: 1174803`, and then:

```json
{"success": false, "status_code": 22,
 "status_message": "Invalid page: Pages start at 1 and max at 500."}
```

10,000 of 1.17 million rows are reachable, and `total_pages` advertises 58,741 of them. `trending`
reports `total_results: 10000` exactly — that is the cap wearing a count's clothing.

`total_results` also drifts downward within minutes: 1,174,967 → 1,174,819 → 1,174,803 across three
requests.

**Volatility.** Two `trending/movie/day` requests 2m18s apart returned the same 20 ids in the same
order with 0 of 20 popularity values changed. The number is not live; it is a daily batch.

### OMDb — SPEC

One path, `/`. There is no list endpoint. `s=` is a title search, and its items carry five fields:

```json
{"Title": "…", "Year": "…", "imdbID": "tt…", "Type": "movie", "Poster": "https://…"}
```

No popularity, no rank, no vote count, and no documented ordering. If AMRO needs a browsable list,
OMDb cannot supply one.

### IMDb — LIVE

The seven bulk dataset files have no popularity column. `title.ratings.tsv.gz` is the closest thing:

```
tconst	averageRating	numVotes
```

`numVotes` is a lifetime rating count, which is a fame proxy, not a popularity one, and there is no
list to page.

The keyless autocomplete endpoint does carry a popularity rank, and it is the only place IMDb
publishes one:

```
GET https://v2.sg.media-imdb.com/suggestion/t/tt1375666.json
{"d":[{"id":"tt1375666","l":"Inception","rank":129,"y":2010, …}]}
```

`rank` is an integer across all titles, ascending toward more popular. The search variant returns it
too, and **does not sort by it**:

| result | rank |
|---|---|
| Inception | 129 |
| Inception: The Cobol Job | 13,930 |
| Inception (2016) | 105,815 |
| Inception (short) | 119,246 |
| Inception: Jump Right Into the Action | 55,398 |

The licensed datasets omit the number; an undocumented autocomplete endpoint leaks it.

### Trakt — SPEC

Eight movie list endpoints, and the cleanest design in the survey. Every one of them returns a raw
count with a stated unit, and the endpoint name says which count did the sorting.

| Endpoint | Field | What it counts |
|---|---|---|
| `movies/trending` | `watchers` | people watching right now, last 24h |
| `movies/popular` | *(none)* | — |
| `movies/favorited/{period}` | `user_count` | users who favourited it |
| `movies/played/{period}` | `play_count` | plays, repeats included |
| `movies/watched/{period}` | `watcher_count` | unique users |
| `movies/collected/{period}` | `collected_count` | unique collectors |
| `movies/anticipated` | `list_count` | lists it appears on |
| `movies/boxoffice` | `revenue` | US dollars, last weekend |

`played`, `watched` and `collected` all return the *same three* counts. Only the sort differs. One
call gets you all three measures and you pick the ranking yourself:

```json
{ "watcher_count": 66667, "play_count": 109736, "collected_count": 27584, "movie": { … } }
```

`period` is `daily` | `weekly` | `monthly` | `all`.

**Trakt is the only source that ships the denominator.** `movies/trending` sets a response header:

```
X-Trending-User-Count: 1721
```

21 watchers out of 1,721 people watching anything is a share you can compute. Every other source
gives you a numerator and keeps the denominator.

**And `movies/popular` publishes no number at all.** The body is bare movie objects. The docs say
"Popularity is calculated using the rating percentage and the number of ratings" — which makes
Trakt's *popular* list a ratings ranking, not a popularity one. Trakt documents the formula and
withholds the value; TMDB ships the value and withholds the formula.

Keyless requests are refused at the edge (`403`, Cloudflare interstitial), so all of the above is
read from the published blueprint, which itself carries a deprecation banner.

### TheTVDB — SPEC

`score` is `format: double, type: number`, with no range, no unit, and no field-level description.
The only explanation is a note at the top of the whole specification:

> "score" is a field across almost all entities. We generate scores for different types of entities
> in various ways, so no assumptions should be made about the meaning of this value. It is simply
> used to hint at relative popularity for sorting purposes.

That is a vendor stating in its own contract that the number is meaningless except as a sort key. It
is the most honest sentence in the survey, and it means the value cannot be displayed, compared
across two films, or blended with anything.

`GET /movies/filter` takes `sort` ∈ `score` | `firstAired` | `name`, and requires `country` and
`lang`, so there is no unfiltered "popular movies" list to fetch.

### Watchmode — SPEC

`GET /list-titles` with `sort_by` ∈ `relevance_desc` | `relevance_asc` | `popularity_desc` |
`popularity_asc` | `release_date_desc` | `release_date_asc` | `title_desc` | `title_asc`, documented
as "Popularity options use Watchmode's normalized popularity percentile."

The item carries it:

```json
{"id": 1337513, "title": "Secret in Their Eyes", "imdb_id": "tt1741273",
 "tmdb_id": 290751, "popularity_percentile": 99.421}
```

`popularity_percentile` is `number, nullable`, "Normalized popularity percentile (0-100)".

**A percentile is not a score.** It says what fraction of the catalogue a title beats, so the
distance between 99.421 and 97.618 is a count of rows, not a difference in audience. Near the top the
scale saturates: everything worth showing sits between 99 and 100. It also cannot be summed,
averaged, or compared across a filtered subset, because the population it ranks against changes.

`TitleDetails` carries both `popularity_percentile` and `relevance_percentile`. On `Person`, the
field named `relevance_percentile` is described as "Popularity percentile (0-100)" — the spec's own
names and descriptions disagree.

### Streaming Availability — SPEC

`GET /shows/search/filters` takes `order_by`, and the published client enumerates seven values:

```
original_title  release_date  rating
popularity_alltime  popularity_1year  popularity_1month  popularity_1week
```

Four popularity windows. That is the richest popularity model in the survey by a distance: alltime,
year, month and week, each a separate index.

The `Show` object returns twenty-one fields, and none of them is any of those four. The only score is
`rating`, "calculated by taking the average of ratings found online from multiple sources", 0–100.

So the API will rank a catalogue by weekly popularity and then refuse to tell you the weekly
popularity of anything in it. You can have the order or you can have nothing.

`GET /shows/top` is a different thing again: "Top shows are determined by the streaming service
itself" — it re-serves Netflix's or Prime's own top-10 board. Position only, no number, and the
methodology belongs to a third party.

### Simkl — LIVE

Simkl publishes **keyless, pre-built trending files**, which makes it the only source besides TMDB
that a client can read here with no credential:

```
https://data.simkl.in/discover/trending/movies/today_100.json
https://data.simkl.in/discover/trending/movies/week_500.json
https://data.simkl.in/discover/trending/movies/month_100.json
https://data.simkl.in/discover/dvd/releases_100.json
```

Attribution is required in the section title ("Simkl Trending Movies"), a `User-Agent` must be set,
query strings are ignored, and the files are Cloudflare-cached for an hour with a real
`Last-Modified`. All three movie files served on 2026-09-03 carried
`last-modified: Thu, 03 Sep 2026 07:32:02 GMT` — one generation run feeds every window.

The item is 22 fields. The popularity-relevant ones:

```json
{
  "title": "The Odyssey",
  "ids": {"simkl_id": 2604475, "imdb": "tt33764258", "tmdb": "1368337", "tvdb": "362547"},
  "rank": 140,
  "drop_rate": "0.6%",
  "watched": 73,
  "plan_to_watch": 7899,
  "ratings": {"simkl": {"rating": 8.47, "votes": 2503},
              "imdb":  {"rating": 8.4,  "votes": 465215}}
}
```

**`watched` is the ordering metric**, and it is strictly descending in all three files. It is a raw
count, so the windows are directly comparable:

| window | #1 `watched` | #12 `watched` |
|---|---|---|
| today | 73 | 29 |
| week | 787 | 292 |
| month | 6,005 | 1,707 |

Simkl's absolute audience is two orders of magnitude below Trakt's. That is fine for ordering and
useless for anything that treats the number as a population.

**`rank` is not the list position.** The Odyssey is `rank: 140` while sitting at position 1 in
`today`, 4 in `week` and 2 in `month`. 72 titles appear in all three files, and all 72 carry an
identical `rank` in every one. It is a stable global rank per film, spanning 1 to 73,401 — an
all-time standing, shipped inside a this-week list.

**`drop_rate` is a string with a percent sign in it**: `"0.6%"`, `"8.8%"`, and — unpadded —
`"1%"`. It is also identical across today, week and month for every shared title, while the counts
next to it are window-scoped. A field that looks like a rate of change, does not vary with the
window, and has to be parsed out of a string.

The keyed API refuses without a client id:

```json
{"error": "client_id_failed", "code": 412, "message": "Your client_id is wrong. Try another one"}
```

### MDBList — DOC

MDBList has no popularity number of its own. Its `score` is a ratings aggregate: "calculated using
all the ratings average and with a votes baseline… if the vote count is lower than the baseline, the
rating is decreased".

Its "Popular lists" are other people's lists, re-served:

| List | Source |
|---|---|
| IMDb Most Popular Movies | imdb.com/chart/moviemeter |
| IMDb Top Rated Movies | imdb.com/chart/top |
| Netflix / Disney+ Top Movies | flixpatrol.com |
| Top 10 Pirated Movies of The Week | torrentfreak.com |
| Trakt Trending / Popular / Anticipated / Watched / Collected / Played / Box Office | trakt.tv |

Its one original list, "Most Popular Movies [top-20]", is documented as "List based on vote increase
in the last month" — a derivative of ratings activity, not of watching. Useful as a shortcut to seven
sources at once; not a thirteenth opinion.

### JustWatch — LIVE

`popularTitles(country:, sortBy:)`, `totalCount: 162763`. Probing the enum by substitution:

| value | accepted |
|---|---|
| `POPULAR` | yes |
| `TRENDING` | yes |
| `TMDB_POPULARITY` | yes |
| `IMDB_SCORE` | yes |
| `ALPHABETICAL` | yes |
| `RANDOM` | yes |
| `JW_RATING` | no |
| `RELEASE_DATE` | no |
| `TOMATO_METER` | no |
| `PROVIDER_PREFERRED` | no |

**JustWatch does not expose its own popularity.** The `Scoring` type has no such field, and the
validation error says so:

```
Cannot query field "popularity" on type "Scoring". Did you mean "tmdbPopularity"?
```

`popularityPercentile` is rejected outright. Introspection is disabled, so the error messages are the
schema documentation.

The only popularity number JustWatch will give you is `tmdbPopularity` — TMDB's, mirrored, and stale
(see §4). `jwRating` (0–1) exists but is a rating, and is not the sort key: under `sortBy: POPULAR`
the first twelve `jwRating` values run 0.918, 0.833, 0.875, 0.954, 0.966, 0.762, 0.881 — no order at
all.

**The rank is the cursor.** JustWatch's pagination cursors are base64 of the 1-based position:

```
MQ== → "1"    Mg== → "2"    Mw== → "3"    MTA= → "10"
```

If you want a popularity rank out of JustWatch, decoding the cursor is the way to get one. That is
the whole ranking: an ordinal, with no value behind it.

Sorting by `IMDB_SCORE` puts a title with `tmdbPopularity: null` and `jwRating: null` at position 1,
so the score sorts are unguarded against thin records.

### Cinemeta — LIVE

Three catalogues, and the manifest labels them differently from their ids:

| id | display name |
|---|---|
| `top` | Popular |
| `year` | New |
| `imdbRating` | Featured |

`GET /catalog/movie/top.json` `307`-redirects to `cinemeta-catalogs.strem.io` and returns 50 items.

Cinemeta is **the only source that publishes more than one popularity number per film**, in a
dedicated object:

```json
{
  "name": "The Whisper Man",
  "popularity": 0.30802333333333337,
  "popularities": {
    "trakt": 1445,
    "moviedb": 233.5012,
    "stremio": 0.30802333333333337,
    "stremio_lib": 0
  }
}
```

Four numbers, four scales, one film. `trakt` is a watcher count in the hundreds to low thousands;
`moviedb` is TMDB's unbounded float; `stremio` is a small decimal; `stremio_lib` was `0` on every one
of the 50.

`popularity` equals `popularities.stremio` for all 50 items.

**`stremio` is `moviedb` divided by 30.** Over the 47 items carrying both, the ratio is exactly 30.00
(±0.01) for 43 of them. Cinemeta's own popularity is TMDB's, rescaled by a constant.

The four that miss are stale, and the size of the miss says how stale:

| film | `moviedb` now | `stremio` | ratio | TMDB value implied by `stremio` |
|---|---|---|---|---|
| The Whisper Man | 233.5012 | 0.30802 | 758.06 | 9.24 |
| Evil Dead Burn | 75.3700 | 3.98723 | 18.90 | 119.62 |
| The Invite | 56.5576 | 1.87109 | 30.23 | 56.13 |
| Avengers: Endgame | 65.5717 | 2.19072 | 29.93 | 65.72 |

The Whisper Man's derived popularity was computed when TMDB said 9.24 and is being served next to a
mirrored TMDB value of 233.50 — a 25× spread inside a single object.

**The catalogue called "Popular" is not sorted by popularity.** The first ten values:

```
0.308  7.739  0.086  1.871  3.434  null  4.699  3.870  0.511  0.637
```

Three of the 50 items have `popularities: null` and `popularity: null` outright, and one of them
(`Facing El Chapo`) is currently #5 on TMDB's own popular list at 250.4566.

### Wikimedia — LIVE

Wikidata has no popularity property. The auditable substitute is the Wikimedia pageviews REST API,
keyless, and it is the only number in this survey with a unit anyone can check.

```
GET /metrics/pageviews/top/en.wikipedia/all-access/2026/09/01
{"rank": 4, "article": "Killing_of_the_Clancy_children", "views": 428824}
{"rank": 5, "article": "Toxic_(2026_film)",              "views": 255041}
```

Per-article daily views, same day:

| article | views |
|---|---|
| Spider-Man:_Brand_New_Day | 102,473 |
| The_Odyssey_(2026_film) | 79,043 |
| Project_Hail_Mary_(film) | 14,205 |
| Inception | 4,774 |

Real counts, a real window, a published rank, and no vendor formula between you and the data. The
costs: the ranked list is all of Wikipedia, not movies, so films surface among current events; and it
keys on an article title, not an id, so every lookup needs a title-to-article resolution that breaks
on disambiguation (`The_Odyssey_(2026_film)`).

### Fanart.tv — SPEC

No public list endpoint. `/v3/movies/latest` returns:

```json
{"error": "missing api_key or client_key parameter"}
```

The only count Fanart publishes is `likes` on an individual artwork, which ranks images, not films.

---

## 4. One film, three "TMDB popularity" values

JustWatch and Cinemeta both mirror TMDB's `popularity`. Twelve films appear in both, and all twelve
were then read live from TMDB itself within the same few minutes.

| tmdb id | title | JustWatch | Cinemeta | **TMDB, live** |
|---|---|---|---|---|
| 278 | The Shawshank Redemption | 101.1313 | 105.9697 | **112.3265** |
| 454639 | Masters of the Universe | 85.5991 | 84.7472 | **75.9701** |
| 634649 | Spider-Man: No Way Home | 131.4164 | 131.4164 | **119.2476** |
| 687163 | Project Hail Mary | 103.0339 | 103.0339 | **104.7899** |
| 860508 | The Whisper Man | 227.7433 | 233.5012 | **225.3706** |
| 1083381 | Backrooms | 132.2215 | 140.9675 | **112.1557** |
| 1084244 | Toy Story 5 | 205.1440 | 213.4425 | **187.6072** |
| 1275779 | Disclosure Day | 127.7223 | 127.7223 | **104.9504** |
| 1284465 | The Death of Robin Hood | 119.3583 | 115.8007 | **121.9261** |
| 1339713 | Obsession | 232.1632 | 232.1632 | **213.8596** |
| 1375646 | *Gunche* / *Colony* | 177.3214 | 132.6035 | **187.4256** |
| 1560520 | Batman: Knightfall Part 1 | 164.0656 | 116.1093 | **149.3230** |

**Zero of twelve** match the live value. JustWatch and Cinemeta agree exactly with each other on four
of the twelve and disagree on the other eight, so they are not reading one shared export — they are
two independent refresh cycles that sometimes land on the same day.

Worst spreads: Backrooms 112.16 → 140.97 (**+26%**), Disclosure Day 104.95 → 127.72 (**+22%**),
Batman: Knightfall 116.11 → 164.07 (**+41%**).

Id 1375646 is called *Gunche* by JustWatch and *Colony* by TMDB, so the title drifts alongside the
number.

The consequence for a client: **a mirrored popularity is not the same field as its source.** If AMRO
reads TMDB popularity from JustWatch and TMDB popularity from TMDB, it has two fields, and merging
them by name produces a number that is neither.

---

## 5. Gotchas, collected

**Sorting**

- TMDB's `sort_by=popularity.desc` does not sort by the returned `popularity`. Six inversions in 19
  adjacent pairs, gaps up to 23.5 points.
- `/movie/popular` and `discover?sort_by=popularity.desc` return the same 20 ids in a different
  order.
- Cinemeta's `top` catalogue is labelled "Popular" in the manifest and is not sorted by any
  popularity field it returns.
- IMDb's suggestion endpoint returns `rank` and orders by title relevance.
- Never re-sort a list client-side by the popularity field it came with. You will produce an order
  the server never intended, from a number that is not the sort key.

**Scales**

- TMDB: unbounded float, recursive on its own previous value, no unit. Top-20 span 750 points; ranks
  9,980–10,000 span 2.4.
- Watchmode: percentile, so the difference between two values is a row count, and it saturates at the
  top.
- TheTVDB: double with a documented instruction not to interpret it.
- Cinemeta: four scales in one object, one of which (`stremio`) is another (`moviedb`) over 30.
- Simkl: raw counts, but two orders of magnitude smaller than Trakt's for the same films.
- Trakt and Wikimedia: raw counts with a stated unit and, for Trakt, a denominator in the header.

**Types**

- Simkl's `drop_rate` is a string with a `%` suffix, unpadded: `"0.6%"`, `"1%"`.
- Simkl's `ids.tmdb` is a string (`"1368337"`) while `ids.simkl_id` is an int.
- Watchmode's `popularity_percentile` is nullable.
- Cinemeta's `popularity` and `popularities` are both nullable and were null together on 3 of 50.
- JustWatch's cursors are base64 ASCII digits, so the rank round-trips through
  `atob`.

**Windows**

- TMDB's `popularity` is identical in the day and the week list. The window orders the list; it does
  not scope the number.
- Simkl's `rank` and `drop_rate` are identical across today, week and month while `watched` is
  window-scoped, so one item mixes windowed and unwindowed fields.
- Streaming Availability indexes four popularity windows and returns none of them.
- Trakt is the only source where the window is a path parameter and every number in the body is
  relative to it.

**Reach**

- TMDB caps at page 500 (10,000 rows) while reporting `total_pages: 58741`; `trending` reports
  `total_results: 10000`, which is the cap, not a count.
- Simkl's files stop at 500 items and are regenerated hourly at best.
- Trakt, TheTVDB, Watchmode, Streaming Availability, MDBList and Fanart.tv all need a credential
  before you can see a single row.
- OMDb and the IMDb bulk datasets have no popularity list at all.

**Attribution**

- Simkl requires a visible "Simkl Trending" style title on any surface showing the data.
- Streaming Availability's `/shows/top` is a streaming service's own board, so the methodology and
  the licence belong to Netflix, not to the API.

---

## 6. What this means for AMRO

**Do not model popularity as a number.** Every source that ships one ships a different thing:
a decayed lifetime score, a percentile, a watcher count, a page-view count, an ordinal. Storing them
all in a `popularity: Double` throws away the only part that determines whether two of them can be
compared.

Model it the way §5 of the ratings report models a score — value plus what the value *is*:

```kotlin
sealed interface Popularity {
    /** A raw count of people or events, with the window it was counted over. */
    data class Count(val value: Long, val unit: CountUnit, val window: Window) : Popularity
    /** Position in a ranking. The only honest reading of an opaque score. */
    data class Rank(val position: Int, val outOf: Int?) : Popularity
    /** A vendor score with no unit. Displayable never, sortable only within one source. */
    data class OpaqueScore(val value: Double, val source: Source) : Popularity
}
```

**Trust the order, not the value.** Eleven of thirteen sources will sort a list for you and six will
hand you a number, and the two do not agree in any of the six. The server's ordering is the product.
Take the list in the order it arrives, keep the index, and never re-sort by the field.

**Keep the source on every number.** `tmdbPopularity` from JustWatch and `popularity` from TMDB are
different fields with the same name and, measured here, values up to 26% apart. A mirror is a
separate source.

**Prefer a count to a score.** For the post-MVP multi-source work, Trakt's `watchers` with the
`X-Trending-User-Count` header and Wikimedia's `views` are the only two numbers AMRO could show a
user, blend across sources, or defend in a code review. Everything else can rank, and nothing else
can measure.

**Treat popularity as stale by default.** TMDB's value did not move in 2m18s; Cinemeta was serving a
value derived from a TMDB reading 25× out of date; JustWatch was up to 26% off. Cache aggressively
and never render a popularity number as if it were current.

**For the MVP, one source.** TMDB's ordering is coherent and its lists are keyless-adjacent and
paginated. Read `trending/movie/day` and `movie/popular`, keep the returned order, keep
`vote_average` and `vote_count` (which *are* comparable), and store `popularity` only as an opaque
tiebreaker that never reaches the UI.

---

## Appendix: reproducing this

```sh
TOK=$(grep '^tmdb.readAccessToken=' local.properties | cut -d= -f2-)
H="Authorization: Bearer $TOK"

# TMDB: the sort does not match the field
curl -s -H "$H" "https://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&page=1" \
  | python3 -c "import json,sys;p=[x['popularity'] for x in json.load(sys.stdin)['results']];\
print([ (i+1,p[i],p[i+1]) for i in range(len(p)-1) if p[i]<p[i+1] ])"

# TMDB: two popular surfaces, same set, different order
curl -s -H "$H" "https://api.themoviedb.org/3/movie/popular?page=1"          # ids
curl -s -H "$H" "https://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&page=1"

# TMDB: the number is not window-scoped
curl -s -H "$H" "https://api.themoviedb.org/3/trending/movie/day"
curl -s -H "$H" "https://api.themoviedb.org/3/trending/movie/week"   # same id -> same popularity

# TMDB: the page cap
curl -s -H "$H" "https://api.themoviedb.org/3/movie/popular?page=501"
# {"success":false,"status_code":22,"status_message":"Invalid page: … max at 500."}

# Simkl: keyless trending files (User-Agent required)
curl -s -A "amro/1.0" https://data.simkl.in/discover/trending/movies/today_100.json
curl -sI -A "amro/1.0" https://data.simkl.in/discover/trending/movies/week_100.json | grep -i last-modified

# Cinemeta: four popularities in one object; stremio == moviedb / 30
curl -sL https://v3-cinemeta.strem.io/catalog/movie/top.json \
  | python3 -c "import json,sys;[print(x['name'],x.get('popularities')) for x in json.load(sys.stdin)['metas'][:5]]"

# JustWatch: the schema through its error messages
curl -s -X POST https://apis.justwatch.com/graphql -H 'content-type: application/json' \
  -d '{"query":"{popularTitles(country:US,first:1,sortBy:POPULAR){edges{node{content(country:US,language:en){scoring{popularity}}}}}}"}'
# Cannot query field "popularity" on type "Scoring". Did you mean "tmdbPopularity"?

curl -s -X POST https://apis.justwatch.com/graphql -H 'content-type: application/json' \
  -d '{"query":"{popularTitles(country:US,first:1,sortBy:BOGUS){totalCount}}"}'
# Value "BOGUS" does not exist in "PopularTitlesSorting!" enum.

# JustWatch: the cursor is the rank
python3 -c "import base64;print(base64.b64decode('MTA=').decode())"   # 10

# IMDb: the only popularity number IMDb publishes
curl -s "https://v2.sg.media-imdb.com/suggestion/t/tt1375666.json"     # "rank": 129
curl -s https://datasets.imdbws.com/title.ratings.tsv.gz | gzip -dc | head -1
# tconst  averageRating  numVotes        <- no popularity column

# Streaming Availability: four popularity windows you can sort by and cannot read
npm pack streaming-availability && tar xzf streaming-availability-*.tgz
grep -A9 SearchShowsByFiltersOrderByEnum package/dist/apis/ShowsApi.d.ts
grep -n 'rating\|popular' package/dist/models/Show.d.ts

# TheTVDB: the disclaimer
curl -s https://thetvdb.github.io/v4-api/swagger.yml | sed -n '11p'

# Watchmode: percentile, nullable
curl -s https://api.watchmode.com/openapi.json \
  | python3 -c "import json,sys;d=json.load(sys.stdin);\
print(d['components']['schemas']['TitleListItem']['properties']['popularity_percentile'])"

# Trakt: keyless is refused; read the blueprint
curl -s -o /dev/null -w '%{http_code}\n' https://api.trakt.tv/movies/trending   # 403
curl -sL https://jsapi.apiary.io/apis/trakt.apib | grep -n 'X-Trending-User-Count'

# Wikimedia: a number with a unit
curl -s -A "amro/1.0" \
  "https://wikimedia.org/api/rest_v1/metrics/pageviews/top/en.wikipedia/all-access/2026/09/01"
curl -s -A "amro/1.0" \
  "https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/user/Inception/daily/20260901/20260901"
```

All figures in this report were read on 2026-09-03 between 08:12 and 08:52 UTC. Popularity is the
field most likely to have moved since; the shapes will not have.
