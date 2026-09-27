# Images, ratings, and the IMDb id across movie APIs

**Date:** 2026-09-03
**Companion to:** [`movie-data-apis.md`](movie-data-apis.md), the twelve-source field survey, and [`genre-models.md`](genre-models.md), which covers the genre field in depth. This document covers three fields: how an image reference is shaped and what it takes to turn it into a loadable URL, what a rating means and on what scale, and whether the detail endpoint hands you an IMDb id.

Evidence tags match the companion reports. **LIVE** means a request was issued from this machine on 2026-09-03 and the response is quoted. **SPEC** means the source's OpenAPI document, API Blueprint, or published client model was downloaded and read. **DOC** means vendor prose, because the endpoint needs a key this project does not hold.

---

## 1. The short answer

| Source | Image reference | URL is | Rating field | Scale | IMDb id on detail | Evidence |
|---|---|---|---|---|---|---|
| **TMDB** | `poster_path`, `backdrop_path` | path fragment, needs base + size | `vote_average` + `vote_count` | 0–10 float | `imdb_id`, always present in 20/20 | LIVE |
| **OMDb** | `Poster` | absolute URL, `_V1_` transform slot | `imdbRating`, `imdbVotes`, `Metascore`, `Ratings[]` | every value is a **string** | `imdbID` | SPEC |
| **IMDb datasets** | none, no image column in any of 7 files | | `averageRating` + `numVotes` | 0–10, one decimal | `tconst` is the id | LIVE |
| **Trakt** | `images.poster[]` etc., only with `?extended=images` | **scheme-less** host+path, WebP | `rating` + `votes` + `distribution` | 0–10 float | `ids.imdb` | SPEC |
| **TheTVDB** | `image`, `artworks[].image`, `.thumbnail` | string, spec names no base; it is `artworks.thetvdb.com/banners/` | `score` on the record, `score` on each artwork | undocumented | `remoteIds[]`, typed by int | SPEC |
| **Watchmode** | `poster`, `posterMedium`, `posterLarge`, `backdrop` | absolute URL, **third-party host, unlicensed** | `user_rating` **and** `critic_score` | 0–10 **and** 0–100 | `imdb_id`, nullable | SPEC |
| **Streaming Availability** | `imageSet` → poster/backdrop × vertical/horizontal × 5 widths | absolute URL, CloudFront-signed, expiring | `rating` | 0–100, provenance opaque | `imdbId`, non-optional | LIVE |
| **Simkl** | `poster` | path fragment, needs domain + prefix + **suffix** + extension | `simkl.rating`, plus per-source sub-objects | 0–10 float | key casing is inconsistent | SPEC |
| **MDBList** | `poster` behind `append_to_response` | absolute URL | `ratings[]`, schema declares it untyped | mixed, see sort enum | `imdb_id` | SPEC |
| **JustWatch** | `posterUrl`, `backdrops[].backdropUrl` | **template** with `{profile}` and `{format}` | `scoring{}`, 8 fields from 4 providers | 0–10, 0–100, 0–1 | `externalIds.imdbId` | LIVE |
| **Cinemeta** | `poster`, `background`, `logo` | absolute URL, size in path | `imdbRating` | **string**, no vote count | `imdb_id` and `id` both | LIVE |
| **Wikidata** | `P18` | Commons filename via `Special:FilePath` | `P444` statements | **denominator baked into the string** | `P345` | LIVE |
| **Fanart.tv** | `movieposter[].url` etc. | absolute URL, **`http://`** | none | | top-level `imdb_id` | SPEC |

Two things fall out immediately. Every source hands you an IMDb id on the detail record, which makes `tt…` the only join key in the survey that always exists. And no two sources agree on what a rating is: the word covers a user average, a critic aggregate, a percentile, a like count, and a string with its own denominator inside it.

---

## 2. Image URLs, and every built variant

Each source below shows the value the API returns, then the full URL assembled from it, one row per variant the source offers. Every URL was requested from this machine on 2026-09-03; the byte count is what came back. Where a variant fails, the failure is the row.

### TMDB — LIVE

The movie object carries a bare path. Turning it into a URL takes the base and a size from `/configuration`.

```jsonc
// GET /3/movie/27205
{ "poster_path":   "/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg",
  "backdrop_path": "/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg" }
```

```jsonc
// GET /3/configuration  ->  images
{ "secure_base_url": "https://image.tmdb.org/t/p/",
  "poster_sizes":   ["w92","w154","w185","w342","w500","w780","original"],
  "backdrop_sizes": ["w300","w780","w1280","original"],
  "logo_sizes":     ["w45","w92","w154","w185","w300","w500","original"],
  "profile_sizes":  ["w45","w185","h632","original"],
  "still_sizes":    ["w92","w185","w300","original"] }
```

The URL is `secure_base_url + size + path`. All eighteen movie-facing variants, built and requested:

| Kind | Size | Built URL | Result |
|---|---|---|---|
| poster | `w92` | `https://image.tmdb.org/t/p/w92/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 5,212 B |
| poster | `w154` | `https://image.tmdb.org/t/p/w154/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 12,276 B |
| poster | `w185` | `https://image.tmdb.org/t/p/w185/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 17,062 B |
| poster | `w342` | `https://image.tmdb.org/t/p/w342/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 53,908 B |
| poster | `w500` | `https://image.tmdb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 107,732 B |
| poster | `w780` | `https://image.tmdb.org/t/p/w780/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 235,590 B |
| poster | `original` | `https://image.tmdb.org/t/p/original/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | 200, 984,791 B |
| poster | `w999` | `https://image.tmdb.org/t/p/w999/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg` | **400** |
| backdrop | `w300` | `https://image.tmdb.org/t/p/w300/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg` | 200, 18,449 B |
| backdrop | `w780` | `https://image.tmdb.org/t/p/w780/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg` | 200, 107,287 B |
| backdrop | `w1280` | `https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg` | 200, 211,450 B |
| backdrop | `original` | `https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg` | 200, 1,356,334 B |
| logo | `w45` | `https://image.tmdb.org/t/p/w45/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 735 B |
| logo | `w92` | `https://image.tmdb.org/t/p/w92/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 1,851 B |
| logo | `w154` | `https://image.tmdb.org/t/p/w154/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 3,341 B |
| logo | `w185` | `https://image.tmdb.org/t/p/w185/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 4,274 B |
| logo | `w300` | `https://image.tmdb.org/t/p/w300/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 6,685 B |
| logo | `w500` | `https://image.tmdb.org/t/p/w500/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 11,396 B |
| logo | `original` | `https://image.tmdb.org/t/p/original/iXYh7y0vI1DZR0taTuvrr8PSnOd.png` | 200, 28,512 B |

An unlisted width is a hard `400`, not a fallback, and the legal set arrives from the API rather than a constant. The CDN takes no `Authorization` header; every row above ran unauthenticated. Note the jump from `w780` to `original`: 236 KB to 985 KB for the poster, 211 KB to 1.36 MB for the backdrop. `original` is never the right default.

`GET /3/movie/{id}/images` returns the full artwork set as objects, and the shape carries more than a path:

```jsonc
// GET /3/movie/27205/images  ->  posters[0]
{ "file_path": "/tXQvtRWfkUUnWJAn2tN3jERIUG.jpg",
  "aspect_ratio": 0.667, "width": 2000, "height": 3000,
  "iso_639_1": "es", "iso_3166_1": "ES",
  "vote_average": 8.362, "vote_count": 6 }
```

Inception returned 227 posters, 99 backdrops, and 50 logos. Three measurements from that set matter for layout:

- **Poster aspect ratio is not fixed.** It ranges 0.657 to 0.75, so a grid that assumes 2:3 (0.667) will letterbox or crop a real fraction of the catalogue.
- **Logo aspect ratio ranges 2.386 to 9.364.** No fixed box works. A logo overlay has to size from the returned `aspect_ratio`.
- **Backdrops are effectively uniform**, 1.777 to 1.784.

`iso_639_1` is `null` for textless art: 74 of the 99 backdrops, 12 of the 227 posters. That null is the value you want for a language-neutral backdrop, so it cannot be filtered out as missing data.

Images carry their own `vote_average` and `vote_count`, a second rating system inside the image object that has nothing to do with the film's rating. 85 of 227 posters have zero votes.

### OMDb — LIVE and DOC

`Poster` is an absolute URL on Amazon's media host, typed `string`, with no size parameter documented. The segment between `._V1_` and `.jpg` is a transform slot rather than part of the filename. The base URL below came from IMDb's public suggestion endpoint, which needs no key; the transforms were then built and requested.

```jsonc
// GET https://v3.sg.media-imdb.com/suggestion/x/inception.json  ->  d[0]
{ "id": "tt1375666", "l": "Inception",
  "i": { "imageUrl": "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_.jpg",
         "width": 700, "height": 1037 } }
```

| Transform | Built URL | Result |
|---|---|---|
| none | `…MjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_.jpg` | 200, 419,890 B |
| `SX300` | `…Mw@@._V1_SX300.jpg` | 200, 36,590 B |
| `SX600` | `…Mw@@._V1_SX600.jpg` | 200, 114,135 B |
| `UX128` | `…Mw@@._V1_UX128.jpg` | 200, 9,030 B |
| `UY268_CR3,0,182,268_` | `…Mw@@._V1_UY268_CR3,0,182,268_.jpg` | 200, 16,549 B |
| `QL75_UX380_` | `…Mw@@._V1_QL75_UX380_.jpg` | 200, 44,723 B |

Full prefix for every row: `https://m.media-amazon.com/images/M/MV5B`.

Unlike TMDB, the slot is open: width (`SX`/`UX`), height (`UY`), crop (`CR`) and JPEG quality (`QL`) all compose, and none of it is published as an allowed value. OMDb itself documents no size parameter at all, and its resizing service is a separate host, `http://img.omdbapi.com/?apikey=[yourkey]&`, which the docs say "is only available to patrons", currently "over 280,000 posters, updated daily with resolutions up to 2000x3000".

All OMDb content is licensed **CC BY-NC 4.0**, stated in both the site footer and the `info.license` block of its OpenAPI document. Non-commercial.

### IMDb datasets — LIVE

There is no image. The bulk export publishes seven files, and none of them has an image column:

```
name.basics   title.akas    title.basics   title.crew
title.episode title.principals            title.ratings
```

```
title.basics   tconst  titleType  primaryTitle  originalTitle  isAdult  startYear  endYear  runtimeMinutes  genres
title.ratings  tconst  averageRating  numVotes
title.akas     titleId  ordering  title  region  language  types  attributes  isOriginalTitle
title.crew     tconst  directors  writers
```

Header rows read from the live `.tsv.gz` files. Any product built on the datasets gets its artwork elsewhere — which is exactly what Cinemeta's `tt`-addressed CDN provides for free, and what the public suggestion endpoint above provides for a single title.

### Trakt — SPEC and LIVE

Images are opt-in: append `?extended=images`. The returned object groups by type, each an array, and the blueprint notes only one URL is returned per type "for now. This is just future proofing."

```jsonc
"images": {
  "fanart":   ["walter-r2.trakt.tv/images/movies/000/012/601/fanarts/medium/5aab754f58.jpg.webp"],
  "poster":   ["walter-r2.trakt.tv/images/movies/000/012/601/posters/thumb/e0d9dd35c5.jpg.webp"],
  "logo":     ["walter-r2.trakt.tv/images/movies/000/012/601/logos/medium/dbce70b4aa.png.webp"],
  "clearart": ["…/cleararts/medium/513a3688d1.png.webp"],
  "banner":   ["…/banners/medium/71dc0c3258.jpg.webp"],
  "thumb":    ["…/thumbs/medium/fcd7d7968c.jpg.webp"]
}
```

**The scheme is missing**: "You'll also need to prepend the `https://` prefix to all image URLs." With it prepended, every type resolves:

| Kind, size | Built URL | Result |
|---|---|---|
| poster, `thumb` | `https://walter-r2.trakt.tv/images/movies/000/012/601/posters/thumb/e0d9dd35c5.jpg.webp` | 200, 12,792 B |
| poster, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/posters/medium/e0d9dd35c5.jpg.webp` | 200, 34,206 B |
| poster, `full` | `https://walter-r2.trakt.tv/images/movies/000/012/601/posters/full/e0d9dd35c5.jpg.webp` | 200, 74,388 B |
| poster, `original` | `https://walter-r2.trakt.tv/images/movies/000/012/601/posters/original/e0d9dd35c5.jpg.webp` | **404** |
| fanart, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/fanarts/medium/5aab754f58.jpg.webp` | 200, 67,676 B |
| logo, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/logos/medium/dbce70b4aa.png.webp` | 200, 24,690 B |
| clearart, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/cleararts/medium/513a3688d1.png.webp` | 200, 46,802 B |
| banner, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/banners/medium/71dc0c3258.jpg.webp` | 200, 6,754 B |
| thumb, `medium` | `https://walter-r2.trakt.tv/images/movies/000/012/601/thumbs/medium/fcd7d7968c.jpg.webp` | 200, 21,240 B |

The size sits inside the returned path and the API hands you exactly one of them. Substituting the token works — `thumb`, `medium` and `full` all resolve, `original` is a `404` — but that set is documented nowhere, so a client wanting a bigger poster than the one it was given is guessing. Everything is served as WebP regardless of the `.jpg` or `.png` in the path.

The missing scheme is a real failure, not a curiosity. `curl` accepts `walter-r2.trakt.tv/…` and silently assumes a scheme, which is why the value looks usable from a shell. An Android image loader does not: `HttpUrl.parse` and `Uri.parse` treat a scheme-less string as a relative reference, so the load fails or resolves against the wrong base.

The blueprint also forbids hotlinking outright: "**Please cache all images!** All images are required to be cached in your app or server and not loaded directly from our CDN. Hotlinking images is not allowed and will be blocked." That is a server-side requirement, not something a mobile client can satisfy on its own.

Trakt's own documentation then points elsewhere for artwork, and its cross-service chart lists TMDB, TheTVDB, Fanart.tv and OMDb as the places to get it. For `movies` it marks TVDB as having no poster and no fanart at all.

### TheTVDB — SPEC and LIVE

`MovieBaseRecord.image` is `type: string`. `ArtworkBaseRecord` carries `image` and `thumbnail`, both `type: string`, alongside `width`, `height`, `language`, `includesText`, and a `type` integer that indexes into `/artwork/types`.

The OpenAPI document never says whether those strings are absolute URLs or paths, and never names a base host. The base is `https://artworks.thetvdb.com/banners/`, recovered from TheTVDB's own public movie pages, and the thumbnail is the same path with a `_t` suffix.

| Kind | Built URL | Result |
|---|---|---|
| poster | `https://artworks.thetvdb.com/banners/movies/113/posters/601c4ea1292a8.jpg` | 200, 212,662 B |
| poster thumb | `https://artworks.thetvdb.com/banners/movies/113/posters/601c4ea1292a8_t.jpg` | 200, 62,136 B |
| background | `https://artworks.thetvdb.com/banners/movies/113/backgrounds/113.jpg` | 200, 476,831 B |
| bucket root | `https://artworks.thetvdb.com/` | **403**, XML |

Two sizes only, full and `_t`. The host is an object store that refuses a listing, so the path vocabulary is not discoverable: you get whatever string the API hands you. Dimensions for a given artwork type are one indirection away, on `/artwork/types`, which returns `ArtworkType` rows with `width`, `height`, `thumbWidth`, `thumbHeight`, `imageFormat`, `recordType` and `slug`.

### Watchmode — SPEC

Four image fields on `TitleDetails`, all `format: uri`, all nullable:

```
poster  posterMedium  posterLarge  backdrop
```

Note the casing. `poster` and `backdrop` are lowercase while `posterMedium` and `posterLarge` are camelCase, sitting in an object where every other key is snake_case (`imdb_id`, `user_rating`, `plot_overview`). Three conventions in one object.

No URL was built, because the vendor publishes no real one. The example value in the OpenAPI document is a placeholder, identically, for all four image fields and for `AutocompleteResult.image_url`:

```jsonc
"poster":       "http://www.some-poster-site.com/image.jpg"
"posterMedium": "http://www.some-poster-site.com/image.jpg"
"posterLarge":  "http://www.some-poster-site.com/image.jpg"
"backdrop":     "http://www.some-poster-site.com/image.jpg"
```

There is no host to name because there is no single host. Each field's `description` says: "May point to a third-party host. Watchmode does not grant any license to the underlying image." The API description expands it:

> Some image-related fields may contain URLs hosted by third parties or publicly accessible sources. These URLs are provided as references only. Watchmode does not grant any license to the underlying images and does not guarantee ownership, copyright status, availability, or permitted use.

So the URL may work and may still not be yours to display. Availability is explicitly not guaranteed.

The list endpoint carries no images at all. `TitleListItem` is `id, title, year, imdb_id, tmdb_id, tmdb_type, type, popularity_percentile`. Only `AutocompleteResult` has an `image_url`. A grid built from a list response needs a detail call per row.

### Streaming Availability — LIVE

The richest image model in the survey: poster and backdrop, each in both orientations, each at five widths, all as finished absolute URLs.

```ts
interface ShowImageSet {
  verticalPoster:    VerticalImage;    // required
  horizontalPoster:  HorizontalImage;  // required
  verticalBackdrop?:  VerticalImage;
  horizontalBackdrop?: HorizontalImage;
}
interface VerticalImage   { w240: string; w360: string; w480: string; w600: string; w720: string }
interface HorizontalImage { w360: string; w480: string; w720: string; w1080: string; w1440: string }
```

The vendor publishes a keyless example response, so all twenty were requested:

```
GET https://www.movieofthenight.com/v4/examples/shows/the-dark-knight
```

| Field | Widths | Bytes returned |
|---|---|---|
| `verticalPoster` | `w240 w360 w480 w600 w720` | 16,430 / 32,575 / 52,029 / 78,383 / 108,593 |
| `horizontalPoster` | `w360 w480 w720 w1080 w1440` | 11,542 / 17,459 / 34,667 / 71,863 / 128,867 |
| `verticalBackdrop` | `w240 w360 w480 w600 w720` | 13,978 / 28,428 / 46,691 / 71,586 / 98,106 |
| `horizontalBackdrop` | `w360 w480 w720 w1080 w1440` | 14,387 / 23,071 / 47,022 / 99,238 / 171,451 |

Twenty out of twenty returned `200`. The underlying path is regular, with a language segment on posters and none on backdrops:

```
https://cdn.movieofthenight.com/show/68/poster/vertical/en/240.jpg
https://cdn.movieofthenight.com/show/68/backdrop/horizontal/360.jpg
```

But that path is not the URL. Every one is a CloudFront **signed** URL:

```
https://cdn.movieofthenight.com/show/68/poster/vertical/en/240.jpg
  ?Expires=1816054628                     # 2027-07-20T03:37:08Z
  &Signature=QFnfKAkupj3KS02QVXRgHFeP4uBpXr7w9~WnCqSVaAkbGTU78BWZ3n…
  &Key-Pair-Id=KK4HN3OO4AT5R
```

466 characters. Strip the query and the same path returns **403**. Two consequences for a client that persists API responses: a stored URL goes dead on its own schedule regardless of your cache policy, and twenty of them is roughly 9 KB of signature per title in the database. Cache the bytes, not the link.

Two more shape details from that response. `tmdbId` is `"movie/155"` — a type-prefixed string, not the bare integer every other source uses — and `id` is the string `"68"`.

### Simkl — LIVE

The movie object gives a fragment, and the fragment is not a path:

```jsonc
{ "title": "King Arthur: Legend of the Sword",
  "poster": "74/74415673dcdc9cdd" }
```

Building a URL takes four more pieces, documented as `{domain}{prefix}{image_url}{suffix}{extension}`:

| Prefix, suffix | Documented size | Built URL | Result |
|---|---|---|---|
| posters `_w` | 600×338, cropped landscape | `https://simkl.in/posters/74/74415673dcdc9cdd_w.webp` | 200, 33,436 B |
| posters `_m` | 340×* | `https://simkl.in/posters/74/74415673dcdc9cdd_m.webp` | 200, 39,994 B |
| posters `_ca` | 190×279(285) | `https://simkl.in/posters/74/74415673dcdc9cdd_ca.webp` | 200, 17,182 B |
| posters `_c` | 170×250(256) | `https://simkl.in/posters/74/74415673dcdc9cdd_c.webp` | 200, 14,712 B |
| posters `_cm` | 84×124 | `https://simkl.in/posters/74/74415673dcdc9cdd_cm.webp` | 200, 4,748 B |
| posters `_s` | 40×57 | `https://simkl.in/posters/74/74415673dcdc9cdd_s.webp` | 200, 1,390 B |
| fanart `_d` | original, darker | `https://simkl.in/fanart/71/710408ec0a1bd207_d.jpg` | 200, 103,192 B |
| fanart `_medium` | 1920×1080 | `https://simkl.in/fanart/71/710408ec0a1bd207_medium.jpg` | 200, 469,144 B |
| fanart `_mobile` | 960×540 | `https://simkl.in/fanart/71/710408ec0a1bd207_mobile.jpg` | 200, 135,668 B |
| fanart `_w` | 600×338 | `https://simkl.in/fanart/71/710408ec0a1bd207_w.jpg` | 200, 64,365 B |
| fanart `_s48` | 48×27 | `https://simkl.in/fanart/71/710408ec0a1bd207_s48.jpg` | 200, 4,415 B |
| via proxy | 190×279, re-encoded | `https://wsrv.nl/?url=https://simkl.in/posters/74/74415673dcdc9cdd_ca.webp` | 200, 17,770 B |

The suffix is the size, and it is a discrete token rather than a width. The tokens are not ordered by size: `_m` comes back larger than `_w`, because `_w` is a cropped landscape crop of a poster. Two of the documented heights are written `279(285)` and `250(256)`, so even the fixed-looking sizes vary. Both `.jpg` and `.webp` resolve on every path.

Simkl asks that you not hit its origin: "Use `https://wsrv.nl/?url=https://simkl.in` domain for images… It is preffered to use https://wsrv.nl/ service to minimise traffic to our servers." The proxied bytes come back slightly larger than the origin's, because it re-encodes.

### MDBList — SPEC

`poster` is an absolute URL, not returned by default. It arrives only via `append_to_response=poster`, alongside `ratings`, `description` and `genres`. The endpoint requires a key this project does not hold, so no URL was built. On the lists endpoint the same parameter means something else: "up to 5 cached list posters".

### JustWatch — LIVE

The URL is a template with two placeholders, returned exactly as written:

```jsonc
// urlV2(fullPath:"/us/movie/inception") -> node.content
{ "posterUrl": "/poster/302449148/{profile}/inception.{format}",
  "backdrops": [{ "backdropUrl": "/backdrop/275196554/{profile}/inception.{format}" }] }
```

Prefix `https://images.justwatch.com`, substitute a profile and a format. The valid profiles are not published, so they were probed:

| Profile | Built URL | `.jpg` | `.webp` |
|---|---|---|---|
| `s66` | `https://images.justwatch.com/poster/302449148/s66/inception.{jpg\|webp}` | **400** | **400** |
| `s166` | `https://images.justwatch.com/poster/302449148/s166/inception.{jpg\|webp}` | 18,677 B | 11,306 B |
| `s276` | `https://images.justwatch.com/poster/302449148/s276/inception.{jpg\|webp}` | 47,584 B | 28,144 B |
| `s332` | `https://images.justwatch.com/poster/302449148/s332/inception.{jpg\|webp}` | 66,514 B | 39,740 B |
| `s592` | `https://images.justwatch.com/poster/302449148/s592/inception.{jpg\|webp}` | 186,358 B | 108,104 B |
| `s718` | `https://images.justwatch.com/poster/302449148/s718/inception.{jpg\|webp}` | 258,739 B | 147,280 B |
| `s999` | `https://images.justwatch.com/poster/302449148/s999/inception.{jpg\|webp}` | **400** | **400** |

The set is closed at both ends and undiscoverable through the API: you learn it by probing. WebP runs about 60% of the JPEG bytes at every size. Poster and backdrop carry different asset ids under the same template.

The API is undocumented and unlicensed for third-party use, which is why the companion report excludes it. Schema introspection is disabled, and the endpoint returns `data: null` for an `__type` query. Field names leak only through validation errors, which is how `tomatoScore` surfaced: `Cannot query field "tomatoUserMeter" on type "Scoring". Did you mean "tomatoMeter" or "tomatoScore"?`

### Cinemeta — LIVE

Absolute URLs, with the size as a path segment rather than a query parameter.

```jsonc
// GET https://v3-cinemeta.strem.io/meta/movie/tt1375666.json -> meta
{ "poster":     "https://images.metahub.space/poster/small/tt1375666/img",
  "background": "https://images.metahub.space/background/medium/tt1375666/img",
  "logo":       "https://images.metahub.space/logo/medium/tt1375666/img" }
```

The path is `/{kind}/{size}/{imdb_id}/img`, so any artwork is addressable from an IMDb id alone with no lookup at all. All twelve combinations:

| Kind | Size | Built URL | Result |
|---|---|---|---|
| poster | `small` | `https://images.metahub.space/poster/small/tt1375666/img` | 200, 41,941 B jpeg |
| poster | `medium` | `https://images.metahub.space/poster/medium/tt1375666/img` | 200, 107,732 B jpeg |
| poster | `large` | `https://images.metahub.space/poster/large/tt1375666/img` | 200, 175,224 B **webp** |
| poster | `original` | `https://images.metahub.space/poster/original/tt1375666/img` | 200, 885,559 B jpeg |
| background | `small` | `https://images.metahub.space/background/small/tt1375666/img` | 200, 22,525 B jpeg |
| background | `medium` | `https://images.metahub.space/background/medium/tt1375666/img` | 200, 695,668 B jpeg |
| background | `large` | `https://images.metahub.space/background/large/tt1375666/img` | 200, 695,668 B jpeg |
| background | `original` | `https://images.metahub.space/background/original/tt1375666/img` | 200, 695,668 B jpeg |
| logo | `small` | `https://images.metahub.space/logo/small/tt1375666/img` | 200, 22,433 B png |
| logo | `medium` | `https://images.metahub.space/logo/medium/tt1375666/img` | 200, 22,433 B png |
| logo | `large` | `https://images.metahub.space/logo/large/tt1375666/img` | 200, 22,433 B png |
| logo | `original` | `https://images.metahub.space/logo/original/tt1375666/img` | 200, 22,433 B png |

Twelve URLs, twelve `200`s, four distinct images. `background` is byte-identical from `medium` up and `logo` is one asset under every token, so nine of the twelve size choices change nothing. The `large` poster comes back as WebP while its neighbours are JPEG, so content type is not predictable from the URL.

### Wikidata — LIVE

`P18` gives a Commons filename resolved through a redirect path, not an image URL:

```
http://commons.wikimedia.org/wiki/Special:FilePath/InceptionCast2July10.jpg
```

| Variant | Built URL | Result |
|---|---|---|
| as returned | `http://commons.wikimedia.org/wiki/Special:FilePath/InceptionCast2July10.jpg` | 200, 239,593 B |
| width 400 | `https://commons.wikimedia.org/wiki/Special:FilePath/InceptionCast2July10.jpg?width=400` | 200, 36,022 B |
| width 1200 | `https://commons.wikimedia.org/wiki/Special:FilePath/InceptionCast2July10.jpg?width=1200` | 200, 239,593 B |

The value is `http://`, and it is a redirect rather than the file. `?width=` gives arbitrary resizing, the one open size parameter in the survey, but it caps at the original and never upscales: `1200` returned the original bytes exactly.

The real problem is upstream of all that. `P18` means "an image of the subject", and for Inception the value is a photograph of the cast at a press event, not a poster. Posters are usually copyrighted and therefore usually absent.

### Fanart.tv — LIVE

Artwork-only, no metadata. Every type is an array of objects with an absolute URL:

```jsonc
"movieposter": [
  { "id": "57317",
    "url": "http://assets.fanart.tv/fanart/movies/120/movieposter/the-lord-of-the-rings-the-fellowship-of-the-ring-528aa45a8633a.jpg",
    "lang": "en", "likes": "4" } ]
```

Thumbnails come from a host-path swap rather than a parameter: "replace fanart with preview in the assets link".

| Variant | Built URL | Result |
|---|---|---|
| poster, as returned | `http://assets.fanart.tv/fanart/movies/120/movieposter/…-528aa45a8633a.jpg` | 200, 1,007,246 B |
| poster, preview | `http://assets.fanart.tv/preview/movies/120/movieposter/…-528aa45a8633a.jpg` | 200, 15,694 B |
| hdmovielogo, over TLS | `https://assets.fanart.tv/fanart/movies/120/hdmovielogo/…-4f78564165f48.png` | 200, 57,802 B |

Two sizes, full and preview, and the preview is 64× smaller — 15 KB against 1 MB, which makes the full URL unusable as a list thumbnail. The host serves TLS correctly even though every URL in the response is `http://`, so rewriting the scheme is safe and necessary.

`id` and `likes` are **strings**, not integers, so `likes` sorts lexicographically unless you parse it: `"7"` sorts after `"51"`.

The documented demo key returns `401 invalid API key`, so the response shape above is from the published blueprint; the URLs were built from its sample values and requested live.

## 3. Ratings

### The word covers five different things

Before the per-source detail, the shapes:

| Shape | Sources |
|---|---|
| User average 0–10 plus a vote count | TMDB, IMDb datasets, Trakt, Simkl, JustWatch (`imdbScore`/`tmdbScore`) |
| Critic aggregate 0–100 | Watchmode `critic_score`, OMDb `Metascore`, JustWatch `tomatoMeter`, Streaming Availability `rating` |
| Normalised 0–1 | JustWatch `jwRating` |
| Percentile rank | Watchmode `relevance_percentile`, `popularity_percentile` |
| Popularity, not quality | TMDB `popularity`, Cinemeta `popularities`, MDBList `imdbpopular`/`tmdbpopular` |
| Community like count | Fanart.tv `likes`, TMDB per-image `vote_average` |

A field named `rating` tells you nothing about its scale. `user_rating: 9.2` and `critic_score: 85` sit next to each other in one Watchmode object.

### TMDB — LIVE

```jsonc
{ "vote_average": 8.373, "vote_count": 42163, "popularity": 55.6277 }
```

`vote_average` is 0–10 with three decimals. `vote_count` is the denominator and it matters, because **an unrated film is `0.0`, not `null`**. Measured across all 20 titles behind trending page 1:

```
vote_average range: 0.0 to 8.9
titles with vote_count == 0: 1   (id 1423191, "Resident Evil", 0.0 / 0)
```

Sorted by rating without a vote-count filter, that title lands at the bottom as if it were the worst film on the page. It has no rating at all. Any UI that renders a rating has to branch on `vote_count == 0` rather than on the rating value.

`popularity` is a separate, unbounded, undocumented number. It ranged 8.27 to 901.11 across the same 20 titles. It is a trend signal, not a quality one, and it is not on the same axis as anything else in the survey.

The image objects carry their own `vote_average`/`vote_count` for artwork quality, which are unrelated to the film's rating and share the field names exactly.

### OMDb — SPEC

Every field is `type: string`. Not the rating fields specifically: every field in the entire response definition.

```
Poster: string   imdbRating: string   imdbVotes: string   imdbID: string
Metascore: string   tomatoMeter: string   Response: string   Type: string
```

So `imdbRating` arrives as `"8.8"`, `Metascore` as `"74"`, and `imdbVotes` carries thousands separators. Every numeric comparison needs a parse, and a missing value is the literal `"N/A"` rather than `null` or an empty string, which means `parseFloat` on an absent rating yields `NaN` rather than failing loudly.

The mirrored OpenAPI document is from the older API surface and still lists the flat `tomato*` family (`tomatoMeter`, `tomatoRating`, `tomatoUserMeter`, `tomatoFresh`, `tomatoRotten`, `tomatoConsensus`, `tomatoImage`, `tomatoURL`). Current OMDb returns a `Ratings` array instead. The spec is stale on this point, and the vendor's own `swagger.yaml` link 404s, so there is no authoritative current contract.

The envelope is verified LIVE. A request with no key returns `200` with:

```json
{"Response":"False","Error":"No API key provided."}
```

Failure is in the body, not the status code, and `Response` is the string `"False"`.

### IMDb datasets — LIVE

```
tconst      averageRating  numVotes
tt0000001   5.7            2229
tt0000002   5.4            324
tt0000003   6.4            2377
```

Two columns, one decimal place. Across the first 20,000 data rows the values ranged 1.0 to 9.3, and the decimal digit distribution is flat (`.8` 2140, `.2` 2139, `.6` 2122, `.0` 2092, `.4` 2018, …), confirming a genuine one-decimal quantisation rather than rounding artefacts.

This is the cleanest rating in the survey and it is also the one you cannot use commercially: the datasets are licensed for personal and non-commercial use.

### Trakt — SPEC

`?extended=full` puts the rating inline:

```jsonc
{ "status": "released", "rating": 8.0, "votes": 111, "comment_count": 92 }
```

A dedicated endpoint adds the histogram, which no other source in the survey publishes:

```jsonc
// GET /movies/{id}/ratings
{ "rating": 7.33778, "votes": 7866,
  "distribution": { "1": 298, "2": 46, "3": 87, "4": 178, "5": 446,
                    "6": 1167, "7": 1855, "8": 1543, "9": 662, "10": 1583 } }
```

The keys are the strings `"1"` through `"10"`, so votes are integers on a 1–10 scale and the average is derived. That `"10": 1583` spike next to `"9": 662` is visible in the data: the distribution is bimodal, which is exactly the information an average hides.

### TheTVDB — SPEC

`MovieBaseRecord.score` is `format: double`, with no description, no range, and no vote count anywhere on the record. `ArtworkBaseRecord.score` is `type: number`, also undescribed, and applies to the artwork rather than the film. Two fields with the same name, different subjects, and no documented scale for either.

### Watchmode — SPEC

Two ratings on one object, on two scales:

```jsonc
{ "user_rating": 9.2,      // "User rating (0-10)",   type: number
  "critic_score": 85 }     // "Critic score (0-100)",  type: integer
```

Both nullable. Plus two percentiles that are ranks, not scores: `relevance_percentile` (example 98.92) and `popularity_percentile` (example 99.99), both 0–100 like `critic_score` and meaning something entirely different.

`TitleDetails` also carries three AI-generated text fields, `will_you_like_this`, `review_summary`, and an "AI-enhanced when available" `plot_overview`. Generated prose sitting in the same object as sourced facts, with nothing in the shape to distinguish them.

### Streaming Availability — SPEC

One field, whose doc comment is most of the story:

```ts
/**
 * Rating of the show. This is calculated by taking the average of ratings
 * found online from multiple sources.
 */
rating: number;
```

No scale, no vote count, no list of sources, no weighting. The vendor's own published example returns `89` for a film IMDb rates 9.0, so the axis is 0–100, but nothing in the contract says so. A blended number you cannot decompose, reconcile against any other source, or explain to a user. It is `number` and required, so there is no null to signal "unrated" either.

### Simkl — SPEC

Ratings are nested per provider, one sub-object each:

```jsonc
{ "id": 268470, "type": "movie", "release_status": "released", "rank": 10628,
  "simkl": { "rating": 7.1, "votes": 151, "droprate": "1%" },
  "imdb":  { "rating": 6.6, "votes": 15119 } }
```

Anime rows swap in a third: `"mal": { "rating": 7.6, "votes": 4132, "rank": 1693 }`, where `rank` appears inside the provider object rather than beside it as it does for movies.

**The provider keys are case-inconsistent across the documentation's own examples.** One response uses `"IMDB"`:

```jsonc
{ "id": 10280, "simkl": { "rating": 7.9, "votes": 3378 },
  "rank": { "type": "show", "value": "1071" },
  "droprate": "3.2%",
  "IMDB": { "rating": 8.1, "votes": 895026 } }
```

Another uses `"imdb"`. A parser keyed on either one silently drops the rating half the time. That same example shows two more shape shifts: `rank` is a nested `{type, value}` object here and a bare integer there, and `droprate` moves between the top level and inside `simkl`. `droprate` itself is a percentage **string**, `"3.2%"`.

There is also a `reactions` block, `{total, reviews, comments, positive, negative, neutral}`, which is engagement rather than rating.

### MDBList — SPEC

The OpenAPI document declares:

```yaml
ratings:
  type: array
  items:
    type: object
```

An untyped array of untyped objects. The contract says a `ratings` field exists and nothing more, so the shape has to be learned from live responses that need a key.

What the vocabulary contains is recoverable from the `sort` enum, which is fully typed:

```
imdbpopular  imdbrating  imdbvotes  letterrating  metacritic
released  releasedigital  rtaudience  rtomatoes  score  score_average
title  tmdbpopular
```

Six rating sources (IMDb, Letterboxd, Metacritic, Rotten Tomatoes critic, Rotten Tomatoes audience, MDBList's own), two popularity signals, and two aggregate scores. `score` and `score_average` are documented on the filter parameters as 0–100.

Ratings, like posters, arrive only under `append_to_response=ratings`.

### JustWatch — LIVE

The widest rating object in the survey, eight fields spanning four providers and three scales:

```jsonc
// urlV2(fullPath:"/us/movie/inception") -> node.content.scoring
{ "imdbScore": 8.8,
  "imdbVotes": 2863692.0,
  "tmdbScore": 8.373,
  "tmdbPopularity": 52.4763,
  "tomatoMeter": 86,
  "tomatoScore": null,
  "certifiedFresh": true,
  "jwRating": 0.97085428887 }
```

Three scales in one flat object: 0–10 (`imdbScore`, `tmdbScore`), 0–100 (`tomatoMeter`), 0–1 (`jwRating`). Plus a boolean award (`certifiedFresh`) and an unbounded popularity.

`imdbVotes` is `2863692.0` — a **float for a count**. And the value pairs are not co-located: `imdbScore` has `imdbVotes`, but `tmdbScore` has no vote count at all, so you can weight one and not the other.

The same query against a smaller title returns `tomatoMeter: null, certifiedFresh: null, jwRating: null` with the IMDb and TMDB fields populated, so partial nulls across providers are the normal case.

### Cinemeta — LIVE

```jsonc
{ "imdbRating": "8.8",
  "popularity": 1.8542566666666667,
  "popularities": { "moviedb": 55.6277, "stremio": 1.8542566666666667,
                    "trakt": 85, "stremio_lib": 0 } }
```

`imdbRating` is a **string** with no vote count anywhere in the response. The value also appears a second time, as the `name` of a link:

```jsonc
{ "name": "8.8", "category": "imdb", "url": "https://imdb.com/title/tt1375666" }
```

The same number in two places in two roles, and both are strings.

`popularities` breaks popularity out by source, which is more honest than a single blended number, but the three values are on three unrelated scales: 55.63, 1.85, and 85.

### Wikidata — LIVE

`P444` (review score) is where the modelling problem is clearest. Querying Inception:

```
"74/100"  by Q150248        Metascore
"8.1/10"  by Rotten Tomatoes  Rotten Tomatoes average of rated reviews
"87%"     by Rotten Tomatoes  Tomatometer score
"8.8/10"  by IMDb             weighted average
```

**The scale is inside the string.** `"74/100"`, `"8.1/10"`, `"87%"`, `"8.8/10"` — three different notations across four statements on one film. Parsing means handling a fraction, a percentage, and a `/10` fraction, then normalising. There is no numeric field and no vote count.

The qualifiers do the disambiguating: `P447` (review score by) names the source and `P459` (determination method) names the metric, which is how the two Rotten Tomatoes statements are told apart. And the `P447` values are not uniformly resolvable to labels: one came back as the bare Q-id `Q150248` while the others resolved to names, so a label service is not a guarantee.

### Fanart.tv — SPEC

No ratings. `likes` is a community vote on the artwork, and it is a string.

---

## 4. The IMDb id

### Every detail endpoint carries one

This is the strongest cross-source result in the survey.

| Source | Field | Type | Present on list responses |
|---|---|---|---|
| TMDB | `imdb_id` | string, nullable per docs | **no** |
| OMDb | `imdbID` | string | yes, in `Search[]` |
| IMDb datasets | `tconst` | string, the primary key | n/a |
| Trakt | `ids.imdb` | string | yes, `ids` is on every media object |
| TheTVDB | `remoteIds[]` | `{id, type, sourceName}` | no |
| Watchmode | `imdb_id` | string, nullable | **yes** |
| Streaming Availability | `imdbId` | string, **non-optional** | yes |
| Simkl | `ids.imdb` | string | varies by endpoint |
| MDBList | `imdb_id` and `ids.imdb` | string | yes |
| JustWatch | `externalIds.imdbId` | string, nullable | yes |
| Cinemeta | `imdb_id`, and `id` is the same value | string | yes |
| Wikidata | `P345` | string | n/a |
| Fanart.tv | `imdb_id` | string, top level | n/a |

Measured on TMDB across all 20 titles behind trending page 1:

```
n=20  imdb_present=20  imdb_missing=0
poster_path present: 20   backdrop_path present: 20
```

Twenty out of twenty. The docs type it nullable, and the field is genuinely nullable for unreleased or obscure titles, so a client still has to handle the null. But on real trending data it is always there.

### The list-response gap

TMDB is the one source where the list response drops the IMDb id entirely. Its trending item keys:

```
adult  backdrop_path  genre_ids  id  media_type  original_language
original_title  overview  popularity  poster_path  release_date
softcore  title  video  vote_average  vote_count
```

No `imdb_id`. So on TMDB the IMDb id costs one detail call per title, or a call to `GET /3/movie/{id}/external_ids`, which returns more than IMDb:

```jsonc
{ "id": 27205, "imdb_id": "tt1375666", "wikidata_id": "Q25188",
  "facebook_id": "", "instagram_id": null, "twitter_id": null }
```

Note the two flavours of absent in one object: `""` for Facebook and `null` for Instagram and Twitter. Missing means two different things.

Watchmode goes the other way. `TitleListItem` has no images but does carry `imdb_id`, `tmdb_id` and `tmdb_type`, so its list responses are joinable without a detail call.

### TheTVDB types its ids

`remoteIds` is an array of `{id: string, type: integer, sourceName: string}`. The id is a string, but which service it belongs to is an integer you have to know the meaning of, with `sourceName` as the human-readable companion. Every other source in the survey names the service in the key.

### Cinemeta makes the IMDb id the primary key

`id` and `imdb_id` hold the same `"tt1375666"`. The whole addressing scheme is built on it: the meta endpoint is `/meta/movie/tt1375666.json`, the artwork is `images.metahub.space/poster/small/tt1375666/img`, and `behaviorHints.defaultVideoId` is the same string again. It also returns `moviedb_id: 27205`, so Cinemeta is a free IMDb-to-TMDB bridge that needs no key.

### Watchmode publishes the join as a file

The ID map is a public CSV, fetchable with no key at all:

```
Watchmode ID,IMDB ID,TMDB ID,TMDB Type,Title,Year
"11","tt1699720","55245","movie","!Women Art Revolution","2010"
"111","tt1442481","282634","movie","#73, Shaanthi Nivaasa","2007"
```

Same pattern as the genre cross-walk in the companion report: Watchmode is the only source in the survey that ships its identity mappings as downloadable data rather than as per-title lookups.

---

## 5. Gotchas, collected

**Images**

- TMDB rejects an unlisted size with `400`, and the size list comes from `/configuration`, not from a constant.
- TMDB poster aspect ratios span 0.657–0.75 and logo ratios span 2.386–9.364. Fixed boxes crop real art.
- Trakt image URLs have no scheme. Prepend `https://` or the load fails, and its size tokens (`thumb`, `medium`, `full`; `original` is a 404) are documented nowhere.
- Trakt forbids hotlinking and requires server-side caching, which a client-only app cannot satisfy.
- Watchmode image URLs point at third-party hosts under no licence and with no availability guarantee, and the vendor publishes only a placeholder example.
- Simkl needs domain, prefix, fragment, size suffix, and extension assembled in order; two documented poster heights vary, and `_m` comes back larger than `_w`.
- JustWatch profiles are a closed, unpublished set. `s66` and `s999` both `400`.
- Cinemeta's `large` poster returns WebP while its other sizes return JPEG, and nine of its twelve size choices return a byte-identical image.
- Fanart.tv serves `http://` though the host does support TLS, and types `likes` as a string.
- Wikidata's `P18` is a photograph of the subject, not a poster, and resolves through a redirect.
- IMDb's bulk datasets contain no images in any of their seven files, though the public suggestion endpoint returns an Amazon URL whose `_V1_` segment is an open transform slot.
- Streaming Availability's URLs are CloudFront leases: they carry an `Expires`, run ~466 characters, and return `403` with the query stripped.

**Ratings**

- TMDB's `0.0` means unrated. Branch on `vote_count`, never on the value.
- OMDb returns every field as a string, missing values as `"N/A"`, and vote counts with thousands separators.
- Wikidata bakes the denominator into the value: `"74/100"`, `"8.1/10"`, `"87%"`.
- Simkl's provider keys appear as both `"imdb"` and `"IMDB"` in its own examples, and `rank` is sometimes an integer and sometimes an object.
- JustWatch mixes 0–10, 0–100 and 0–1 in one flat object and gives a vote count to only one provider.
- Watchmode's `user_rating` (0–10) and `critic_score` (0–100) are adjacent keys on different scales, next to two percentiles that are ranks.
- Streaming Availability's `rating` blends unnamed sources and states no scale; its published example returns `89`, so it is 0–100.
- MDBList's OpenAPI declares `ratings` as an untyped array of untyped objects.
- TheTVDB's `score` has no documented range, no vote count, and shares its name with an unrelated artwork score.
- TMDB's per-image `vote_average` and `vote_count` share names with the film's and mean something else.
- `popularity` is not a rating anywhere it appears, and TMDB's ranged 8.27 to 901.11 across one page.

**IMDb ids**

- Present on every detail endpoint in the survey.
- Absent from TMDB list responses, which is the one place it costs an extra call.
- TMDB's `external_ids` returns `""` for one missing id and `null` for another in the same object.
- TheTVDB identifies the source by integer, not by key name.

---

## 6. What this means for AMRO

The brief's post-MVP ambition to collect from multiple sources runs into the same wall in all three fields, and the fix is the same shape as the genre recommendation in the companion report: **resolve at the source boundary into an app-owned type, and never let a source's representation reach the UI.**

**Images.** Model an app-owned image reference that holds a resolved absolute URL per size bucket the UI actually asks for, and give each source's mapper the job of building it. TMDB concatenates base and size, Trakt prepends a scheme, Simkl assembles five pieces, JustWatch substitutes two placeholders, Cinemeta and Streaming Availability pass through. Only the mapper should ever know which. Store the returned `aspect_ratio` where the source gives one, because TMDB's posters are not uniformly 2:3. Treat every source URL as perishable: Streaming Availability's expire by signature, Trakt's must not be hotlinked at all, and Watchmode's are not guaranteed to stay up.

**Ratings.** Model a rating as a value plus a scale plus a vote count plus a provenance, and make all four non-optional in the app type. A bare `Float` cannot distinguish 8.8 from IMDb, 86 from Rotten Tomatoes, and 0.97 from JustWatch, and it cannot express "unrated", which TMDB encodes as `0.0` and Streaming Availability cannot express at all. Sort and filter on the normalised value; display the original scale, because a user who knows a film is 87% on Rotten Tomatoes will not recognise 8.7.

**The IMDb id is the join key.** It is the only identifier present on the detail record of every source in the survey, and it is a stable, opaque, globally unique string. Make it the cross-source identity in the domain model. Two consequences follow. TMDB list responses do not carry it, so a multi-source pipeline either keys on TMDB ids internally and resolves to `tt…` at detail time, or accepts one extra call per title. And Cinemeta and Watchmode both hand out IMDb-to-TMDB mappings for free, Cinemeta per title with no key and Watchmode as a bulk CSV, so the join does not have to be paid for at TMDB rates.

**The licence constraints bind hardest on images.** TMDB caps caching at six months and forbids commercial use without an agreement. OMDb is CC BY-NC. IMDb's datasets are non-commercial. Trakt requires images be cached server-side and blocks hotlinking. Watchmode disclaims any licence to the images it links. Streaming Availability ships the best-formed URLs in the survey, but they are CloudFront leases that expire on their own schedule, so what an app persists is the bytes, never the link. No source in the survey both hands over durable URLs and attaches no rule the app has to work around.

---

## Appendix: reproducing this

```bash
# TMDB, image base URLs and the closed size list
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/configuration" | python3 -m json.tool

# TMDB, an unlisted size is a hard 400
for s in w92 w342 w500 original w999; do
  printf "%-9s " "$s"
  curl -s -o /dev/null -w "%{http_code} %{size_download}B\n" \
    "https://image.tmdb.org/t/p/$s/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg"
done

# TMDB, poster and logo aspect ratios are not fixed
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/movie/27205/images" \
  | python3 -c "
import sys,json
d=json.load(sys.stdin)
for k in ['posters','backdrops','logos']:
    r=[x['aspect_ratio'] for x in d[k]]
    print(k, len(r), min(r), max(r))"

# TMDB, imdb_id coverage and the zero-vote trap across trending page 1
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/trending/movie/week" \
  | python3 -c 'import sys,json;print(" ".join(str(r["id"]) for r in json.load(sys.stdin)["results"]))' \
  | tr " " "\n" | while read id; do
    curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
      "https://api.themoviedb.org/3/movie/$id" \
      | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(d['id'], repr(d.get('imdb_id')), d['vote_average'], d['vote_count'])"
  done

# TMDB, external_ids mixes empty string and null for absent
curl -s -H "Authorization: Bearer $TMDB_TOKEN" \
  "https://api.themoviedb.org/3/movie/27205/external_ids"

# JustWatch, the scoring object and the URL template
curl -s https://apis.justwatch.com/graphql -H 'content-type: application/json' \
  -d '{"query":"{ urlV2(fullPath:\"/us/movie/inception\"){ node { ... on Movie { content(country:\"US\",language:\"en\"){ posterUrl scoring{ imdbScore imdbVotes tmdbScore tomatoMeter certifiedFresh jwRating } externalIds{ imdbId tmdbId wikidataId } } } } } }"}'

# JustWatch, the profile set is closed and has to be probed
for p in s66 s166 s276 s332 s592 s718 s999; do
  printf "%-6s " "$p"
  curl -s -o /dev/null -w "%{http_code} %{size_download}B\n" \
    "https://images.justwatch.com/poster/343657134/$p/x.webp"
done

# Trakt, the scheme has to be prepended, and the size set is undocumented
for s in thumb medium full original; do
  curl -s -o /dev/null -w "$s %{http_code} %{size_download}B\n" \
    "https://walter-r2.trakt.tv/images/movies/000/012/601/posters/$s/e0d9dd35c5.jpg.webp"
done

# TheTVDB, the base the spec never names, recovered from the public site
curl -s https://thetvdb.com/movies/inception | grep -o 'https://artworks.thetvdb.com/[^"]*' | sort -u
curl -s -o /dev/null -w "%{http_code} %{size_download}B\n" \
  "https://artworks.thetvdb.com/banners/movies/113/posters/601c4ea1292a8_t.jpg"

# Streaming Availability, the keyless example, and what the signature is doing
curl -s https://www.movieofthenight.com/v4/examples/shows/the-dark-knight \
  | python3 -c "
import sys,json,datetime,urllib.parse
d=json.load(sys.stdin); u=d['imageSet']['verticalPoster']['w240']
q=urllib.parse.parse_qs(urllib.parse.urlparse(u).query)
print('rating', d['rating'], '| tmdbId', d['tmdbId'])
print('len', len(u), '| params', list(q))
print('expires', datetime.datetime.fromtimestamp(int(q['Expires'][0]), datetime.UTC))"
# strip the query and it is a 403
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://cdn.movieofthenight.com/show/68/poster/vertical/en/240.jpg"

# OMDb host family: the _V1_ segment is an open transform slot
curl -s https://v3.sg.media-imdb.com/suggestion/x/inception.json
for t in SX300 SX600 UX128 QL75_UX380_; do
  curl -s -o /dev/null -w "$t %{http_code} %{size_download}B\n" \
    "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_$t.jpg"
done

# Simkl, every documented suffix, and the proxy the docs ask you to use
for s in _w _m _ca _c _cm _s; do
  curl -s -o /dev/null -w "$s %{http_code} %{size_download}B\n" \
    "https://simkl.in/posters/74/74415673dcdc9cdd$s.webp"
done
curl -s -o /dev/null -w "%{http_code} %{size_download}B\n" \
  "https://wsrv.nl/?url=https://simkl.in/posters/74/74415673dcdc9cdd_ca.webp"

# Fanart.tv, thumbnails are a path swap, and the host does support TLS
curl -s -o /dev/null -w "%{http_code} %{size_download}B\n" \
  "https://assets.fanart.tv/preview/movies/120/movieposter/the-lord-of-the-rings-the-fellowship-of-the-ring-528aa45a8633a.jpg"

# Wikidata, ?width= resizes but never upscales
for w in 400 1200; do
  curl -s -o /dev/null -L -w "$w %{http_code} %{size_download}B\n" \
    "https://commons.wikimedia.org/wiki/Special:FilePath/InceptionCast2July10.jpg?width=$w"
done

# Cinemeta, artwork addressed straight from an IMDb id
curl -s "https://v3-cinemeta.strem.io/meta/movie/tt1375666.json" | python3 -m json.tool
for s in small medium large original; do
  curl -s -o /dev/null -w "$s %{http_code} %{size_download}B %{content_type}\n" \
    -L "https://images.metahub.space/poster/$s/tt1375666/img"
done

# Wikidata, the denominator lives inside the value
curl -s -G https://query.wikidata.org/sparql --data-urlencode format=json \
  --data-urlencode 'query=
SELECT ?scoreVal ?byLabel ?revLabel WHERE {
  wd:Q25188 p:P444 ?st . ?st ps:P444 ?scoreVal .
  OPTIONAL { ?st pq:P447 ?by } OPTIONAL { ?st pq:P459 ?rev }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en" } }'

# IMDb, no image column in any of the seven files
for f in title.basics title.ratings title.akas title.crew; do
  curl -s -r 0-200000 "https://datasets.imdbws.com/$f.tsv.gz" | gzip -dc 2>/dev/null | head -1
done

# OMDb, failure is a 200 with a string "False"
curl -s "https://www.omdbapi.com/?i=tt1375666"

# Watchmode, the ID map needs no key
curl -s -r 0-500 "https://api.watchmode.com/datasets/title_id_map.csv"
```

Contracts read for the SPEC findings:

```bash
curl -s https://api.watchmode.com/openapi.json                      # TitleDetails, image and rating fields
curl -s https://api.apis.guru/v2/specs/omdbapi.com/1/swagger.json   # every field typed string
curl -s https://api.mdblist.com/schema/                             # ratings declared untyped
curl -s https://raw.githubusercontent.com/thetvdb/v4-api/main/docs/swagger.yml
curl -s https://trakt.docs.apiary.io/api-description-document       # images and ratings distribution
curl -s https://simkl.docs.apiary.io/api-description-document       # image URL assembly, per-provider ratings
curl -s https://fanarttv.docs.apiary.io/api-description-document    # artwork objects, likes as string
curl -s https://raw.githubusercontent.com/movieofthenight/ts-streaming-availability/main/src/models/ShowImageSet.ts
curl -s https://raw.githubusercontent.com/movieofthenight/ts-streaming-availability/main/src/models/Show.ts
curl -s https://raw.githubusercontent.com/movieofthenight/streaming-availability-api/main/openapi.yaml
```
