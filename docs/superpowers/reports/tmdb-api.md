# TMDB API Reference

The full TMDB HTTP API in one file: every endpoint, every parameter, and the shape of every
response. Generated from TMDB's own OpenAPI definition and its guide pages, so nothing here has to
be looked up online again.

**Source:** `https://developer.themoviedb.org/openapi/tmdb-api.json` (v3, 148 paths / 152
operations) plus the per endpoint OpenAPI blocks published under `developer.themoviedb.org/v4`
(v4, 17 paths / 21 operations). Genre tables and image configuration were re-read from the live API.

**Captured:** 2026-09-09.

Sample payloads pulled from the live API sit in [`tmdb-sample-data.md`](tmdb-sample-data.md).

**How to use this file.** Section 2 is a table of every endpoint linking to its entry. Section 1
holds the rules that apply to all of them: auth, paging, errors, images, and the shared query
parameters. Section 3 is the v3 reference, section 4 the v4 reference.

Response tables flatten nested objects with dotted paths, and `[]` marks an array element
(`results[].title` is the title of an item in the `results` array). Example payloads are trimmed:
long arrays are cut and marked, long strings truncated.

---

## 1. Basics

### 1.1 Base URL and versions

```
https://api.themoviedb.org/3/...
https://api.themoviedb.org/4/...
```

Two versions run side by side and neither replaces the other. v3 carries the whole catalogue: all
movie, TV, person, search, discover and trending data. v4 adds only account lists, a rewritten
list API, and the user authentication flow. There is no v4 equivalent of the v3 read endpoints.

JSON is the only response format. A `callback` query parameter wraps the response as JSONP.

### 1.2 Authentication

Two credentials exist, and both grant the same read access.

| Credential | How it travels | Notes |
|---|---|---|
| API Read Access Token (v4) | `Authorization: Bearer <token>` header | Works against both v3 and v4 endpoints. The credential never enters the URL. |
| API key (v3) | `api_key=<key>` query parameter | v3 endpoints only. Leaks into logs, proxy history and crash reports. |

Both are issued from the API settings page of a TMDB account.

```
curl --request GET \
     --url 'https://api.themoviedb.org/3/movie/11' \
     --header 'Authorization: Bearer <access_token>' \
     --header 'accept: application/json'
```

Endpoints that act on behalf of a person (rating, watchlist, favourites, private lists) need a
user credential on top of the application one.

**v4 user flow.** Three steps, and the app authenticates each of them with its own read access
token.

1. `POST /4/auth/request_token` returns a request token that expires unused after 15 minutes.
2. Send the user to `https://www.themoviedb.org/auth/access?request_token={request_token}`. On
   approval TMDB returns them to the `redirect_to` given in step 1, which may be an app scheme.
3. `POST /4/auth/access_token` exchanges the approved request token for a user access token and an
   `account_id`. `DELETE /4/auth/access_token` revokes it.

**v3 session flow.** `GET /3/authentication/token/new`, user approval at
`https://www.themoviedb.org/authenticate/{request_token}`, then
`POST /3/authentication/session/new` for a `session_id` passed as a query parameter. A v4 user
access token can be converted with `POST /3/authentication/session/convert/4`.

**Guest sessions.** `GET /3/authentication/guest_session/new` yields a session that can rate media
and nothing else. It expires after 60 minutes of inactivity.

### 1.3 Rate limits

The original limit of 40 requests per 10 seconds was disabled in December 2019. TMDB documents the
current ceiling as "somewhere in the 40 requests per second range", subject to change, and expects
clients to back off on a `429`.

### 1.4 Errors

Every error carries a JSON body, and the HTTP status is not the whole story: TMDB's own
`status_code` identifies the cause.

```json
{ "status_code": 7, "status_message": "Invalid API key: You must be granted a valid key.", "success": false }
```

| `status_code` | HTTP | Message |
|---|---|---|
| 1 | 200 | Success. |
| 2 | 501 | Invalid service: this service does not exist. |
| 3 | 401 | Authentication failed: You do not have permissions to access the service. |
| 4 | 405 | Invalid format: This service doesn't exist in that format. |
| 5 | 422 | Invalid parameters: Your request parameters are incorrect. |
| 6 | 404 | Invalid id: The pre-requisite id is invalid or not found. |
| 7 | 401 | Invalid API key: You must be granted a valid key. |
| 8 | 403 | Duplicate entry: The data you tried to submit already exists. |
| 9 | 503 | Service offline: This service is temporarily offline, try again later. |
| 10 | 401 | Suspended API key: Access to your account has been suspended, contact TMDB. |
| 11 | 500 | Internal error: Something went wrong, contact TMDB. |
| 12 | 201 | The item/record was updated successfully. |
| 13 | 200 | The item/record was deleted successfully. |
| 14 | 401 | Authentication failed. |
| 15 | 500 | Failed. |
| 16 | 401 | Device denied. |
| 17 | 401 | Session denied. |
| 18 | 400 | Validation failed. |
| 19 | 406 | Invalid accept header. |
| 20 | 422 | Invalid date range: Should be a range no longer than 14 days. |
| 21 | 200 | Entry not found: The item you are trying to edit cannot be found. |
| 22 | 400 | Invalid page: Pages start at 1 and max at 500. They are expected to be an integer. |
| 23 | 400 | Invalid date: Format needs to be YYYY-MM-DD. |
| 24 | 504 | Your request to the backend server timed out. Try again. |
| 25 | 429 | Your request count (#) is over the allowed limit of (40). |
| 26 | 400 | You must provide a username and password. |
| 27 | 400 | Too many append to response objects: The maximum number of remote calls is 20. |
| 28 | 400 | Invalid timezone: Please consult the documentation for a valid timezone. |
| 29 | 400 | You must confirm this action: Please provide a confirm=true parameter. |
| 30 | 401 | Invalid username and/or password: You did not provide a valid login. |
| 31 | 401 | Account disabled: Your account is no longer active. Contact TMDB if this is an error. |
| 32 | 401 | Email not verified: Your email address has not been verified. |
| 33 | 401 | Invalid request token: The request token is either expired or invalid. |
| 34 | 404 | The resource you requested could not be found. |
| 35 | 401 | Invalid token. |
| 36 | 401 | This token hasn't been granted write permission by the user. |
| 37 | 404 | The requested session could not be found. |
| 38 | 401 | You don't have permission to edit this resource. |
| 39 | 401 | This resource is private. |
| 40 | 200 | Nothing to update. |
| 41 | 422 | This request token hasn't been approved by the user. |
| 42 | 405 | This request method is not supported for this resource. |
| 43 | 502 | Couldn't connect to the backend server. |
| 44 | 500 | The ID is invalid. |
| 45 | 403 | This user has been suspended. |
| 46 | 503 | The API is undergoing maintenance. Try again later. |
| 47 | 400 | The input is not valid. |

### 1.5 Pagination

Every list endpoint returns the same envelope.

| Field | Type | Meaning |
|---|---|---|
| `page` | integer | The page returned, 1 based. |
| `results` | array | The items. |
| `total_pages` | integer | Pages available, capped at 500. |
| `total_results` | integer | Items available. |

**Page size is 20 and is not configurable.** No parameter changes it, and requests for a page above
500 return `status_code` 22. `total_results` is therefore capped at 10000 for any endpoint that
reports 500 pages.

Ranking shifts between requests on trending and popularity ordered endpoints, so an item on a page
boundary can be served twice across sequential page requests. Deduplicate by id when assembling
more than one page.

### 1.6 Shared query parameters

| Parameter | Applies to | Notes |
|---|---|---|
| `language` | most read endpoints | `ISO 639-1` optionally joined to `ISO 3166-1`, as `en-US`. Default `en-US`. Falls back to the original language, then to any value, when a translation is missing. |
| `region` | search, discover, and release aware endpoints | `ISO 3166-1` code. Filters and presents release dates for that country, falling back to the primary release date. |
| `page` | list endpoints | 1 to 500. |
| `include_adult` | search and discover | Default `false`. Trending does not accept it; adult content is filtered server side there. |
| `include_image_language` | `/images` endpoints and appended image requests | Comma separated `ISO 639-1` codes plus the literal `null` for untagged images, as `en,null`. Acts as a fallback beyond `language`. |
| `timezone` | TV airing endpoints | An invalid value returns `status_code` 28. |
| `append_to_response` | movie, TV series, TV season, TV episode and person detail endpoints | See below. |

### 1.7 `append_to_response`

Folds sub-resources of a detail endpoint into one request. The value is a comma separated list of
endpoint names within that namespace, 20 maximum, and each appended sub-request honours the query
parameters of the endpoint it names.

```
https://api.themoviedb.org/3/movie/11?append_to_response=videos,images&include_image_language=en,null
```

The appended data arrives as extra top level keys on the response object, keyed by the sub-resource
name. Exceeding 20 items returns `status_code` 27.

### 1.8 Images

Image fields (`poster_path`, `backdrop_path`, `logo_path`, `profile_path`, `still_path`) hold a
file path, not a URL. Build a URL from three pieces: base, size, path.

```
https://image.tmdb.org/t/p/ + w500 + /1E5baAaEse26fej7uHcjOgEE2t2.jpg
```

Sizes come from `GET /3/configuration`, and re-reading it live gives:

| Set | Sizes | Aspect |
|---|---|---|
| `poster_sizes` | `w92`, `w154`, `w185`, `w342`, `w500`, `w780`, `original` | 2:3 |
| `backdrop_sizes` | `w300`, `w780`, `w1280`, `original` | 16:9 |
| `logo_sizes` | `w45`, `w92`, `w154`, `w185`, `w300`, `w500`, `original` | varies |
| `profile_sizes` | `w45`, `w185`, `h632`, `original` | 2:3 |
| `still_sizes` | `w92`, `w185`, `w300`, `original` | 16:9 |

`base_url` is `http://image.tmdb.org/t/p/` and `secure_base_url` is
`https://image.tmdb.org/t/p/`. Use the secure one.

**Language behaviour.** A `poster_path` resolves to the requested language first, then the media's
original language, then the highest rated image of any language. Backdrops and stills rarely carry
a language, so they resolve to the highest rated untagged image. Pass `include_image_language` on
the `/images` endpoints to widen that.

**Logos** come in PNG and SVG. Every `logo_path` field returns `.png` for backwards compatibility;
the `/images` endpoints expose a `file_type` giving the original upload. SVGs are not resized, so
request `original` for them, and swapping the extension between `.svg` and `.png` works on the
`original` size.

### 1.9 Genre ids

`genre_ids` on list items are ids only. Detail endpoints return `genres` as objects. The tables are
fixed and small enough to hold in the client, so `GET /3/genre/movie/list` exists mainly to fetch
localised names. Read live on 2026-09-09 with `language=en`.

**Movies, 19 genres**

| Id | Name | Id | Name |
|---|---|---|---|
| 28 | Action | 9648 | Mystery |
| 12 | Adventure | 10402 | Music |
| 16 | Animation | 10749 | Romance |
| 35 | Comedy | 878 | Science Fiction |
| 80 | Crime | 10770 | TV Movie |
| 99 | Documentary | 53 | Thriller |
| 18 | Drama | 10752 | War |
| 10751 | Family | 37 | Western |
| 14 | Fantasy | 36 | History |
| 27 | Horror | | |

**TV, 16 genres**

| Id | Name | Id | Name |
|---|---|---|---|
| 10759 | Action & Adventure | 10763 | News |
| 16 | Animation | 10764 | Reality |
| 35 | Comedy | 10765 | Sci-Fi & Fantasy |
| 80 | Crime | 10766 | Soap |
| 99 | Documentary | 10767 | Talk |
| 18 | Drama | 10768 | War & Politics |
| 10751 | Family | 37 | Western |
| 10762 | Kids | 9648 | Mystery |

The two sets overlap on shared ids (16, 18, 35, 37, 80, 99, 9648, 10751) and diverge everywhere
else, so a movie id table cannot resolve a TV `genre_ids` array.

### 1.10 Discover sort keys

`sort_by` on `/3/discover/movie`, default `popularity.desc`:

`original_title.asc`, `original_title.desc`, `popularity.asc`, `popularity.desc`, `revenue.asc`,
`revenue.desc`, `primary_release_date.asc`, `primary_release_date.desc`, `title.asc`, `title.desc`,
`vote_average.asc`, `vote_average.desc`, `vote_count.asc`, `vote_count.desc`

`sort_by` on `/3/discover/tv`, default `popularity.desc`:

`first_air_date.asc`, `first_air_date.desc`, `name.asc`, `name.desc`, `original_name.asc`,
`original_name.desc`, `popularity.asc`, `popularity.desc`, `vote_average.asc`, `vote_average.desc`,
`vote_count.asc`, `vote_count.desc`

Trending endpoints accept no `sort_by`. Their order is the trending rank, which is not popularity
order, so client side sorting re-orders the response.

### 1.11 Popularity and trending

`popularity` is a lifetime score recomputed daily from votes, views, favourites, watchlist adds,
release date, total votes and the previous day's score. `trending` is a separate short window score
over a day or a week. Neither is exposed as a queryable breakdown.

"Lifetime" describes scope, not stability: because each day's score folds in only that day's
activity plus the previous day's score, `popularity` tracks current attention and decays once that
attention drops. A title can move an order of magnitude within weeks with no re-release, and an
unreleased title's pre-release anticipation traffic can outscore decades-old classics that hold far
more total votes. Treat it as volatile: don't cache it and re-sort against the cached value later,
and don't read it as a proxy for quality or lifetime reach, `vote_count` and `vote_average` are the
stable fields for that. The daily id exports (1.13) are the only way to see a title's popularity
over time; there is no history endpoint.

Trending and discover are not interchangeable feeds. Trending's result set is bounded by the 500
page cap and dedupes to roughly 8,000 titles, two thirds of them below popularity 5, and it holds
its recency skew at every depth. Discover ranks the whole movie catalogue and its first 2,000 rows
all clear popularity 9. Discover's opening page resembles a trending list because its default sort
is `popularity.desc` and current attention floats to the top, but the resemblance is shallow: by
the third page discover is mostly back catalogue, and it reaches titles trending holds nowhere in
its pool.

Re-sorting a trending response by `popularity` produces neither feed. Popularity rewards steady
traffic, so an older title with a broad audience outranks a genuinely moving new release, and the
sort lifts the back catalogue that sits deep in trending rank to the top.

Over the first 2,000 titles the re-sorted pool and discover become near twins by composition:
similar median popularity, a comparable share of current year releases, and roughly three quarters
of each predating the current year. They share around seven in ten of discover's titles. What does
not converge is the order, barely a dozen titles land at the same position, and the tail quality,
where the re-sorted pool spends close to a third of those rows on titles at popularity 10 or below
against a handful for discover. Composition alone does not make them substitutes. Page trending in
its served order, and reach for discover when catalogue depth matters.

### 1.12 Release types

`with_release_type` on discover, and the `type` field on `/3/movie/{movie_id}/release_dates`:

| Type | Release |
|---|---|
| 1 | Premiere |
| 2 | Theatrical (limited) |
| 3 | Theatrical |
| 4 | Digital |
| 5 | Physical |
| 6 | TV |

### 1.13 Daily id exports

TMDB publishes daily id dumps at `https://files.tmdb.org/p/exports/`, generated from 07:00 UTC and
complete by 08:00 UTC, retained for three months. Files are gzipped and each *line* is a JSON
object, so the file as a whole is not valid JSON.

| Media type | File |
|---|---|
| Movies | `movie_ids_MM_DD_YYYY.json.gz` |
| TV series | `tv_series_ids_MM_DD_YYYY.json.gz` |
| People | `person_ids_MM_DD_YYYY.json.gz` |
| Collections | `collection_ids_MM_DD_YYYY.json.gz` |
| TV networks | `tv_network_ids_MM_DD_YYYY.json.gz` |
| Keywords | `keyword_ids_MM_DD_YYYY.json.gz` |
| Production companies | `production_company_ids_MM_DD_YYYY.json.gz` |
| Adult movies | `adult_movie_ids_MM_DD_YYYY.json.gz` |
| Adult TV series | `adult_tv_series_ids_MM_DD_YYYY.json.gz` |
| Adult people | `adult_person_ids_MM_DD_YYYY.json.gz` |

Each line carries the id plus filtering attributes such as `adult`, `video` and `popularity`. These
are id lists, not data exports.

### 1.14 Change tracking

`GET /3/movie/changes`, `/3/tv/changes` and `/3/person/changes` list ids changed in a date range of
at most 14 days; a wider range returns `status_code` 20. `GET /3/movie/{movie_id}/changes` gives
the field level changes for one record, keyed by the `change_keys` that `GET /3/configuration`
enumerates.

---

## 2. Endpoint index

### v3

| Endpoint | Reference |
|---|---|
| `GET /3/configuration` | [Configuration — Details](#configuration--details) |
| `GET /3/configuration/countries` | [Configuration — Countries](#configuration--countries) |
| `GET /3/configuration/jobs` | [Configuration — Jobs](#configuration--jobs) |
| `GET /3/configuration/languages` | [Configuration — Languages](#configuration--languages) |
| `GET /3/configuration/primary_translations` | [Configuration — Primary Translations](#configuration--primary-translations) |
| `GET /3/configuration/timezones` | [Configuration — Timezones](#configuration--timezones) |
| `GET /3/authentication` | [Authentication — Validate Key](#authentication--validate-key) |
| `GET /3/authentication/guest_session/new` | [Authentication — Create Guest Session](#authentication--create-guest-session) |
| `DELETE /3/authentication/session` | [Authentication — Delete Session](#authentication--delete-session) |
| `POST /3/authentication/session/convert/4` | [Authentication — Create Session (from v4 token)](#authentication--create-session-from-v4-token) |
| `POST /3/authentication/session/new` | [Authentication — Create Session](#authentication--create-session) |
| `GET /3/authentication/token/new` | [Authentication — Create Request Token](#authentication--create-request-token) |
| `POST /3/authentication/token/validate_with_login` | [Authentication — Create Session (with login)](#authentication--create-session-with-login) |
| `GET /3/guest_session/{guest_session_id}/rated/movies` | [Guest sessions — Rated Movies](#guest-sessions--rated-movies) |
| `GET /3/guest_session/{guest_session_id}/rated/tv` | [Guest sessions — Rated TV](#guest-sessions--rated-tv) |
| `GET /3/guest_session/{guest_session_id}/rated/tv/episodes` | [Guest sessions — Rated TV Episodes](#guest-sessions--rated-tv-episodes) |
| `GET /3/trending/all/{time_window}` | [Trending — All](#trending--all) |
| `GET /3/trending/movie/{time_window}` | [Trending — Movies](#trending--movies) |
| `GET /3/trending/person/{time_window}` | [Trending — People](#trending--people) |
| `GET /3/trending/tv/{time_window}` | [Trending — TV](#trending--tv) |
| `GET /3/movie/changes` | [Movies — Movie List](#movies--movie-list) |
| `GET /3/movie/latest` | [Movies — Latest](#movies--latest) |
| `GET /3/movie/now_playing` | [Movies — Now Playing](#movies--now-playing) |
| `GET /3/movie/popular` | [Movies — Popular](#movies--popular) |
| `GET /3/movie/top_rated` | [Movies — Top Rated](#movies--top-rated) |
| `GET /3/movie/upcoming` | [Movies — Upcoming](#movies--upcoming) |
| `GET /3/movie/{movie_id}` | [Movies — Details](#movies--details) |
| `GET /3/movie/{movie_id}/account_states` | [Movies — Account States](#movies--account-states) |
| `GET /3/movie/{movie_id}/alternative_titles` | [Movies — Alternative Titles](#movies--alternative-titles) |
| `GET /3/movie/{movie_id}/changes` | [Movies — Changes](#movies--changes) |
| `GET /3/movie/{movie_id}/credits` | [Movies — Credits](#movies--credits) |
| `GET /3/movie/{movie_id}/external_ids` | [Movies — External IDs](#movies--external-ids) |
| `GET /3/movie/{movie_id}/images` | [Movies — Images](#movies--images) |
| `GET /3/movie/{movie_id}/keywords` | [Movies — Keywords](#movies--keywords) |
| `GET /3/movie/{movie_id}/lists` | [Movies — Lists](#movies--lists) |
| `POST /3/movie/{movie_id}/rating` | [Movies — Add Rating](#movies--add-rating) |
| `DELETE /3/movie/{movie_id}/rating` | [Movies — Delete Rating](#movies--delete-rating) |
| `GET /3/movie/{movie_id}/recommendations` | [Movies — Recommendations](#movies--recommendations) |
| `GET /3/movie/{movie_id}/release_dates` | [Movies — Release Dates](#movies--release-dates) |
| `GET /3/movie/{movie_id}/reviews` | [Movies — Reviews](#movies--reviews) |
| `GET /3/movie/{movie_id}/similar` | [Movies — Similar](#movies--similar) |
| `GET /3/movie/{movie_id}/translations` | [Movies — Translations](#movies--translations) |
| `GET /3/movie/{movie_id}/videos` | [Movies — Videos](#movies--videos) |
| `GET /3/movie/{movie_id}/watch/providers` | [Movies — Watch Providers](#movies--watch-providers) |
| `GET /3/discover/movie` | [Discover — Movie](#discover--movie) |
| `GET /3/discover/tv` | [Discover — TV](#discover--tv) |
| `GET /3/search/collection` | [Search — Collection](#search--collection) |
| `GET /3/search/company` | [Search — Company](#search--company) |
| `GET /3/search/keyword` | [Search — Keyword](#search--keyword) |
| `GET /3/search/movie` | [Search — Movie](#search--movie) |
| `GET /3/search/multi` | [Search — Multi](#search--multi) |
| `GET /3/search/person` | [Search — Person](#search--person) |
| `GET /3/search/tv` | [Search — TV](#search--tv) |
| `GET /3/find/{external_id}` | [Find — Find By ID](#find--find-by-id) |
| `GET /3/genre/movie/list` | [Genres — Movie List](#genres--movie-list) |
| `GET /3/genre/tv/list` | [Genres — TV List](#genres--tv-list) |
| `GET /3/keyword/{keyword_id}` | [Keywords — Details](#keywords--details) |
| `GET /3/keyword/{keyword_id}/movies` | [Keywords — Movies](#keywords--movies) |
| `GET /3/collection/{collection_id}` | [Collections — Details](#collections--details) |
| `GET /3/collection/{collection_id}/images` | [Collections — Images](#collections--images) |
| `GET /3/collection/{collection_id}/translations` | [Collections — Translations](#collections--translations) |
| `GET /3/company/{company_id}` | [Companies — Details](#companies--details) |
| `GET /3/company/{company_id}/alternative_names` | [Companies — Alternative Names](#companies--alternative-names) |
| `GET /3/company/{company_id}/images` | [Companies — Images](#companies--images) |
| `GET /3/credit/{credit_id}` | [Credits — Details](#credits--details) |
| `GET /3/certification/movie/list` | [Certifications — Movie Certifications](#certifications--movie-certifications) |
| `GET /3/certification/tv/list` | [Certifications — TV Certifications](#certifications--tv-certifications) |
| `GET /3/review/{review_id}` | [Reviews — Details](#reviews--details) |
| `GET /3/watch/providers/movie` | [Watch providers — Movie Providers](#watch-providers--movie-providers) |
| `GET /3/watch/providers/regions` | [Watch providers — Available Regions](#watch-providers--available-regions) |
| `GET /3/watch/providers/tv` | [Watch providers — TV Providers](#watch-providers--tv-providers) |
| `GET /3/tv/airing_today` | [TV — Airing Today](#tv--airing-today) |
| `GET /3/tv/changes` | [TV — TV List](#tv--tv-list) |
| `GET /3/tv/episode/{episode_id}/changes` | [TV — Changes](#tv--changes) |
| `GET /3/tv/episode_group/{tv_episode_group_id}` | [TV — Details](#tv--details) |
| `GET /3/tv/latest` | [TV — Latest](#tv--latest) |
| `GET /3/tv/on_the_air` | [TV — On The Air](#tv--on-the-air) |
| `GET /3/tv/popular` | [TV — Popular](#tv--popular) |
| `GET /3/tv/season/{season_id}/changes` | [TV — Changes](#tv--changes-1) |
| `GET /3/tv/top_rated` | [TV — Top Rated](#tv--top-rated) |
| `GET /3/tv/{series_id}` | [TV — Details](#tv--details-1) |
| `GET /3/tv/{series_id}/account_states` | [TV — Account States](#tv--account-states) |
| `GET /3/tv/{series_id}/aggregate_credits` | [TV — Aggregate Credits](#tv--aggregate-credits) |
| `GET /3/tv/{series_id}/alternative_titles` | [TV — Alternative Titles](#tv--alternative-titles) |
| `GET /3/tv/{series_id}/changes` | [TV — Changes](#tv--changes-2) |
| `GET /3/tv/{series_id}/content_ratings` | [TV — Content Ratings](#tv--content-ratings) |
| `GET /3/tv/{series_id}/credits` | [TV — Credits](#tv--credits) |
| `GET /3/tv/{series_id}/episode_groups` | [TV — Episode Groups](#tv--episode-groups) |
| `GET /3/tv/{series_id}/external_ids` | [TV — External IDs](#tv--external-ids) |
| `GET /3/tv/{series_id}/images` | [TV — Images](#tv--images) |
| `GET /3/tv/{series_id}/keywords` | [TV — Keywords](#tv--keywords) |
| `GET /3/tv/{series_id}/lists` | [TV — Lists](#tv--lists) |
| `POST /3/tv/{series_id}/rating` | [TV — Add Rating](#tv--add-rating) |
| `DELETE /3/tv/{series_id}/rating` | [TV — Delete Rating](#tv--delete-rating) |
| `GET /3/tv/{series_id}/recommendations` | [TV — Recommendations](#tv--recommendations) |
| `GET /3/tv/{series_id}/reviews` | [TV — Reviews](#tv--reviews) |
| `GET /3/tv/{series_id}/screened_theatrically` | [TV — Screened Theatrically](#tv--screened-theatrically) |
| `GET /3/tv/{series_id}/season/{season_number}` | [TV — Details](#tv--details-2) |
| `GET /3/tv/{series_id}/season/{season_number}/account_states` | [TV — Account States](#tv--account-states-1) |
| `GET /3/tv/{series_id}/season/{season_number}/aggregate_credits` | [TV — Aggregate Credits](#tv--aggregate-credits-1) |
| `GET /3/tv/{series_id}/season/{season_number}/credits` | [TV — Credits](#tv--credits-1) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}` | [TV — Details](#tv--details-3) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/account_states` | [TV — Account States](#tv--account-states-2) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/credits` | [TV — Credits](#tv--credits-2) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/external_ids` | [TV — External IDs](#tv--external-ids-1) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/images` | [TV — Images](#tv--images-1) |
| `POST /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating` | [TV — Add Rating](#tv--add-rating-1) |
| `DELETE /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating` | [TV — Delete Rating](#tv--delete-rating-1) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/translations` | [TV — Translations](#tv--translations) |
| `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}/videos` | [TV — Videos](#tv--videos) |
| `GET /3/tv/{series_id}/season/{season_number}/external_ids` | [TV — External IDs](#tv--external-ids-2) |
| `GET /3/tv/{series_id}/season/{season_number}/images` | [TV — Images](#tv--images-2) |
| `GET /3/tv/{series_id}/season/{season_number}/translations` | [TV — Translations](#tv--translations-1) |
| `GET /3/tv/{series_id}/season/{season_number}/videos` | [TV — Videos](#tv--videos-1) |
| `GET /3/tv/{series_id}/season/{season_number}/watch/providers` | [TV — Watch Providers](#tv--watch-providers) |
| `GET /3/tv/{series_id}/similar` | [TV — Similar](#tv--similar) |
| `GET /3/tv/{series_id}/translations` | [TV — Translations](#tv--translations-2) |
| `GET /3/tv/{series_id}/videos` | [TV — Videos](#tv--videos-2) |
| `GET /3/tv/{series_id}/watch/providers` | [TV — Watch Providers](#tv--watch-providers-1) |
| `GET /3/network/{network_id}` | [Networks — Details](#networks--details) |
| `GET /3/network/{network_id}/alternative_names` | [Networks — Alternative Names](#networks--alternative-names) |
| `GET /3/network/{network_id}/images` | [Networks — Images](#networks--images) |
| `GET /3/person/changes` | [People — People List](#people--people-list) |
| `GET /3/person/latest` | [People — Latest](#people--latest) |
| `GET /3/person/popular` | [People — Popular](#people--popular) |
| `GET /3/person/{person_id}` | [People — Details](#people--details) |
| `GET /3/person/{person_id}/changes` | [People — Changes](#people--changes) |
| `GET /3/person/{person_id}/combined_credits` | [People — Combined Credits](#people--combined-credits) |
| `GET /3/person/{person_id}/external_ids` | [People — External IDs](#people--external-ids) |
| `GET /3/person/{person_id}/images` | [People — Images](#people--images) |
| `GET /3/person/{person_id}/movie_credits` | [People — Movie Credits](#people--movie-credits) |
| `GET /3/person/{person_id}/tagged_images` | [People — Tagged Images](#people--tagged-images) |
| `GET /3/person/{person_id}/translations` | [People — Translations](#people--translations) |
| `GET /3/person/{person_id}/tv_credits` | [People — TV Credits](#people--tv-credits) |
| `POST /3/list` | [Lists — Create](#lists--create) |
| `GET /3/list/{list_id}` | [Lists — Details](#lists--details) |
| `DELETE /3/list/{list_id}` | [Lists — Delete](#lists--delete) |
| `POST /3/list/{list_id}/add_item` | [Lists — Add Movie](#lists--add-movie) |
| `POST /3/list/{list_id}/clear` | [Lists — Clear](#lists--clear) |
| `GET /3/list/{list_id}/item_status` | [Lists — Check Item Status](#lists--check-item-status) |
| `POST /3/list/{list_id}/remove_item` | [Lists — Remove Movie](#lists--remove-movie) |
| `GET /3/account/{account_id}` | [Account — Details](#account--details) |
| `POST /3/account/{account_id}/favorite` | [Account — Add Favorite](#account--add-favorite) |
| `GET /3/account/{account_id}/favorite/movies` | [Account — Favorite Movies](#account--favorite-movies) |
| `GET /3/account/{account_id}/favorite/tv` | [Account — Favorite TV](#account--favorite-tv) |
| `GET /3/account/{account_id}/lists` | [Account — Lists](#account--lists) |
| `GET /3/account/{account_id}/rated/movies` | [Account — Rated Movies](#account--rated-movies) |
| `GET /3/account/{account_id}/rated/tv` | [Account — Rated TV](#account--rated-tv) |
| `GET /3/account/{account_id}/rated/tv/episodes` | [Account — Rated TV Episodes](#account--rated-tv-episodes) |
| `POST /3/account/{account_id}/watchlist` | [Account — Add To Watchlist](#account--add-to-watchlist) |
| `GET /3/account/{account_id}/watchlist/movies` | [Account — Watchlist Movies](#account--watchlist-movies) |
| `GET /3/account/{account_id}/watchlist/tv` | [Account — Watchlist TV](#account--watchlist-tv) |

### v4

| Endpoint | Reference |
|---|---|
| `POST /4/auth/access_token` | [Authentication — Create Access Token](#authentication--create-access-token) |
| `DELETE /4/auth/access_token` | [Authentication — Logout](#authentication--logout) |
| `POST /4/auth/request_token` | [Authentication — Create Request Token](#authentication--create-request-token-1) |
| `POST /4/list` | [Lists — Create](#lists--create-1) |
| `GET /4/list/{list_id}` | [Lists — Details](#lists--details-1) |
| `PUT /4/list/{list_id}` | [Lists — Update](#lists--update) |
| `DELETE /4/list/{list_id}` | [Lists — Delete](#lists--delete-1) |
| `GET /4/list/{list_id}/clear` | [Lists — Clear](#lists--clear-1) |
| `GET /4/list/{list_id}/item_status` | [Lists — Item Status](#lists--item-status) |
| `POST /4/list/{list_id}/items` | [Lists — Add Items](#lists--add-items) |
| `PUT /4/list/{list_id}/items` | [Lists — Update Items](#lists--update-items) |
| `DELETE /4/list/{list_id}/items` | [Lists — Remove Items](#lists--remove-items) |
| `GET /4/account/{account_object_id}/lists` | [Account — Lists](#account--lists-1) |
| `GET /4/account/{account_object_id}/movie/favorites` | [Account — Favorite Movies](#account--favorite-movies-1) |
| `GET /4/account/{account_object_id}/movie/rated` | [Account — Rated Movies](#account--rated-movies-1) |
| `GET /4/account/{account_object_id}/movie/recommendations` | [Account — Recommended Movies](#account--recommended-movies) |
| `GET /4/account/{account_object_id}/movie/watchlist` | [Account — Watchlist Movies](#account--watchlist-movies-1) |
| `GET /4/account/{account_object_id}/tv/favorites` | [Account — Favorite TV Shows](#account--favorite-tv-shows) |
| `GET /4/account/{account_object_id}/tv/rated` | [Account — Rated TV Shows](#account--rated-tv-shows) |
| `GET /4/account/{account_object_id}/tv/recommendations` | [Account — Recommended TV Shows](#account--recommended-tv-shows) |
| `GET /4/account/{account_object_id}/tv/watchlist` | [Account — Watchlist TV Shows](#account--watchlist-tv-shows) |

---

## 3. v3 endpoints

### Configuration

#### Configuration — Details

```
GET https://api.themoviedb.org/3/configuration
```

Query the API configuration details.

Operation id: `configuration-details`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `images` | object | - |
| `images.base_url` | string | http://image.tmdb.org/t/p/ |
| `images.secure_base_url` | string | https://image.tmdb.org/t/p/ |
| `images.backdrop_sizes` | array<string> | - |
| `images.logo_sizes` | array<string> | - |
| `images.poster_sizes` | array<string> | - |
| `images.profile_sizes` | array<string> | - |
| `images.still_sizes` | array<string> | - |
| `change_keys` | array<string> | - |

<details><summary>Example response</summary>

```json
{
  "images": {
    "base_url": "http://image.tmdb.org/t/p/",
    "secure_base_url": "https://image.tmdb.org/t/p/",
    "backdrop_sizes": [
      "w300",
      "w780",
      "w1280",
      "original"
    ],
    "logo_sizes": [
      "w45",
      "w92",
      "w154",
      "w185",
      "w300",
      "w500",
      "original"
    ],
    "poster_sizes": [
      "w92",
      "w154",
      "w185",
      "w342",
      "w500",
      "w780",
      "original"
    ],
    "profile_sizes": [
      "w45",
      "w185",
      "h632",
      "original"
    ],
    "still_sizes": [
      "w92",
      "w185",
      "w300",
      "original"
    ]
  },
  "change_keys": [
    "adult",
    "air_date",
    "also_known_as",
    "alternative_titles",
    "biography",
    "birthday",
    "budget",
    "cast",
    "… 45 more items"
  ]
}
```

</details>


#### Configuration — Countries

```
GET https://api.themoviedb.org/3/configuration/countries
```

Get the list of countries (ISO 3166-1 tags) used throughout TMDB.

Operation id: `configuration-countries`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `[].iso_3166_1` | string | AD |
| `[].english_name` | string | Andorra |
| `[].native_name` | string | Andorra |

<details><summary>Example response</summary>

```json
[
  {
    "iso_3166_1": "AD",
    "english_name": "Andorra",
    "native_name": "Andorra"
  },
  {
    "iso_3166_1": "AE",
    "english_name": "United Arab Emirates",
    "native_name": "United Arab Emirates"
  },
  "… 249 more items"
]
```

</details>


#### Configuration — Jobs

```
GET https://api.themoviedb.org/3/configuration/jobs
```

Get the list of the jobs and departments we use on TMDB.

Operation id: `configuration-jobs`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `[].department` | string | Production |
| `[].jobs` | array<string> | - |

<details><summary>Example response</summary>

```json
[
  {
    "department": "Production",
    "jobs": [
      "Casting",
      "Line Producer",
      "Co-Producer",
      "Accounting Trainee",
      "Assistant Extras Casting",
      "First Assistant Accountant",
      "General Manager",
      "Head of Production",
      "… 114 more items"
    ]
  },
  {
    "department": "Costume & Make-Up",
    "jobs": [
      "Costume Design",
      "First Assistant Hairstylist",
      "Hair Department Head",
      "Ager/Dyer",
      "Key Costumer",
      "Costume Supervisor",
      "Costume Coordinator",
      "Key Set Costumer",
      "… 75 more items"
    ]
  },
  "… 10 more items"
]
```

</details>


#### Configuration — Languages

```
GET https://api.themoviedb.org/3/configuration/languages
```

Get the list of languages (ISO 639-1 tags) used throughout TMDB.

Operation id: `configuration-languages`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `[].iso_639_1` | string | bi |
| `[].english_name` | string | Bislama |
| `[].name` | string | - |

<details><summary>Example response</summary>

```json
[
  {
    "iso_639_1": "bi",
    "english_name": "Bislama",
    "name": ""
  },
  {
    "iso_639_1": "cs",
    "english_name": "Czech",
    "name": "Český"
  },
  "… 185 more items"
]
```

</details>


#### Configuration — Primary Translations

```
GET https://api.themoviedb.org/3/configuration/primary_translations
```

Get a list of the officially supported translations on TMDB.

Operation id: `configuration-primary-translations`

**Parameters:** none.

**Response `200`**

<details><summary>Example response</summary>

```json
[
  "af-ZA",
  "ar-AE",
  "ar-SA",
  "be-BY",
  "bg-BG",
  "bn-BD",
  "ca-ES",
  "ch-GU",
  "… 72 more items"
]
```

</details>


#### Configuration — Timezones

```
GET https://api.themoviedb.org/3/configuration/timezones
```

Get the list of timezones used throughout TMDB.

Operation id: `configuration-timezones`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `[].iso_3166_1` | string | AD |
| `[].zones` | array<string> | - |

<details><summary>Example response</summary>

```json
[
  {
    "iso_3166_1": "AD",
    "zones": [
      "Europe/Andorra"
    ]
  },
  {
    "iso_3166_1": "AE",
    "zones": [
      "Asia/Dubai"
    ]
  },
  "… 247 more items"
]
```

</details>



### Authentication

#### Authentication — Validate Key

```
GET https://api.themoviedb.org/3/authentication
```

Test your API Key to see if it's valid.

Operation id: `authentication-validate-key`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "status_code": 1,
  "status_message": "Success."
}
```

</details>

**Response `401`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 7 |
| `status_message` | string | Invalid API key: You must be granted a vali… |
| `success` | boolean | false |

<details><summary>Example response</summary>

```json
{
  "status_code": 7,
  "status_message": "Invalid API key: You must be granted a valid key.",
  "success": false
}
```

</details>


#### Authentication — Create Guest Session

```
GET https://api.themoviedb.org/3/authentication/guest_session/new
```

Operation id: `authentication-create-guest-session`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `guest_session_id` | string | 1ce82ec1223641636ad4a60b07de3581 |
| `expires_at` | string | 2016-08-27 16:26:40 UTC |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "guest_session_id": "1ce82ec1223641636ad4a60b07de3581",
  "expires_at": "2016-08-27 16:26:40 UTC"
}
```

</details>


#### Authentication — Delete Session

```
DELETE https://api.themoviedb.org/3/authentication/session
```

Operation id: `authentication-delete-session`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "session_id": "2629f70fb498edc263a0adb99118ac41f0053e8c"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |

<details><summary>Example response</summary>

```json
{
  "success": true
}
```

</details>


#### Authentication — Create Session (from v4 token)

```
POST https://api.themoviedb.org/3/authentication/session/convert/4
```

Operation id: `authentication-create-session-from-v4-token`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "access_token": "eyK0eXAiOiJAV1QiLCJhbGciOiUIUzI1NiJ9.eyJhdWQiOiI0Ozc2YzA1ZTg4YTY1Yzk0MjFjZDI1NmBiYzRiNGE0NyIsInN1YiI6IjRiYzg4OTJhMDE3YTNjMGY5MjAwMDAwMiIsInNjb3BlayI6WyJhcGlfcmVhZCJdLCL2ZXJzaW9uIjoxfQ.Bn660W0Vi-_AI5HvwIEqtc2s5mAXDknBnTrUREZYH7A"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `session_id` | string | 2629f70fb498edc263a0adb99118ac41f0053e8c |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "session_id": "2629f70fb498edc263a0adb99118ac41f0053e8c"
}
```

</details>


#### Authentication — Create Session

```
POST https://api.themoviedb.org/3/authentication/session/new
```

Operation id: `authentication-create-session`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "request_token": "6bc047b88f669d1fb86574f06381005d93d3517a"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `session_id` | string | 79191836ddaa0da3df76a5ffef6f07ad6ab0c641 |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "session_id": "79191836ddaa0da3df76a5ffef6f07ad6ab0c641"
}
```

</details>


#### Authentication — Create Request Token

```
GET https://api.themoviedb.org/3/authentication/token/new
```

Operation id: `authentication-create-request-token`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `expires_at` | string | 2016-08-26 17:04:39 UTC |
| `request_token` | string | ff5c7eeb5a8870efe3cd7fc5c282cffd26800ecd |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "expires_at": "2016-08-26 17:04:39 UTC",
  "request_token": "ff5c7eeb5a8870efe3cd7fc5c282cffd26800ecd"
}
```

</details>


#### Authentication — Create Session (with login)

```
POST https://api.themoviedb.org/3/authentication/token/validate_with_login
```

This method allows an application to validate a request token by entering a username and password.

Operation id: `authentication-create-session-from-login`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "username": "johnny_appleseed",
  "password": "test123",
  "request_token": "1531f1a558c8357ce8990cf887ff196e8f5402ec"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `success` | boolean | true |
| `expires_at` | string | 2018-07-24 04:10:26 UTC |
| `request_token` | string | 1531f1a558c8357ce8990cf887ff196e8f5402ec |

<details><summary>Example response</summary>

```json
{
  "success": true,
  "expires_at": "2018-07-24 04:10:26 UTC",
  "request_token": "1531f1a558c8357ce8990cf887ff196e8f5402ec"
}
```

</details>



### Guest sessions

#### Guest sessions — Rated Movies

```
GET https://api.themoviedb.org/3/guest_session/{guest_session_id}/rated/movies
```

Get the rated movies for a guest session.

Operation id: `guest-session-rated-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `guest_session_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /ikR2qy9xJCHX7M8i5rcvuNfdYXs.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 16 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Dancer in the Dark |
| `results[].overview` | string | Selma, a Czech immigrant on the verge of bl… |
| `results[].popularity` | number | 14.684 |
| `results[].poster_path` | string | /8Wdd3fQfbbQeoSfWpHrDfaFNhBU.jpg |
| `results[].release_date` | string | 2000-06-30 |
| `results[].title` | string | Dancer in the Dark |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.885 |
| `results[].vote_count` | integer | 1549 |
| `results[].rating` | number | 8.5 |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/ikR2qy9xJCHX7M8i5rcvuNfdYXs.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 16,
      "original_language": "en",
      "original_title": "Dancer in the Dark",
      "overview": "Selma, a Czech immigrant on the verge of blindness, struggles to make ends meet for herself and her son, who has inherited the same genetic disorder and will suffer the same fate without an expensive operation. When life gets too difficu...",
      "popularity": 14.684,
      "poster_path": "/8Wdd3fQfbbQeoSfWpHrDfaFNhBU.jpg",
      "release_date": "2000-06-30",
      "title": "Dancer in the Dark",
      "video": false,
      "vote_average": 7.885,
      "vote_count": 1549,
      "rating": 8.5
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>


#### Guest sessions — Rated TV

```
GET https://api.themoviedb.org/3/guest_session/{guest_session_id}/rated/tv
```

Get the rated TV shows for a guest session.

Operation id: `guest-session-rated-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `guest_session_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1399 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Game of Thrones |
| `results[].overview` | string | Seven noble families fight for control of t… |
| `results[].popularity` | number | 404.299 |
| `results[].poster_path` | string | /7WUHnWGx5OO145IRxPDUkQSh4C7.jpg |
| `results[].first_air_date` | string | 2011-04-17 |
| `results[].name` | string | Game of Thrones |
| `results[].vote_average` | number | 8.436 |
| `results[].vote_count` | integer | 21025 |
| `results[].rating` | number | 8.5 |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
      "genre_ids": [
        10765,
        18,
        10759
      ],
      "id": 1399,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Game of Thrones",
      "overview": "Seven noble families fight for control of the mythical land of Westeros. Friction between the houses leads to full-scale war. All while a very ancient evil awakens in the farthest north. Amidst the war, a neglected military order of misf...",
      "popularity": 404.299,
      "poster_path": "/7WUHnWGx5OO145IRxPDUkQSh4C7.jpg",
      "first_air_date": "2011-04-17",
      "name": "Game of Thrones",
      "vote_average": 8.436,
      "vote_count": 21025,
      "rating": 8.5
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>


#### Guest sessions — Rated TV Episodes

```
GET https://api.themoviedb.org/3/guest_session/{guest_session_id}/rated/tv/episodes
```

Get the rated TV episodes for a guest session.

Operation id: `guest-session-rated-tv-episodes`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `guest_session_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].air_date` | string | 2011-04-17 |
| `results[].episode_number` | integer | 1 |
| `results[].id` | integer | 63056 |
| `results[].name` | string | Winter Is Coming |
| `results[].overview` | string | Jon Arryn, the Hand of the King, is dead. K… |
| `results[].production_code` | string | 101 |
| `results[].runtime` | integer | 62 |
| `results[].season_number` | integer | 1 |
| `results[].show_id` | integer | 1399 |
| `results[].still_path` | string | /9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg |
| `results[].vote_average` | number | 7.843 |
| `results[].vote_count` | integer | 286 |
| `results[].rating` | number | 8.5 |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "air_date": "2011-04-17",
      "episode_number": 1,
      "id": 63056,
      "name": "Winter Is Coming",
      "overview": "Jon Arryn, the Hand of the King, is dead. King Robert Baratheon plans to ask his oldest friend, Eddard Stark, to take Jon's place. Across the sea, Viserys Targaryen plans to wed his sister to a nomadic warlord in exchange for an army.",
      "production_code": "101",
      "runtime": 62,
      "season_number": 1,
      "show_id": 1399,
      "still_path": "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
      "vote_average": 7.843,
      "vote_count": 286,
      "rating": 8.5
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>



### Trending

#### Trending — All

```
GET https://api.themoviedb.org/3/trending/all/{time_window}
```

Get the trending movies, TV shows and people.

Operation id: `trending-all`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `time_window` | path | string | yes | `day` | One of: `day`, `week` |
| `language` | query | string | - | `en-US` | `ISO-639-1`-`ISO-3166-1` code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /44immBwzhDVyjn87b3x3l9mlhAD.jpg |
| `results[].id` | integer | 934433 |
| `results[].title` | string | Scream VI |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Scream VI |
| `results[].overview` | string | Following the latest Ghostface killings, th… |
| `results[].poster_path` | string | /wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 609.941 |
| `results[].release_date` | string | 2023-03-08 |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.374 |
| `results[].vote_count` | integer | 684 |
| `total_pages` | integer | 1000 |
| `total_results` | integer | 20000 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/44immBwzhDVyjn87b3x3l9mlhAD.jpg",
      "id": 934433,
      "title": "Scream VI",
      "original_language": "en",
      "original_title": "Scream VI",
      "overview": "Following the latest Ghostface killings, the four survivors leave Woodsboro behind and start a fresh chapter.",
      "poster_path": "/wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg",
      "media_type": "movie",
      "genre_ids": [
        27,
        9648,
        53
      ],
      "popularity": 609.941,
      "release_date": "2023-03-08",
      "video": false,
      "vote_average": 7.374,
      "vote_count": 684
    },
    {
      "adult": false,
      "backdrop_path": "/b9UCfDzwiWw7mIFsIQR9ZJUeh7q.jpg",
      "id": 868759,
      "title": "Ghosted",
      "original_language": "en",
      "original_title": "Ghosted",
      "overview": "Salt-of-the-earth Cole falls head over heels for enigmatic Sadie — but then makes the shocking discovery that she’s a secret agent. Before they can decide on a second date, Cole and Sadie are swept away on an international adventure to s...",
      "poster_path": "/liLN69YgoovHVgmlHJ876PKi5Yi.jpg",
      "media_type": "movie",
      "genre_ids": [
        10749,
        28,
        35
      ],
      "popularity": 619.83,
      "release_date": "2023-04-21",
      "video": false,
      "vote_average": 7.318,
      "vote_count": 359
    },
    "… 18 more items"
  ],
  "total_pages": 1000,
  "total_results": 20000
}
```

</details>


#### Trending — Movies

```
GET https://api.themoviedb.org/3/trending/movie/{time_window}
```

Get the trending movies on TMDB.

Operation id: `trending-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `time_window` | path | string | yes | `day` | One of: `day`, `week` |
| `language` | query | string | - | `en-US` | `ISO-639-1`-`ISO-3166-1` code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /44immBwzhDVyjn87b3x3l9mlhAD.jpg |
| `results[].id` | integer | 934433 |
| `results[].title` | string | Scream VI |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Scream VI |
| `results[].overview` | string | Following the latest Ghostface killings, th… |
| `results[].poster_path` | string | /wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 609.941 |
| `results[].release_date` | string | 2023-03-08 |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.374 |
| `results[].vote_count` | integer | 684 |
| `total_pages` | integer | 1000 |
| `total_results` | integer | 20000 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/44immBwzhDVyjn87b3x3l9mlhAD.jpg",
      "id": 934433,
      "title": "Scream VI",
      "original_language": "en",
      "original_title": "Scream VI",
      "overview": "Following the latest Ghostface killings, the four survivors leave Woodsboro behind and start a fresh chapter.",
      "poster_path": "/wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg",
      "media_type": "movie",
      "genre_ids": [
        27,
        9648,
        53
      ],
      "popularity": 609.941,
      "release_date": "2023-03-08",
      "video": false,
      "vote_average": 7.374,
      "vote_count": 684
    },
    {
      "adult": false,
      "backdrop_path": "/b9UCfDzwiWw7mIFsIQR9ZJUeh7q.jpg",
      "id": 868759,
      "title": "Ghosted",
      "original_language": "en",
      "original_title": "Ghosted",
      "overview": "Salt-of-the-earth Cole falls head over heels for enigmatic Sadie — but then makes the shocking discovery that she’s a secret agent. Before they can decide on a second date, Cole and Sadie are swept away on an international adventure to s...",
      "poster_path": "/liLN69YgoovHVgmlHJ876PKi5Yi.jpg",
      "media_type": "movie",
      "genre_ids": [
        10749,
        28,
        35
      ],
      "popularity": 619.83,
      "release_date": "2023-04-21",
      "video": false,
      "vote_average": 7.318,
      "vote_count": 359
    },
    "… 18 more items"
  ],
  "total_pages": 1000,
  "total_results": 20000
}
```

</details>


#### Trending — People

```
GET https://api.themoviedb.org/3/trending/person/{time_window}
```

Get the trending people on TMDB.

Operation id: `trending-people`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `time_window` | path | string | yes | `day` | One of: `day`, `week` |
| `language` | query | string | - | `en-US` | `ISO-639-1`-`ISO-3166-1` code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].id` | integer | 224513 |
| `results[].name` | string | Ana de Armas |
| `results[].original_name` | string | Ana de Armas |
| `results[].media_type` | string | person |
| `results[].popularity` | number | 349.766 |
| `results[].gender` | integer | 1 |
| `results[].known_for_department` | string | Acting |
| `results[].profile_path` | string | /3vxvsmYLTf4jnr163SUlBIw51ee.jpg |
| `results[].known_for` | array<object> | - |
| `results[].known_for[].adult` | boolean | false |
| `results[].known_for[].backdrop_path` | string | /ilRyazdMJwN05exqhwK4tMKBYZs.jpg |
| `results[].known_for[].id` | integer | 335984 |
| `results[].known_for[].title` | string | Blade Runner 2049 |
| `results[].known_for[].original_language` | string | en |
| `results[].known_for[].original_title` | string | Blade Runner 2049 |
| `results[].known_for[].overview` | string | Thirty years after the events of the first … |
| `results[].known_for[].poster_path` | string | /gajva2L0rPYkEWjzgFlBXCAVBE5.jpg |
| `results[].known_for[].media_type` | string | movie |
| `results[].known_for[].genre_ids` | array<integer> | - |
| `results[].known_for[].popularity` | number | 79.571 |
| `results[].known_for[].release_date` | string | 2017-10-04 |
| `results[].known_for[].video` | boolean | false |
| `results[].known_for[].vote_average` | number | 7.531 |
| `results[].known_for[].vote_count` | integer | 11771 |
| `total_pages` | integer | 1000 |
| `total_results` | integer | 20000 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "id": 224513,
      "name": "Ana de Armas",
      "original_name": "Ana de Armas",
      "media_type": "person",
      "popularity": 349.766,
      "gender": 1,
      "known_for_department": "Acting",
      "profile_path": "/3vxvsmYLTf4jnr163SUlBIw51ee.jpg",
      "known_for": [
        {
          "adult": false,
          "backdrop_path": "/ilRyazdMJwN05exqhwK4tMKBYZs.jpg",
          "id": 335984,
          "title": "Blade Runner 2049",
          "original_language": "en",
          "original_title": "Blade Runner 2049",
          "overview": "Thirty years after the events of the first film, a new blade runner, LAPD Officer K, unearths a long-buried secret that has the potential to plunge what's left of society into chaos. K's discovery leads him on a quest to find Rick Deckar...",
          "poster_path": "/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
          "media_type": "movie",
          "genre_ids": [
            878,
            18
          ],
          "popularity": 79.571,
          "release_date": "2017-10-04",
          "video": false,
          "vote_average": 7.531,
          "vote_count": 11771
        },
        "… 2 more items"
      ]
    },
    "… 19 more items"
  ],
  "total_pages": 1000,
  "total_results": 20000
}
```

</details>


#### Trending — TV

```
GET https://api.themoviedb.org/3/trending/tv/{time_window}
```

Get the trending TV shows on TMDB.

Operation id: `trending-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `time_window` | path | string | yes | `day` | One of: `day`, `week` |
| `language` | query | string | - | `en-US` | `ISO-639-1`-`ISO-3166-1` code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /8P15FsYcTwQZ4G5rRMd1TKD14Aq.jpg |
| `results[].id` | integer | 103768 |
| `results[].name` | string | Sweet Tooth |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Sweet Tooth |
| `results[].overview` | string | On a perilous adventure across a post-apoca… |
| `results[].poster_path` | string | /dBxxtfhC4vYrxB2fLsSxOTY2dQc.jpg |
| `results[].media_type` | string | tv |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 137.498 |
| `results[].first_air_date` | string | 2021-06-04 |
| `results[].vote_average` | number | 7.928 |
| `results[].vote_count` | integer | 1094 |
| `results[].origin_country` | array<string> | - |
| `total_pages` | integer | 1000 |
| `total_results` | integer | 20000 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/8P15FsYcTwQZ4G5rRMd1TKD14Aq.jpg",
      "id": 103768,
      "name": "Sweet Tooth",
      "original_language": "en",
      "original_name": "Sweet Tooth",
      "overview": "On a perilous adventure across a post-apocalyptic world, a lovable boy who's half-human and half-deer searches for a new beginning with a gruff protector.",
      "poster_path": "/dBxxtfhC4vYrxB2fLsSxOTY2dQc.jpg",
      "media_type": "tv",
      "genre_ids": [
        18,
        10765
      ],
      "popularity": 137.498,
      "first_air_date": "2021-06-04",
      "vote_average": 7.928,
      "vote_count": 1094,
      "origin_country": [
        "US"
      ]
    },
    {
      "adult": false,
      "backdrop_path": "/pGtvfzfIRCsiudAWlzlGZ74co5l.jpg",
      "id": 124800,
      "name": "Love & Death",
      "original_language": "en",
      "original_name": "Love & Death",
      "overview": "The true story of Candy and Pat Montgomery and Betty and Allan Gore – two churchgoing couples enjoying their small town Texas life… until an extramarital affair leads somebody to pick up an axe.",
      "poster_path": "/8WchbT0UPQeo3PZ6G01fhccYaQB.jpg",
      "media_type": "tv",
      "genre_ids": [
        80,
        18
      ],
      "popularity": 72.051,
      "first_air_date": "2023-04-27",
      "vote_average": 7.2,
      "vote_count": 4,
      "origin_country": [
        "US"
      ]
    },
    "… 18 more items"
  ],
  "total_pages": 1000,
  "total_results": 20000
}
```

</details>



### Movies

#### Movies — Movie List

```
GET https://api.themoviedb.org/3/movie/changes
```

Get a list of all of the movie ids that have been changed in the past 24 hours.

Operation id: `changes-movie-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `end_date` | query | string (date) | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string (date) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].id` | integer | 1120293 |
| `results[].adult` | boolean | false |
| `page` | integer | 3 |
| `total_pages` | integer | 57 |
| `total_results` | integer | 5700 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "id": 1120293,
      "adult": false
    },
    {
      "id": 3686,
      "adult": false
    },
    "… 98 more items"
  ],
  "page": 3,
  "total_pages": 57,
  "total_results": 5700
}
```

</details>


#### Movies — Latest

```
GET https://api.themoviedb.org/3/movie/latest
```

Get the newest movie ID.

Operation id: `movie-latest-id`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `backdrop_path` | object | - |
| `belongs_to_collection` | object | - |
| `budget` | integer | 0 |
| `genres` | array<object> | - |
| `homepage` | string | - |
| `id` | integer | 1119232 |
| `imdb_id` | object | - |
| `original_language` | string | fr |
| `original_title` | string | König Charles III |
| `overview` | string | - |
| `popularity` | integer | 0 |
| `poster_path` | object | - |
| `production_companies` | array<object> | - |
| `production_countries` | array<object> | - |
| `release_date` | string | - |
| `revenue` | integer | 0 |
| `runtime` | integer | 0 |
| `spoken_languages` | array<object> | - |
| `status` | string | Released |
| `tagline` | string | - |
| `title` | string | König Charles III |
| `video` | boolean | false |
| `vote_average` | integer | 0 |
| `vote_count` | integer | 0 |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "backdrop_path": null,
  "belongs_to_collection": null,
  "budget": 0,
  "genres": [],
  "homepage": "",
  "id": 1119232,
  "imdb_id": null,
  "original_language": "fr",
  "original_title": "König Charles III",
  "overview": "",
  "popularity": 0.0,
  "poster_path": null,
  "production_companies": [],
  "production_countries": [],
  "release_date": "",
  "revenue": 0,
  "runtime": 0,
  "spoken_languages": [],
  "status": "Released",
  "tagline": "",
  "title": "König Charles III",
  "video": false,
  "vote_average": 0.0,
  "vote_count": 0
}
```

</details>


#### Movies — Now Playing

```
GET https://api.themoviedb.org/3/movie/now_playing
```

Get a list of movies that are currently in theatres.

Operation id: `movie-now-playing-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | ISO-3166-1 code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `dates` | object | - |
| `dates.maximum` | string | 2023-05-03 |
| `dates.minimum` | string | 2023-03-16 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /iJQIbOPm81fPEGKt5BPuZmfnA54.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 502356 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Super Mario Bros. Movie |
| `results[].overview` | string | While working underground to fix a water ma… |
| `results[].popularity` | number | 6572.614 |
| `results[].poster_path` | string | /qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg |
| `results[].release_date` | string | 2023-04-05 |
| `results[].title` | string | The Super Mario Bros. Movie |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.5 |
| `results[].vote_count` | integer | 1456 |
| `total_pages` | integer | 87 |
| `total_results` | integer | 1734 |

<details><summary>Example response</summary>

```json
{
  "dates": {
    "maximum": "2023-05-03",
    "minimum": "2023-03-16"
  },
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/iJQIbOPm81fPEGKt5BPuZmfnA54.jpg",
      "genre_ids": [
        16,
        12,
        10751,
        14,
        35
      ],
      "id": 502356,
      "original_language": "en",
      "original_title": "The Super Mario Bros. Movie",
      "overview": "While working underground to fix a water main, Brooklyn plumbers—and brothers—Mario and Luigi are transported down a mysterious pipe and wander into a magical new world. But when the brothers are separated, Mario embarks on an epic quest...",
      "popularity": 6572.614,
      "poster_path": "/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg",
      "release_date": "2023-04-05",
      "title": "The Super Mario Bros. Movie",
      "video": false,
      "vote_average": 7.5,
      "vote_count": 1456
    },
    {
      "adult": false,
      "backdrop_path": "/nDxJJyA5giRhXx96q1sWbOUjMBI.jpg",
      "genre_ids": [
        28,
        35,
        14
      ],
      "id": 594767,
      "original_language": "en",
      "original_title": "Shazam! Fury of the Gods",
      "overview": "Billy Batson and his foster siblings, who transform into superheroes by saying \"Shazam!\", are forced to get back into action and fight the Daughters of Atlas, who they must stop from using a weapon that could destroy the world.",
      "popularity": 4274.232,
      "poster_path": "/2VK4d3mqqTc7LVZLnLPeRiPaJ71.jpg",
      "release_date": "2023-03-15",
      "title": "Shazam! Fury of the Gods",
      "video": false,
      "vote_average": 6.9,
      "vote_count": 1231
    },
    "… 18 more items"
  ],
  "total_pages": 87,
  "total_results": 1734
}
```

</details>


#### Movies — Popular

```
GET https://api.themoviedb.org/3/movie/popular
```

Get a list of movies ordered by popularity.

Operation id: `movie-popular-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | ISO-3166-1 code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /gMJngTNfaqCSCqGD4y8lVMZXKDn.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 640146 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].overview` | string | Super-Hero partners Scott Lang and Hope van… |
| `results[].popularity` | number | 8567.865 |
| `results[].poster_path` | string | /ngl2FKBlU4fhbdsrtdom9LVLBXw.jpg |
| `results[].release_date` | string | 2023-02-15 |
| `results[].title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 6.5 |
| `results[].vote_count` | integer | 1886 |
| `total_pages` | integer | 38029 |
| `total_results` | integer | 760569 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/gMJngTNfaqCSCqGD4y8lVMZXKDn.jpg",
      "genre_ids": [
        28,
        12,
        878
      ],
      "id": 640146,
      "original_language": "en",
      "original_title": "Ant-Man and the Wasp: Quantumania",
      "overview": "Super-Hero partners Scott Lang and Hope van Dyne, along with with Hope's parents Janet van Dyne and Hank Pym, and Scott's daughter Cassie Lang, find themselves exploring the Quantum Realm, interacting with strange new creatures and embar...",
      "popularity": 8567.865,
      "poster_path": "/ngl2FKBlU4fhbdsrtdom9LVLBXw.jpg",
      "release_date": "2023-02-15",
      "title": "Ant-Man and the Wasp: Quantumania",
      "video": false,
      "vote_average": 6.5,
      "vote_count": 1886
    },
    {
      "adult": false,
      "backdrop_path": "/iJQIbOPm81fPEGKt5BPuZmfnA54.jpg",
      "genre_ids": [
        16,
        12,
        10751,
        14,
        35
      ],
      "id": 502356,
      "original_language": "en",
      "original_title": "The Super Mario Bros. Movie",
      "overview": "While working underground to fix a water main, Brooklyn plumbers—and brothers—Mario and Luigi are transported down a mysterious pipe and wander into a magical new world. But when the brothers are separated, Mario embarks on an epic quest...",
      "popularity": 6572.614,
      "poster_path": "/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg",
      "release_date": "2023-04-05",
      "title": "The Super Mario Bros. Movie",
      "video": false,
      "vote_average": 7.5,
      "vote_count": 1456
    },
    "… 18 more items"
  ],
  "total_pages": 38029,
  "total_results": 760569
}
```

</details>


#### Movies — Top Rated

```
GET https://api.themoviedb.org/3/movie/top_rated
```

Get a list of movies ordered by rating.

Operation id: `movie-top-rated-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | ISO-3166-1 code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /tmU7GeKVybMWFButWEGl2M4GeiP.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 238 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Godfather |
| `results[].overview` | string | Spanning the years 1945 to 1955, a chronicl… |
| `results[].popularity` | number | 100.932 |
| `results[].poster_path` | string | /3bhkrj58Vtu7enYsRolD1fZdja1.jpg |
| `results[].release_date` | string | 1972-03-14 |
| `results[].title` | string | The Godfather |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.7 |
| `results[].vote_count` | integer | 17806 |
| `total_pages` | integer | 552 |
| `total_results` | integer | 11032 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 238,
      "original_language": "en",
      "original_title": "The Godfather",
      "overview": "Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family. When organized crime family patriarch, Vito Corleone barely survives an attempt on his life, his youngest son, Michael steps in to take...",
      "popularity": 100.932,
      "poster_path": "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
      "release_date": "1972-03-14",
      "title": "The Godfather",
      "video": false,
      "vote_average": 8.7,
      "vote_count": 17806
    },
    {
      "adult": false,
      "backdrop_path": "/kXfqcdQKsToO0OUXHcrrNCHDBzO.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 278,
      "original_language": "en",
      "original_title": "The Shawshank Redemption",
      "overview": "Framed in the 1940s for the double murder of his wife and her lover, upstanding banker Andy Dufresne begins a new life at the Shawshank prison, where he puts his accounting skills to work for an amoral warden. During his long stretch in ...",
      "popularity": 98.25,
      "poster_path": "/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg",
      "release_date": "1994-09-23",
      "title": "The Shawshank Redemption",
      "video": false,
      "vote_average": 8.7,
      "vote_count": 23656
    },
    "… 18 more items"
  ],
  "total_pages": 552,
  "total_results": 11032
}
```

</details>


#### Movies — Upcoming

```
GET https://api.themoviedb.org/3/movie/upcoming
```

Get a list of movies that are being released soon.

Operation id: `movie-upcoming-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | ISO-3166-1 code |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `dates` | object | - |
| `dates.maximum` | string | 2023-05-23 |
| `dates.minimum` | string | 2023-05-04 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /7bWxAsNPv9CXHOhZbJVlj2KxgfP.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 713704 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Evil Dead Rise |
| `results[].overview` | string | Two sisters find an ancient vinyl that give… |
| `results[].popularity` | number | 1696.367 |
| `results[].poster_path` | string | /mIBCtPvKZQlxubxKMeViO2UrP3q.jpg |
| `results[].release_date` | string | 2023-04-12 |
| `results[].title` | string | Evil Dead Rise |
| `results[].video` | boolean | false |
| `results[].vote_average` | integer | 7 |
| `results[].vote_count` | integer | 207 |
| `total_pages` | integer | 19 |
| `total_results` | integer | 369 |

<details><summary>Example response</summary>

```json
{
  "dates": {
    "maximum": "2023-05-23",
    "minimum": "2023-05-04"
  },
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/7bWxAsNPv9CXHOhZbJVlj2KxgfP.jpg",
      "genre_ids": [
        27,
        53
      ],
      "id": 713704,
      "original_language": "en",
      "original_title": "Evil Dead Rise",
      "overview": "Two sisters find an ancient vinyl that gives birth to bloodthirsty demons that run amok in a Los Angeles apartment building and thrusts them into a primal battle for survival as they face the most nightmarish version of family imaginable.",
      "popularity": 1696.367,
      "poster_path": "/mIBCtPvKZQlxubxKMeViO2UrP3q.jpg",
      "release_date": "2023-04-12",
      "title": "Evil Dead Rise",
      "video": false,
      "vote_average": 7,
      "vote_count": 207
    },
    {
      "adult": false,
      "backdrop_path": "/5Y5pz0NX7SZS9036I733F7uNcwK.jpg",
      "genre_ids": [
        27,
        53
      ],
      "id": 758323,
      "original_language": "en",
      "original_title": "The Pope's Exorcist",
      "overview": "Father Gabriele Amorth, Chief Exorcist of the Vatican, investigates a young boy's terrifying possession and ends up uncovering a centuries-old conspiracy the Vatican has desperately tried to keep hidden.",
      "popularity": 1073.229,
      "poster_path": "/9JBEPLTPSm0d1mbEcLxULjJq9Eh.jpg",
      "release_date": "2023-04-05",
      "title": "The Pope's Exorcist",
      "video": false,
      "vote_average": 6.5,
      "vote_count": 143
    },
    "… 18 more items"
  ],
  "total_pages": 19,
  "total_results": 369
}
```

</details>


#### Movies — Details

```
GET https://api.themoviedb.org/3/movie/{movie_id}
```

Get the top level details of a movie by ID.

Operation id: `movie-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `append_to_response` | query | string | - | - | comma separated list of endpoints within this namespace, 20 items max |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `backdrop_path` | string | /2w4xG178RpB4MDAIfTkqAuSJzec.jpg |
| `belongs_to_collection` | object | - |
| `belongs_to_collection.id` | integer | 10 |
| `belongs_to_collection.name` | string | Star Wars Collection |
| `belongs_to_collection.poster_path` | string | /pWVLFh4OuejTpUaDQbB1C4zoS2p.jpg |
| `belongs_to_collection.backdrop_path` | string | /iY2ujEY2m68OTTlPFTiHub9joHS.jpg |
| `budget` | integer | 11000000 |
| `genres` | array<object> | - |
| `genres[].id` | integer | 12 |
| `genres[].name` | string | Adventure |
| `homepage` | string | http://www.starwars.com/films/star-wars-epi… |
| `id` | integer | 11 |
| `imdb_id` | string | tt0076759 |
| `origin_country` | array<string> | - |
| `original_language` | string | en |
| `original_title` | string | Star Wars |
| `overview` | string | Princess Leia is captured and held hostage … |
| `popularity` | number | 20.6912 |
| `poster_path` | string | /6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg |
| `production_companies` | array<object> | - |
| `production_companies[].id` | integer | 1 |
| `production_companies[].logo_path` | string | /tlVSws0RvvtPBwViUyOFAO0vcQS.png |
| `production_companies[].name` | string | Lucasfilm Ltd. |
| `production_companies[].origin_country` | string | US |
| `production_countries` | array<object> | - |
| `production_countries[].iso_3166_1` | string | US |
| `production_countries[].name` | string | United States of America |
| `release_date` | string | 1977-05-25 |
| `revenue` | integer | 775398007 |
| `runtime` | integer | 121 |
| `spoken_languages` | array<object> | - |
| `spoken_languages[].english_name` | string | English |
| `spoken_languages[].iso_639_1` | string | en |
| `spoken_languages[].name` | string | English |
| `status` | string | Released |
| `tagline` | string | A long time ago in a galaxy far, far away... |
| `title` | string | Star Wars |
| `video` | boolean | false |
| `vote_average` | number | 8.2 |
| `vote_count` | integer | 22061 |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "backdrop_path": "/2w4xG178RpB4MDAIfTkqAuSJzec.jpg",
  "belongs_to_collection": {
    "id": 10,
    "name": "Star Wars Collection",
    "poster_path": "/pWVLFh4OuejTpUaDQbB1C4zoS2p.jpg",
    "backdrop_path": "/iY2ujEY2m68OTTlPFTiHub9joHS.jpg"
  },
  "budget": 11000000,
  "genres": [
    {
      "id": 12,
      "name": "Adventure"
    },
    {
      "id": 28,
      "name": "Action"
    },
    "… 1 more items"
  ],
  "homepage": "http://www.starwars.com/films/star-wars-episode-iv-a-new-hope",
  "id": 11,
  "imdb_id": "tt0076759",
  "origin_country": [
    "US"
  ],
  "original_language": "en",
  "original_title": "Star Wars",
  "overview": "Princess Leia is captured and held hostage by the evil Imperial forces in their effort to take over the galactic Empire. Venturesome Luke Skywalker and dashing captain Han Solo team together with the loveable robot duo R2-D2 and C-3PO to...",
  "popularity": 20.6912,
  "poster_path": "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg",
  "production_companies": [
    {
      "id": 1,
      "logo_path": "/tlVSws0RvvtPBwViUyOFAO0vcQS.png",
      "name": "Lucasfilm Ltd.",
      "origin_country": "US"
    },
    {
      "id": 25,
      "logo_path": "/qZCc1lty5FzX30aOCVRBLzaVmcp.png",
      "name": "20th Century Fox",
      "origin_country": "US"
    }
  ],
  "production_countries": [
    {
      "iso_3166_1": "US",
      "name": "United States of America"
    }
  ],
  "release_date": "1977-05-25",
  "revenue": 775398007,
  "runtime": 121,
  "spoken_languages": [
    {
      "english_name": "English",
      "iso_639_1": "en",
      "name": "English"
    }
  ],
  "status": "Released",
  "tagline": "A long time ago in a galaxy far, far away...",
  "title": "Star Wars",
  "video": false,
  "vote_average": 8.2,
  "vote_count": 22061
}
```

</details>


#### Movies — Account States

```
GET https://api.themoviedb.org/3/movie/{movie_id}/account_states
```

Get the rating, watchlist and favourite status of an account.

Operation id: `movie-account-states`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |
| `guest_session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `favorite` | boolean | true |
| `rated` | object | - |
| `rated.value` | integer | 9 |
| `watchlist` | boolean | false |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "favorite": true,
  "rated": {
    "value": 9.0
  },
  "watchlist": false
}
```

</details>


#### Movies — Alternative Titles

```
GET https://api.themoviedb.org/3/movie/{movie_id}/alternative_titles
```

Get the alternative titles for a movie.

Operation id: `movie-alternative-titles`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `country` | query | string | - | - | specify a ISO-3166-1 value to filter the results |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `titles` | array<object> | - |
| `titles[].iso_3166_1` | string | RS |
| `titles[].title` | string | Borilački klub |
| `titles[].type` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "titles": [
    {
      "iso_3166_1": "RS",
      "title": "Borilački klub",
      "type": ""
    },
    {
      "iso_3166_1": "IL",
      "title": "Mo'adon Krav",
      "type": "romanization"
    },
    "… 10 more items"
  ]
}
```

</details>


#### Movies — Changes

```
GET https://api.themoviedb.org/3/movie/{movie_id}/changes
```

Get the recent changes for a movie.

Operation id: `movie-changes`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `end_date` | query | string (date) | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string (date) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `changes` | array<object> | - |
| `changes[].key` | string | images |
| `changes[].items` | array<object> | - |
| `changes[].items[].id` | string | 643197b96dea3a00d4377270 |
| `changes[].items[].action` | string | added |
| `changes[].items[].time` | string | 2023-04-08 16:35:05 UTC |
| `changes[].items[].iso_639_1` | string | - |
| `changes[].items[].iso_3166_1` | string | - |
| `changes[].items[].value` | object | - |
| `changes[].items[].value.poster` | object | - |
| `changes[].items[].value.poster.file_path` | string | /s9ZrHprviFCx3azfWNBtt1LPSnL.jpg |

<details><summary>Example response</summary>

```json
{
  "changes": [
    {
      "key": "images",
      "items": [
        {
          "id": "643197b96dea3a00d4377270",
          "action": "added",
          "time": "2023-04-08 16:35:05 UTC",
          "iso_639_1": "",
          "iso_3166_1": "",
          "value": {
            "poster": {
              "file_path": "/s9ZrHprviFCx3azfWNBtt1LPSnL.jpg"
            }
          }
        },
        {
          "id": "643197bae92d8300f46c92f5",
          "action": "added",
          "time": "2023-04-08 16:35:06 UTC",
          "iso_639_1": "",
          "iso_3166_1": "",
          "value": {
            "poster": {
              "file_path": "/xQjGTWyNHXqqACOanXwOk1mOnxU.jpg"
            }
          }
        },
        "… 4 more items"
      ]
    },
    {
      "key": "plot_keywords",
      "items": [
        {
          "id": "6431bb5d8289a000b73bd64d",
          "action": "added",
          "time": "2023-04-08 19:07:09 UTC",
          "iso_639_1": "",
          "iso_3166_1": "",
          "value": {
            "name": "breaking the fourth wall",
            "id": 11687
          }
        }
      ]
    }
  ]
}
```

</details>


#### Movies — Credits

```
GET https://api.themoviedb.org/3/movie/{movie_id}/credits
```

Operation id: `movie-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 2 |
| `cast[].id` | integer | 819 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Edward Norton |
| `cast[].original_name` | string | Edward Norton |
| `cast[].popularity` | number | 26.99 |
| `cast[].profile_path` | string | /8nytsqL59SFJTVYVrN72k6qkGgJ.jpg |
| `cast[].cast_id` | integer | 4 |
| `cast[].character` | string | The Narrator |
| `cast[].credit_id` | string | 52fe4250c3a36847f80149f3 |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 2 |
| `crew[].id` | integer | 376 |
| `crew[].known_for_department` | string | Production |
| `crew[].name` | string | Arnon Milchan |
| `crew[].original_name` | string | Arnon Milchan |
| `crew[].popularity` | number | 2.931 |
| `crew[].profile_path` | string | /b2hBExX4NnczNAnLuTBF4kmNhZm.jpg |
| `crew[].credit_id` | string | 55731b8192514111610027d7 |
| `crew[].department` | string | Production |
| `crew[].job` | string | Executive Producer |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 819,
      "known_for_department": "Acting",
      "name": "Edward Norton",
      "original_name": "Edward Norton",
      "popularity": 26.99,
      "profile_path": "/8nytsqL59SFJTVYVrN72k6qkGgJ.jpg",
      "cast_id": 4,
      "character": "The Narrator",
      "credit_id": "52fe4250c3a36847f80149f3",
      "order": 0
    },
    {
      "adult": false,
      "gender": 2,
      "id": 287,
      "known_for_department": "Acting",
      "name": "Brad Pitt",
      "original_name": "Brad Pitt",
      "popularity": 45.202,
      "profile_path": "/huV2cdcolEUwJy37QvH914vup7d.jpg",
      "cast_id": 5,
      "character": "Tyler Durden",
      "credit_id": "52fe4250c3a36847f80149f7",
      "order": 1
    },
    "… 74 more items"
  ],
  "crew": [
    {
      "adult": false,
      "gender": 2,
      "id": 376,
      "known_for_department": "Production",
      "name": "Arnon Milchan",
      "original_name": "Arnon Milchan",
      "popularity": 2.931,
      "profile_path": "/b2hBExX4NnczNAnLuTBF4kmNhZm.jpg",
      "credit_id": "55731b8192514111610027d7",
      "department": "Production",
      "job": "Executive Producer"
    },
    {
      "adult": false,
      "gender": 2,
      "id": 605,
      "known_for_department": "Costume & Make-Up",
      "name": "Michael Kaplan",
      "original_name": "Michael Kaplan",
      "popularity": 4.294,
      "profile_path": "/bNarnI5K4XYIKaHsX6HAitllyQr.jpg",
      "credit_id": "5894c4eac3a3685ec6000218",
      "department": "Costume & Make-Up",
      "job": "Costume Design"
    },
    "… 173 more items"
  ]
}
```

</details>


#### Movies — External IDs

```
GET https://api.themoviedb.org/3/movie/{movie_id}/external_ids
```

Operation id: `movie-external-ids`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `imdb_id` | string | tt0137523 |
| `wikidata_id` | object | - |
| `facebook_id` | string | FightClub |
| `instagram_id` | object | - |
| `twitter_id` | object | - |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "imdb_id": "tt0137523",
  "wikidata_id": null,
  "facebook_id": "FightClub",
  "instagram_id": null,
  "twitter_id": null
}
```

</details>


#### Movies — Images

```
GET https://api.themoviedb.org/3/movie/{movie_id}/images
```

Get the images that belong to a movie.

Operation id: `movie-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `include_image_language` | query | string | - | - | specify a comma separated list of ISO-639-1 values to query, for example: `en-US,null` |
| `language` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `backdrops` | array<object> | - |
| `backdrops[].aspect_ratio` | number | 1.778 |
| `backdrops[].height` | integer | 800 |
| `backdrops[].iso_639_1` | object | - |
| `backdrops[].file_path` | string | /hZkgoQYus5vegHoetLkCJzb17zJ.jpg |
| `backdrops[].vote_average` | number | 5.622 |
| `backdrops[].vote_count` | integer | 20 |
| `backdrops[].width` | integer | 1422 |
| `id` | integer | 550 |
| `logos` | array<object> | - |
| `logos[].aspect_ratio` | number | 5.203 |
| `logos[].height` | integer | 79 |
| `logos[].iso_639_1` | string | he |
| `logos[].file_path` | string | /c1KLulrIhUqY5fT42nmC5aERGCp.png |
| `logos[].vote_average` | number | 5.312 |
| `logos[].vote_count` | integer | 1 |
| `logos[].width` | integer | 411 |
| `posters` | array<object> | - |
| `posters[].aspect_ratio` | number | 0.667 |
| `posters[].height` | integer | 900 |
| `posters[].iso_639_1` | string | pt |
| `posters[].file_path` | string | /r3pPehX4ik8NLYPpbDRAh0YRtMb.jpg |
| `posters[].vote_average` | number | 5.258 |
| `posters[].vote_count` | integer | 6 |
| `posters[].width` | integer | 600 |

<details><summary>Example response</summary>

```json
{
  "backdrops": [
    {
      "aspect_ratio": 1.778,
      "height": 800,
      "iso_639_1": null,
      "file_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
      "vote_average": 5.622,
      "vote_count": 20,
      "width": 1422
    },
    {
      "aspect_ratio": 1.778,
      "height": 720,
      "iso_639_1": "en",
      "file_path": "/fygeMr16EcxJiYhdiO1LEr7iHtI.jpg",
      "vote_average": 5.318,
      "vote_count": 3,
      "width": 1280
    },
    "… 39 more items"
  ],
  "id": 550,
  "logos": [
    {
      "aspect_ratio": 5.203,
      "height": 79,
      "iso_639_1": "he",
      "file_path": "/c1KLulrIhUqY5fT42nmC5aERGCp.png",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 411
    },
    {
      "aspect_ratio": 8.502,
      "height": 235,
      "iso_639_1": "pt",
      "file_path": "/qqAcl1YIT5Sa2nx8tKQcervQCco.png",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 1998
    },
    "… 26 more items"
  ],
  "posters": [
    {
      "aspect_ratio": 0.667,
      "height": 900,
      "iso_639_1": "pt",
      "file_path": "/r3pPehX4ik8NLYPpbDRAh0YRtMb.jpg",
      "vote_average": 5.258,
      "vote_count": 6,
      "width": 600
    },
    {
      "aspect_ratio": 0.667,
      "height": 1350,
      "iso_639_1": "ru",
      "file_path": "/66RvLrRJTm4J8l3uHXWF09AICol.jpg",
      "vote_average": 5.522,
      "vote_count": 4,
      "width": 900
    },
    "… 79 more items"
  ]
}
```

</details>


#### Movies — Keywords

```
GET https://api.themoviedb.org/3/movie/{movie_id}/keywords
```

Operation id: `movie-keywords`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | string | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `keywords` | array<object> | - |
| `keywords[].id` | integer | 818 |
| `keywords[].name` | string | based on novel or book |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "keywords": [
    {
      "id": 818,
      "name": "based on novel or book"
    },
    {
      "id": 825,
      "name": "support group"
    },
    "… 13 more items"
  ]
}
```

</details>


#### Movies — Lists

```
GET https://api.themoviedb.org/3/movie/{movie_id}/lists
```

Get the lists that a movie has been added to.

Operation id: `movie-lists`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].description` | string | Movies I own |
| `results[].favorite_count` | integer | 0 |
| `results[].id` | integer | 8248696 |
| `results[].item_count` | integer | 409 |
| `results[].iso_639_1` | string | en |
| `results[].list_type` | string | movie |
| `results[].name` | string | My Movies |
| `results[].poster_path` | object | - |
| `total_pages` | integer | 122 |
| `total_results` | integer | 2422 |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "page": 1,
  "results": [
    {
      "description": "Movies I own",
      "favorite_count": 0,
      "id": 8248696,
      "item_count": 409,
      "iso_639_1": "en",
      "list_type": "movie",
      "name": "My Movies",
      "poster_path": null
    },
    {
      "description": "",
      "favorite_count": 0,
      "id": 8211179,
      "item_count": 842,
      "iso_639_1": "en",
      "list_type": "movie",
      "name": "myList",
      "poster_path": null
    },
    "… 18 more items"
  ],
  "total_pages": 122,
  "total_results": 2422
}
```

</details>


#### Movies — Add Rating

```
POST https://api.themoviedb.org/3/movie/{movie_id}/rating
```

Rate a movie and save it to your rated list.

Operation id: `movie-add-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |
| `Content-Type` | header | string | yes | `application/json;charset=utf-8` | - |

**Request body** (`application/json`)

```json
{
  "value": 8.5
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

</details>


#### Movies — Delete Rating

```
DELETE https://api.themoviedb.org/3/movie/{movie_id}/rating
```

Delete a user rating.

Operation id: `movie-delete-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `Content-Type` | header | string | - | `application/json;charset=utf-8` | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 13 |
| `status_message` | string | The item/record was deleted successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 13,
  "status_message": "The item/record was deleted successfully."
}
```

</details>


#### Movies — Recommendations

```
GET https://api.themoviedb.org/3/movie/{movie_id}/recommendations
```

Operation id: `movie-recommendations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

<details><summary>Example response</summary>

```json
{}
```

</details>


#### Movies — Release Dates

```
GET https://api.themoviedb.org/3/movie/{movie_id}/release_dates
```

Get the release dates and certifications for a movie.

Operation id: `movie-release-dates`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `results` | array<object> | - |
| `results[].iso_3166_1` | string | BG |
| `results[].release_dates` | array<object> | - |
| `results[].release_dates[].certification` | string | c |
| `results[].release_dates[].descriptors` | array<object> | - |
| `results[].release_dates[].iso_639_1` | string | - |
| `results[].release_dates[].note` | string | - |
| `results[].release_dates[].release_date` | string | 2012-08-28T00:00:00.000Z |
| `results[].release_dates[].type` | integer | 3 |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "results": [
    {
      "iso_3166_1": "BG",
      "release_dates": [
        {
          "certification": "c",
          "descriptors": [],
          "iso_639_1": "",
          "note": "",
          "release_date": "2012-08-28T00:00:00.000Z",
          "type": 3
        }
      ]
    },
    {
      "iso_3166_1": "CH",
      "release_dates": [
        {
          "certification": "18",
          "descriptors": [],
          "iso_639_1": "de",
          "note": "",
          "release_date": "1999-11-04T00:00:00.000Z",
          "type": 3
        },
        {
          "certification": "18",
          "descriptors": [],
          "iso_639_1": "fr",
          "note": "",
          "release_date": "1999-11-10T00:00:00.000Z",
          "type": 3
        }
      ]
    },
    "… 48 more items"
  ]
}
```

</details>


#### Movies — Reviews

```
GET https://api.themoviedb.org/3/movie/{movie_id}/reviews
```

Get the user reviews for a movie.

Operation id: `movie-reviews`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].author` | string | Goddard |
| `results[].author_details` | object | - |
| `results[].author_details.name` | string | - |
| `results[].author_details.username` | string | Goddard |
| `results[].author_details.avatar_path` | string | /https://secure.gravatar.com/avatar/f248ec3… |
| `results[].author_details.rating` | object | - |
| `results[].content` | string | Pretty awesome movie.  It shows what one cr… |
| `results[].created_at` | string | 2018-06-09T17:51:53.359Z |
| `results[].id` | string | 5b1c13b9c3a36848f2026384 |
| `results[].updated_at` | string | 2021-06-23T15:58:09.421Z |
| `results[].url` | string | https://www.themoviedb.org/review/5b1c13b9c… |
| `total_pages` | integer | 1 |
| `total_results` | integer | 8 |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "page": 1,
  "results": [
    {
      "author": "Goddard",
      "author_details": {
        "name": "",
        "username": "Goddard",
        "avatar_path": "/https://secure.gravatar.com/avatar/f248ec34f953bc62cafcbdd81fddd6b6.jpg",
        "rating": null
      },
      "content": "Pretty awesome movie.  It shows what one crazy person can convince other crazy people to do.  Everyone needs something to believe in.  I recommend Jesus Christ, but they want Tyler Durden.",
      "created_at": "2018-06-09T17:51:53.359Z",
      "id": "5b1c13b9c3a36848f2026384",
      "updated_at": "2021-06-23T15:58:09.421Z",
      "url": "https://www.themoviedb.org/review/5b1c13b9c3a36848f2026384"
    },
    {
      "author": "Brett Pascoe",
      "author_details": {
        "name": "Brett Pascoe",
        "username": "SneekyNuts",
        "avatar_path": "/https://secure.gravatar.com/avatar/04d45e186650672a416315dac947b3d6.jpg",
        "rating": 9.0
      },
      "content": "In my top 5 of all time favourite movies. Great story line and a movie you can watch over and over again.",
      "created_at": "2018-07-05T13:22:41.754Z",
      "id": "5b3e1ba1925141144c007f17",
      "updated_at": "2021-06-23T15:58:10.199Z",
      "url": "https://www.themoviedb.org/review/5b3e1ba1925141144c007f17"
    },
    "… 6 more items"
  ],
  "total_pages": 1,
  "total_results": 8
}
```

</details>


#### Movies — Similar

```
GET https://api.themoviedb.org/3/movie/{movie_id}/similar
```

Get the similar movies based on genres and keywords.

Operation id: `movie-similar`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /3YAldML4EDyoC6RBpzceALigrAZ.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 9300 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Orlando |
| `results[].overview` | string | England, 1600. Queen Elizabeth I promises O… |
| `results[].popularity` | number | 7.768 |
| `results[].poster_path` | string | /xvz0qZkXXMq3dH2Revxii8drxWc.jpg |
| `results[].release_date` | string | 1992-12-11 |
| `results[].title` | string | Orlando |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 6.966 |
| `results[].vote_count` | integer | 262 |
| `total_pages` | integer | 364 |
| `total_results` | integer | 7269 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/3YAldML4EDyoC6RBpzceALigrAZ.jpg",
      "genre_ids": [
        18,
        14
      ],
      "id": 9300,
      "original_language": "en",
      "original_title": "Orlando",
      "overview": "England, 1600. Queen Elizabeth I promises Orlando, a young nobleman obsessed with poetry, that she will grant him land and fortune if he agrees to satisfy a very particular request.",
      "popularity": 7.768,
      "poster_path": "/xvz0qZkXXMq3dH2Revxii8drxWc.jpg",
      "release_date": "1992-12-11",
      "title": "Orlando",
      "video": false,
      "vote_average": 6.966,
      "vote_count": 262
    },
    {
      "adult": false,
      "backdrop_path": "/4TQgxCYdaizZr9bfjQYVsZPZVGE.jpg",
      "genre_ids": [
        28,
        80,
        18,
        9648,
        53
      ],
      "id": 9311,
      "original_language": "en",
      "original_title": "Smilla's Sense of Snow",
      "overview": "Smilla Jaspersen, half Danish, half Greenlander, attempts to understand the death of a small boy who falls from the roof of her apartment building. Suspecting wrongdoing, Smilla uncovers a trail of clues leading towards a secretive corpo...",
      "popularity": 11.958,
      "poster_path": "/3eeCYGVWb4Ic8SFpARGWOjyl9Md.jpg",
      "release_date": "1997-02-28",
      "title": "Smilla's Sense of Snow",
      "video": false,
      "vote_average": 6.351,
      "vote_count": 202
    },
    "… 18 more items"
  ],
  "total_pages": 364,
  "total_results": 7269
}
```

</details>


#### Movies — Translations

```
GET https://api.themoviedb.org/3/movie/{movie_id}/translations
```

Get the translations for a movie.

Operation id: `movie-translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | SA |
| `translations[].iso_639_1` | string | ar |
| `translations[].name` | string | العربية |
| `translations[].english_name` | string | Arabic |
| `translations[].data` | object | - |
| `translations[].data.homepage` | string | - |
| `translations[].data.overview` | string | إدوارد يتعرض لضغوط حتى يصل به الحال إلى أنه… |
| `translations[].data.runtime` | integer | 0 |
| `translations[].data.tagline` | string | - |
| `translations[].data.title` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "translations": [
    {
      "iso_3166_1": "SA",
      "iso_639_1": "ar",
      "name": "العربية",
      "english_name": "Arabic",
      "data": {
        "homepage": "",
        "overview": "إدوارد يتعرض لضغوط حتى يصل به الحال إلى أنه لا يستطيع النوم لفتراتٍ طويلة، لكنه يجد بعض السلام في جلسات العلاج النفسي الجماعي، يتعرف إدوارد على أحد الأشخاص وهو (تايلر ديردن) الذي يحرره من تعلقه بالأشياء الذي تستعبده ،ثم يحرره من خوفه من ...",
        "runtime": 0,
        "tagline": "",
        "title": ""
      }
    },
    {
      "iso_3166_1": "AZ",
      "iso_639_1": "az",
      "name": "Azərbaycan",
      "english_name": "Azerbaijani",
      "data": {
        "homepage": "",
        "overview": "Xəyalları olmayan bir gənc yuxusuzluğa qarşı mübarizə aparır, bəlkə də boz və gündəlik həyatı ilə cansıxıcılığının nəticəsidir. Təyyarə gəzintisində o, çox xüsusi bir fəlsəfəyə sahib olan xarizmatik sabun satıcısı Tyler Durden ilə tanış ...",
        "runtime": 0,
        "tagline": "",
        "title": "Döyüshçü Klubu"
      }
    },
    "… 45 more items"
  ]
}
```

</details>


#### Movies — Videos

```
GET https://api.themoviedb.org/3/movie/{movie_id}/videos
```

Operation id: `movie-videos`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `results` | array<object> | - |
| `results[].iso_639_1` | string | en |
| `results[].iso_3166_1` | string | US |
| `results[].name` | string | Fight Club (1999) Trailer - Starring Brad P… |
| `results[].key` | string | O-b2VfmmbyA |
| `results[].site` | string | YouTube |
| `results[].size` | integer | 720 |
| `results[].type` | string | Trailer |
| `results[].official` | boolean | false |
| `results[].published_at` | string | 2016-03-05T02:03:14.000Z |
| `results[].id` | string | 639d5326be6d88007f170f44 |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "results": [
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Fight Club (1999) Trailer - Starring Brad Pitt, Edward Norton, Helena Bonham Carter",
      "key": "O-b2VfmmbyA",
      "site": "YouTube",
      "size": 720,
      "type": "Trailer",
      "official": false,
      "published_at": "2016-03-05T02:03:14.000Z",
      "id": "639d5326be6d88007f170f44"
    },
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "#TBT Trailer",
      "key": "BdJKm16Co6M",
      "site": "YouTube",
      "size": 1080,
      "type": "Trailer",
      "official": true,
      "published_at": "2014-10-02T19:20:22.000Z",
      "id": "5c9294240e0a267cd516835f"
    }
  ]
}
```

</details>


#### Movies — Watch Providers

```
GET https://api.themoviedb.org/3/movie/{movie_id}/watch/providers
```

Get the list of streaming providers we have for a movie.

Operation id: `movie-watch-providers`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `movie_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `results` | object | - |
| `results.AE` | object | - |
| `results.AE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.AE.flatrate` | array<object> | - |
| `results.AE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.AE.flatrate[].provider_id` | integer | 119 |
| `results.AE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.AE.flatrate[].display_priority` | integer | 12 |
| `results.AE.rent` | array<object> | - |
| `results.AE.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.AE.rent[].provider_id` | integer | 2 |
| `results.AE.rent[].provider_name` | string | Apple TV |
| `results.AE.rent[].display_priority` | integer | 1 |
| `results.AE.buy` | array<object> | - |
| `results.AE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.AE.buy[].provider_id` | integer | 2 |
| `results.AE.buy[].provider_name` | string | Apple TV |
| `results.AE.buy[].display_priority` | integer | 1 |
| `results.AL` | object | - |
| `results.AL.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.AL.buy` | array<object> | - |
| `results.AL.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.AL.buy[].provider_id` | integer | 35 |
| `results.AL.buy[].provider_name` | string | Rakuten TV |
| `results.AL.buy[].display_priority` | integer | 9 |
| `results.AR` | object | - |
| `results.AR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.AR.buy` | array<object> | - |
| `results.AR.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.AR.buy[].provider_id` | integer | 3 |
| `results.AR.buy[].provider_name` | string | Google Play Movies |
| `results.AR.buy[].display_priority` | integer | 6 |
| `results.AR.flatrate` | array<object> | - |
| `results.AR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.AR.flatrate[].provider_id` | integer | 119 |
| `results.AR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.AR.flatrate[].display_priority` | integer | 2 |
| `results.AR.rent` | array<object> | - |
| `results.AR.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.AR.rent[].provider_id` | integer | 3 |
| `results.AR.rent[].provider_name` | string | Google Play Movies |
| `results.AR.rent[].display_priority` | integer | 6 |
| `results.AT` | object | - |
| `results.AT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.AT.flatrate` | array<object> | - |
| `results.AT.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.AT.flatrate[].provider_id` | integer | 337 |
| `results.AT.flatrate[].provider_name` | string | Disney Plus |
| `results.AT.flatrate[].display_priority` | integer | 2 |
| `results.AT.buy` | array<object> | - |
| `results.AT.buy[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.AT.buy[].provider_id` | integer | 10 |
| `results.AT.buy[].provider_name` | string | Amazon Video |
| `results.AT.buy[].display_priority` | integer | 3 |
| `results.AT.rent` | array<object> | - |
| `results.AT.rent[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.AT.rent[].provider_id` | integer | 10 |
| `results.AT.rent[].provider_name` | string | Amazon Video |
| `results.AT.rent[].display_priority` | integer | 3 |
| `results.AU` | object | - |
| `results.AU.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.AU.flatrate` | array<object> | - |
| `results.AU.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.AU.flatrate[].provider_id` | integer | 119 |
| `results.AU.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.AU.flatrate[].display_priority` | integer | 1 |
| `results.AU.buy` | array<object> | - |
| `results.AU.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.AU.buy[].provider_id` | integer | 2 |
| `results.AU.buy[].provider_name` | string | Apple TV |
| `results.AU.buy[].display_priority` | integer | 10 |
| `results.BA` | object | - |
| `results.BA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BA.buy` | array<object> | - |
| `results.BA.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.BA.buy[].provider_id` | integer | 35 |
| `results.BA.buy[].provider_name` | string | Rakuten TV |
| `results.BA.buy[].display_priority` | integer | 9 |
| `results.BA.flatrate` | array<object> | - |
| `results.BA.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.BA.flatrate[].provider_id` | integer | 119 |
| `results.BA.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.BA.flatrate[].display_priority` | integer | 1 |
| `results.BB` | object | - |
| `results.BB.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BB.flatrate` | array<object> | - |
| `results.BB.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BB.flatrate[].provider_id` | integer | 384 |
| `results.BB.flatrate[].provider_name` | string | HBO Max |
| `results.BB.flatrate[].display_priority` | integer | 28 |
| `results.BE` | object | - |
| `results.BE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BE.rent` | array<object> | - |
| `results.BE.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.BE.rent[].provider_id` | integer | 2 |
| `results.BE.rent[].provider_name` | string | Apple TV |
| `results.BE.rent[].display_priority` | integer | 6 |
| `results.BE.flatrate` | array<object> | - |
| `results.BE.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.BE.flatrate[].provider_id` | integer | 337 |
| `results.BE.flatrate[].provider_name` | string | Disney Plus |
| `results.BE.flatrate[].display_priority` | integer | 1 |
| `results.BE.buy` | array<object> | - |
| `results.BE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.BE.buy[].provider_id` | integer | 2 |
| `results.BE.buy[].provider_name` | string | Apple TV |
| `results.BE.buy[].display_priority` | integer | 6 |
| `results.BG` | object | - |
| `results.BG.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BG.rent` | array<object> | - |
| `results.BG.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.BG.rent[].provider_id` | integer | 2 |
| `results.BG.rent[].provider_name` | string | Apple TV |
| `results.BG.rent[].display_priority` | integer | 2 |
| `results.BG.buy` | array<object> | - |
| `results.BG.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.BG.buy[].provider_id` | integer | 2 |
| `results.BG.buy[].provider_name` | string | Apple TV |
| `results.BG.buy[].display_priority` | integer | 2 |
| `results.BG.flatrate` | array<object> | - |
| `results.BG.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.BG.flatrate[].provider_id` | integer | 119 |
| `results.BG.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.BG.flatrate[].display_priority` | integer | 1 |
| `results.BH` | object | - |
| `results.BH.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BH.buy` | array<object> | - |
| `results.BH.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.BH.buy[].provider_id` | integer | 3 |
| `results.BH.buy[].provider_name` | string | Google Play Movies |
| `results.BH.buy[].display_priority` | integer | 1000 |
| `results.BO` | object | - |
| `results.BO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BO.flatrate` | array<object> | - |
| `results.BO.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.BO.flatrate[].provider_id` | integer | 119 |
| `results.BO.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.BO.flatrate[].display_priority` | integer | 1 |
| `results.BR` | object | - |
| `results.BR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BR.flatrate` | array<object> | - |
| `results.BR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.BR.flatrate[].provider_id` | integer | 119 |
| `results.BR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.BR.flatrate[].display_priority` | integer | 2 |
| `results.BS` | object | - |
| `results.BS.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.BS.flatrate` | array<object> | - |
| `results.BS.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BS.flatrate[].provider_id` | integer | 384 |
| `results.BS.flatrate[].provider_name` | string | HBO Max |
| `results.BS.flatrate[].display_priority` | integer | 28 |
| `results.CA` | object | - |
| `results.CA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CA.rent` | array<object> | - |
| `results.CA.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CA.rent[].provider_id` | integer | 3 |
| `results.CA.rent[].provider_name` | string | Google Play Movies |
| `results.CA.rent[].display_priority` | integer | 8 |
| `results.CA.buy` | array<object> | - |
| `results.CA.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.CA.buy[].provider_id` | integer | 2 |
| `results.CA.buy[].provider_name` | string | Apple TV |
| `results.CA.buy[].display_priority` | integer | 6 |
| `results.CA.flatrate` | array<object> | - |
| `results.CA.flatrate[].logo_path` | string | /sB5vHrmYmliwUvBwZe8HpXo9r8m.jpg |
| `results.CA.flatrate[].provider_id` | integer | 305 |
| `results.CA.flatrate[].provider_name` | string | Crave Starz |
| `results.CA.flatrate[].display_priority` | integer | 5 |
| `results.CH` | object | - |
| `results.CH.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CH.flatrate` | array<object> | - |
| `results.CH.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.CH.flatrate[].provider_id` | integer | 119 |
| `results.CH.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.CH.flatrate[].display_priority` | integer | 1 |
| `results.CH.buy` | array<object> | - |
| `results.CH.buy[].logo_path` | string | /rVOOhp6V8FheEAKtFAJMLMbnaMZ.jpg |
| `results.CH.buy[].provider_id` | integer | 150 |
| `results.CH.buy[].provider_name` | string | blue TV |
| `results.CH.buy[].display_priority` | integer | 3 |
| `results.CH.rent` | array<object> | - |
| `results.CH.rent[].logo_path` | string | /rVOOhp6V8FheEAKtFAJMLMbnaMZ.jpg |
| `results.CH.rent[].provider_id` | integer | 150 |
| `results.CH.rent[].provider_name` | string | blue TV |
| `results.CH.rent[].display_priority` | integer | 3 |
| `results.CL` | object | - |
| `results.CL.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CL.buy` | array<object> | - |
| `results.CL.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CL.buy[].provider_id` | integer | 3 |
| `results.CL.buy[].provider_name` | string | Google Play Movies |
| `results.CL.buy[].display_priority` | integer | 4 |
| `results.CL.flatrate` | array<object> | - |
| `results.CL.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.CL.flatrate[].provider_id` | integer | 119 |
| `results.CL.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.CL.flatrate[].display_priority` | integer | 2 |
| `results.CL.rent` | array<object> | - |
| `results.CL.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CL.rent[].provider_id` | integer | 3 |
| `results.CL.rent[].provider_name` | string | Google Play Movies |
| `results.CL.rent[].display_priority` | integer | 4 |
| `results.CO` | object | - |
| `results.CO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CO.buy` | array<object> | - |
| `results.CO.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CO.buy[].provider_id` | integer | 3 |
| `results.CO.buy[].provider_name` | string | Google Play Movies |
| `results.CO.buy[].display_priority` | integer | 3 |
| `results.CO.flatrate` | array<object> | - |
| `results.CO.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.CO.flatrate[].provider_id` | integer | 119 |
| `results.CO.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.CO.flatrate[].display_priority` | integer | 2 |
| `results.CO.rent` | array<object> | - |
| `results.CO.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CO.rent[].provider_id` | integer | 3 |
| `results.CO.rent[].provider_name` | string | Google Play Movies |
| `results.CO.rent[].display_priority` | integer | 3 |
| `results.CR` | object | - |
| `results.CR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CR.flatrate` | array<object> | - |
| `results.CR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.CR.flatrate[].provider_id` | integer | 119 |
| `results.CR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.CR.flatrate[].display_priority` | integer | 1 |
| `results.CV` | object | - |
| `results.CV.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CV.buy` | array<object> | - |
| `results.CV.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.CV.buy[].provider_id` | integer | 2 |
| `results.CV.buy[].provider_name` | string | Apple TV |
| `results.CV.buy[].display_priority` | integer | 13 |
| `results.CV.rent` | array<object> | - |
| `results.CV.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.CV.rent[].provider_id` | integer | 2 |
| `results.CV.rent[].provider_name` | string | Apple TV |
| `results.CV.rent[].display_priority` | integer | 13 |
| `results.CZ` | object | - |
| `results.CZ.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.CZ.flatrate` | array<object> | - |
| `results.CZ.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.CZ.flatrate[].provider_id` | integer | 119 |
| `results.CZ.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.CZ.flatrate[].display_priority` | integer | 1 |
| `results.CZ.rent` | array<object> | - |
| `results.CZ.rent[].logo_path` | string | /wTF37o4jOkQfjnWe41gmeuASYZA.jpg |
| `results.CZ.rent[].provider_id` | integer | 308 |
| `results.CZ.rent[].provider_name` | string | O2 TV |
| `results.CZ.rent[].display_priority` | integer | 2 |
| `results.CZ.buy` | array<object> | - |
| `results.CZ.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.CZ.buy[].provider_id` | integer | 2 |
| `results.CZ.buy[].provider_name` | string | Apple TV |
| `results.CZ.buy[].display_priority` | integer | 3 |
| `results.DE` | object | - |
| `results.DE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.DE.flatrate` | array<object> | - |
| `results.DE.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.DE.flatrate[].provider_id` | integer | 337 |
| `results.DE.flatrate[].provider_name` | string | Disney Plus |
| `results.DE.flatrate[].display_priority` | integer | 2 |
| `results.DE.buy` | array<object> | - |
| `results.DE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.DE.buy[].provider_id` | integer | 2 |
| `results.DE.buy[].provider_name` | string | Apple TV |
| `results.DE.buy[].display_priority` | integer | 4 |
| `results.DE.rent` | array<object> | - |
| `results.DE.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.DE.rent[].provider_id` | integer | 2 |
| `results.DE.rent[].provider_name` | string | Apple TV |
| `results.DE.rent[].display_priority` | integer | 4 |
| `results.DK` | object | - |
| `results.DK.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.DK.rent` | array<object> | - |
| `results.DK.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.DK.rent[].provider_id` | integer | 2 |
| `results.DK.rent[].provider_name` | string | Apple TV |
| `results.DK.rent[].display_priority` | integer | 7 |
| `results.DK.flatrate` | array<object> | - |
| `results.DK.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.DK.flatrate[].provider_id` | integer | 337 |
| `results.DK.flatrate[].provider_name` | string | Disney Plus |
| `results.DK.flatrate[].display_priority` | integer | 1 |
| `results.DK.buy` | array<object> | - |
| `results.DK.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.DK.buy[].provider_id` | integer | 2 |
| `results.DK.buy[].provider_name` | string | Apple TV |
| `results.DK.buy[].display_priority` | integer | 7 |
| `results.DO` | object | - |
| `results.DO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.DO.flatrate` | array<object> | - |
| `results.DO.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.DO.flatrate[].provider_id` | integer | 119 |
| `results.DO.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.DO.flatrate[].display_priority` | integer | 1 |
| `results.EC` | object | - |
| `results.EC.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.EC.buy` | array<object> | - |
| `results.EC.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.EC.buy[].provider_id` | integer | 3 |
| `results.EC.buy[].provider_name` | string | Google Play Movies |
| `results.EC.buy[].display_priority` | integer | 11 |
| `results.EC.flatrate` | array<object> | - |
| `results.EC.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.EC.flatrate[].provider_id` | integer | 119 |
| `results.EC.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.EC.flatrate[].display_priority` | integer | 2 |
| `results.EC.rent` | array<object> | - |
| `results.EC.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.EC.rent[].provider_id` | integer | 3 |
| `results.EC.rent[].provider_name` | string | Google Play Movies |
| `results.EC.rent[].display_priority` | integer | 11 |
| `results.EE` | object | - |
| `results.EE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.EE.buy` | array<object> | - |
| `results.EE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.EE.buy[].provider_id` | integer | 2 |
| `results.EE.buy[].provider_name` | string | Apple TV |
| `results.EE.buy[].display_priority` | integer | 3 |
| `results.EE.flatrate` | array<object> | - |
| `results.EE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.EE.flatrate[].provider_id` | integer | 119 |
| `results.EE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.EE.flatrate[].display_priority` | integer | 1 |
| `results.EE.rent` | array<object> | - |
| `results.EE.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.EE.rent[].provider_id` | integer | 3 |
| `results.EE.rent[].provider_name` | string | Google Play Movies |
| `results.EE.rent[].display_priority` | integer | 4 |
| `results.EG` | object | - |
| `results.EG.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.EG.rent` | array<object> | - |
| `results.EG.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.EG.rent[].provider_id` | integer | 2 |
| `results.EG.rent[].provider_name` | string | Apple TV |
| `results.EG.rent[].display_priority` | integer | 2 |
| `results.EG.buy` | array<object> | - |
| `results.EG.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.EG.buy[].provider_id` | integer | 2 |
| `results.EG.buy[].provider_name` | string | Apple TV |
| `results.EG.buy[].display_priority` | integer | 2 |
| `results.ES` | object | - |
| `results.ES.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.ES.rent` | array<object> | - |
| `results.ES.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.ES.rent[].provider_id` | integer | 2 |
| `results.ES.rent[].provider_name` | string | Apple TV |
| `results.ES.rent[].display_priority` | integer | 4 |
| `results.ES.ads` | array<object> | - |
| `results.ES.ads[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.ES.ads[].provider_id` | integer | 35 |
| `results.ES.ads[].provider_name` | string | Rakuten TV |
| `results.ES.ads[].display_priority` | integer | 11 |
| `results.ES.flatrate` | array<object> | - |
| `results.ES.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.ES.flatrate[].provider_id` | integer | 119 |
| `results.ES.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.ES.flatrate[].display_priority` | integer | 1 |
| `results.ES.buy` | array<object> | - |
| `results.ES.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.ES.buy[].provider_id` | integer | 2 |
| `results.ES.buy[].provider_name` | string | Apple TV |
| `results.ES.buy[].display_priority` | integer | 4 |
| `results.FI` | object | - |
| `results.FI.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.FI.flatrate` | array<object> | - |
| `results.FI.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.FI.flatrate[].provider_id` | integer | 337 |
| `results.FI.flatrate[].provider_name` | string | Disney Plus |
| `results.FI.flatrate[].display_priority` | integer | 1 |
| `results.FI.buy` | array<object> | - |
| `results.FI.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.FI.buy[].provider_id` | integer | 2 |
| `results.FI.buy[].provider_name` | string | Apple TV |
| `results.FI.buy[].display_priority` | integer | 10 |
| `results.FI.rent` | array<object> | - |
| `results.FI.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.FI.rent[].provider_id` | integer | 2 |
| `results.FI.rent[].provider_name` | string | Apple TV |
| `results.FI.rent[].display_priority` | integer | 10 |
| `results.FJ` | object | - |
| `results.FJ.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.FJ.buy` | array<object> | - |
| `results.FJ.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.FJ.buy[].provider_id` | integer | 3 |
| `results.FJ.buy[].provider_name` | string | Google Play Movies |
| `results.FJ.buy[].display_priority` | integer | 1000 |
| `results.FR` | object | - |
| `results.FR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.FR.rent` | array<object> | - |
| `results.FR.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.FR.rent[].provider_id` | integer | 2 |
| `results.FR.rent[].provider_name` | string | Apple TV |
| `results.FR.rent[].display_priority` | integer | 5 |
| `results.FR.buy` | array<object> | - |
| `results.FR.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.FR.buy[].provider_id` | integer | 2 |
| `results.FR.buy[].provider_name` | string | Apple TV |
| `results.FR.buy[].display_priority` | integer | 5 |
| `results.FR.flatrate` | array<object> | - |
| `results.FR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.FR.flatrate[].provider_id` | integer | 119 |
| `results.FR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.FR.flatrate[].display_priority` | integer | 1 |
| `results.GB` | object | - |
| `results.GB.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.GB.flatrate` | array<object> | - |
| `results.GB.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.GB.flatrate[].provider_id` | integer | 337 |
| `results.GB.flatrate[].provider_name` | string | Disney Plus |
| `results.GB.flatrate[].display_priority` | integer | 1 |
| `results.GB.buy` | array<object> | - |
| `results.GB.buy[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.GB.buy[].provider_id` | integer | 10 |
| `results.GB.buy[].provider_name` | string | Amazon Video |
| `results.GB.buy[].display_priority` | integer | 4 |
| `results.GB.rent` | array<object> | - |
| `results.GB.rent[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.GB.rent[].provider_id` | integer | 10 |
| `results.GB.rent[].provider_name` | string | Amazon Video |
| `results.GB.rent[].display_priority` | integer | 4 |
| `results.GF` | object | - |
| `results.GF.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.GF.flatrate` | array<object> | - |
| `results.GF.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.GF.flatrate[].provider_id` | integer | 384 |
| `results.GF.flatrate[].provider_name` | string | HBO Max |
| `results.GF.flatrate[].display_priority` | integer | 30 |
| `results.GI` | object | - |
| `results.GI.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.GI.flatrate` | array<object> | - |
| `results.GI.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.GI.flatrate[].provider_id` | integer | 119 |
| `results.GI.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.GI.flatrate[].display_priority` | integer | 1 |
| `results.GR` | object | - |
| `results.GR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.GR.flatrate` | array<object> | - |
| `results.GR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.GR.flatrate[].provider_id` | integer | 119 |
| `results.GR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.GR.flatrate[].display_priority` | integer | 1 |
| `results.GR.rent` | array<object> | - |
| `results.GR.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.GR.rent[].provider_id` | integer | 2 |
| `results.GR.rent[].provider_name` | string | Apple TV |
| `results.GR.rent[].display_priority` | integer | 2 |
| `results.GR.buy` | array<object> | - |
| `results.GR.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.GR.buy[].provider_id` | integer | 2 |
| `results.GR.buy[].provider_name` | string | Apple TV |
| `results.GR.buy[].display_priority` | integer | 2 |
| `results.GT` | object | - |
| `results.GT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.GT.flatrate` | array<object> | - |
| `results.GT.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.GT.flatrate[].provider_id` | integer | 119 |
| `results.GT.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.GT.flatrate[].display_priority` | integer | 1 |
| `results.HK` | object | - |
| `results.HK.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.HK.flatrate` | array<object> | - |
| `results.HK.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.HK.flatrate[].provider_id` | integer | 119 |
| `results.HK.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.HK.flatrate[].display_priority` | integer | 7 |
| `results.HN` | object | - |
| `results.HN.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.HN.flatrate` | array<object> | - |
| `results.HN.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.HN.flatrate[].provider_id` | integer | 119 |
| `results.HN.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.HN.flatrate[].display_priority` | integer | 1 |
| `results.HR` | object | - |
| `results.HR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.HR.buy` | array<object> | - |
| `results.HR.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.HR.buy[].provider_id` | integer | 3 |
| `results.HR.buy[].provider_name` | string | Google Play Movies |
| `results.HR.buy[].display_priority` | integer | 3 |
| `results.HR.ads` | array<object> | - |
| `results.HR.ads[].logo_path` | string | /xrHrIraInfRXnrz1zHhY1tXJowg.jpg |
| `results.HR.ads[].provider_id` | integer | 572 |
| `results.HR.ads[].provider_name` | string | RTL Play |
| `results.HR.ads[].display_priority` | integer | 30 |
| `results.HR.flatrate` | array<object> | - |
| `results.HR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.HR.flatrate[].provider_id` | integer | 119 |
| `results.HR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.HR.flatrate[].display_priority` | integer | 1 |
| `results.HU` | object | - |
| `results.HU.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.HU.flatrate` | array<object> | - |
| `results.HU.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.HU.flatrate[].provider_id` | integer | 119 |
| `results.HU.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.HU.flatrate[].display_priority` | integer | 2 |
| `results.HU.buy` | array<object> | - |
| `results.HU.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.HU.buy[].provider_id` | integer | 2 |
| `results.HU.buy[].provider_name` | string | Apple TV |
| `results.HU.buy[].display_priority` | integer | 3 |
| `results.HU.rent` | array<object> | - |
| `results.HU.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.HU.rent[].provider_id` | integer | 2 |
| `results.HU.rent[].provider_name` | string | Apple TV |
| `results.HU.rent[].display_priority` | integer | 3 |
| `results.ID` | object | - |
| `results.ID.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.ID.flatrate` | array<object> | - |
| `results.ID.flatrate[].logo_path` | string | /7Fl8ylPDclt3ZYgNbW2t7rbZE9I.jpg |
| `results.ID.flatrate[].provider_id` | integer | 122 |
| `results.ID.flatrate[].provider_name` | string | Hotstar |
| `results.ID.flatrate[].display_priority` | integer | 3 |
| `results.IE` | object | - |
| `results.IE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IE.rent` | array<object> | - |
| `results.IE.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.IE.rent[].provider_id` | integer | 2 |
| `results.IE.rent[].provider_name` | string | Apple TV |
| `results.IE.rent[].display_priority` | integer | 4 |
| `results.IE.flatrate` | array<object> | - |
| `results.IE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.IE.flatrate[].provider_id` | integer | 119 |
| `results.IE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.IE.flatrate[].display_priority` | integer | 1 |
| `results.IE.buy` | array<object> | - |
| `results.IE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.IE.buy[].provider_id` | integer | 2 |
| `results.IE.buy[].provider_name` | string | Apple TV |
| `results.IE.buy[].display_priority` | integer | 4 |
| `results.IL` | object | - |
| `results.IL.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IL.buy` | array<object> | - |
| `results.IL.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.IL.buy[].provider_id` | integer | 2 |
| `results.IL.buy[].provider_name` | string | Apple TV |
| `results.IL.buy[].display_priority` | integer | 28 |
| `results.IL.flatrate` | array<object> | - |
| `results.IL.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.IL.flatrate[].provider_id` | integer | 119 |
| `results.IL.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.IL.flatrate[].display_priority` | integer | 1 |
| `results.IN` | object | - |
| `results.IN.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IN.flatrate` | array<object> | - |
| `results.IN.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.IN.flatrate[].provider_id` | integer | 119 |
| `results.IN.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.IN.flatrate[].display_priority` | integer | 1 |
| `results.IN.rent` | array<object> | - |
| `results.IN.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.IN.rent[].provider_id` | integer | 2 |
| `results.IN.rent[].provider_name` | string | Apple TV |
| `results.IN.rent[].display_priority` | integer | 8 |
| `results.IN.buy` | array<object> | - |
| `results.IN.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.IN.buy[].provider_id` | integer | 2 |
| `results.IN.buy[].provider_name` | string | Apple TV |
| `results.IN.buy[].display_priority` | integer | 8 |
| `results.IQ` | object | - |
| `results.IQ.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IQ.flatrate` | array<object> | - |
| `results.IQ.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.IQ.flatrate[].provider_id` | integer | 119 |
| `results.IQ.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.IQ.flatrate[].display_priority` | integer | 1 |
| `results.IS` | object | - |
| `results.IS.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IS.buy` | array<object> | - |
| `results.IS.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.IS.buy[].provider_id` | integer | 3 |
| `results.IS.buy[].provider_name` | string | Google Play Movies |
| `results.IS.buy[].display_priority` | integer | 4 |
| `results.IS.flatrate` | array<object> | - |
| `results.IS.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.IS.flatrate[].provider_id` | integer | 337 |
| `results.IS.flatrate[].provider_name` | string | Disney Plus |
| `results.IS.flatrate[].display_priority` | integer | 1 |
| `results.IT` | object | - |
| `results.IT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.IT.buy` | array<object> | - |
| `results.IT.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.IT.buy[].provider_id` | integer | 35 |
| `results.IT.buy[].provider_name` | string | Rakuten TV |
| `results.IT.buy[].display_priority` | integer | 3 |
| `results.IT.rent` | array<object> | - |
| `results.IT.rent[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.IT.rent[].provider_id` | integer | 35 |
| `results.IT.rent[].provider_name` | string | Rakuten TV |
| `results.IT.rent[].display_priority` | integer | 3 |
| `results.IT.flatrate` | array<object> | - |
| `results.IT.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.IT.flatrate[].provider_id` | integer | 119 |
| `results.IT.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.IT.flatrate[].display_priority` | integer | 1 |
| `results.JM` | object | - |
| `results.JM.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.JM.flatrate` | array<object> | - |
| `results.JM.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.JM.flatrate[].provider_id` | integer | 384 |
| `results.JM.flatrate[].provider_name` | string | HBO Max |
| `results.JM.flatrate[].display_priority` | integer | 27 |
| `results.JO` | object | - |
| `results.JO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.JO.flatrate` | array<object> | - |
| `results.JO.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.JO.flatrate[].provider_id` | integer | 119 |
| `results.JO.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.JO.flatrate[].display_priority` | integer | 1 |
| `results.JO.buy` | array<object> | - |
| `results.JO.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.JO.buy[].provider_id` | integer | 3 |
| `results.JO.buy[].provider_name` | string | Google Play Movies |
| `results.JO.buy[].display_priority` | integer | 1000 |
| `results.JP` | object | - |
| `results.JP.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.JP.flatrate` | array<object> | - |
| `results.JP.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.JP.flatrate[].provider_id` | integer | 337 |
| `results.JP.flatrate[].provider_name` | string | Disney Plus |
| `results.JP.flatrate[].display_priority` | integer | 3 |
| `results.JP.rent` | array<object> | - |
| `results.JP.rent[].logo_path` | string | /g8jqHtXJsMlc8B1Gb0Rt8AvUJMn.jpg |
| `results.JP.rent[].provider_id` | integer | 85 |
| `results.JP.rent[].provider_name` | string | dTV |
| `results.JP.rent[].display_priority` | integer | 2 |
| `results.JP.buy` | array<object> | - |
| `results.JP.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.JP.buy[].provider_id` | integer | 2 |
| `results.JP.buy[].provider_name` | string | Apple TV |
| `results.JP.buy[].display_priority` | integer | 7 |
| `results.KR` | object | - |
| `results.KR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.KR.flatrate` | array<object> | - |
| `results.KR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.KR.flatrate[].provider_id` | integer | 119 |
| `results.KR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.KR.flatrate[].display_priority` | integer | 1 |
| `results.KR.buy` | array<object> | - |
| `results.KR.buy[].logo_path` | string | /2ioan5BX5L9tz4fIGU93blTeFhv.jpg |
| `results.KR.buy[].provider_id` | integer | 356 |
| `results.KR.buy[].provider_name` | string | wavve |
| `results.KR.buy[].display_priority` | integer | 3 |
| `results.KW` | object | - |
| `results.KW.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.KW.buy` | array<object> | - |
| `results.KW.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.KW.buy[].provider_id` | integer | 3 |
| `results.KW.buy[].provider_name` | string | Google Play Movies |
| `results.KW.buy[].display_priority` | integer | 1000 |
| `results.KW.flatrate` | array<object> | - |
| `results.KW.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.KW.flatrate[].provider_id` | integer | 119 |
| `results.KW.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.KW.flatrate[].display_priority` | integer | 1 |
| `results.LB` | object | - |
| `results.LB.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.LB.flatrate` | array<object> | - |
| `results.LB.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.LB.flatrate[].provider_id` | integer | 119 |
| `results.LB.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.LB.flatrate[].display_priority` | integer | 1 |
| `results.LB.buy` | array<object> | - |
| `results.LB.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.LB.buy[].provider_id` | integer | 3 |
| `results.LB.buy[].provider_name` | string | Google Play Movies |
| `results.LB.buy[].display_priority` | integer | 1000 |
| `results.LI` | object | - |
| `results.LI.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.LI.flatrate` | array<object> | - |
| `results.LI.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.LI.flatrate[].provider_id` | integer | 337 |
| `results.LI.flatrate[].provider_name` | string | Disney Plus |
| `results.LI.flatrate[].display_priority` | integer | 30 |
| `results.LT` | object | - |
| `results.LT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.LT.rent` | array<object> | - |
| `results.LT.rent[].logo_path` | string | /xTVM8uXT9QocigQ07LE7Irc65W2.jpg |
| `results.LT.rent[].provider_id` | integer | 553 |
| `results.LT.rent[].provider_name` | string | Telia Play |
| `results.LT.rent[].display_priority` | integer | 15 |
| `results.LT.buy` | array<object> | - |
| `results.LT.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.LT.buy[].provider_id` | integer | 2 |
| `results.LT.buy[].provider_name` | string | Apple TV |
| `results.LT.buy[].display_priority` | integer | 3 |
| `results.LT.flatrate` | array<object> | - |
| `results.LT.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.LT.flatrate[].provider_id` | integer | 119 |
| `results.LT.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.LT.flatrate[].display_priority` | integer | 1 |
| `results.LV` | object | - |
| `results.LV.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.LV.flatrate` | array<object> | - |
| `results.LV.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.LV.flatrate[].provider_id` | integer | 119 |
| `results.LV.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.LV.flatrate[].display_priority` | integer | 1 |
| `results.LV.buy` | array<object> | - |
| `results.LV.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.LV.buy[].provider_id` | integer | 2 |
| `results.LV.buy[].provider_name` | string | Apple TV |
| `results.LV.buy[].display_priority` | integer | 3 |
| `results.MD` | object | - |
| `results.MD.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MD.buy` | array<object> | - |
| `results.MD.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.MD.buy[].provider_id` | integer | 3 |
| `results.MD.buy[].provider_name` | string | Google Play Movies |
| `results.MD.buy[].display_priority` | integer | 1000 |
| `results.MD.flatrate` | array<object> | - |
| `results.MD.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.MD.flatrate[].provider_id` | integer | 384 |
| `results.MD.flatrate[].provider_name` | string | HBO Max |
| `results.MD.flatrate[].display_priority` | integer | 26 |
| `results.MK` | object | - |
| `results.MK.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MK.flatrate` | array<object> | - |
| `results.MK.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.MK.flatrate[].provider_id` | integer | 384 |
| `results.MK.flatrate[].provider_name` | string | HBO Max |
| `results.MK.flatrate[].display_priority` | integer | 29 |
| `results.MK.buy` | array<object> | - |
| `results.MK.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.MK.buy[].provider_id` | integer | 35 |
| `results.MK.buy[].provider_name` | string | Rakuten TV |
| `results.MK.buy[].display_priority` | integer | 9 |
| `results.MT` | object | - |
| `results.MT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MT.flatrate` | array<object> | - |
| `results.MT.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.MT.flatrate[].provider_id` | integer | 119 |
| `results.MT.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.MT.flatrate[].display_priority` | integer | 1 |
| `results.MT.buy` | array<object> | - |
| `results.MT.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.MT.buy[].provider_id` | integer | 35 |
| `results.MT.buy[].provider_name` | string | Rakuten TV |
| `results.MT.buy[].display_priority` | integer | 9 |
| `results.MT.rent` | array<object> | - |
| `results.MT.rent[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.MT.rent[].provider_id` | integer | 35 |
| `results.MT.rent[].provider_name` | string | Rakuten TV |
| `results.MT.rent[].display_priority` | integer | 9 |
| `results.MU` | object | - |
| `results.MU.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MU.buy` | array<object> | - |
| `results.MU.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.MU.buy[].provider_id` | integer | 2 |
| `results.MU.buy[].provider_name` | string | Apple TV |
| `results.MU.buy[].display_priority` | integer | 15 |
| `results.MU.rent` | array<object> | - |
| `results.MU.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.MU.rent[].provider_id` | integer | 2 |
| `results.MU.rent[].provider_name` | string | Apple TV |
| `results.MU.rent[].display_priority` | integer | 15 |
| `results.MX` | object | - |
| `results.MX.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MX.flatrate` | array<object> | - |
| `results.MX.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.MX.flatrate[].provider_id` | integer | 119 |
| `results.MX.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.MX.flatrate[].display_priority` | integer | 1 |
| `results.MY` | object | - |
| `results.MY.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MY.flatrate` | array<object> | - |
| `results.MY.flatrate[].logo_path` | string | /7Fl8ylPDclt3ZYgNbW2t7rbZE9I.jpg |
| `results.MY.flatrate[].provider_id` | integer | 122 |
| `results.MY.flatrate[].provider_name` | string | Hotstar |
| `results.MY.flatrate[].display_priority` | integer | 0 |
| `results.MZ` | object | - |
| `results.MZ.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.MZ.rent` | array<object> | - |
| `results.MZ.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.MZ.rent[].provider_id` | integer | 2 |
| `results.MZ.rent[].provider_name` | string | Apple TV |
| `results.MZ.rent[].display_priority` | integer | 16 |
| `results.MZ.buy` | array<object> | - |
| `results.MZ.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.MZ.buy[].provider_id` | integer | 2 |
| `results.MZ.buy[].provider_name` | string | Apple TV |
| `results.MZ.buy[].display_priority` | integer | 16 |
| `results.NL` | object | - |
| `results.NL.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.NL.buy` | array<object> | - |
| `results.NL.buy[].logo_path` | string | /llmnYOyknekZsXtkCaazKjhTLvG.jpg |
| `results.NL.buy[].provider_id` | integer | 71 |
| `results.NL.buy[].provider_name` | string | Pathé Thuis |
| `results.NL.buy[].display_priority` | integer | 7 |
| `results.NL.rent` | array<object> | - |
| `results.NL.rent[].logo_path` | string | /llmnYOyknekZsXtkCaazKjhTLvG.jpg |
| `results.NL.rent[].provider_id` | integer | 71 |
| `results.NL.rent[].provider_name` | string | Pathé Thuis |
| `results.NL.rent[].display_priority` | integer | 7 |
| `results.NL.flatrate` | array<object> | - |
| `results.NL.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.NL.flatrate[].provider_id` | integer | 337 |
| `results.NL.flatrate[].provider_name` | string | Disney Plus |
| `results.NL.flatrate[].display_priority` | integer | 1 |
| `results.NO` | object | - |
| `results.NO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.NO.rent` | array<object> | - |
| `results.NO.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.NO.rent[].provider_id` | integer | 2 |
| `results.NO.rent[].provider_name` | string | Apple TV |
| `results.NO.rent[].display_priority` | integer | 6 |
| `results.NO.buy` | array<object> | - |
| `results.NO.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.NO.buy[].provider_id` | integer | 2 |
| `results.NO.buy[].provider_name` | string | Apple TV |
| `results.NO.buy[].display_priority` | integer | 6 |
| `results.NO.flatrate` | array<object> | - |
| `results.NO.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.NO.flatrate[].provider_id` | integer | 337 |
| `results.NO.flatrate[].provider_name` | string | Disney Plus |
| `results.NO.flatrate[].display_priority` | integer | 1 |
| `results.NZ` | object | - |
| `results.NZ.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.NZ.flatrate` | array<object> | - |
| `results.NZ.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.NZ.flatrate[].provider_id` | integer | 119 |
| `results.NZ.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.NZ.flatrate[].display_priority` | integer | 1 |
| `results.NZ.buy` | array<object> | - |
| `results.NZ.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.NZ.buy[].provider_id` | integer | 2 |
| `results.NZ.buy[].provider_name` | string | Apple TV |
| `results.NZ.buy[].display_priority` | integer | 4 |
| `results.OM` | object | - |
| `results.OM.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.OM.buy` | array<object> | - |
| `results.OM.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.OM.buy[].provider_id` | integer | 3 |
| `results.OM.buy[].provider_name` | string | Google Play Movies |
| `results.OM.buy[].display_priority` | integer | 1000 |
| `results.OM.rent` | array<object> | - |
| `results.OM.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.OM.rent[].provider_id` | integer | 3 |
| `results.OM.rent[].provider_name` | string | Google Play Movies |
| `results.OM.rent[].display_priority` | integer | 1000 |
| `results.OM.flatrate` | array<object> | - |
| `results.OM.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.OM.flatrate[].provider_id` | integer | 119 |
| `results.OM.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.OM.flatrate[].display_priority` | integer | 1 |
| `results.PA` | object | - |
| `results.PA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PA.flatrate` | array<object> | - |
| `results.PA.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PA.flatrate[].provider_id` | integer | 119 |
| `results.PA.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PA.flatrate[].display_priority` | integer | 1 |
| `results.PE` | object | - |
| `results.PE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PE.rent` | array<object> | - |
| `results.PE.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.PE.rent[].provider_id` | integer | 3 |
| `results.PE.rent[].provider_name` | string | Google Play Movies |
| `results.PE.rent[].display_priority` | integer | 5 |
| `results.PE.buy` | array<object> | - |
| `results.PE.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.PE.buy[].provider_id` | integer | 3 |
| `results.PE.buy[].provider_name` | string | Google Play Movies |
| `results.PE.buy[].display_priority` | integer | 5 |
| `results.PE.flatrate` | array<object> | - |
| `results.PE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PE.flatrate[].provider_id` | integer | 119 |
| `results.PE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PE.flatrate[].display_priority` | integer | 2 |
| `results.PH` | object | - |
| `results.PH.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PH.flatrate` | array<object> | - |
| `results.PH.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PH.flatrate[].provider_id` | integer | 119 |
| `results.PH.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PH.flatrate[].display_priority` | integer | 1 |
| `results.PK` | object | - |
| `results.PK.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PK.flatrate` | array<object> | - |
| `results.PK.flatrate[].logo_path` | string | /t2yyOv40HZeVlLjYsCsPHnWLk4W.jpg |
| `results.PK.flatrate[].provider_id` | integer | 8 |
| `results.PK.flatrate[].provider_name` | string | Netflix |
| `results.PK.flatrate[].display_priority` | integer | 0 |
| `results.PL` | object | - |
| `results.PL.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PL.buy` | array<object> | - |
| `results.PL.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.PL.buy[].provider_id` | integer | 2 |
| `results.PL.buy[].provider_name` | string | Apple TV |
| `results.PL.buy[].display_priority` | integer | 1 |
| `results.PL.flatrate` | array<object> | - |
| `results.PL.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PL.flatrate[].provider_id` | integer | 119 |
| `results.PL.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PL.flatrate[].display_priority` | integer | 4 |
| `results.PL.rent` | array<object> | - |
| `results.PL.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.PL.rent[].provider_id` | integer | 2 |
| `results.PL.rent[].provider_name` | string | Apple TV |
| `results.PL.rent[].display_priority` | integer | 1 |
| `results.PS` | object | - |
| `results.PS.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PS.flatrate` | array<object> | - |
| `results.PS.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PS.flatrate[].provider_id` | integer | 119 |
| `results.PS.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PS.flatrate[].display_priority` | integer | 1 |
| `results.PT` | object | - |
| `results.PT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PT.buy` | array<object> | - |
| `results.PT.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.PT.buy[].provider_id` | integer | 3 |
| `results.PT.buy[].provider_name` | string | Google Play Movies |
| `results.PT.buy[].display_priority` | integer | 5 |
| `results.PT.rent` | array<object> | - |
| `results.PT.rent[].logo_path` | string | /dUeHhim2WUZz8S7EWjv0Ws6anRP.jpg |
| `results.PT.rent[].provider_id` | integer | 242 |
| `results.PT.rent[].provider_name` | string | Meo |
| `results.PT.rent[].display_priority` | integer | 3 |
| `results.PT.flatrate` | array<object> | - |
| `results.PT.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.PT.flatrate[].provider_id` | integer | 337 |
| `results.PT.flatrate[].provider_name` | string | Disney Plus |
| `results.PT.flatrate[].display_priority` | integer | 1 |
| `results.PY` | object | - |
| `results.PY.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.PY.flatrate` | array<object> | - |
| `results.PY.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.PY.flatrate[].provider_id` | integer | 119 |
| `results.PY.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.PY.flatrate[].display_priority` | integer | 1 |
| `results.QA` | object | - |
| `results.QA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.QA.flatrate` | array<object> | - |
| `results.QA.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.QA.flatrate[].provider_id` | integer | 119 |
| `results.QA.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.QA.flatrate[].display_priority` | integer | 1 |
| `results.QA.buy` | array<object> | - |
| `results.QA.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.QA.buy[].provider_id` | integer | 3 |
| `results.QA.buy[].provider_name` | string | Google Play Movies |
| `results.QA.buy[].display_priority` | integer | 1000 |
| `results.RO` | object | - |
| `results.RO.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.RO.flatrate` | array<object> | - |
| `results.RO.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.RO.flatrate[].provider_id` | integer | 119 |
| `results.RO.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.RO.flatrate[].display_priority` | integer | 1 |
| `results.RS` | object | - |
| `results.RS.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.RS.flatrate` | array<object> | - |
| `results.RS.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.RS.flatrate[].provider_id` | integer | 119 |
| `results.RS.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.RS.flatrate[].display_priority` | integer | 1 |
| `results.RS.buy` | array<object> | - |
| `results.RS.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.RS.buy[].provider_id` | integer | 35 |
| `results.RS.buy[].provider_name` | string | Rakuten TV |
| `results.RS.buy[].display_priority` | integer | 9 |
| `results.RU` | object | - |
| `results.RU.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.RU.rent` | array<object> | - |
| `results.RU.rent[].logo_path` | string | /o9ExgOSLF3OTwR6T3DJOuwOKJgq.jpg |
| `results.RU.rent[].provider_id` | integer | 113 |
| `results.RU.rent[].provider_name` | string | Ivi |
| `results.RU.rent[].display_priority` | integer | 1000 |
| `results.RU.buy` | array<object> | - |
| `results.RU.buy[].logo_path` | string | /o9ExgOSLF3OTwR6T3DJOuwOKJgq.jpg |
| `results.RU.buy[].provider_id` | integer | 113 |
| `results.RU.buy[].provider_name` | string | Ivi |
| `results.RU.buy[].display_priority` | integer | 1000 |
| `results.RU.flatrate` | array<object> | - |
| `results.RU.flatrate[].logo_path` | string | /zLM7f1w2L8TU2Fspzns72m6h3yY.jpg |
| `results.RU.flatrate[].provider_id` | integer | 501 |
| `results.RU.flatrate[].provider_name` | string | Wink |
| `results.RU.flatrate[].display_priority` | integer | 1000 |
| `results.SA` | object | - |
| `results.SA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SA.flatrate` | array<object> | - |
| `results.SA.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.SA.flatrate[].provider_id` | integer | 119 |
| `results.SA.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.SA.flatrate[].display_priority` | integer | 28 |
| `results.SA.rent` | array<object> | - |
| `results.SA.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.SA.rent[].provider_id` | integer | 2 |
| `results.SA.rent[].provider_name` | string | Apple TV |
| `results.SA.rent[].display_priority` | integer | 1 |
| `results.SA.buy` | array<object> | - |
| `results.SA.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.SA.buy[].provider_id` | integer | 2 |
| `results.SA.buy[].provider_name` | string | Apple TV |
| `results.SA.buy[].display_priority` | integer | 1 |
| `results.SE` | object | - |
| `results.SE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SE.buy` | array<object> | - |
| `results.SE.buy[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.SE.buy[].provider_id` | integer | 68 |
| `results.SE.buy[].provider_name` | string | Microsoft Store |
| `results.SE.buy[].display_priority` | integer | 7 |
| `results.SE.rent` | array<object> | - |
| `results.SE.rent[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.SE.rent[].provider_id` | integer | 68 |
| `results.SE.rent[].provider_name` | string | Microsoft Store |
| `results.SE.rent[].display_priority` | integer | 7 |
| `results.SE.flatrate` | array<object> | - |
| `results.SE.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.SE.flatrate[].provider_id` | integer | 337 |
| `results.SE.flatrate[].provider_name` | string | Disney Plus |
| `results.SE.flatrate[].display_priority` | integer | 1 |
| `results.SG` | object | - |
| `results.SG.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SG.flatrate` | array<object> | - |
| `results.SG.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.SG.flatrate[].provider_id` | integer | 119 |
| `results.SG.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.SG.flatrate[].display_priority` | integer | 1 |
| `results.SI` | object | - |
| `results.SI.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SI.buy` | array<object> | - |
| `results.SI.buy[].logo_path` | string | /5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg |
| `results.SI.buy[].provider_id` | integer | 35 |
| `results.SI.buy[].provider_name` | string | Rakuten TV |
| `results.SI.buy[].display_priority` | integer | 9 |
| `results.SI.flatrate` | array<object> | - |
| `results.SI.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.SI.flatrate[].provider_id` | integer | 119 |
| `results.SI.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.SI.flatrate[].display_priority` | integer | 1 |
| `results.SK` | object | - |
| `results.SK.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SK.buy` | array<object> | - |
| `results.SK.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.SK.buy[].provider_id` | integer | 2 |
| `results.SK.buy[].provider_name` | string | Apple TV |
| `results.SK.buy[].display_priority` | integer | 3 |
| `results.SK.flatrate` | array<object> | - |
| `results.SK.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.SK.flatrate[].provider_id` | integer | 119 |
| `results.SK.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.SK.flatrate[].display_priority` | integer | 1 |
| `results.SK.rent` | array<object> | - |
| `results.SK.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.SK.rent[].provider_id` | integer | 2 |
| `results.SK.rent[].provider_name` | string | Apple TV |
| `results.SK.rent[].display_priority` | integer | 3 |
| `results.SM` | object | - |
| `results.SM.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SM.flatrate` | array<object> | - |
| `results.SM.flatrate[].logo_path` | string | /7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg |
| `results.SM.flatrate[].provider_id` | integer | 337 |
| `results.SM.flatrate[].provider_name` | string | Disney Plus |
| `results.SM.flatrate[].display_priority` | integer | 30 |
| `results.SV` | object | - |
| `results.SV.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.SV.flatrate` | array<object> | - |
| `results.SV.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.SV.flatrate[].provider_id` | integer | 119 |
| `results.SV.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.SV.flatrate[].display_priority` | integer | 1 |
| `results.TH` | object | - |
| `results.TH.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.TH.flatrate` | array<object> | - |
| `results.TH.flatrate[].logo_path` | string | /7Fl8ylPDclt3ZYgNbW2t7rbZE9I.jpg |
| `results.TH.flatrate[].provider_id` | integer | 122 |
| `results.TH.flatrate[].provider_name` | string | Hotstar |
| `results.TH.flatrate[].display_priority` | integer | 0 |
| `results.TR` | object | - |
| `results.TR.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.TR.rent` | array<object> | - |
| `results.TR.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.TR.rent[].provider_id` | integer | 3 |
| `results.TR.rent[].provider_name` | string | Google Play Movies |
| `results.TR.rent[].display_priority` | integer | 5 |
| `results.TR.flatrate` | array<object> | - |
| `results.TR.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.TR.flatrate[].provider_id` | integer | 119 |
| `results.TR.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.TR.flatrate[].display_priority` | integer | 4 |
| `results.TR.buy` | array<object> | - |
| `results.TR.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.TR.buy[].provider_id` | integer | 3 |
| `results.TR.buy[].provider_name` | string | Google Play Movies |
| `results.TR.buy[].display_priority` | integer | 5 |
| `results.TT` | object | - |
| `results.TT.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.TT.flatrate` | array<object> | - |
| `results.TT.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.TT.flatrate[].provider_id` | integer | 384 |
| `results.TT.flatrate[].provider_name` | string | HBO Max |
| `results.TT.flatrate[].display_priority` | integer | 11 |
| `results.TW` | object | - |
| `results.TW.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.TW.flatrate` | array<object> | - |
| `results.TW.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.TW.flatrate[].provider_id` | integer | 119 |
| `results.TW.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.TW.flatrate[].display_priority` | integer | 1 |
| `results.UG` | object | - |
| `results.UG.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.UG.rent` | array<object> | - |
| `results.UG.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.UG.rent[].provider_id` | integer | 2 |
| `results.UG.rent[].provider_name` | string | Apple TV |
| `results.UG.rent[].display_priority` | integer | 16 |
| `results.UG.buy` | array<object> | - |
| `results.UG.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.UG.buy[].provider_id` | integer | 2 |
| `results.UG.buy[].provider_name` | string | Apple TV |
| `results.UG.buy[].display_priority` | integer | 16 |
| `results.US` | object | - |
| `results.US.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.US.rent` | array<object> | - |
| `results.US.rent[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.US.rent[].provider_id` | integer | 10 |
| `results.US.rent[].provider_name` | string | Amazon Video |
| `results.US.rent[].display_priority` | integer | 13 |
| `results.US.flatrate` | array<object> | - |
| `results.US.flatrate[].logo_path` | string | /jPXksae158ukMLFhhlNvzsvaEyt.jpg |
| `results.US.flatrate[].provider_id` | integer | 257 |
| `results.US.flatrate[].provider_name` | string | fuboTV |
| `results.US.flatrate[].display_priority` | integer | 5 |
| `results.US.buy` | array<object> | - |
| `results.US.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.US.buy[].provider_id` | integer | 2 |
| `results.US.buy[].provider_name` | string | Apple TV |
| `results.US.buy[].display_priority` | integer | 4 |
| `results.UY` | object | - |
| `results.UY.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.UY.flatrate` | array<object> | - |
| `results.UY.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.UY.flatrate[].provider_id` | integer | 119 |
| `results.UY.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.UY.flatrate[].display_priority` | integer | 1 |
| `results.VE` | object | - |
| `results.VE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.VE.rent` | array<object> | - |
| `results.VE.rent[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.VE.rent[].provider_id` | integer | 3 |
| `results.VE.rent[].provider_name` | string | Google Play Movies |
| `results.VE.rent[].display_priority` | integer | 3 |
| `results.VE.flatrate` | array<object> | - |
| `results.VE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.VE.flatrate[].provider_id` | integer | 119 |
| `results.VE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.VE.flatrate[].display_priority` | integer | 2 |
| `results.VE.buy` | array<object> | - |
| `results.VE.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.VE.buy[].provider_id` | integer | 3 |
| `results.VE.buy[].provider_name` | string | Google Play Movies |
| `results.VE.buy[].display_priority` | integer | 3 |
| `results.YE` | object | - |
| `results.YE.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.YE.flatrate` | array<object> | - |
| `results.YE.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.YE.flatrate[].provider_id` | integer | 119 |
| `results.YE.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.YE.flatrate[].display_priority` | integer | 1 |
| `results.ZA` | object | - |
| `results.ZA.link` | string | https://www.themoviedb.org/movie/550-fight-… |
| `results.ZA.flatrate` | array<object> | - |
| `results.ZA.flatrate[].logo_path` | string | /emthp39XA2YScoYL1p0sdbAH2WA.jpg |
| `results.ZA.flatrate[].provider_id` | integer | 119 |
| `results.ZA.flatrate[].provider_name` | string | Amazon Prime Video |
| `results.ZA.flatrate[].display_priority` | integer | 1 |
| `results.ZA.rent` | array<object> | - |
| `results.ZA.rent[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.ZA.rent[].provider_id` | integer | 2 |
| `results.ZA.rent[].provider_name` | string | Apple TV |
| `results.ZA.rent[].display_priority` | integer | 2 |
| `results.ZA.buy` | array<object> | - |
| `results.ZA.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.ZA.buy[].provider_id` | integer | 2 |
| `results.ZA.buy[].provider_name` | string | Apple TV |
| `results.ZA.buy[].display_priority` | integer | 2 |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "results": {
    "AE": {
      "link": "https://www.themoviedb.org/movie/550-fight-club/watch?locale=AE",
      "flatrate": [
        {
          "logo_path": "/emthp39XA2YScoYL1p0sdbAH2WA.jpg",
          "provider_id": 119,
          "provider_name": "Amazon Prime Video",
          "display_priority": 12
        }
      ],
      "rent": [
        {
          "logo_path": "/peURlLlr8jggOwK53fJ5wdQl05y.jpg",
          "provider_id": 2,
          "provider_name": "Apple TV",
          "display_priority": 1
        },
        "… 1 more items"
      ],
      "buy": [
        {
          "logo_path": "/peURlLlr8jggOwK53fJ5wdQl05y.jpg",
          "provider_id": 2,
          "provider_name": "Apple TV",
          "display_priority": 1
        },
        "… 1 more items"
      ]
    },
    "AL": {
      "link": "https://www.themoviedb.org/movie/550-fight-club/watch?locale=AL",
      "buy": [
        {
          "logo_path": "/5GEbAhFW2S5T8zVc1MNvz00pIzM.jpg",
          "provider_id": 35,
          "provider_name": "Rakuten TV",
          "display_priority": 9
        },
        "… 1 more items"
      ]
    },
    "AR": {
      "link": "https://www.themoviedb.org/movie/550-fight-club/watch?locale=AR",
      "buy": [
        {
          "logo_path": "/tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg",
          "provider_id": 3,
          "provider_name": "Google Play Movies",
          "display_priority": 6
        }
      ],
      "flatrate": [
        {
          "logo_path": "/emthp39XA2YScoYL1p0sdbAH2WA.jpg",
          "provider_id": 119,
          "provider_name": "Amazon Prime Video",
          "display_priority": 2
        },
        "… 3 more items"
      ],
      "rent": [
        {
          "logo_path": "/tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg",
          "provider_id": 3,
          "provider_name": "Google Play Movies",
          "display_priority": 6
        }
      ]
    },
    "AT": {
      "link": "https://www.themoviedb.org/movie/550-fight-club/watch?locale=AT",
      "flatrate": [
        {
          "logo_path": "/7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg",
          "provider_id": 337,
          "provider_name": "Disney Plus",
          "display_priority": 2
        }
      ],
      "buy": [
        {
          "logo_path": "/5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg",
          "provider_id": 10,
          "provider_name": "Amazon Video",
          "display_priority": 3
        },
        "… 6 more items"
      ],
      "rent": [
        {
          "logo_path": "/5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg",
          "provider_id": 10,
          "provider_name": "Amazon Video",

… truncated
```

</details>



### Discover

#### Discover — Movie

```
GET https://api.themoviedb.org/3/discover/movie
```

Find movies using over 30 filters and sort options.

Operation id: `discover-movie`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `certification` | query | string | - | - | use in conjunction with `region` |
| `certification.gte` | query | string | - | - | use in conjunction with `region` |
| `certification.lte` | query | string | - | - | use in conjunction with `region` |
| `certification_country` | query | string | - | - | use in conjunction with the `certification`, `certification.gte` and `certification.lte` filters |
| `include_adult` | query | boolean | - | `false` | - |
| `include_video` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `primary_release_year` | query | integer (int32) | - | - | - |
| `primary_release_date.gte` | query | string (date) | - | - | - |
| `primary_release_date.lte` | query | string (date) | - | - | - |
| `region` | query | string | - | - | - |
| `release_date.gte` | query | string (date) | - | - | - |
| `release_date.lte` | query | string (date) | - | - | - |
| `sort_by` | query | string | - | `popularity.desc` | One of: `original_title.asc`, `original_title.desc`, `popularity.asc`, `popularity.desc`, `revenue.asc`, `revenue.desc`, `primary_release_date.asc`, `title.asc`, `title.desc`, `primary_release_date.desc`, `vote_average.asc`, `vote_average.desc`, `vote_count.asc`, `vote_count.desc` |
| `vote_average.gte` | query | number (float) | - | - | - |
| `vote_average.lte` | query | number (float) | - | - | - |
| `vote_count.gte` | query | number (float) | - | - | - |
| `vote_count.lte` | query | number (float) | - | - | - |
| `watch_region` | query | string | - | - | use in conjunction with `with_watch_monetization_types ` or `with_watch_providers ` |
| `with_cast` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_companies` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_crew` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_genres` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_keywords` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_origin_country` | query | string | - | - | - |
| `with_original_language` | query | string | - | - | - |
| `with_people` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_release_type` | query | integer (int32) | - | - | possible values are: [1, 2, 3, 4, 5, 6] can be a comma (`AND`) or pipe (`OR`) separated query, can be used in conjunction with `region` |
| `with_runtime.gte` | query | integer (int32) | - | - | - |
| `with_runtime.lte` | query | integer (int32) | - | - | - |
| `with_watch_monetization_types` | query | string | - | - | possible values are: [flatrate, free, ads, rent, buy] use in conjunction with `watch_region`, can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_watch_providers` | query | string | - | - | use in conjunction with `watch_region`, can be a comma (`AND`) or pipe (`OR`) separated query |
| `without_companies` | query | string | - | - | - |
| `without_genres` | query | string | - | - | - |
| `without_keywords` | query | string | - | - | - |
| `without_watch_providers` | query | string | - | - | - |
| `year` | query | integer (int32) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /8YFL5QQVPy3AgrEQxNYVSgiPEbe.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 640146 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].overview` | string | Super-Hero partners Scott Lang and Hope van… |
| `results[].popularity` | number | 9272.643 |
| `results[].poster_path` | string | /ngl2FKBlU4fhbdsrtdom9LVLBXw.jpg |
| `results[].release_date` | string | 2023-02-15 |
| `results[].title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 6.5 |
| `results[].vote_count` | integer | 1856 |
| `total_pages` | integer | 38020 |
| `total_results` | integer | 760385 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/8YFL5QQVPy3AgrEQxNYVSgiPEbe.jpg",
      "genre_ids": [
        28,
        12,
        878
      ],
      "id": 640146,
      "original_language": "en",
      "original_title": "Ant-Man and the Wasp: Quantumania",
      "overview": "Super-Hero partners Scott Lang and Hope van Dyne, along with with Hope's parents Janet van Dyne and Hank Pym, and Scott's daughter Cassie Lang, find themselves exploring the Quantum Realm, interacting with strange new creatures and embar...",
      "popularity": 9272.643,
      "poster_path": "/ngl2FKBlU4fhbdsrtdom9LVLBXw.jpg",
      "release_date": "2023-02-15",
      "title": "Ant-Man and the Wasp: Quantumania",
      "video": false,
      "vote_average": 6.5,
      "vote_count": 1856
    },
    {
      "adult": false,
      "backdrop_path": "/iJQIbOPm81fPEGKt5BPuZmfnA54.jpg",
      "genre_ids": [
        16,
        12,
        10751,
        14,
        35
      ],
      "id": 502356,
      "original_language": "en",
      "original_title": "The Super Mario Bros. Movie",
      "overview": "While working underground to fix a water main, Brooklyn plumbers—and brothers—Mario and Luigi are transported down a mysterious pipe and wander into a magical new world. But when the brothers are separated, Mario embarks on an epic quest...",
      "popularity": 7212.464,
      "poster_path": "/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg",
      "release_date": "2023-04-05",
      "title": "The Super Mario Bros. Movie",
      "video": false,
      "vote_average": 7.5,
      "vote_count": 1435
    },
    "… 18 more items"
  ],
  "total_pages": 38020,
  "total_results": 760385
}
```

</details>


#### Discover — TV

```
GET https://api.themoviedb.org/3/discover/tv
```

Find TV shows using over 30 filters and sort options.

Operation id: `discover-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `air_date.gte` | query | string (date) | - | - | - |
| `air_date.lte` | query | string (date) | - | - | - |
| `first_air_date_year` | query | integer (int32) | - | - | - |
| `first_air_date.gte` | query | string (date) | - | - | - |
| `first_air_date.lte` | query | string (date) | - | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `include_null_first_air_dates` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `screened_theatrically` | query | boolean | - | - | - |
| `sort_by` | query | string | - | `popularity.desc` | One of: `first_air_date.asc`, `first_air_date.desc`, `name.asc`, `name.desc`, `original_name.asc`, `original_name.desc`, `popularity.asc`, `popularity.desc`, `vote_average.asc`, `vote_average.desc`, `vote_count.asc`, `vote_count.desc` |
| `timezone` | query | string | - | - | - |
| `vote_average.gte` | query | number (float) | - | - | - |
| `vote_average.lte` | query | number (float) | - | - | - |
| `vote_count.gte` | query | number (float) | - | - | - |
| `vote_count.lte` | query | number (float) | - | - | - |
| `watch_region` | query | string | - | - | use in conjunction with `with_watch_monetization_types ` or `with_watch_providers ` |
| `with_companies` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_genres` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_keywords` | query | string | - | - | can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_networks` | query | integer (int32) | - | - | - |
| `with_origin_country` | query | string | - | - | - |
| `with_original_language` | query | string | - | - | - |
| `with_runtime.gte` | query | integer (int32) | - | - | - |
| `with_runtime.lte` | query | integer (int32) | - | - | - |
| `with_status` | query | string | - | - | possible values are: [0, 1, 2, 3, 4, 5], can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_watch_monetization_types` | query | string | - | - | possible values are: [flatrate, free, ads, rent, buy] use in conjunction with `watch_region`, can be a comma (`AND`) or pipe (`OR`) separated query |
| `with_watch_providers` | query | string | - | - | use in conjunction with `watch_region`, can be a comma (`AND`) or pipe (`OR`) separated query |
| `without_companies` | query | string | - | - | - |
| `without_genres` | query | string | - | - | - |
| `without_keywords` | query | string | - | - | - |
| `without_watch_providers` | query | string | - | - | - |
| `with_type` | query | string | - | - | possible values are: [0, 1, 2, 3, 4, 5, 6], can be a comma (`AND`) or pipe (`OR`) separated query |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].backdrop_path` | string | /mAJ84W6I8I272Da87qplS2Dp9ST.jpg |
| `results[].first_air_date` | string | 2023-01-23 |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 202250 |
| `results[].name` | string | Dirty Linen |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | tl |
| `results[].original_name` | string | Dirty Linen |
| `results[].overview` | string | To exact vengeance, a young woman infiltrat… |
| `results[].popularity` | number | 2684.061 |
| `results[].poster_path` | string | /ujlkQtHAVShWyWTloGU2Vh5Jbo9.jpg |
| `results[].vote_average` | integer | 5 |
| `results[].vote_count` | integer | 13 |
| `total_pages` | integer | 7414 |
| `total_results` | integer | 148265 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "backdrop_path": "/mAJ84W6I8I272Da87qplS2Dp9ST.jpg",
      "first_air_date": "2023-01-23",
      "genre_ids": [
        9648,
        18
      ],
      "id": 202250,
      "name": "Dirty Linen",
      "origin_country": [
        "PH"
      ],
      "original_language": "tl",
      "original_name": "Dirty Linen",
      "overview": "To exact vengeance, a young woman infiltrates the household of an influential family as a housemaid to expose their dirty secrets. However, love will get in the way of her revenge plot.",
      "popularity": 2684.061,
      "poster_path": "/ujlkQtHAVShWyWTloGU2Vh5Jbo9.jpg",
      "vote_average": 5,
      "vote_count": 13
    },
    {
      "backdrop_path": "/wJmcuxa0C4AERmA9mejxm9qRYDj.jpg",
      "first_air_date": "2022-06-06",
      "genre_ids": [
        80,
        9648
      ],
      "id": 203504,
      "name": "Aashiqana",
      "origin_country": [
        "IN"
      ],
      "original_language": "hi",
      "original_name": "आशिकाना",
      "overview": "A serial killer sparks the story of uptight Yashvardhan and feisty Chikki. Plagued by misunderstandings, how far do they have to go to nab the murderer?",
      "popularity": 2658.037,
      "poster_path": "/a4Z6Uohb6Ln5vcPvMUzwyn3WBjP.jpg",
      "vote_average": 6,
      "vote_count": 8
    },
    "… 18 more items"
  ],
  "total_pages": 7414,
  "total_results": 148265
}
```

</details>



### Search

#### Search — Collection

```
GET https://api.themoviedb.org/3/search/collection
```

Search for collections by their original, translated and alternative names.

Operation id: `search-collection`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /zuW6fOiusv4X9nnW3paHGfXcSll.jpg |
| `results[].id` | integer | 86311 |
| `results[].name` | string | The Avengers Collection |
| `results[].original_language` | string | en |
| `results[].original_name` | string | The Avengers Collection |
| `results[].overview` | string | A superhero film series produced by Marvel … |
| `results[].poster_path` | string | /yFSIUVTCvgYrpalUktulvk3Gi5Y.jpg |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/zuW6fOiusv4X9nnW3paHGfXcSll.jpg",
      "id": 86311,
      "name": "The Avengers Collection",
      "original_language": "en",
      "original_name": "The Avengers Collection",
      "overview": "A superhero film series produced by Marvel Studios based on the Marvel Comics superhero team of the same name, and part of the Marvel Cinematic Universe (MCU).  The series features an ensemble cast from the Marvel Cinematic Universe seri...",
      "poster_path": "/yFSIUVTCvgYrpalUktulvk3Gi5Y.jpg"
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>


#### Search — Company

```
GET https://api.themoviedb.org/3/search/company
```

Search for companies by their original and alternative names.

Operation id: `search-company`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].id` | integer | 3268 |
| `results[].logo_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `results[].name` | string | HBO |
| `results[].origin_country` | string | US |
| `total_pages` | integer | 2 |
| `total_results` | integer | 22 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "id": 3268,
      "logo_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
      "name": "HBO",
      "origin_country": "US"
    },
    {
      "id": 142414,
      "logo_path": "/tC3oVZvqpwhAG1uSjpMnmj4nW3O.png",
      "name": "HBO Asia",
      "origin_country": "SG"
    },
    "… 18 more items"
  ],
  "total_pages": 2,
  "total_results": 22
}
```

</details>


#### Search — Keyword

```
GET https://api.themoviedb.org/3/search/keyword
```

Search for keywords by their name.

Operation id: `search-keyword`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].id` | integer | 262419 |
| `results[].name` | string | lost |
| `total_pages` | integer | 5 |
| `total_results` | integer | 84 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "id": 262419,
      "name": "lost"
    },
    {
      "id": 5930,
      "name": "getting lost"
    },
    "… 18 more items"
  ],
  "total_pages": 5,
  "total_results": 84
}
```

</details>


#### Search — Movie

```
GET https://api.themoviedb.org/3/search/movie
```

Search for movies by their original, translated and alternative titles.

Operation id: `search-movie`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `primary_release_year` | query | string | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `region` | query | string | - | - | - |
| `year` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /hZkgoQYus5vegHoetLkCJzb17zJ.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 550 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Fight Club |
| `results[].overview` | string | A ticking-time-bomb insomniac and a slipper… |
| `results[].popularity` | number | 73.433 |
| `results[].poster_path` | string | /pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg |
| `results[].release_date` | string | 1999-10-15 |
| `results[].title` | string | Fight Club |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.433 |
| `results[].vote_count` | integer | 26279 |
| `total_pages` | integer | 2 |
| `total_results` | integer | 39 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
      "genre_ids": [
        18,
        53,
        35
      ],
      "id": 550,
      "original_language": "en",
      "original_title": "Fight Club",
      "overview": "A ticking-time-bomb insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy. Their concept catches on, with underground \"fight clubs\" forming in every town, until an eccentric gets in the...",
      "popularity": 73.433,
      "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
      "release_date": "1999-10-15",
      "title": "Fight Club",
      "video": false,
      "vote_average": 8.433,
      "vote_count": 26279
    },
    {
      "adult": false,
      "backdrop_path": "/1VqE5z4VIOcNcajJuHLk4fDkY9G.jpg",
      "genre_ids": [
        28,
        27
      ],
      "id": 289732,
      "original_language": "zh",
      "original_title": "屍城",
      "overview": "It's the end of the century at a corner of the city in a building riddled with crime - Everyone in the building has turned into zombies. After Jenny's boyfriend is killed in a zombie attack, she faces the challenge of surviving in the fa...",
      "popularity": 15.263,
      "poster_path": "/u8u3KVq0qfJYmNDsaTVOXy4So6f.jpg",
      "release_date": "2014-10-23",
      "title": "Zombie Fight Club",
      "video": false,
      "vote_average": 4.721,
      "vote_count": 52
    },
    "… 18 more items"
  ],
  "total_pages": 2,
  "total_results": 39
}
```

</details>


#### Search — Multi

```
GET https://api.themoviedb.org/3/search/multi
```

Use multi search when you want to search for movies, TV shows and people in a single request.

Operation id: `search-multi`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /aDYSnJAK0BTVeE8osOy22Kz3SXY.jpg |
| `results[].id` | integer | 11 |
| `results[].title` | string | Star Wars |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Star Wars |
| `results[].overview` | string | Princess Leia is captured and held hostage … |
| `results[].poster_path` | string | /6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 78.047 |
| `results[].release_date` | string | 1977-05-25 |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.208 |
| `results[].vote_count` | integer | 18528 |
| `results[].name` | string | - |
| `results[].original_name` | string | - |
| `total_pages` | integer | 11 |
| `total_results` | integer | 201 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/aDYSnJAK0BTVeE8osOy22Kz3SXY.jpg",
      "id": 11,
      "title": "Star Wars",
      "original_language": "en",
      "original_title": "Star Wars",
      "overview": "Princess Leia is captured and held hostage by the evil Imperial forces in their effort to take over the galactic Empire. Venturesome Luke Skywalker and dashing captain Han Solo team together with the loveable robot duo R2-D2 and C-3PO to...",
      "poster_path": "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg",
      "media_type": "movie",
      "genre_ids": [
        12,
        28,
        878
      ],
      "popularity": 78.047,
      "release_date": "1977-05-25",
      "video": false,
      "vote_average": 8.208,
      "vote_count": 18528
    },
    {
      "adult": false,
      "backdrop_path": "/iflKt34Ck2JpY2PY9wW1zwdJgJi.jpg",
      "id": 782054,
      "title": "Doraemon: Nobita's Little Star Wars 2021",
      "original_language": "ja",
      "original_title": "映画ドラえもん のび太の宇宙小戦争 2021",
      "overview": "One day during summer vacation, a palm-sized alien named Papi appears from a small rocket that Nobita picks up. He is the president of Pirika, a small planet in outer space, and has come to Earth to escape the rebels. Doraemon and his fr...",
      "poster_path": "/48gKZioIDeUOI0afbYv3kh9u9RQ.jpg",
      "media_type": "movie",
      "genre_ids": [
        16,
        878,
        12
      ],
      "popularity": 33.466,
      "release_date": "2022-03-04",
      "video": false,
      "vote_average": 5.825,
      "vote_count": 60
    },
    "… 18 more items"
  ],
  "total_pages": 11,
  "total_results": 201
}
```

</details>


#### Search — Person

```
GET https://api.themoviedb.org/3/search/person
```

Search for people by their name and also known as names.

Operation id: `search-person`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].gender` | integer | 2 |
| `results[].id` | integer | 31 |
| `results[].known_for_department` | string | Acting |
| `results[].name` | string | Tom Hanks |
| `results[].original_name` | string | Tom Hanks |
| `results[].popularity` | number | 84.631 |
| `results[].profile_path` | string | /xndWFsBlClOJFRdhSt4NBwiPq2o.jpg |
| `results[].known_for` | array<object> | - |
| `results[].known_for[].adult` | boolean | false |
| `results[].known_for[].backdrop_path` | string | /3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg |
| `results[].known_for[].id` | integer | 13 |
| `results[].known_for[].title` | string | Forrest Gump |
| `results[].known_for[].original_language` | string | en |
| `results[].known_for[].original_title` | string | Forrest Gump |
| `results[].known_for[].overview` | string | A man with a low IQ has accomplished great … |
| `results[].known_for[].poster_path` | string | /arw2vcBveWOVZr6pxd9XTd1TdQa.jpg |
| `results[].known_for[].media_type` | string | movie |
| `results[].known_for[].genre_ids` | array<integer> | - |
| `results[].known_for[].popularity` | number | 67.209 |
| `results[].known_for[].release_date` | string | 1994-06-23 |
| `results[].known_for[].video` | boolean | false |
| `results[].known_for[].vote_average` | number | 8.481 |
| `results[].known_for[].vote_count` | integer | 24525 |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "gender": 2,
      "id": 31,
      "known_for_department": "Acting",
      "name": "Tom Hanks",
      "original_name": "Tom Hanks",
      "popularity": 84.631,
      "profile_path": "/xndWFsBlClOJFRdhSt4NBwiPq2o.jpg",
      "known_for": [
        {
          "adult": false,
          "backdrop_path": "/3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg",
          "id": 13,
          "title": "Forrest Gump",
          "original_language": "en",
          "original_title": "Forrest Gump",
          "overview": "A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do. But despite all he has achieved, his one true love eludes him.",
          "poster_path": "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
          "media_type": "movie",
          "genre_ids": [
            35,
            18,
            10749
          ],
          "popularity": 67.209,
          "release_date": "1994-06-23",
          "video": false,
          "vote_average": 8.481,
          "vote_count": 24525
        },
        {
          "adult": false,
          "backdrop_path": "/3Rfvhy1Nl6sSGJwyjb0QiZzZYlB.jpg",
          "id": 862,
          "title": "Toy Story",
          "original_language": "en",
          "original_title": "Toy Story",
          "overview": "Led by Woody, Andy's toys live happily in his room until Andy's birthday brings Buzz Lightyear onto the scene. Afraid of losing his place in Andy's heart, Woody plots against Buzz. But when circumstances separate Buzz and Woody from thei...",
          "poster_path": "/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg",
          "media_type": "movie",
          "genre_ids": [
            16,
            12,
            10751,
            35
          ],
          "popularity": 119.802,
          "release_date": "1995-10-30",
          "video": false,
          "vote_average": 7.969,
          "vote_count": 16613
        },
        "… 1 more items"
      ]
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>


#### Search — TV

```
GET https://api.themoviedb.org/3/search/tv
```

Search for TV shows by their original, translated and also known as names.

Operation id: `search-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `query` | query | string | yes | - | - |
| `first_air_date_year` | query | integer (int32) | - | - | Search only the first air date. Valid values are: 1000..9999 |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `year` | query | integer (int32) | - | - | Search the first air date and all episode air dates. Valid values are: 1000..9999 |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1396 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Breaking Bad |
| `results[].overview` | string | When Walter White, a New Mexico chemistry t… |
| `results[].popularity` | number | 298.884 |
| `results[].poster_path` | string | /ggFHVNu6YYI5L9pCfOacjizRGt.jpg |
| `results[].first_air_date` | string | 2008-01-20 |
| `results[].name` | string | Breaking Bad |
| `results[].vote_average` | number | 8.879 |
| `results[].vote_count` | integer | 11536 |
| `total_pages` | integer | 1 |
| `total_results` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 1396,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Breaking Bad",
      "overview": "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of only two years left to live. He becomes filled with a sense of fearlessness and an unrelenting desire to secure his family's f...",
      "popularity": 298.884,
      "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
      "first_air_date": "2008-01-20",
      "name": "Breaking Bad",
      "vote_average": 8.879,
      "vote_count": 11536
    }
  ],
  "total_pages": 1,
  "total_results": 1
}
```

</details>



### Find

#### Find — Find By ID

```
GET https://api.themoviedb.org/3/find/{external_id}
```

Find data by external ID's.

Operation id: `find-by-id`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `external_id` | path | string | yes | - | - |
| `external_source` | query | string | yes | - | One of: `imdb_id`, `facebook_id`, `instagram_id`, `tvdb_id`, `tiktok_id`, `twitter_id`, `wikidata_id`, `youtube_id` |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `movie_results` | array<object> | - |
| `movie_results[].adult` | boolean | false |
| `movie_results[].backdrop_path` | string | /44immBwzhDVyjn87b3x3l9mlhAD.jpg |
| `movie_results[].id` | integer | 934433 |
| `movie_results[].title` | string | Scream VI |
| `movie_results[].original_language` | string | en |
| `movie_results[].original_title` | string | Scream VI |
| `movie_results[].overview` | string | Following the latest Ghostface killings, th… |
| `movie_results[].poster_path` | string | /wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg |
| `movie_results[].media_type` | string | movie |
| `movie_results[].genre_ids` | array<integer> | - |
| `movie_results[].popularity` | number | 853.917 |
| `movie_results[].release_date` | string | 2023-03-08 |
| `movie_results[].video` | boolean | false |
| `movie_results[].vote_average` | number | 7.388 |
| `movie_results[].vote_count` | integer | 708 |
| `person_results` | array<object> | - |
| `tv_results` | array<object> | - |
| `tv_episode_results` | array<object> | - |
| `tv_season_results` | array<object> | - |

<details><summary>Example response</summary>

```json
{
  "movie_results": [
    {
      "adult": false,
      "backdrop_path": "/44immBwzhDVyjn87b3x3l9mlhAD.jpg",
      "id": 934433,
      "title": "Scream VI",
      "original_language": "en",
      "original_title": "Scream VI",
      "overview": "Following the latest Ghostface killings, the four survivors leave Woodsboro behind and start a fresh chapter.",
      "poster_path": "/wDWwtvkRRlgTiUr6TyLSMX8FCuZ.jpg",
      "media_type": "movie",
      "genre_ids": [
        27,
        9648,
        53
      ],
      "popularity": 853.917,
      "release_date": "2023-03-08",
      "video": false,
      "vote_average": 7.388,
      "vote_count": 708
    }
  ],
  "person_results": [],
  "tv_results": [],
  "tv_episode_results": [],
  "tv_season_results": []
}
```

</details>



### Genres

#### Genres — Movie List

```
GET https://api.themoviedb.org/3/genre/movie/list
```

Get the list of official genres for movies.

Operation id: `genre-movie-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `genres` | array<object> | - |
| `genres[].id` | integer | 28 |
| `genres[].name` | string | Action |

<details><summary>Example response</summary>

```json
{
  "genres": [
    {
      "id": 28,
      "name": "Action"
    },
    {
      "id": 12,
      "name": "Abenteuer"
    },
    "… 17 more items"
  ]
}
```

</details>


#### Genres — TV List

```
GET https://api.themoviedb.org/3/genre/tv/list
```

Get the list of official genres for TV shows.

Operation id: `genre-tv-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `genres` | array<object> | - |
| `genres[].id` | integer | 10759 |
| `genres[].name` | string | Action & Adventure |

<details><summary>Example response</summary>

```json
{
  "genres": [
    {
      "id": 10759,
      "name": "Action & Adventure"
    },
    {
      "id": 16,
      "name": "Animation"
    },
    "… 14 more items"
  ]
}
```

</details>



### Keywords

#### Keywords — Details

```
GET https://api.themoviedb.org/3/keyword/{keyword_id}
```

Operation id: `keyword-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `keyword_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1701 |
| `name` | string | hero |

<details><summary>Example response</summary>

```json
{
  "id": 1701,
  "name": "hero"
}
```

</details>


#### Keywords — Movies

```
GET https://api.themoviedb.org/3/keyword/{keyword_id}/movies
```

Operation id: `keyword-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `keyword_id` | path | string | yes | - | - |
| `include_adult` | query | boolean | - | `false` | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1701 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /3CxUndGhUcZdt1Zggjdb2HkLLQX.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 640146 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].overview` | string | Das Superhelden-Duo Scott Lang und Hope Van… |
| `results[].popularity` | number | 9200.005 |
| `results[].poster_path` | string | /nA5otwVxAfpBP4PVgeuBk3qHcLY.jpg |
| `results[].release_date` | string | 2023-02-15 |
| `results[].title` | string | Ant-Man and the Wasp: Quantumania |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 6.5 |
| `results[].vote_count` | integer | 2079 |
| `total_pages` | integer | 11 |
| `total_results` | integer | 211 |

<details><summary>Example response</summary>

```json
{
  "id": 1701,
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/3CxUndGhUcZdt1Zggjdb2HkLLQX.jpg",
      "genre_ids": [
        28,
        12,
        878
      ],
      "id": 640146,
      "original_language": "en",
      "original_title": "Ant-Man and the Wasp: Quantumania",
      "overview": "Das Superhelden-Duo Scott Lang und Hope Van Dyne erkundet zusammen mit Hopes Eltern Hank Pym und Janet Van Dyne das Quantenreich, interagiert mit seltsamen neuen Kreaturen und begibt sich auf ein Abenteuer, das sie über die Grenzen desse...",
      "popularity": 9200.005,
      "poster_path": "/nA5otwVxAfpBP4PVgeuBk3qHcLY.jpg",
      "release_date": "2023-02-15",
      "title": "Ant-Man and the Wasp: Quantumania",
      "video": false,
      "vote_average": 6.5,
      "vote_count": 2079
    },
    {
      "adult": false,
      "backdrop_path": "/xDMIl84Qo5Tsu62c9DGWhmPI67A.jpg",
      "genre_ids": [
        28,
        12,
        878
      ],
      "id": 505642,
      "original_language": "en",
      "original_title": "Black Panther: Wakanda Forever",
      "overview": "Der König ist tot! König T'Challa, der als Black Panther auch Mitglied der Avengers-Heldentruppe war, stirbt an einer unbekannten Krankheit und ganz Wakanda ist in Trauer. Die Weltgemeinschaft sieht ihre Chance gekommen, um das vermeintl...",
      "popularity": 931.513,
      "poster_path": "/vo9QISNKYtDN40VKfQkPxbMOO9j.jpg",
      "release_date": "2022-11-09",
      "title": "Black Panther: Wakanda Forever",
      "video": false,
      "vote_average": 7.3,
      "vote_count": 4695
    },
    "… 18 more items"
  ],
  "total_pages": 11,
  "total_results": 211
}
```

</details>



### Collections

#### Collections — Details

```
GET https://api.themoviedb.org/3/collection/{collection_id}
```

Get collection details by ID.

Operation id: `collection-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `collection_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 10 |
| `name` | string | Star Wars Collection |
| `original_language` | string | en |
| `original_name` | string | Star Wars Collection |
| `overview` | string | An epic space-opera theatrical film series,… |
| `poster_path` | string | /22dj38IckjzEEUZwN1tPU5VJ1qq.jpg |
| `backdrop_path` | string | /4z9ijhgEthfRHShoOvMaBlpciXS.jpg |
| `parts` | array<object> | - |
| `parts[].adult` | boolean | false |
| `parts[].backdrop_path` | string | /2w4xG178RpB4MDAIfTkqAuSJzec.jpg |
| `parts[].id` | integer | 11 |
| `parts[].name` | string | Star Wars |
| `parts[].original_name` | string | Star Wars |
| `parts[].overview` | string | Princess Leia is captured and held hostage … |
| `parts[].poster_path` | string | /6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg |
| `parts[].media_type` | string | movie |
| `parts[].original_language` | string | en |
| `parts[].genre_ids` | array<integer> | - |
| `parts[].popularity` | number | 15.8557 |
| `parts[].release_date` | string | 1977-05-25 |
| `parts[].video` | boolean | false |
| `parts[].vote_average` | number | 8.205 |
| `parts[].vote_count` | integer | 21522 |

<details><summary>Example response</summary>

```json
{
  "id": 10,
  "name": "Star Wars Collection",
  "original_language": "en",
  "original_name": "Star Wars Collection",
  "overview": "An epic space-opera theatrical film series, which depicts the adventures of various characters \"a long time ago in a galaxy far, far away….\"",
  "poster_path": "/22dj38IckjzEEUZwN1tPU5VJ1qq.jpg",
  "backdrop_path": "/4z9ijhgEthfRHShoOvMaBlpciXS.jpg",
  "parts": [
    {
      "adult": false,
      "backdrop_path": "/2w4xG178RpB4MDAIfTkqAuSJzec.jpg",
      "id": 11,
      "name": "Star Wars",
      "original_name": "Star Wars",
      "overview": "Princess Leia is captured and held hostage by the evil Imperial forces in their effort to take over the galactic Empire. Venturesome Luke Skywalker and dashing captain Han Solo team together with the loveable robot duo R2-D2 and C-3PO to...",
      "poster_path": "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg",
      "media_type": "movie",
      "original_language": "en",
      "genre_ids": [
        12,
        28,
        878
      ],
      "popularity": 15.8557,
      "release_date": "1977-05-25",
      "video": false,
      "vote_average": 8.205,
      "vote_count": 21522
    },
    {
      "adult": false,
      "backdrop_path": "/dMZxEdrWIzUmUoOz2zvmFuutbj7.jpg",
      "id": 1891,
      "name": "The Empire Strikes Back",
      "original_name": "The Empire Strikes Back",
      "overview": "The epic saga continues as Luke Skywalker, in hopes of defeating the evil Galactic Empire, learns the ways of the Jedi from aging master Yoda. But Darth Vader is more determined than ever to capture Luke. Meanwhile, rebel leader Princess...",
      "poster_path": "/nNAeTmF4CtdSgMDplXTDPOpYzsX.jpg",
      "media_type": "movie",
      "original_language": "en",
      "genre_ids": [
        12,
        28,
        878
      ],
      "popularity": 6.7352,
      "release_date": "1980-05-20",
      "video": false,
      "vote_average": 8.395,
      "vote_count": 17713
    },
    "… 7 more items"
  ]
}
```

</details>


#### Collections — Images

```
GET https://api.themoviedb.org/3/collection/{collection_id}/images
```

Get the images that belong to a collection.

Operation id: `collection-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `collection_id` | path | integer (int32) | yes | - | - |
| `include_image_language` | query | string | - | - | specify a comma separated list of ISO-639-1 values to query, for example: `en-US,null` |
| `language` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 10 |
| `backdrops` | array<object> | - |
| `backdrops[].aspect_ratio` | number | 1.778 |
| `backdrops[].height` | integer | 1080 |
| `backdrops[].iso_639_1` | object | - |
| `backdrops[].file_path` | string | /d8duYyyC9J5T825Hg7grmaabfxQ.jpg |
| `backdrops[].vote_average` | number | 5.464 |
| `backdrops[].vote_count` | integer | 30 |
| `backdrops[].width` | integer | 1920 |
| `posters` | array<object> | - |
| `posters[].aspect_ratio` | number | 0.667 |
| `posters[].height` | integer | 3000 |
| `posters[].iso_639_1` | string | en |
| `posters[].file_path` | string | /r8Ph5MYXL04Qzu4QBbq2KjqwtkQ.jpg |
| `posters[].vote_average` | number | 5.516 |
| `posters[].vote_count` | integer | 14 |
| `posters[].width` | integer | 2000 |

<details><summary>Example response</summary>

```json
{
  "id": 10,
  "backdrops": [
    {
      "aspect_ratio": 1.778,
      "height": 1080,
      "iso_639_1": null,
      "file_path": "/d8duYyyC9J5T825Hg7grmaabfxQ.jpg",
      "vote_average": 5.464,
      "vote_count": 30,
      "width": 1920
    },
    {
      "aspect_ratio": 1.778,
      "height": 720,
      "iso_639_1": null,
      "file_path": "/zZDkgOmFMVYpGAkR9Tkxw0CRnxX.jpg",
      "vote_average": 5.454,
      "vote_count": 3,
      "width": 1280
    },
    "… 31 more items"
  ],
  "posters": [
    {
      "aspect_ratio": 0.667,
      "height": 3000,
      "iso_639_1": "en",
      "file_path": "/r8Ph5MYXL04Qzu4QBbq2KjqwtkQ.jpg",
      "vote_average": 5.516,
      "vote_count": 14,
      "width": 2000
    },
    {
      "aspect_ratio": 0.667,
      "height": 3000,
      "iso_639_1": "pt",
      "file_path": "/suaJuFprbgdZaTE0mOt0xWIGFyQ.jpg",
      "vote_average": 5.454,
      "vote_count": 3,
      "width": 2000
    },
    "… 107 more items"
  ]
}
```

</details>


#### Collections — Translations

```
GET https://api.themoviedb.org/3/collection/{collection_id}/translations
```

Operation id: `collection-translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `collection_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 10 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | AE |
| `translations[].iso_639_1` | string | ar |
| `translations[].name` | string | العربية |
| `translations[].english_name` | string | Arabic |
| `translations[].data` | object | - |
| `translations[].data.title` | string | - |
| `translations[].data.overview` | string | - |
| `translations[].data.homepage` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 10,
  "translations": [
    {
      "iso_3166_1": "AE",
      "iso_639_1": "ar",
      "name": "العربية",
      "english_name": "Arabic",
      "data": {
        "title": "",
        "overview": "",
        "homepage": ""
      }
    },
    {
      "iso_3166_1": "BG",
      "iso_639_1": "bg",
      "name": "български език",
      "english_name": "Bulgarian",
      "data": {
        "title": "Междузвездни войни (поредица)",
        "overview": "Междузвездни войни е поредица от шест филма проследяващи живота на Анакин Скайуокър. Открит от джедая Куай-Гон Джин на пустинната планета Татуин Анакин е все още дете, но постепенно се превръща в силния Дарт Вейдър, дясната ръка на Импер...",
        "homepage": ""
      }
    },
    "… 39 more items"
  ]
}
```

</details>



### Companies

#### Companies — Details

```
GET https://api.themoviedb.org/3/company/{company_id}
```

Get the company details by ID.

Operation id: `company-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `company_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `description` | string | - |
| `headquarters` | string | San Francisco, California |
| `homepage` | string | https://www.lucasfilm.com |
| `id` | integer | 1 |
| `logo_path` | string | /o86DbpburjxrqAzEDhXZcyE8pDb.png |
| `name` | string | Lucasfilm Ltd. |
| `origin_country` | string | US |
| `parent_company` | object | - |

<details><summary>Example response</summary>

```json
{
  "description": "",
  "headquarters": "San Francisco, California",
  "homepage": "https://www.lucasfilm.com",
  "id": 1,
  "logo_path": "/o86DbpburjxrqAzEDhXZcyE8pDb.png",
  "name": "Lucasfilm Ltd.",
  "origin_country": "US",
  "parent_company": null
}
```

</details>


#### Companies — Alternative Names

```
GET https://api.themoviedb.org/3/company/{company_id}/alternative_names
```

Get the company details by ID.

Operation id: `company-alternative-names`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `company_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1 |
| `results` | array<object> | - |
| `results[].name` | string | 루카스필름 |
| `results[].type` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 1,
  "results": [
    {
      "name": "루카스필름",
      "type": ""
    },
    {
      "name": "Lucasfilm Limited, LLC",
      "type": ""
    },
    "… 2 more items"
  ]
}
```

</details>


#### Companies — Images

```
GET https://api.themoviedb.org/3/company/{company_id}/images
```

Get the company logos by id.

Operation id: `company-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `company_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1 |
| `logos` | array<object> | - |
| `logos[].aspect_ratio` | number | 2.97979797979798 |
| `logos[].file_path` | string | /o86DbpburjxrqAzEDhXZcyE8pDb.png |
| `logos[].height` | integer | 99 |
| `logos[].id` | string | 5aa080d6c3a3683fea00011e |
| `logos[].file_type` | string | .svg |
| `logos[].vote_average` | number | 5.384 |
| `logos[].vote_count` | integer | 2 |
| `logos[].width` | integer | 295 |

<details><summary>Example response</summary>

```json
{
  "id": 1,
  "logos": [
    {
      "aspect_ratio": 2.97979797979798,
      "file_path": "/o86DbpburjxrqAzEDhXZcyE8pDb.png",
      "height": 99,
      "id": "5aa080d6c3a3683fea00011e",
      "file_type": ".svg",
      "vote_average": 5.384,
      "vote_count": 2,
      "width": 295
    },
    {
      "aspect_ratio": 3.03951367781155,
      "file_path": "/tlVSws0RvvtPBwViUyOFAO0vcQS.png",
      "height": 329,
      "id": "63306b352b8a430096598b3d",
      "file_type": ".svg",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 1000
    }
  ]
}
```

</details>



### Credits

#### Credits — Details

```
GET https://api.themoviedb.org/3/credit/{credit_id}
```

Get a movie or TV credit details by ID.

Operation id: `credit-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `credit_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `credit_type` | string | cast |
| `department` | string | Acting |
| `job` | string | Actor |
| `media` | object | - |
| `media.adult` | boolean | false |
| `media.backdrop_path` | string | /uDgy6hyPd82kOHh6I95FLtLnj6p.jpg |
| `media.id` | integer | 100088 |
| `media.name` | string | The Last of Us |
| `media.original_language` | string | en |
| `media.original_name` | string | The Last of Us |
| `media.overview` | string | Zwanzig Jahre nachdem die moderne Zivilisat… |
| `media.poster_path` | string | /igwIPNClQpGVzb61QlGqcpT5zUy.jpg |
| `media.media_type` | string | tv |
| `media.genre_ids` | array<integer> | - |
| `media.popularity` | number | 898.378 |
| `media.first_air_date` | string | 2023-01-15 |
| `media.vote_average` | number | 8.749 |
| `media.vote_count` | integer | 3341 |
| `media.origin_country` | array<string> | - |
| `media.character` | string | Joel Miller |
| `media.episodes` | array<object> | - |
| `media.seasons` | array<object> | - |
| `media.seasons[].air_date` | string | 2023-01-15 |
| `media.seasons[].episode_count` | integer | 9 |
| `media.seasons[].id` | integer | 144593 |
| `media.seasons[].name` | string | Staffel 1 |
| `media.seasons[].overview` | string | Die 1. Staffel der Endzeit-Horrorserie The … |
| `media.seasons[].poster_path` | string | /aUQKIpZZ31KWbpdHMCmaV76u78T.jpg |
| `media.seasons[].season_number` | integer | 1 |
| `media.seasons[].show_id` | integer | 100088 |
| `media_type` | string | tv |
| `id` | string | 6024a814c0ae36003d59cc3c |
| `person` | object | - |
| `person.adult` | boolean | false |
| `person.id` | integer | 1253360 |
| `person.name` | string | Pedro Pascal |
| `person.original_name` | string | Pedro Pascal |
| `person.media_type` | string | person |
| `person.popularity` | number | 106.095 |
| `person.gender` | integer | 2 |
| `person.known_for_department` | string | Acting |
| `person.profile_path` | string | /dBOrm29cr7NUrjiDQMTtrTyDpfy.jpg |

<details><summary>Example response</summary>

```json
{
  "credit_type": "cast",
  "department": "Acting",
  "job": "Actor",
  "media": {
    "adult": false,
    "backdrop_path": "/uDgy6hyPd82kOHh6I95FLtLnj6p.jpg",
    "id": 100088,
    "name": "The Last of Us",
    "original_language": "en",
    "original_name": "The Last of Us",
    "overview": "Zwanzig Jahre nachdem die moderne Zivilisation zerstört wurde. – Joel, ein abgehärteter Überlebender, wird angeheuert, um Ellie, ein 14-jähriges Mädchen, aus einer bedrückenden Quarantänezone zu schmuggeln. Was als kleiner Job beginnt, w...",
    "poster_path": "/igwIPNClQpGVzb61QlGqcpT5zUy.jpg",
    "media_type": "tv",
    "genre_ids": [
      18
    ],
    "popularity": 898.378,
    "first_air_date": "2023-01-15",
    "vote_average": 8.749,
    "vote_count": 3341,
    "origin_country": [
      "US"
    ],
    "character": "Joel Miller",
    "episodes": [],
    "seasons": [
      {
        "air_date": "2023-01-15",
        "episode_count": 9,
        "id": 144593,
        "name": "Staffel 1",
        "overview": "Die 1. Staffel der Endzeit-Horrorserie The Last of Us feierte ihre Premiere am 15. Januar 2023 bei HBO. In Staffel 1 beginnt für den Überlebenden Joel und das Mädchen Ellie eine Reise durch das postapokalyptische Amerika, in dem Plündere...",
        "poster_path": "/aUQKIpZZ31KWbpdHMCmaV76u78T.jpg",
        "season_number": 1,
        "show_id": 100088
      }
    ]
  },
  "media_type": "tv",
  "id": "6024a814c0ae36003d59cc3c",
  "person": {
    "adult": false,
    "id": 1253360,
    "name": "Pedro Pascal",
    "original_name": "Pedro Pascal",
    "media_type": "person",
    "popularity": 106.095,
    "gender": 2,
    "known_for_department": "Acting",
    "profile_path": "/dBOrm29cr7NUrjiDQMTtrTyDpfy.jpg"
  }
}
```

</details>



### Certifications

#### Certifications — Movie Certifications

```
GET https://api.themoviedb.org/3/certification/movie/list
```

Get an up to date list of the officially supported movie certifications on TMDB.

Operation id: `certification-movie-list`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `certifications` | object | - |
| `certifications.AU` | array<object> | - |
| `certifications.AU[].certification` | string | E |
| `certifications.AU[].meaning` | string | Exempt from classification. Films that are … |
| `certifications.AU[].order` | integer | 1 |
| `certifications.BG` | array<object> | - |
| `certifications.BG[].certification` | string | D |
| `certifications.BG[].meaning` | string | Prohibited for persons under 16. |
| `certifications.BG[].order` | integer | 4 |
| `certifications.BR` | array<object> | - |
| `certifications.BR[].certification` | string | 14 |
| `certifications.BR[].meaning` | string | Not recommended for minors under fourteen. … |
| `certifications.BR[].order` | integer | 4 |
| `certifications.CA` | array<object> | - |
| `certifications.CA[].certification` | string | G |
| `certifications.CA[].meaning` | string | All ages. |
| `certifications.CA[].order` | integer | 2 |
| `certifications.CA-QC` | array<object> | - |
| `certifications.CA-QC[].certification` | string | NR |
| `certifications.CA-QC[].meaning` | string | No rating information. |
| `certifications.CA-QC[].order` | integer | 0 |
| `certifications.DE` | array<object> | - |
| `certifications.DE[].certification` | string | 12 |
| `certifications.DE[].meaning` | string | Children 12 or older admitted, children bet… |
| `certifications.DE[].order` | integer | 3 |
| `certifications.DK` | array<object> | - |
| `certifications.DK[].certification` | string | NR |
| `certifications.DK[].meaning` | string | No rating information. |
| `certifications.DK[].order` | integer | 0 |
| `certifications.ES` | array<object> | - |
| `certifications.ES[].certification` | string | A |
| `certifications.ES[].meaning` | string | General admission. |
| `certifications.ES[].order` | integer | 1 |
| `certifications.FI` | array<object> | - |
| `certifications.FI[].certification` | string | K-16 |
| `certifications.FI[].meaning` | string | Over 16 years. |
| `certifications.FI[].order` | integer | 4 |
| `certifications.FR` | array<object> | - |
| `certifications.FR[].certification` | string | TP |
| `certifications.FR[].meaning` | string | Valid for all audiences. |
| `certifications.FR[].order` | integer | 1 |
| `certifications.GB` | array<object> | - |
| `certifications.GB[].certification` | string | 15 |
| `certifications.GB[].meaning` | string | Only those over 15 years are admitted. Nobo… |
| `certifications.GB[].order` | integer | 5 |
| `certifications.HU` | array<object> | - |
| `certifications.HU[].certification` | string | 6 |
| `certifications.HU[].meaning` | string | Not recommended below age of 6. |
| `certifications.HU[].order` | integer | 2 |
| `certifications.IN` | array<object> | - |
| `certifications.IN[].certification` | string | U |
| `certifications.IN[].meaning` | string | Unrestricted Public Exhibition throughout I… |
| `certifications.IN[].order` | integer | 1 |
| `certifications.IT` | array<object> | - |
| `certifications.IT[].certification` | string | NR |
| `certifications.IT[].meaning` | string | No rating information. |
| `certifications.IT[].order` | integer | 0 |
| `certifications.LT` | array<object> | - |
| `certifications.LT[].certification` | string | NR |
| `certifications.LT[].meaning` | string | No rating information. |
| `certifications.LT[].order` | integer | 0 |
| `certifications.MY` | array<object> | - |
| `certifications.MY[].certification` | string | NR |
| `certifications.MY[].meaning` | string | No rating information. |
| `certifications.MY[].order` | integer | 0 |
| `certifications.NL` | array<object> | - |
| `certifications.NL[].certification` | string | AL |
| `certifications.NL[].meaning` | string | All ages. |
| `certifications.NL[].order` | integer | 1 |
| `certifications.NO` | array<object> | - |
| `certifications.NO[].certification` | string | 6 |
| `certifications.NO[].meaning` | string | 6 years (no restriction for children accomp… |
| `certifications.NO[].order` | integer | 2 |
| `certifications.NZ` | array<object> | - |
| `certifications.NZ[].certification` | string | G |
| `certifications.NZ[].meaning` | string | Suitable for general audiences. |
| `certifications.NZ[].order` | integer | 1 |
| `certifications.PH` | array<object> | - |
| `certifications.PH[].certification` | string | NR |
| `certifications.PH[].meaning` | string | No rating information. |
| `certifications.PH[].order` | integer | 0 |
| `certifications.PT` | array<object> | - |
| `certifications.PT[].certification` | string | Públicos |
| `certifications.PT[].meaning` | string | For all the public (especially designed for… |
| `certifications.PT[].order` | integer | 1 |
| `certifications.RU` | array<object> | - |
| `certifications.RU[].certification` | string | 6+ |
| `certifications.RU[].meaning` | string | (For children above 6) – Unsuitable for chi… |
| `certifications.RU[].order` | integer | 2 |
| `certifications.SE` | array<object> | - |
| `certifications.SE[].certification` | string | 11 |
| `certifications.SE[].meaning` | string | Children over the age of 7, who are accompa… |
| `certifications.SE[].order` | integer | 3 |
| `certifications.US` | array<object> | - |
| `certifications.US[].certification` | string | R |
| `certifications.US[].meaning` | string | Under 17 requires accompanying parent or ad… |
| `certifications.US[].order` | integer | 4 |
| `certifications.KR` | array<object> | - |
| `certifications.KR[].certification` | string | All |
| `certifications.KR[].meaning` | string | Film suitable for all ages. |
| `certifications.KR[].order` | integer | 0 |
| `certifications.SK` | array<object> | - |
| `certifications.SK[].certification` | string | U |
| `certifications.SK[].meaning` | string | General audience. |
| `certifications.SK[].order` | integer | 1 |
| `certifications.TH` | array<object> | - |
| `certifications.TH[].certification` | string | P |
| `certifications.TH[].meaning` | string | Educational. |
| `certifications.TH[].order` | integer | 1 |
| `certifications.MX` | array<object> | - |
| `certifications.MX[].certification` | string | AA |
| `certifications.MX[].meaning` | string | Informative-only rating: Understandable for… |
| `certifications.MX[].order` | integer | 1 |
| `certifications.ID` | array<object> | - |
| `certifications.ID[].certification` | string | SU |
| `certifications.ID[].meaning` | string | All ages. |
| `certifications.ID[].order` | integer | 1 |
| `certifications.TR` | array<object> | - |
| `certifications.TR[].certification` | string | Genel İzleyici Kitlesi |
| `certifications.TR[].meaning` | string | General audience. |
| `certifications.TR[].order` | integer | 1 |
| `certifications.AR` | array<object> | - |
| `certifications.AR[].certification` | string | ATP |
| `certifications.AR[].meaning` | string | For all public. |
| `certifications.AR[].order` | integer | 1 |
| `certifications.GR` | array<object> | - |
| `certifications.GR[].certification` | string | K |
| `certifications.GR[].meaning` | string | No restrictions. |
| `certifications.GR[].order` | integer | 1 |
| `certifications.TW` | array<object> | - |
| `certifications.TW[].certification` | string | 0+ |
| `certifications.TW[].meaning` | string | Viewing is permitted for audiences of all a… |
| `certifications.TW[].order` | integer | 1 |
| `certifications.ZA` | array<object> | - |
| `certifications.ZA[].certification` | string | A |
| `certifications.ZA[].meaning` | string | Suitable for all. |
| `certifications.ZA[].order` | integer | 1 |
| `certifications.SG` | array<object> | - |
| `certifications.SG[].certification` | string | G |
| `certifications.SG[].meaning` | string | Suitable for all ages. |
| `certifications.SG[].order` | integer | 1 |
| `certifications.IE` | array<object> | - |
| `certifications.IE[].certification` | string | G |
| `certifications.IE[].meaning` | string | Suitable for children of school going age (… |
| `certifications.IE[].order` | integer | 1 |
| `certifications.PR` | array<object> | - |
| `certifications.PR[].certification` | string | G |
| `certifications.PR[].meaning` | string | - |
| `certifications.PR[].order` | integer | 1 |
| `certifications.JP` | array<object> | - |
| `certifications.JP[].certification` | string | G |
| `certifications.JP[].meaning` | string | General, suitable for all ages. |
| `certifications.JP[].order` | integer | 1 |
| `certifications.VI` | array<object> | - |
| `certifications.VI[].certification` | string | G |
| `certifications.VI[].meaning` | string | All ages admitted. |
| `certifications.VI[].order` | integer | 1 |
| `certifications.CH` | array<object> | - |
| `certifications.CH[].certification` | string | 0 |
| `certifications.CH[].meaning` | string | - |
| `certifications.CH[].order` | integer | 1 |
| `certifications.IL` | array<object> | - |
| `certifications.IL[].certification` | string | All |
| `certifications.IL[].meaning` | string | - |
| `certifications.IL[].order` | integer | 1 |
| `certifications.HK` | array<object> | - |
| `certifications.HK[].certification` | string | I |
| `certifications.HK[].meaning` | string | - |
| `certifications.HK[].order` | integer | 1 |
| `certifications.MO` | array<object> | - |
| `certifications.MO[].certification` | string | A |
| `certifications.MO[].meaning` | string | - |
| `certifications.MO[].order` | integer | 1 |
| `certifications.LV` | array<object> | - |
| `certifications.LV[].certification` | string | U |
| `certifications.LV[].meaning` | string | - |
| `certifications.LV[].order` | integer | 1 |
| `certifications.LU` | array<object> | - |
| `certifications.LU[].certification` | string | EA |
| `certifications.LU[].meaning` | string | - |
| `certifications.LU[].order` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "certifications": {
    "AU": [
      {
        "certification": "E",
        "meaning": "Exempt from classification. Films that are exempt from classification must not contain contentious material (i.e. material that would ordinarily be rated M or higher).",
        "order": 1
      },
      "… 7 more items"
    ],
    "BG": [
      {
        "certification": "D",
        "meaning": "Prohibited for persons under 16.",
        "order": 4
      },
      "… 4 more items"
    ],
    "BR": [
      {
        "certification": "14",
        "meaning": "Not recommended for minors under fourteen. More violent material, stronger sex references and/or nudity.",
        "order": 4
      },
      "… 5 more items"
    ],
    "CA": [
      {
        "certification": "G",
        "meaning": "All ages.",
        "order": 2
      },
      "… 6 more items"
    ],
    "CA-QC": [
      {
        "certification": "NR",
        "meaning": "No rating information.",
        "order": 0
      },
      "… 4 more items"
    ],
    "DE": [
      {
        "certification": "12",
        "meaning": "Children 12 or older admitted, children between 6 and 11 only when accompanied by parent or a legal guardian.",
        "order": 3
      },
      "… 4 more items"
    ],
    "DK": [
      {
        "certification": "NR",
        "meaning": "No rating information.",
        "order": 0
      },
      "… 5 more items"
    ],
    "ES": [
      {
        "certification": "A",
        "meaning": "General admission.",
        "order": 1
      },
      "… 7 more items"
    ],
    "FI": [
      {
        "certification": "K-16",
        "meaning": "Over 16 years.",
        "order": 4
      },
      "… 5 more items"
    ],
    "FR": [
      {
        "certification": "TP",
        "meaning": "Valid for all audiences.",
        "order": 1
      },
      "… 3 more items"
    ],
    "GB": [
      {
        "certification": "15",
        "meaning": "Only those over 15 years are admitted. Nobody younger than 15 can rent or buy a 15-rated VHS, DVD, Blu-ray Disc, UMD or game, or watch a film in the cinema with this rating. Films under this category can contain adult themes, hard drugs,...",
        "order": 5
      },
      "… 6 more items"
    ],
    "HU": [
      {
        "certification": "6",
        "meaning": "Not recommended below age of 6.",
        "order": 2
      },
      "… 6 more items"
    ],
    "IN": [
      {
        "certification": "U",
        "meaning": "Unrestricted Public Exhibition throughout India, suitable for all age groups. Films under this category should not upset children 
… truncated
```

</details>


#### Certifications — TV Certifications

```
GET https://api.themoviedb.org/3/certification/tv/list
```

Operation id: `certifications-tv-list`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `certifications` | object | - |
| `certifications.AU` | array<object> | - |
| `certifications.AU[].certification` | string | P |
| `certifications.AU[].meaning` | string | Programming is intended for younger childre… |
| `certifications.AU[].order` | integer | 1 |
| `certifications.BR` | array<object> | - |
| `certifications.BR[].certification` | string | 14 |
| `certifications.BR[].meaning` | string | Content suitable for viewers over the age o… |
| `certifications.BR[].order` | integer | 3 |
| `certifications.CA` | array<object> | - |
| `certifications.CA[].certification` | string | Exempt |
| `certifications.CA[].meaning` | string | Shows which are exempt from ratings (such a… |
| `certifications.CA[].order` | integer | 1 |
| `certifications.CA-QC` | array<object> | - |
| `certifications.CA-QC[].certification` | string | 18+ |
| `certifications.CA-QC[].meaning` | string | Only to be viewed by adults and may contain… |
| `certifications.CA-QC[].order` | integer | 5 |
| `certifications.DE` | array<object> | - |
| `certifications.DE[].certification` | string | 0 |
| `certifications.DE[].meaning` | string | Can be aired at any time. |
| `certifications.DE[].order` | integer | 1 |
| `certifications.ES` | array<object> | - |
| `certifications.ES[].certification` | string | NR |
| `certifications.ES[].meaning` | string | No rating information. |
| `certifications.ES[].order` | integer | 0 |
| `certifications.FR` | array<object> | - |
| `certifications.FR[].certification` | string | NR |
| `certifications.FR[].meaning` | string | No rating information. |
| `certifications.FR[].order` | integer | 0 |
| `certifications.GB` | array<object> | - |
| `certifications.GB[].certification` | string | U |
| `certifications.GB[].meaning` | string | The U symbol stands for Universal. A U film… |
| `certifications.GB[].order` | integer | 0 |
| `certifications.HU` | array<object> | - |
| `certifications.HU[].certification` | string | Unrated |
| `certifications.HU[].meaning` | string | Without age restriction. |
| `certifications.HU[].order` | integer | 1 |
| `certifications.KR` | array<object> | - |
| `certifications.KR[].certification` | string | Exempt |
| `certifications.KR[].meaning` | string | This rating is only for knowledge based gam… |
| `certifications.KR[].order` | integer | 1 |
| `certifications.LT` | array<object> | - |
| `certifications.LT[].certification` | string | S |
| `certifications.LT[].meaning` | string | Intended for adult viewers from the age of … |
| `certifications.LT[].order` | integer | 3 |
| `certifications.NL` | array<object> | - |
| `certifications.NL[].certification` | string | NR |
| `certifications.NL[].meaning` | string | No rating information. |
| `certifications.NL[].order` | integer | 0 |
| `certifications.PH` | array<object> | - |
| `certifications.PH[].certification` | string | NR |
| `certifications.PH[].meaning` | string | No rating information. |
| `certifications.PH[].order` | integer | 0 |
| `certifications.PT` | array<object> | - |
| `certifications.PT[].certification` | string | 12AP |
| `certifications.PT[].meaning` | string | Acompanhamento Parental (may not be suitabl… |
| `certifications.PT[].order` | integer | 3 |
| `certifications.RU` | array<object> | - |
| `certifications.RU[].certification` | string | 16+ |
| `certifications.RU[].meaning` | string | Only teens the age of 16 or older can watch. |
| `certifications.RU[].order` | integer | 4 |
| `certifications.SK` | array<object> | - |
| `certifications.SK[].certification` | string | NR |
| `certifications.SK[].meaning` | string | No rating information. |
| `certifications.SK[].order` | integer | 0 |
| `certifications.TH` | array<object> | - |
| `certifications.TH[].certification` | string | ส |
| `certifications.TH[].meaning` | string | Sor - Educational movies which the public s… |
| `certifications.TH[].order` | integer | 1 |
| `certifications.US` | array<object> | - |
| `certifications.US[].certification` | string | TV-MA |
| `certifications.US[].meaning` | string | This program is specifically designed to be… |
| `certifications.US[].order` | integer | 6 |
| `certifications.IT` | array<object> | - |
| `certifications.IT[].certification` | string | T |
| `certifications.IT[].meaning` | string | All ages admitted. |
| `certifications.IT[].order` | integer | 1 |
| `certifications.FI` | array<object> | - |
| `certifications.FI[].certification` | string | S |
| `certifications.FI[].meaning` | string | Allowed at all times. |
| `certifications.FI[].order` | integer | 0 |
| `certifications.MY` | array<object> | - |
| `certifications.MY[].certification` | string | U |
| `certifications.MY[].meaning` | string | No age limit. Can be broadcast anytime. |
| `certifications.MY[].order` | integer | 1 |
| `certifications.NZ` | array<object> | - |
| `certifications.NZ[].certification` | string | G |
| `certifications.NZ[].meaning` | string | Approved for general viewing. |
| `certifications.NZ[].order` | integer | 1 |
| `certifications.NO` | array<object> | - |
| `certifications.NO[].certification` | string | A |
| `certifications.NO[].meaning` | string | Allowed at all times. |
| `certifications.NO[].order` | integer | 1 |
| `certifications.BG` | array<object> | - |
| `certifications.BG[].certification` | string | Unrated |
| `certifications.BG[].meaning` | string | Can be viewed for each age. |
| `certifications.BG[].order` | integer | 1 |
| `certifications.MX` | array<object> | - |
| `certifications.MX[].certification` | string | AA |
| `certifications.MX[].meaning` | string | Aimed at children (can be broadcast anytime… |
| `certifications.MX[].order` | integer | 1 |
| `certifications.IN` | array<object> | - |
| `certifications.IN[].certification` | string | U |
| `certifications.IN[].meaning` | string | Viewable for all ages. |
| `certifications.IN[].order` | integer | 1 |
| `certifications.DK` | array<object> | - |
| `certifications.DK[].certification` | string | A |
| `certifications.DK[].meaning` | string | Suitable for a general audience. |
| `certifications.DK[].order` | integer | 1 |
| `certifications.SE` | array<object> | - |
| `certifications.SE[].certification` | string | Btl |
| `certifications.SE[].meaning` | string | - |
| `certifications.SE[].order` | integer | 1 |
| `certifications.ID` | array<object> | - |
| `certifications.ID[].certification` | string | SU |
| `certifications.ID[].meaning` | string | Suitable for general audiences over the age… |
| `certifications.ID[].order` | integer | 1 |
| `certifications.TR` | array<object> | - |
| `certifications.TR[].certification` | string | Genel İzleyici |
| `certifications.TR[].meaning` | string | General audience. Suitable for all ages. |
| `certifications.TR[].order` | integer | 1 |
| `certifications.AR` | array<object> | - |
| `certifications.AR[].certification` | string | ATP |
| `certifications.AR[].meaning` | string | Suitable for all audiences. |
| `certifications.AR[].order` | integer | 1 |
| `certifications.PL` | array<object> | - |
| `certifications.PL[].certification` | string | 0 |
| `certifications.PL[].meaning` | string | Positive or neutral view of the world, litt… |
| `certifications.PL[].order` | integer | 1 |
| `certifications.MA` | array<object> | - |
| `certifications.MA[].certification` | string | NR |
| `certifications.MA[].meaning` | string | All audiences. |
| `certifications.MA[].order` | integer | 0 |
| `certifications.GR` | array<object> | - |
| `certifications.GR[].certification` | string | K |
| `certifications.GR[].meaning` | string | Suitable for all ages. |
| `certifications.GR[].order` | integer | 1 |
| `certifications.IL` | array<object> | - |
| `certifications.IL[].certification` | string | E |
| `certifications.IL[].meaning` | string | Exempt from classification. This rating is … |
| `certifications.IL[].order` | integer | 1 |
| `certifications.TW` | array<object> | - |
| `certifications.TW[].certification` | string | 0+ |
| `certifications.TW[].meaning` | string | Suitable for watching by general audiences. |
| `certifications.TW[].order` | integer | 1 |
| `certifications.ZA` | array<object> | - |
| `certifications.ZA[].certification` | string | All |
| `certifications.ZA[].meaning` | string | This is a programme/film that does not cont… |
| `certifications.ZA[].order` | integer | 1 |
| `certifications.SG` | array<object> | - |
| `certifications.SG[].certification` | string | G |
| `certifications.SG[].meaning` | string | - |
| `certifications.SG[].order` | integer | 1 |
| `certifications.PR` | array<object> | - |
| `certifications.PR[].certification` | string | NR |
| `certifications.PR[].meaning` | string | - |
| `certifications.PR[].order` | integer | 0 |
| `certifications.VI` | array<object> | - |
| `certifications.VI[].certification` | string | NR |
| `certifications.VI[].meaning` | string | - |
| `certifications.VI[].order` | integer | 0 |

<details><summary>Example response</summary>

```json
{
  "certifications": {
    "AU": [
      {
        "certification": "P",
        "meaning": "Programming is intended for younger children 2–11; commercial stations must show at least 30 minutes of P-rated content each weekday and weekends at all times. No advertisements may be shown during P-rated programs.",
        "order": 1
      },
      "… 7 more items"
    ],
    "BR": [
      {
        "certification": "14",
        "meaning": "Content suitable for viewers over the age of 14.",
        "order": 3
      },
      "… 5 more items"
    ],
    "CA": [
      {
        "certification": "Exempt",
        "meaning": "Shows which are exempt from ratings (such as news and sports programming) will not display an on-screen rating at all.",
        "order": 1
      },
      "… 6 more items"
    ],
    "CA-QC": [
      {
        "certification": "18+",
        "meaning": "Only to be viewed by adults and may contain extreme violence and graphic sexual content. It is mostly used for 18+ movies and pornography.",
        "order": 5
      },
      "… 5 more items"
    ],
    "DE": [
      {
        "certification": "0",
        "meaning": "Can be aired at any time.",
        "order": 1
      },
      "… 4 more items"
    ],
    "ES": [
      {
        "certification": "NR",
        "meaning": "No rating information.",
        "order": 0
      },
      "… 8 more items"
    ],
    "FR": [
      {
        "certification": "NR",
        "meaning": "No rating information.",
        "order": 0
      },
      "… 4 more items"
    ],
    "GB": [
      {
        "certification": "U",
        "meaning": "The U symbol stands for Universal. A U film should be suitable for audiences aged four years and over.",
        "order": 0
      },
      "… 6 more items"
    ],
    "HU": [
      {
        "certification": "Unrated",
        "meaning": "Without age restriction.",
        "order": 1
      },
      "… 6 more items"
    ],
    "KR": [
      {
        "certification": "Exempt",
        "meaning": "This rating is only for knowledge based game shows; lifestyle shows; documentary shows; news; current topic discussion shows; education/culture shows; sports that excludes MMA or other violent sports; and other programs that Korea Commun...",
        "order": 1
      },
      "… 5 more items"
    ],
    "LT": [
      {
        "certification": "S",
        "meaning": "Intended for adult viewers from the age of 18 (corresponding to the age-appropriate index N-18) and broadcast between 23 (11pm) and 6 (6am) hours; Limited to minors and intended for adult audiences.",
        "order": 
… truncated
```

</details>



### Reviews

#### Reviews — Details

```
GET https://api.themoviedb.org/3/review/{review_id}
```

Retrieve the details of a movie or TV show review.

Operation id: `review-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `review_id` | path | string | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | string | 640b2aeecaaca20079decdcc |
| `author` | string | Ricardo Oliveira |
| `author_details` | object | - |
| `author_details.name` | string | Ricardo Oliveira |
| `author_details.username` | string | RSOliveira |
| `author_details.avatar_path` | string | /23Cl7rhsknc7IIAcZZAGKzovjTu.jpg |
| `author_details.rating` | integer | 9 |
| `content` | string | "The Last of Us" is a post-apocalyptic TV s… |
| `created_at` | string | 2023-03-10T13:04:46.674Z |
| `iso_639_1` | string | en |
| `media_id` | integer | 100088 |
| `media_title` | string | The Last of Us |
| `media_type` | string | tv |
| `updated_at` | string | 2023-03-10T13:04:46.734Z |
| `url` | string | https://www.themoviedb.org/review/640b2aeec… |

<details><summary>Example response</summary>

```json
{
  "id": "640b2aeecaaca20079decdcc",
  "author": "Ricardo Oliveira",
  "author_details": {
    "name": "Ricardo Oliveira",
    "username": "RSOliveira",
    "avatar_path": "/23Cl7rhsknc7IIAcZZAGKzovjTu.jpg",
    "rating": 9.0
  },
  "content": "\"The Last of Us\" is a post-apocalyptic TV series based on the popular video game of the same name. The story follows the journey of Joel, a smuggler, and Ellie, a teenage girl who may be the key to finding a cure for a deadly fungal infe...",
  "created_at": "2023-03-10T13:04:46.674Z",
  "iso_639_1": "en",
  "media_id": 100088,
  "media_title": "The Last of Us",
  "media_type": "tv",
  "updated_at": "2023-03-10T13:04:46.734Z",
  "url": "https://www.themoviedb.org/review/640b2aeecaaca20079decdcc"
}
```

</details>



### Watch providers

#### Watch providers — Movie Providers

```
GET https://api.themoviedb.org/3/watch/providers/movie
```

Get the list of streaming providers we have for movies.

Operation id: `watch-providers-movie-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `watch_region` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].display_priorities` | object | - |
| `results[].display_priorities.CA` | integer | 6 |
| `results[].display_priorities.AE` | integer | 1 |
| `results[].display_priorities.AR` | integer | 3 |
| `results[].display_priorities.AT` | integer | 4 |
| `results[].display_priorities.AU` | integer | 10 |
| `results[].display_priorities.BE` | integer | 6 |
| `results[].display_priorities.BO` | integer | 6 |
| `results[].display_priorities.BR` | integer | 8 |
| `results[].display_priorities.BG` | integer | 2 |
| `results[].display_priorities.CH` | integer | 4 |
| `results[].display_priorities.CL` | integer | 3 |
| `results[].display_priorities.CO` | integer | 4 |
| `results[].display_priorities.CR` | integer | 5 |
| `results[].display_priorities.CZ` | integer | 3 |
| `results[].display_priorities.DE` | integer | 4 |
| `results[].display_priorities.DK` | integer | 7 |
| `results[].display_priorities.EC` | integer | 7 |
| `results[].display_priorities.EE` | integer | 3 |
| `results[].display_priorities.EG` | integer | 2 |
| `results[].display_priorities.ES` | integer | 4 |
| `results[].display_priorities.FI` | integer | 10 |
| `results[].display_priorities.FR` | integer | 5 |
| `results[].display_priorities.GB` | integer | 5 |
| `results[].display_priorities.GR` | integer | 2 |
| `results[].display_priorities.GT` | integer | 7 |
| `results[].display_priorities.HK` | integer | 5 |
| `results[].display_priorities.HN` | integer | 7 |
| `results[].display_priorities.HU` | integer | 3 |
| `results[].display_priorities.ID` | integer | 4 |
| `results[].display_priorities.IE` | integer | 4 |
| `results[].display_priorities.IN` | integer | 8 |
| `results[].display_priorities.IT` | integer | 4 |
| `results[].display_priorities.JP` | integer | 7 |
| `results[].display_priorities.LT` | integer | 3 |
| `results[].display_priorities.LV` | integer | 3 |
| `results[].display_priorities.MX` | integer | 4 |
| `results[].display_priorities.MY` | integer | 4 |
| `results[].display_priorities.NL` | integer | 8 |
| `results[].display_priorities.NO` | integer | 6 |
| `results[].display_priorities.NZ` | integer | 4 |
| `results[].display_priorities.PE` | integer | 3 |
| `results[].display_priorities.PH` | integer | 4 |
| `results[].display_priorities.PL` | integer | 1 |
| `results[].display_priorities.PT` | integer | 4 |
| `results[].display_priorities.PY` | integer | 7 |
| `results[].display_priorities.RU` | integer | 2 |
| `results[].display_priorities.SA` | integer | 1 |
| `results[].display_priorities.SE` | integer | 8 |
| `results[].display_priorities.SG` | integer | 5 |
| `results[].display_priorities.SK` | integer | 3 |
| `results[].display_priorities.TH` | integer | 4 |
| `results[].display_priorities.TR` | integer | 6 |
| `results[].display_priorities.TW` | integer | 7 |
| `results[].display_priorities.US` | integer | 4 |
| `results[].display_priorities.VE` | integer | 4 |
| `results[].display_priorities.ZA` | integer | 2 |
| `results[].display_priorities.SI` | integer | 31 |
| `results[].display_priorities.CV` | integer | 13 |
| `results[].display_priorities.GH` | integer | 17 |
| `results[].display_priorities.MU` | integer | 15 |
| `results[].display_priorities.MZ` | integer | 16 |
| `results[].display_priorities.UG` | integer | 16 |
| `results[].display_priorities.IL` | integer | 28 |
| `results[].display_priority` | integer | 2 |
| `results[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results[].provider_name` | string | Apple TV |
| `results[].provider_id` | integer | 2 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "display_priorities": {
        "CA": 6,
        "AE": 1,
        "AR": 3,
        "AT": 4,
        "AU": 10,
        "BE": 6,
        "BO": 6,
        "BR": 8,
        "BG": 2,
        "CH": 4,
        "CL": 3,
        "CO": 4,
        "CR": 5,
        "CZ": 3,
        "DE": 4,
        "DK": 7,
        "EC": 7,
        "EE": 3,
        "EG": 2,
        "ES": 4,
        "FI": 10,
        "FR": 5,
        "GB": 5,
        "GR": 2,
        "GT": 7,
        "HK": 5,
        "HN": 7,
        "HU": 3,
        "ID": 4,
        "IE": 4,
        "IN": 8,
        "IT": 4,
        "JP": 7,
        "LT": 3,
        "LV": 3,
        "MX": 4,
        "MY": 4,
        "NL": 8,
        "NO": 6,
        "NZ": 4,
        "PE": 3,
        "PH": 4,
        "PL": 1,
        "PT": 4,
        "PY": 7,
        "RU": 2,
        "SA": 1,
        "SE": 8,
        "SG": 5,
        "SK": 3,
        "TH": 4,
        "TR": 6,
        "TW": 7,
        "US": 4,
        "VE": 4,
        "ZA": 2,
        "SI": 31,
        "CV": 13,
        "GH": 17,
        "MU": 15,
        "MZ": 16,
        "UG": 16,
        "IL": 28
      },
      "display_priority": 2,
      "logo_path": "/peURlLlr8jggOwK53fJ5wdQl05y.jpg",
      "provider_name": "Apple TV",
      "provider_id": 2
    },
    {
      "display_priorities": {
        "CA": 8,
        "AE": 3,
        "AR": 6,
        "AT": 8,
        "AU": 14,
        "BE": 7,
        "BO": 22,
        "BR": 16,
        "CH": 5,
        "CL": 4,
        "CO": 3,
        "CR": 22,
        "CZ": 4,
        "DE": 8,
        "DK": 8,
        "EC": 11,
        "EE": 4,
        "EG": 4,
        "ES": 13,
        "FI": 11,
        "FR": 7,
        "GB": 12,
        "GR": 3,
        "GT": 22,
        "HK": 13,
        "HN": 22,
        "HR": 3,
        "HU": 4,
        "ID": 6,
        "IE": 7,
        "IN": 10,
        "IS": 4,
        "IT": 7,
        "JP": 9,
        "KR": 7,
        "LT": 4,
        "LV": 4,
        "MX": 15,
        "MY": 5,
        "NL": 11,
        "NO": 8,
        "NZ": 7,
        "PE": 5,
        "PH": 5,
        "PL": 3,
        "PT": 5,
        "PY": 22,
        "RU": 4,
        "SA": 3,
        "SE": 9,
        "SG": 6,
        "SK": 5,
        "TH": 5,
        "TR": 5,
        "TW": 3,
        "US": 14,
        "VE": 3,
        "ZA": 3,
        "TZ": 20
      },
      "display_priority": 3,
      "logo_path": "/tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg",
      "provider_name": "Google Play Movies",
      "provider_id": 3
    },
    "… 527 more items"
  ]
}
```

</details>


#### Watch providers — Available Regions

```
GET https://api.themoviedb.org/3/watch/providers/regions
```

Get the list of the countries we have watch provider (OTT/streaming) data for.

Operation id: `watch-providers-available-regions`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].iso_3166_1` | string | AD |
| `results[].english_name` | string | Andorra |
| `results[].native_name` | string | Andorra |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "iso_3166_1": "AD",
      "english_name": "Andorra",
      "native_name": "Andorra"
    },
    {
      "iso_3166_1": "AE",
      "english_name": "United Arab Emirates",
      "native_name": "United Arab Emirates"
    },
    "… 118 more items"
  ]
}
```

</details>


#### Watch providers — TV Providers

```
GET https://api.themoviedb.org/3/watch/providers/tv
```

Get the list of streaming providers we have for TV shows.

Operation id: `watch-provider-tv-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `watch_region` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].display_priorities` | object | - |
| `results[].display_priorities.CA` | integer | 6 |
| `results[].display_priorities.AE` | integer | 1 |
| `results[].display_priorities.AR` | integer | 3 |
| `results[].display_priorities.AT` | integer | 4 |
| `results[].display_priorities.AU` | integer | 10 |
| `results[].display_priorities.BE` | integer | 6 |
| `results[].display_priorities.BO` | integer | 6 |
| `results[].display_priorities.BR` | integer | 8 |
| `results[].display_priorities.BG` | integer | 2 |
| `results[].display_priorities.CH` | integer | 4 |
| `results[].display_priorities.CL` | integer | 3 |
| `results[].display_priorities.CO` | integer | 4 |
| `results[].display_priorities.CR` | integer | 5 |
| `results[].display_priorities.CZ` | integer | 3 |
| `results[].display_priorities.DE` | integer | 4 |
| `results[].display_priorities.DK` | integer | 7 |
| `results[].display_priorities.EC` | integer | 7 |
| `results[].display_priorities.EE` | integer | 3 |
| `results[].display_priorities.EG` | integer | 2 |
| `results[].display_priorities.ES` | integer | 4 |
| `results[].display_priorities.FI` | integer | 10 |
| `results[].display_priorities.FR` | integer | 5 |
| `results[].display_priorities.GB` | integer | 5 |
| `results[].display_priorities.GR` | integer | 2 |
| `results[].display_priorities.GT` | integer | 7 |
| `results[].display_priorities.HK` | integer | 5 |
| `results[].display_priorities.HN` | integer | 7 |
| `results[].display_priorities.HU` | integer | 3 |
| `results[].display_priorities.ID` | integer | 4 |
| `results[].display_priorities.IE` | integer | 4 |
| `results[].display_priorities.IN` | integer | 8 |
| `results[].display_priorities.IT` | integer | 4 |
| `results[].display_priorities.JP` | integer | 7 |
| `results[].display_priorities.LT` | integer | 3 |
| `results[].display_priorities.LV` | integer | 3 |
| `results[].display_priorities.MX` | integer | 4 |
| `results[].display_priorities.MY` | integer | 4 |
| `results[].display_priorities.NL` | integer | 8 |
| `results[].display_priorities.NO` | integer | 6 |
| `results[].display_priorities.NZ` | integer | 4 |
| `results[].display_priorities.PE` | integer | 3 |
| `results[].display_priorities.PH` | integer | 4 |
| `results[].display_priorities.PL` | integer | 1 |
| `results[].display_priorities.PT` | integer | 4 |
| `results[].display_priorities.PY` | integer | 7 |
| `results[].display_priorities.RU` | integer | 2 |
| `results[].display_priorities.SA` | integer | 1 |
| `results[].display_priorities.SE` | integer | 8 |
| `results[].display_priorities.SG` | integer | 5 |
| `results[].display_priorities.SK` | integer | 3 |
| `results[].display_priorities.TH` | integer | 4 |
| `results[].display_priorities.TR` | integer | 6 |
| `results[].display_priorities.TW` | integer | 7 |
| `results[].display_priorities.US` | integer | 4 |
| `results[].display_priorities.VE` | integer | 4 |
| `results[].display_priorities.ZA` | integer | 2 |
| `results[].display_priorities.SI` | integer | 31 |
| `results[].display_priorities.CV` | integer | 13 |
| `results[].display_priorities.GH` | integer | 17 |
| `results[].display_priorities.MU` | integer | 15 |
| `results[].display_priorities.MZ` | integer | 16 |
| `results[].display_priorities.UG` | integer | 16 |
| `results[].display_priorities.IL` | integer | 28 |
| `results[].display_priority` | integer | 2 |
| `results[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results[].provider_name` | string | Apple TV |
| `results[].provider_id` | integer | 2 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "display_priorities": {
        "CA": 6,
        "AE": 1,
        "AR": 3,
        "AT": 4,
        "AU": 10,
        "BE": 6,
        "BO": 6,
        "BR": 8,
        "BG": 2,
        "CH": 4,
        "CL": 3,
        "CO": 4,
        "CR": 5,
        "CZ": 3,
        "DE": 4,
        "DK": 7,
        "EC": 7,
        "EE": 3,
        "EG": 2,
        "ES": 4,
        "FI": 10,
        "FR": 5,
        "GB": 5,
        "GR": 2,
        "GT": 7,
        "HK": 5,
        "HN": 7,
        "HU": 3,
        "ID": 4,
        "IE": 4,
        "IN": 8,
        "IT": 4,
        "JP": 7,
        "LT": 3,
        "LV": 3,
        "MX": 4,
        "MY": 4,
        "NL": 8,
        "NO": 6,
        "NZ": 4,
        "PE": 3,
        "PH": 4,
        "PL": 1,
        "PT": 4,
        "PY": 7,
        "RU": 2,
        "SA": 1,
        "SE": 8,
        "SG": 5,
        "SK": 3,
        "TH": 4,
        "TR": 6,
        "TW": 7,
        "US": 4,
        "VE": 4,
        "ZA": 2,
        "SI": 31,
        "CV": 13,
        "GH": 17,
        "MU": 15,
        "MZ": 16,
        "UG": 16,
        "IL": 28
      },
      "display_priority": 2,
      "logo_path": "/peURlLlr8jggOwK53fJ5wdQl05y.jpg",
      "provider_name": "Apple TV",
      "provider_id": 2
    },
    {
      "display_priorities": {
        "CA": 8,
        "AE": 3,
        "AR": 6,
        "AT": 8,
        "AU": 14,
        "BE": 7,
        "BO": 22,
        "BR": 16,
        "CH": 5,
        "CL": 4,
        "CO": 3,
        "CR": 22,
        "CZ": 4,
        "DE": 8,
        "DK": 8,
        "EC": 11,
        "EE": 4,
        "EG": 4,
        "ES": 13,
        "FI": 11,
        "FR": 7,
        "GB": 12,
        "GR": 3,
        "GT": 22,
        "HK": 13,
        "HN": 22,
        "HR": 3,
        "HU": 4,
        "ID": 6,
        "IE": 7,
        "IN": 10,
        "IS": 4,
        "IT": 7,
        "JP": 9,
        "KR": 7,
        "LT": 4,
        "LV": 4,
        "MX": 15,
        "MY": 5,
        "NL": 11,
        "NO": 8,
        "NZ": 7,
        "PE": 5,
        "PH": 5,
        "PL": 3,
        "PT": 5,
        "PY": 22,
        "RU": 4,
        "SA": 3,
        "SE": 9,
        "SG": 6,
        "SK": 5,
        "TH": 5,
        "TR": 5,
        "TW": 3,
        "US": 14,
        "VE": 3,
        "ZA": 3,
        "TZ": 20
      },
      "display_priority": 3,
      "logo_path": "/tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg",
      "provider_name": "Google Play Movies",
      "provider_id": 3
    },
    "… 472 more items"
  ]
}
```

</details>



### TV

#### TV — Airing Today

```
GET https://api.themoviedb.org/3/tv/airing_today
```

Get a list of TV shows airing today.

Operation id: `tv-series-airing-today-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `timezone` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].backdrop_path` | string | /mAJ84W6I8I272Da87qplS2Dp9ST.jpg |
| `results[].first_air_date` | string | 2023-01-23 |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 202250 |
| `results[].name` | string | Dirty Linen |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | tl |
| `results[].original_name` | string | Dirty Linen |
| `results[].overview` | string | To exact vengeance, a young woman infiltrat… |
| `results[].popularity` | number | 2797.914 |
| `results[].poster_path` | string | /aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg |
| `results[].vote_average` | integer | 5 |
| `results[].vote_count` | integer | 13 |
| `total_pages` | integer | 14 |
| `total_results` | integer | 265 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "backdrop_path": "/mAJ84W6I8I272Da87qplS2Dp9ST.jpg",
      "first_air_date": "2023-01-23",
      "genre_ids": [
        9648,
        18
      ],
      "id": 202250,
      "name": "Dirty Linen",
      "origin_country": [
        "PH"
      ],
      "original_language": "tl",
      "original_name": "Dirty Linen",
      "overview": "To exact vengeance, a young woman infiltrates the household of an influential family as a housemaid to expose their dirty secrets. However, love will get in the way of her revenge plot.",
      "popularity": 2797.914,
      "poster_path": "/aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg",
      "vote_average": 5,
      "vote_count": 13
    },
    {
      "backdrop_path": "/wJmcuxa0C4AERmA9mejxm9qRYDj.jpg",
      "first_air_date": "2022-06-06",
      "genre_ids": [
        80,
        9648
      ],
      "id": 203504,
      "name": "Aashiqana",
      "origin_country": [
        "IN"
      ],
      "original_language": "hi",
      "original_name": "आशिकाना",
      "overview": "A serial killer sparks the story of uptight Yashvardhan and feisty Chikki. Plagued by misunderstandings, how far do they have to go to nab the murderer?",
      "popularity": 2732.908,
      "poster_path": "/a4Z6Uohb6Ln5vcPvMUzwyn3WBjP.jpg",
      "vote_average": 6.1,
      "vote_count": 10
    },
    "… 18 more items"
  ],
  "total_pages": 14,
  "total_results": 265
}
```

</details>


#### TV — TV List

```
GET https://api.themoviedb.org/3/tv/changes
```

Operation id: `changes-tv-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `end_date` | query | string (date) | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string (date) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].id` | integer | 225591 |
| `results[].adult` | boolean | false |
| `page` | integer | 1 |
| `total_pages` | integer | 18 |
| `total_results` | integer | 1763 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "id": 225591,
      "adult": false
    },
    {
      "id": 220819,
      "adult": false
    },
    "… 98 more items"
  ],
  "page": 1,
  "total_pages": 18,
  "total_results": 1763
}
```

</details>


#### TV — Changes

```
GET https://api.themoviedb.org/3/tv/episode/{episode_id}/changes
```

Get the recent changes for a TV episode.

Operation id: `tv-episode-changes-by-id`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `episode_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `changes` | array<object> | - |
| `changes[].key` | string | production_code |
| `changes[].items` | array<object> | - |
| `changes[].items[].id` | string | 54bd9ed7c3a3686c6b00da66 |
| `changes[].items[].action` | string | added |
| `changes[].items[].time` | string | 2015-01-20 00:18:31 UTC |
| `changes[].items[].value` | string | 101 |

<details><summary>Example response</summary>

```json
{
  "changes": [
    {
      "key": "production_code",
      "items": [
        {
          "id": "54bd9ed7c3a3686c6b00da66",
          "action": "added",
          "time": "2015-01-20 00:18:31 UTC",
          "value": "101"
        }
      ]
    },
    {
      "key": "overview",
      "items": [
        {
          "id": "54bd9ed7c3a3686c6b00da67",
          "action": "updated",
          "time": "2015-01-20 00:18:31 UTC",
          "iso_639_1": "fr",
          "value": "Sur le continent de Westeros, un jeune patrouilleur de la Garde de Nuit, chargée de veiller sur le Mur, est condamné à mort pour désertion par Eddard Stark, seigneur de Winterfell et Gardien du Nord. Mais de sombres nouvelles arrivent de...",
          "original_value": "Sur le continent de Westeros, un jeune patrouilleur de la Garde de Nuit, chargée de veiller sur le Mur, est condamné à mort pour désertion par Eddard Stark, seigneur de Winterfell et Gardien du Nord. Mais de sombres nouvelles arrivent de..."
        },
        {
          "id": "54c53255c3a36814210141d0",
          "action": "added",
          "time": "2015-01-25 18:13:41 UTC",
          "iso_639_1": "es",
          "value": "Ned Stark, Señor de Invernalia se entera de que su mentor, Jon Arryn, ha muerto y por esto el Rey Robert se dirige hacia el norte para ofrecerle ser la Mano del Rey. Al otro lado del Mar en Pentos, Viserys Targaryen planea casar a su her..."
        }
      ]
    },
    "… 2 more items"
  ]
}
```

</details>


#### TV — Details

```
GET https://api.themoviedb.org/3/tv/episode_group/{tv_episode_group_id}
```

Get the details of a TV episode group.

Operation id: `tv-episode-group-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `tv_episode_group_id` | path | string | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `description` | string | Comedians in Cars organized in Netflix's co… |
| `episode_count` | integer | 83 |
| `group_count` | integer | 6 |
| `groups` | array<object> | - |
| `groups[].id` | string | 5acf93efc3a368739a0000a9 |
| `groups[].name` | string | First Cup |
| `groups[].order` | integer | 1 |
| `groups[].episodes` | array<object> | - |
| `groups[].episodes[].air_date` | string | 2015-06-17 |
| `groups[].episodes[].episode_number` | integer | 3 |
| `groups[].episodes[].id` | integer | 1078262 |
| `groups[].episodes[].name` | string | Jim Carrey: We Love Breathing What You're B… |
| `groups[].episodes[].overview` | string | Jerry’s full of testosterone as he steps in… |
| `groups[].episodes[].production_code` | string | - |
| `groups[].episodes[].runtime` | object | - |
| `groups[].episodes[].season_number` | integer | 6 |
| `groups[].episodes[].show_id` | integer | 59717 |
| `groups[].episodes[].still_path` | string | /aOyE420zuFq9zWtEWjIccAiTrzU.jpg |
| `groups[].episodes[].vote_average` | number | 7.4 |
| `groups[].episodes[].vote_count` | integer | 5 |
| `groups[].episodes[].order` | integer | 0 |
| `groups[].locked` | boolean | true |
| `id` | string | 5acf93e60e0a26346d0000ce |
| `name` | string | Netflix Collections |
| `network` | object | - |
| `network.id` | integer | 213 |
| `network.logo_path` | string | /wwemzKWzjKYJFfCeiB57q3r4Bcm.png |
| `network.name` | string | Netflix |
| `network.origin_country` | string | - |
| `type` | integer | 4 |

<details><summary>Example response</summary>

```json
{
  "description": "Comedians in Cars organized in Netflix's collections.",
  "episode_count": 83,
  "group_count": 6,
  "groups": [
    {
      "id": "5acf93efc3a368739a0000a9",
      "name": "First Cup",
      "order": 1,
      "episodes": [
        {
          "air_date": "2015-06-17",
          "episode_number": 3,
          "id": 1078262,
          "name": "Jim Carrey: We Love Breathing What You're Burning, Baby",
          "overview": "Jerry’s full of testosterone as he steps into a ‘76 Lamborghini Countach with Jim Carrey, who’s between a three-week cleanse and a five-day silent retreat. After coffee, it’s off to Carrey’s studio to study a portrait of a gorilla with a...",
          "production_code": "",
          "runtime": null,
          "season_number": 6,
          "show_id": 59717,
          "still_path": "/aOyE420zuFq9zWtEWjIccAiTrzU.jpg",
          "vote_average": 7.4,
          "vote_count": 5,
          "order": 0
        },
        "… 19 more items"
      ],
      "locked": true
    },
    "… 5 more items"
  ],
  "id": "5acf93e60e0a26346d0000ce",
  "name": "Netflix Collections",
  "network": {
    "id": 213,
    "logo_path": "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
    "name": "Netflix",
    "origin_country": ""
  },
  "type": 4
}
```

</details>


#### TV — Latest

```
GET https://api.themoviedb.org/3/tv/latest
```

Get the newest TV show ID.

Operation id: `tv-series-latest-id`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `backdrop_path` | object | - |
| `created_by` | array<object> | - |
| `episode_run_time` | array<object> | - |
| `first_air_date` | string | - |
| `genres` | array<object> | - |
| `homepage` | string | - |
| `id` | integer | 225491 |
| `in_production` | boolean | true |
| `languages` | array<object> | - |
| `last_air_date` | string | 2023-04-21 |
| `last_episode_to_air` | object | - |
| `last_episode_to_air.id` | integer | 4398801 |
| `last_episode_to_air.name` | string | Episode 8 |
| `last_episode_to_air.overview` | string | - |
| `last_episode_to_air.vote_average` | integer | 0 |
| `last_episode_to_air.vote_count` | integer | 0 |
| `last_episode_to_air.air_date` | string | 2023-04-21 |
| `last_episode_to_air.episode_number` | integer | 8 |
| `last_episode_to_air.production_code` | string | - |
| `last_episode_to_air.runtime` | object | - |
| `last_episode_to_air.season_number` | integer | 1 |
| `last_episode_to_air.show_id` | integer | 225491 |
| `last_episode_to_air.still_path` | object | - |
| `name` | string | 妖怪传 |
| `next_episode_to_air` | object | - |
| `networks` | array<object> | - |
| `number_of_episodes` | integer | 1 |
| `number_of_seasons` | integer | 1 |
| `origin_country` | array<string> | - |
| `original_language` | string | zh |
| `original_name` | string | 妖怪传 |
| `overview` | string | - |
| `popularity` | integer | 0 |
| `poster_path` | object | - |
| `production_companies` | array<object> | - |
| `production_countries` | array<object> | - |
| `seasons` | array<object> | - |
| `seasons[].air_date` | object | - |
| `seasons[].episode_count` | integer | 1 |
| `seasons[].id` | integer | 338956 |
| `seasons[].name` | string | Season 1 |
| `seasons[].overview` | string | - |
| `seasons[].poster_path` | object | - |
| `seasons[].season_number` | integer | 1 |
| `spoken_languages` | array<object> | - |
| `status` | string | Returning Series |
| `tagline` | string | - |
| `type` | string | Scripted |
| `vote_average` | integer | 0 |
| `vote_count` | integer | 0 |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "backdrop_path": null,
  "created_by": [],
  "episode_run_time": [],
  "first_air_date": "",
  "genres": [],
  "homepage": "",
  "id": 225491,
  "in_production": true,
  "languages": [],
  "last_air_date": "2023-04-21",
  "last_episode_to_air": {
    "id": 4398801,
    "name": "Episode 8",
    "overview": "",
    "vote_average": 0.0,
    "vote_count": 0,
    "air_date": "2023-04-21",
    "episode_number": 8,
    "production_code": "",
    "runtime": null,
    "season_number": 1,
    "show_id": 225491,
    "still_path": null
  },
  "name": "妖怪传",
  "next_episode_to_air": null,
  "networks": [],
  "number_of_episodes": 1,
  "number_of_seasons": 1,
  "origin_country": [
    "CN"
  ],
  "original_language": "zh",
  "original_name": "妖怪传",
  "overview": "",
  "popularity": 0.0,
  "poster_path": null,
  "production_companies": [],
  "production_countries": [],
  "seasons": [
    {
      "air_date": null,
      "episode_count": 1,
      "id": 338956,
      "name": "Season 1",
      "overview": "",
      "poster_path": null,
      "season_number": 1
    }
  ],
  "spoken_languages": [],
  "status": "Returning Series",
  "tagline": "",
  "type": "Scripted",
  "vote_average": 0.0,
  "vote_count": 0
}
```

</details>


#### TV — On The Air

```
GET https://api.themoviedb.org/3/tv/on_the_air
```

Get a list of TV shows that air in the next 7 days.

Operation id: `tv-series-on-the-air-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `timezone` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].backdrop_path` | string | /mAJ84W6I8I272Da87qplS2Dp9ST.jpg |
| `results[].first_air_date` | string | 2023-01-23 |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 202250 |
| `results[].name` | string | Dirty Linen |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | tl |
| `results[].original_name` | string | Dirty Linen |
| `results[].overview` | string | To exact vengeance, a young woman infiltrat… |
| `results[].popularity` | number | 2797.914 |
| `results[].poster_path` | string | /aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg |
| `results[].vote_average` | integer | 5 |
| `results[].vote_count` | integer | 13 |
| `total_pages` | integer | 58 |
| `total_results` | integer | 1151 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "backdrop_path": "/mAJ84W6I8I272Da87qplS2Dp9ST.jpg",
      "first_air_date": "2023-01-23",
      "genre_ids": [
        9648,
        18
      ],
      "id": 202250,
      "name": "Dirty Linen",
      "origin_country": [
        "PH"
      ],
      "original_language": "tl",
      "original_name": "Dirty Linen",
      "overview": "To exact vengeance, a young woman infiltrates the household of an influential family as a housemaid to expose their dirty secrets. However, love will get in the way of her revenge plot.",
      "popularity": 2797.914,
      "poster_path": "/aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg",
      "vote_average": 5,
      "vote_count": 13
    },
    {
      "backdrop_path": "/wJmcuxa0C4AERmA9mejxm9qRYDj.jpg",
      "first_air_date": "2022-06-06",
      "genre_ids": [
        80,
        9648
      ],
      "id": 203504,
      "name": "Aashiqana",
      "origin_country": [
        "IN"
      ],
      "original_language": "hi",
      "original_name": "आशिकाना",
      "overview": "A serial killer sparks the story of uptight Yashvardhan and feisty Chikki. Plagued by misunderstandings, how far do they have to go to nab the murderer?",
      "popularity": 2732.908,
      "poster_path": "/a4Z6Uohb6Ln5vcPvMUzwyn3WBjP.jpg",
      "vote_average": 6.1,
      "vote_count": 10
    },
    "… 18 more items"
  ],
  "total_pages": 58,
  "total_results": 1151
}
```

</details>


#### TV — Popular

```
GET https://api.themoviedb.org/3/tv/popular
```

Get a list of TV shows ordered by popularity.

Operation id: `tv-series-popular-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].backdrop_path` | string | /mAJ84W6I8I272Da87qplS2Dp9ST.jpg |
| `results[].first_air_date` | string | 2023-01-23 |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 202250 |
| `results[].name` | string | Dirty Linen |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | tl |
| `results[].original_name` | string | Dirty Linen |
| `results[].overview` | string | To exact vengeance, a young woman infiltrat… |
| `results[].popularity` | number | 2797.914 |
| `results[].poster_path` | string | /aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg |
| `results[].vote_average` | integer | 5 |
| `results[].vote_count` | integer | 13 |
| `total_pages` | integer | 7416 |
| `total_results` | integer | 148302 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "backdrop_path": "/mAJ84W6I8I272Da87qplS2Dp9ST.jpg",
      "first_air_date": "2023-01-23",
      "genre_ids": [
        9648,
        18
      ],
      "id": 202250,
      "name": "Dirty Linen",
      "origin_country": [
        "PH"
      ],
      "original_language": "tl",
      "original_name": "Dirty Linen",
      "overview": "To exact vengeance, a young woman infiltrates the household of an influential family as a housemaid to expose their dirty secrets. However, love will get in the way of her revenge plot.",
      "popularity": 2797.914,
      "poster_path": "/aoAZgnmMzY9vVy9VWnO3U5PZENh.jpg",
      "vote_average": 5,
      "vote_count": 13
    },
    {
      "backdrop_path": "/wJmcuxa0C4AERmA9mejxm9qRYDj.jpg",
      "first_air_date": "2022-06-06",
      "genre_ids": [
        80,
        9648
      ],
      "id": 203504,
      "name": "Aashiqana",
      "origin_country": [
        "IN"
      ],
      "original_language": "hi",
      "original_name": "आशिकाना",
      "overview": "A serial killer sparks the story of uptight Yashvardhan and feisty Chikki. Plagued by misunderstandings, how far do they have to go to nab the murderer?",
      "popularity": 2732.908,
      "poster_path": "/a4Z6Uohb6Ln5vcPvMUzwyn3WBjP.jpg",
      "vote_average": 6.1,
      "vote_count": 10
    },
    "… 18 more items"
  ],
  "total_pages": 7416,
  "total_results": 148302
}
```

</details>


#### TV — Changes

```
GET https://api.themoviedb.org/3/tv/season/{season_id}/changes
```

Get the recent changes for a TV season.

Operation id: `tv-season-changes-by-id`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `end_date` | query | string | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string | - | - | - |
| `season_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `changes` | array<object> | - |
| `changes[].key` | string | episode |
| `changes[].items` | array<object> | - |
| `changes[].items[].id` | string | 5717c8c69251414cfd00250f |
| `changes[].items[].action` | string | updated |
| `changes[].items[].time` | string | 2016-04-20 18:21:58 UTC |
| `changes[].items[].value` | object | - |
| `changes[].items[].value.episode_id` | integer | 63056 |
| `changes[].items[].value.episode_number` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "changes": [
    {
      "key": "episode",
      "items": [
        {
          "id": "5717c8c69251414cfd00250f",
          "action": "updated",
          "time": "2016-04-20 18:21:58 UTC",
          "value": {
            "episode_id": 63056,
            "episode_number": 1
          }
        },
        {
          "id": "5717c90dc3a3686cb7001dd5",
          "action": "updated",
          "time": "2016-04-20 18:23:09 UTC",
          "value": {
            "episode_id": 63057,
            "episode_number": 2
          }
        },
        "… 8 more items"
      ]
    },
    {
      "key": "air_date",
      "items": [
        {
          "id": "5717cfefc3a3686c83001cb1",
          "action": "updated",
          "time": "2016-04-20 18:52:31 UTC",
          "value": "2011-04-16",
          "original_value": "2011-04-17"
        },
        {
          "id": "5717cffa9251413d3000345d",
          "action": "updated",
          "time": "2016-04-20 18:52:42 UTC",
          "value": "2011-04-16",
          "original_value": "2011-04-17"
        },
        "… 2 more items"
      ]
    },
    "… 2 more items"
  ]
}
```

</details>


#### TV — Top Rated

```
GET https://api.themoviedb.org/3/tv/top_rated
```

Get a list of TV shows ordered by rating.

Operation id: `tv-series-top-rated-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].backdrop_path` | string | /99vBORZixICa32Pwdwj0lWcr8K.jpg |
| `results[].first_air_date` | string | 2021-09-03 |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 130392 |
| `results[].name` | string | The D'Amelio Show |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | The D'Amelio Show |
| `results[].overview` | string | From relative obscurity and a seemingly nor… |
| `results[].popularity` | number | 12.459 |
| `results[].poster_path` | string | /phv2Jc4H8cvRzvTKb9X1uKMboTu.jpg |
| `results[].vote_average` | number | 8.9 |
| `results[].vote_count` | integer | 3190 |
| `total_pages` | integer | 142 |
| `total_results` | integer | 2833 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "backdrop_path": "/99vBORZixICa32Pwdwj0lWcr8K.jpg",
      "first_air_date": "2021-09-03",
      "genre_ids": [
        10764
      ],
      "id": 130392,
      "name": "The D'Amelio Show",
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "The D'Amelio Show",
      "overview": "From relative obscurity and a seemingly normal life, to overnight success and thrust into the Hollywood limelight overnight, the D’Amelios are faced with new challenges and opportunities they could not have imagined.",
      "popularity": 12.459,
      "poster_path": "/phv2Jc4H8cvRzvTKb9X1uKMboTu.jpg",
      "vote_average": 8.9,
      "vote_count": 3190
    },
    {
      "backdrop_path": "/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
      "first_air_date": "2008-01-20",
      "genre_ids": [
        18,
        80
      ],
      "id": 1396,
      "name": "Breaking Bad",
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Breaking Bad",
      "overview": "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of only two years left to live. He becomes filled with a sense of fearlessness and an unrelenting desire to secure his family's f...",
      "popularity": 292.904,
      "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
      "vote_average": 8.9,
      "vote_count": 11543
    },
    "… 18 more items"
  ],
  "total_pages": 142,
  "total_results": 2833
}
```

</details>


#### TV — Details

```
GET https://api.themoviedb.org/3/tv/{series_id}
```

Get the details of a TV show.

Operation id: `tv-series-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `append_to_response` | query | string | - | - | comma separated list of endpoints within this namespace, 20 items max |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `backdrop_path` | string | /6LWy0jvMpmjoS9fojNgHIKoWL05.jpg |
| `created_by` | array<object> | - |
| `created_by[].id` | integer | 9813 |
| `created_by[].credit_id` | string | 5256c8c219c2956ff604858a |
| `created_by[].name` | string | David Benioff |
| `created_by[].gender` | integer | 2 |
| `created_by[].profile_path` | string | /xvNN5huL0X8yJ7h3IZfGG4O2zBD.jpg |
| `episode_run_time` | array<integer> | - |
| `first_air_date` | string | 2011-04-17 |
| `genres` | array<object> | - |
| `genres[].id` | integer | 10765 |
| `genres[].name` | string | Sci-Fi & Fantasy |
| `homepage` | string | http://www.hbo.com/game-of-thrones |
| `id` | integer | 1399 |
| `in_production` | boolean | false |
| `languages` | array<string> | - |
| `last_air_date` | string | 2019-05-19 |
| `last_episode_to_air` | object | - |
| `last_episode_to_air.id` | integer | 1551830 |
| `last_episode_to_air.name` | string | The Iron Throne |
| `last_episode_to_air.overview` | string | In the aftermath of the devastating attack … |
| `last_episode_to_air.vote_average` | number | 4.809 |
| `last_episode_to_air.vote_count` | integer | 241 |
| `last_episode_to_air.air_date` | string | 2019-05-19 |
| `last_episode_to_air.episode_number` | integer | 6 |
| `last_episode_to_air.production_code` | string | 806 |
| `last_episode_to_air.runtime` | integer | 80 |
| `last_episode_to_air.season_number` | integer | 8 |
| `last_episode_to_air.show_id` | integer | 1399 |
| `last_episode_to_air.still_path` | string | /zBi2O5EJfgTS6Ae0HdAYLm9o2nf.jpg |
| `name` | string | Game of Thrones |
| `next_episode_to_air` | object | - |
| `networks` | array<object> | - |
| `networks[].id` | integer | 49 |
| `networks[].logo_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `networks[].name` | string | HBO |
| `networks[].origin_country` | string | US |
| `number_of_episodes` | integer | 73 |
| `number_of_seasons` | integer | 8 |
| `origin_country` | array<string> | - |
| `original_language` | string | en |
| `original_name` | string | Game of Thrones |
| `overview` | string | Seven noble families fight for control of t… |
| `popularity` | number | 346.098 |
| `poster_path` | string | /1XS1oqL89opfnbLl8WnZY1O1uJx.jpg |
| `production_companies` | array<object> | - |
| `production_companies[].id` | integer | 76043 |
| `production_companies[].logo_path` | string | /9RO2vbQ67otPrBLXCaC8UMp3Qat.png |
| `production_companies[].name` | string | Revolution Sun Studios |
| `production_companies[].origin_country` | string | US |
| `production_countries` | array<object> | - |
| `production_countries[].iso_3166_1` | string | GB |
| `production_countries[].name` | string | United Kingdom |
| `seasons` | array<object> | - |
| `seasons[].air_date` | string | 2010-12-05 |
| `seasons[].episode_count` | integer | 272 |
| `seasons[].id` | integer | 3627 |
| `seasons[].name` | string | Specials |
| `seasons[].overview` | string | - |
| `seasons[].poster_path` | string | /kMTcwNRfFKCZ0O2OaBZS0nZ2AIe.jpg |
| `seasons[].season_number` | integer | 0 |
| `seasons[].vote_average` | integer | 0 |
| `spoken_languages` | array<object> | - |
| `spoken_languages[].english_name` | string | English |
| `spoken_languages[].iso_639_1` | string | en |
| `spoken_languages[].name` | string | English |
| `status` | string | Ended |
| `tagline` | string | Winter Is Coming |
| `type` | string | Scripted |
| `vote_average` | number | 8.438 |
| `vote_count` | integer | 21390 |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "backdrop_path": "/6LWy0jvMpmjoS9fojNgHIKoWL05.jpg",
  "created_by": [
    {
      "id": 9813,
      "credit_id": "5256c8c219c2956ff604858a",
      "name": "David Benioff",
      "gender": 2,
      "profile_path": "/xvNN5huL0X8yJ7h3IZfGG4O2zBD.jpg"
    },
    "… 1 more items"
  ],
  "episode_run_time": [
    60
  ],
  "first_air_date": "2011-04-17",
  "genres": [
    {
      "id": 10765,
      "name": "Sci-Fi & Fantasy"
    },
    "… 2 more items"
  ],
  "homepage": "http://www.hbo.com/game-of-thrones",
  "id": 1399,
  "in_production": false,
  "languages": [
    "en"
  ],
  "last_air_date": "2019-05-19",
  "last_episode_to_air": {
    "id": 1551830,
    "name": "The Iron Throne",
    "overview": "In the aftermath of the devastating attack on King's Landing, Daenerys must face the survivors.",
    "vote_average": 4.809,
    "vote_count": 241,
    "air_date": "2019-05-19",
    "episode_number": 6,
    "production_code": "806",
    "runtime": 80,
    "season_number": 8,
    "show_id": 1399,
    "still_path": "/zBi2O5EJfgTS6Ae0HdAYLm9o2nf.jpg"
  },
  "name": "Game of Thrones",
  "next_episode_to_air": null,
  "networks": [
    {
      "id": 49,
      "logo_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
      "name": "HBO",
      "origin_country": "US"
    }
  ],
  "number_of_episodes": 73,
  "number_of_seasons": 8,
  "origin_country": [
    "US"
  ],
  "original_language": "en",
  "original_name": "Game of Thrones",
  "overview": "Seven noble families fight for control of the mythical land of Westeros. Friction between the houses leads to full-scale war. All while a very ancient evil awakens in the farthest north. Amidst the war, a neglected military order of misf...",
  "popularity": 346.098,
  "poster_path": "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
  "production_companies": [
    {
      "id": 76043,
      "logo_path": "/9RO2vbQ67otPrBLXCaC8UMp3Qat.png",
      "name": "Revolution Sun Studios",
      "origin_country": "US"
    },
    "… 3 more items"
  ],
  "production_countries": [
    {
      "iso_3166_1": "GB",
      "name": "United Kingdom"
    },
    "… 1 more items"
  ],
  "seasons": [
    {
      "air_date": "2010-12-05",
      "episode_count": 272,
      "id": 3627,
      "name": "Specials",
      "overview": "",
      "poster_path": "/kMTcwNRfFKCZ0O2OaBZS0nZ2AIe.jpg",
      "season_number": 0,
      "vote_average": 0
    },
    "… 8 more items"
  ],
  "spoken_languages": [
    {
      "english_name": "English",
      "iso_639_1": "en",
      "name": "English"
    }
  ],
  "status": "Ended",
  "tagline": "Winter Is Coming",
  "type": "
… truncated
```

</details>


#### TV — Account States

```
GET https://api.themoviedb.org/3/tv/{series_id}/account_states
```

Get the rating, watchlist and favourite status.

Operation id: `tv-series-account-states`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |
| `guest_session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `favorite` | boolean | true |
| `rated` | object | - |
| `rated.value` | integer | 9 |
| `watchlist` | boolean | false |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "favorite": true,
  "rated": {
    "value": 9.0
  },
  "watchlist": false
}
```

</details>


#### TV — Aggregate Credits

```
GET https://api.themoviedb.org/3/tv/{series_id}/aggregate_credits
```

Get the aggregate credits (cast and crew) that have been added to a TV show.

Operation id: `tv-series-aggregate-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 1 |
| `cast[].id` | integer | 1223786 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Emilia Clarke |
| `cast[].original_name` | string | Emilia Clarke |
| `cast[].popularity` | number | 42.737 |
| `cast[].profile_path` | string | /u59kTmNHXzaGZqokivxLPiBVIML.jpg |
| `cast[].roles` | array<object> | - |
| `cast[].roles[].credit_id` | string | 5256c8af19c2956ff60479f6 |
| `cast[].roles[].character` | string | Daenerys Targaryen |
| `cast[].roles[].episode_count` | integer | 78 |
| `cast[].total_episode_count` | integer | 78 |
| `cast[].order` | integer | 6 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 1 |
| `crew[].id` | integer | 6411 |
| `crew[].known_for_department` | string | Art |
| `crew[].name` | string | Deborah Riley |
| `crew[].original_name` | string | Deborah Riley |
| `crew[].popularity` | number | 1.4 |
| `crew[].profile_path` | string | /cjhADpqdrnwB1PdDUKaBnWrIj2Q.jpg |
| `crew[].jobs` | array<object> | - |
| `crew[].jobs[].credit_id` | string | 54eee9e5c3a3686d5800584e |
| `crew[].jobs[].job` | string | Production Design |
| `crew[].jobs[].episode_count` | integer | 43 |
| `crew[].department` | string | Art |
| `crew[].total_episode_count` | integer | 43 |
| `id` | integer | 1399 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "gender": 1,
      "id": 1223786,
      "known_for_department": "Acting",
      "name": "Emilia Clarke",
      "original_name": "Emilia Clarke",
      "popularity": 42.737,
      "profile_path": "/u59kTmNHXzaGZqokivxLPiBVIML.jpg",
      "roles": [
        {
          "credit_id": "5256c8af19c2956ff60479f6",
          "character": "Daenerys Targaryen",
          "episode_count": 78
        }
      ],
      "total_episode_count": 78,
      "order": 6
    },
    {
      "adult": false,
      "gender": 2,
      "id": 239019,
      "known_for_department": "Acting",
      "name": "Kit Harington",
      "original_name": "Kit Harington",
      "popularity": 19.319,
      "profile_path": "/htGBMno71BJAEGF3Y9f62MdA3Yt.jpg",
      "roles": [
        {
          "credit_id": "5256c8af19c2956ff6047af6",
          "character": "Jon Snow",
          "episode_count": 78
        }
      ],
      "total_episode_count": 78,
      "order": 9
    },
    "… 512 more items"
  ],
  "crew": [
    {
      "adult": false,
      "gender": 1,
      "id": 6411,
      "known_for_department": "Art",
      "name": "Deborah Riley",
      "original_name": "Deborah Riley",
      "popularity": 1.4,
      "profile_path": "/cjhADpqdrnwB1PdDUKaBnWrIj2Q.jpg",
      "jobs": [
        {
          "credit_id": "54eee9e5c3a3686d5800584e",
          "job": "Production Design",
          "episode_count": 43
        }
      ],
      "department": "Art",
      "total_episode_count": 43
    },
    {
      "adult": false,
      "gender": 2,
      "id": 80424,
      "known_for_department": "Art",
      "name": "Philip Elton",
      "original_name": "Philip Elton",
      "popularity": 0.84,
      "profile_path": null,
      "jobs": [
        {
          "credit_id": "56b8665b9251414c4500967c",
          "job": "Art Direction",
          "episode_count": 30
        }
      ],
      "department": "Art",
      "total_episode_count": 30
    },
    "… 269 more items"
  ],
  "id": 1399
}
```

</details>


#### TV — Alternative Titles

```
GET https://api.themoviedb.org/3/tv/{series_id}/alternative_titles
```

Get the alternative titles that have been added to a TV show.

Operation id: `tv-series-alternative-titles`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `results` | array<object> | - |
| `results[].iso_3166_1` | string | AL |
| `results[].title` | string | Froni i shpatave |
| `results[].type` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "results": [
    {
      "iso_3166_1": "AL",
      "title": "Froni i shpatave",
      "type": ""
    },
    {
      "iso_3166_1": "AR",
      "title": "El Juego de Tronos",
      "type": ""
    },
    "… 26 more items"
  ]
}
```

</details>


#### TV — Changes

```
GET https://api.themoviedb.org/3/tv/{series_id}/changes
```

Get the recent changes for a TV show.

Operation id: `tv-series-changes`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `end_date` | query | string | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `changes` | array<object> | - |
| `changes[].key` | string | images |
| `changes[].items` | array<object> | - |
| `changes[].items[].id` | string | 640435cf021cee0084710972 |
| `changes[].items[].action` | string | updated |
| `changes[].items[].time` | string | 2023-03-05 06:25:19 UTC |
| `changes[].items[].iso_639_1` | string | en |
| `changes[].items[].iso_3166_1` | string | - |
| `changes[].items[].value` | object | - |
| `changes[].items[].value.poster` | object | - |
| `changes[].items[].value.poster.file_path` | string | /ouudK6RCNnsbT1CSXrlATXQIQTG.jpg |
| `changes[].items[].value.poster.iso_639_1` | string | en |
| `changes[].items[].original_value` | object | - |
| `changes[].items[].original_value.poster` | object | - |
| `changes[].items[].original_value.poster.file_path` | string | /ouudK6RCNnsbT1CSXrlATXQIQTG.jpg |
| `changes[].items[].original_value.poster.iso_639_1` | string | fr |

<details><summary>Example response</summary>

```json
{
  "changes": [
    {
      "key": "images",
      "items": [
        {
          "id": "640435cf021cee0084710972",
          "action": "updated",
          "time": "2023-03-05 06:25:19 UTC",
          "iso_639_1": "en",
          "iso_3166_1": "",
          "value": {
            "poster": {
              "file_path": "/ouudK6RCNnsbT1CSXrlATXQIQTG.jpg",
              "iso_639_1": "en"
            }
          },
          "original_value": {
            "poster": {
              "file_path": "/ouudK6RCNnsbT1CSXrlATXQIQTG.jpg",
              "iso_639_1": "fr"
            }
          }
        },
        {
          "id": "640435df383df200d90638fa",
          "action": "updated",
          "time": "2023-03-05 06:25:35 UTC",
          "iso_639_1": "en",
          "iso_3166_1": "",
          "value": {
            "backdrop": {
              "file_path": "/xQgcyrg7QFyeNXVTpK3hPkPrH5z.jpg",
              "iso_639_1": "en"
            }
          },
          "original_value": {
            "backdrop": {
              "file_path": "/xQgcyrg7QFyeNXVTpK3hPkPrH5z.jpg",
              "iso_639_1": "fr"
            }
          }
        },
        "… 2 more items"
      ]
    }
  ]
}
```

</details>


#### TV — Content Ratings

```
GET https://api.themoviedb.org/3/tv/{series_id}/content_ratings
```

Get the content ratings that have been added to a TV show.

Operation id: `tv-series-content-ratings`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].descriptors` | array<object> | - |
| `results[].iso_3166_1` | string | DE |
| `results[].rating` | string | 16 |
| `id` | integer | 1399 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "descriptors": [],
      "iso_3166_1": "DE",
      "rating": "16"
    },
    {
      "descriptors": [],
      "iso_3166_1": "AU",
      "rating": "R18+"
    },
    "… 14 more items"
  ],
  "id": 1399
}
```

</details>


#### TV — Credits

```
GET https://api.themoviedb.org/3/tv/{series_id}/credits
```

Get the latest season credits of a TV show.

Operation id: `tv-series-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 2 |
| `cast[].id` | integer | 22970 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Peter Dinklage |
| `cast[].original_name` | string | Peter Dinklage |
| `cast[].popularity` | number | 30.6 |
| `cast[].profile_path` | string | /lRsRgnksAhBRXwAB68MFjmTtLrk.jpg |
| `cast[].character` | string | Tyrion Lannister |
| `cast[].credit_id` | string | 5256c8b219c2956ff6047cd8 |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 2 |
| `crew[].id` | integer | 1406855 |
| `crew[].known_for_department` | string | Production |
| `crew[].name` | string | Duncan Muggoch |
| `crew[].original_name` | string | Duncan Muggoch |
| `crew[].popularity` | number | 1.592 |
| `crew[].profile_path` | string | /ukGjJ62Ejd4cFziald03G34Fsrp.jpg |
| `crew[].credit_id` | string | 5ceab029c3a3682e93217a85 |
| `crew[].department` | string | Production |
| `crew[].job` | string | Producer |
| `id` | integer | 1399 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 22970,
      "known_for_department": "Acting",
      "name": "Peter Dinklage",
      "original_name": "Peter Dinklage",
      "popularity": 30.6,
      "profile_path": "/lRsRgnksAhBRXwAB68MFjmTtLrk.jpg",
      "character": "Tyrion Lannister",
      "credit_id": "5256c8b219c2956ff6047cd8",
      "order": 0
    },
    {
      "adult": false,
      "gender": 1,
      "id": 17286,
      "known_for_department": "Acting",
      "name": "Lena Headey",
      "original_name": "Lena Headey",
      "popularity": 24.88,
      "profile_path": "/xR2IBnBlUdyBe5hecaVdtRuQqUE.jpg",
      "character": "Cersei Lannister",
      "credit_id": "5256c8ad19c2956ff60479ce",
      "order": 3
    },
    "… 14 more items"
  ],
  "crew": [
    {
      "adult": false,
      "gender": 2,
      "id": 1406855,
      "known_for_department": "Production",
      "name": "Duncan Muggoch",
      "original_name": "Duncan Muggoch",
      "popularity": 1.592,
      "profile_path": "/ukGjJ62Ejd4cFziald03G34Fsrp.jpg",
      "credit_id": "5ceab029c3a3682e93217a85",
      "department": "Production",
      "job": "Producer"
    },
    {
      "adult": false,
      "gender": 2,
      "id": 114404,
      "known_for_department": "Directing",
      "name": "Miguel Sapochnik",
      "original_name": "Miguel Sapochnik",
      "popularity": 3.583,
      "profile_path": "/jlZGTjiifvvFDoEtlwrKz7QxuJS.jpg",
      "credit_id": "5ceab0820e0a261a50cb87a9",
      "department": "Production",
      "job": "Executive Producer"
    },
    "… 21 more items"
  ],
  "id": 1399
}
```

</details>


#### TV — Episode Groups

```
GET https://api.themoviedb.org/3/tv/{series_id}/episode_groups
```

Get the episode groups that have been added to a TV show.

Operation id: `tv-series-episode-groups`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].description` | string | - |
| `results[].episode_count` | integer | 102 |
| `results[].group_count` | integer | 9 |
| `results[].id` | string | 5e9077d2e640d600151f32bd |
| `results[].name` | string | Aired Order |
| `results[].network` | object | - |
| `results[].network.id` | integer | 49 |
| `results[].network.logo_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `results[].network.name` | string | HBO |
| `results[].network.origin_country` | string | US |
| `results[].type` | integer | 1 |
| `id` | integer | 1399 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "description": "",
      "episode_count": 102,
      "group_count": 9,
      "id": "5e9077d2e640d600151f32bd",
      "name": "Aired Order",
      "network": {
        "id": 49,
        "logo_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
        "name": "HBO",
        "origin_country": "US"
      },
      "type": 1
    }
  ],
  "id": 1399
}
```

</details>


#### TV — External IDs

```
GET https://api.themoviedb.org/3/tv/{series_id}/external_ids
```

Get a list of external IDs that have been added to a TV show.

Operation id: `tv-series-external-ids`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `imdb_id` | string | tt0944947 |
| `freebase_mid` | string | /m/0524b41 |
| `freebase_id` | string | /en/game_of_thrones |
| `tvdb_id` | integer | 121361 |
| `tvrage_id` | integer | 24493 |
| `wikidata_id` | string | Q23572 |
| `facebook_id` | string | GameOfThrones |
| `instagram_id` | string | gameofthrones |
| `twitter_id` | string | GameOfThrones |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "imdb_id": "tt0944947",
  "freebase_mid": "/m/0524b41",
  "freebase_id": "/en/game_of_thrones",
  "tvdb_id": 121361,
  "tvrage_id": 24493,
  "wikidata_id": "Q23572",
  "facebook_id": "GameOfThrones",
  "instagram_id": "gameofthrones",
  "twitter_id": "GameOfThrones"
}
```

</details>


#### TV — Images

```
GET https://api.themoviedb.org/3/tv/{series_id}/images
```

Get the images that belong to a TV series.

Operation id: `tv-series-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_image_language` | query | string | - | - | specify a comma separated list of ISO-639-1 values to query, for example: `en-US,null` |
| `language` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `backdrops` | array<object> | - |
| `backdrops[].aspect_ratio` | number | 1.778 |
| `backdrops[].height` | integer | 800 |
| `backdrops[].iso_639_1` | object | - |
| `backdrops[].file_path` | string | /hZkgoQYus5vegHoetLkCJzb17zJ.jpg |
| `backdrops[].vote_average` | number | 5.622 |
| `backdrops[].vote_count` | integer | 20 |
| `backdrops[].width` | integer | 1422 |
| `id` | integer | 550 |
| `logos` | array<object> | - |
| `logos[].aspect_ratio` | number | 5.203 |
| `logos[].height` | integer | 79 |
| `logos[].iso_639_1` | string | he |
| `logos[].file_path` | string | /c1KLulrIhUqY5fT42nmC5aERGCp.png |
| `logos[].vote_average` | number | 5.312 |
| `logos[].vote_count` | integer | 1 |
| `logos[].width` | integer | 411 |
| `posters` | array<object> | - |
| `posters[].aspect_ratio` | number | 0.667 |
| `posters[].height` | integer | 900 |
| `posters[].iso_639_1` | string | pt |
| `posters[].file_path` | string | /r3pPehX4ik8NLYPpbDRAh0YRtMb.jpg |
| `posters[].vote_average` | number | 5.258 |
| `posters[].vote_count` | integer | 6 |
| `posters[].width` | integer | 600 |

<details><summary>Example response</summary>

```json
{
  "backdrops": [
    {
      "aspect_ratio": 1.778,
      "height": 800,
      "iso_639_1": null,
      "file_path": "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
      "vote_average": 5.622,
      "vote_count": 20,
      "width": 1422
    },
    {
      "aspect_ratio": 1.778,
      "height": 720,
      "iso_639_1": "en",
      "file_path": "/fygeMr16EcxJiYhdiO1LEr7iHtI.jpg",
      "vote_average": 5.318,
      "vote_count": 3,
      "width": 1280
    },
    "… 39 more items"
  ],
  "id": 550,
  "logos": [
    {
      "aspect_ratio": 5.203,
      "height": 79,
      "iso_639_1": "he",
      "file_path": "/c1KLulrIhUqY5fT42nmC5aERGCp.png",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 411
    },
    {
      "aspect_ratio": 8.502,
      "height": 235,
      "iso_639_1": "pt",
      "file_path": "/qqAcl1YIT5Sa2nx8tKQcervQCco.png",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 1998
    },
    "… 26 more items"
  ],
  "posters": [
    {
      "aspect_ratio": 0.667,
      "height": 900,
      "iso_639_1": "pt",
      "file_path": "/r3pPehX4ik8NLYPpbDRAh0YRtMb.jpg",
      "vote_average": 5.258,
      "vote_count": 6,
      "width": 600
    },
    {
      "aspect_ratio": 0.667,
      "height": 1350,
      "iso_639_1": "ru",
      "file_path": "/66RvLrRJTm4J8l3uHXWF09AICol.jpg",
      "vote_average": 5.522,
      "vote_count": 4,
      "width": 900
    },
    "… 79 more items"
  ]
}
```

</details>


#### TV — Keywords

```
GET https://api.themoviedb.org/3/tv/{series_id}/keywords
```

Get a list of keywords that have been added to a TV show.

Operation id: `tv-series-keywords`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `results` | array<object> | - |
| `results[].name` | string | based on novel or book |
| `results[].id` | integer | 818 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "results": [
    {
      "name": "based on novel or book",
      "id": 818
    },
    {
      "name": "kingdom",
      "id": 4152
    },
    "… 4 more items"
  ]
}
```

</details>


#### TV — Lists

```
GET https://api.themoviedb.org/3/tv/{series_id}/lists
```

Get the lists that a TV series has been added to.

Operation id: `lists-copy`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].description` | string | - |
| `results[].favorite_count` | integer | 0 |
| `results[].id` | integer | 8257231 |
| `results[].item_count` | integer | 182 |
| `results[].iso_639_1` | string | en |
| `results[].iso_3166_1` | string | US |
| `results[].name` | string | Done |
| `results[].poster_path` | object | - |
| `total_pages` | integer | 96 |
| `total_results` | integer | 1906 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "page": 1,
  "results": [
    {
      "description": "",
      "favorite_count": 0,
      "id": 8257231,
      "item_count": 182,
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Done",
      "poster_path": null
    },
    {
      "description": "",
      "favorite_count": 0,
      "id": 8258601,
      "item_count": 220,
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "All",
      "poster_path": null
    },
    "… 18 more items"
  ],
  "total_pages": 96,
  "total_results": 1906
}
```

</details>


#### TV — Add Rating

```
POST https://api.themoviedb.org/3/tv/{series_id}/rating
```

Rate a TV show and save it to your rated list.

Operation id: `tv-series-add-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |
| `Content-Type` | header | string | yes | `application/json;charset=utf-8` | - |

**Request body** (`application/json`)

```json
{
  "value": 8.5
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

</details>


#### TV — Delete Rating

```
DELETE https://api.themoviedb.org/3/tv/{series_id}/rating
```

Operation id: `tv-series-delete-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `Content-Type` | header | string | - | `application/json;charset=utf-8` | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 13 |
| `status_message` | string | The item/record was deleted successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 13,
  "status_message": "The item/record was deleted successfully."
}
```

</details>


#### TV — Recommendations

```
GET https://api.themoviedb.org/3/tv/{series_id}/recommendations
```

Operation id: `tv-series-recommendations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg |
| `results[].id` | integer | 1396 |
| `results[].name` | string | Breaking Bad |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Breaking Bad |
| `results[].overview` | string | When Walter White, a New Mexico chemistry t… |
| `results[].poster_path` | string | /ggFHVNu6YYI5L9pCfOacjizRGt.jpg |
| `results[].media_type` | string | tv |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 292.904 |
| `results[].first_air_date` | string | 2008-01-20 |
| `results[].vote_average` | number | 8.878 |
| `results[].vote_count` | integer | 11544 |
| `results[].origin_country` | array<string> | - |
| `total_pages` | integer | 2 |
| `total_results` | integer | 40 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
      "id": 1396,
      "name": "Breaking Bad",
      "original_language": "en",
      "original_name": "Breaking Bad",
      "overview": "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of only two years left to live. He becomes filled with a sense of fearlessness and an unrelenting desire to secure his family's f...",
      "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
      "media_type": "tv",
      "genre_ids": [
        18,
        80
      ],
      "popularity": 292.904,
      "first_air_date": "2008-01-20",
      "vote_average": 8.878,
      "vote_count": 11544,
      "origin_country": [
        "US"
      ]
    },
    {
      "adult": false,
      "backdrop_path": "/wvdWb5kTQipdMDqCclC6Y3zr4j3.jpg",
      "id": 1402,
      "name": "The Walking Dead",
      "original_language": "en",
      "original_name": "The Walking Dead",
      "overview": "Sheriff's deputy Rick Grimes awakens from a coma to find a post-apocalyptic world dominated by flesh-eating zombies. He sets out to find his family and encounters many other survivors along the way.",
      "poster_path": "/xf9wuDcqlUPWABZNeDKPbZUjWx0.jpg",
      "media_type": "tv",
      "genre_ids": [
        10759,
        18,
        10765
      ],
      "popularity": 306.145,
      "first_air_date": "2010-10-31",
      "vote_average": 8.117,
      "vote_count": 14869,
      "origin_country": [
        "US"
      ]
    },
    "… 19 more items"
  ],
  "total_pages": 2,
  "total_results": 40
}
```

</details>


#### TV — Reviews

```
GET https://api.themoviedb.org/3/tv/{series_id}/reviews
```

Get the reviews that have been added to a TV show.

Operation id: `tv-series-reviews`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].author` | string | lmao7 |
| `results[].author_details` | object | - |
| `results[].author_details.name` | string | lmao7 |
| `results[].author_details.username` | string | lmao7 |
| `results[].author_details.avatar_path` | string | /ekmYOUU4tfx9zGGadjRdE7UPce.jpg |
| `results[].author_details.rating` | integer | 9 |
| `results[].content` | string | I started watching when it came out as I he… |
| `results[].created_at` | string | 2017-02-20T05:47:28.872Z |
| `results[].id` | string | 58aa82f09251416f92006a3a |
| `results[].updated_at` | string | 2021-06-23T15:57:54.649Z |
| `results[].url` | string | https://www.themoviedb.org/review/58aa82f09… |
| `total_pages` | integer | 1 |
| `total_results` | integer | 11 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "page": 1,
  "results": [
    {
      "author": "lmao7",
      "author_details": {
        "name": "lmao7",
        "username": "lmao7",
        "avatar_path": "/ekmYOUU4tfx9zGGadjRdE7UPce.jpg",
        "rating": 9.0
      },
      "content": "I started watching when it came out as I heard that fans of LOTR also liked this. I stopped watching after Season 1 as I was devastated lol kinda. Only 2015 I decided to continue watching and got addicted like it seemed complicated at fi...",
      "created_at": "2017-02-20T05:47:28.872Z",
      "id": "58aa82f09251416f92006a3a",
      "updated_at": "2021-06-23T15:57:54.649Z",
      "url": "https://www.themoviedb.org/review/58aa82f09251416f92006a3a"
    },
    {
      "author": "Vlad Ulbricht",
      "author_details": {
        "name": "Vlad Ulbricht",
        "username": "PresidentPutin",
        "avatar_path": "/srVsbbWgrmA4lmpqsrIYRYxJerc.jpg",
        "rating": 9.0
      },
      "content": "Cruel, bloody, vulgar, Machiavellian, unrepentant. And that is just the writing. The camera angles, the score, the pacing mesh together for grand storytelling: a mix of horror, swords and sorcery, and endless treachery. \r\n\r\nAnd all of th...",
      "created_at": "2017-05-11T03:53:19.211Z",
      "id": "5913e02fc3a3683a93004984",
      "updated_at": "2021-06-23T15:57:57.184Z",
      "url": "https://www.themoviedb.org/review/5913e02fc3a3683a93004984"
    },
    "… 9 more items"
  ],
  "total_pages": 1,
  "total_results": 11
}
```

</details>


#### TV — Screened Theatrically

```
GET https://api.themoviedb.org/3/tv/{series_id}/screened_theatrically
```

Get the seasons and episodes that have screened theatrically.

Operation id: `tv-series-screened-theatrically`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `results` | array<object> | - |
| `results[].id` | integer | 1159054 |
| `results[].episode_number` | integer | 10 |
| `results[].season_number` | integer | 5 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "results": [
    {
      "id": 1159054,
      "episode_number": 10,
      "season_number": 5
    },
    {
      "id": 63103,
      "episode_number": 10,
      "season_number": 4
    },
    "… 2 more items"
  ]
}
```

</details>


#### TV — Details

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}
```

Query the details of a TV season.

Operation id: `tv-season-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `append_to_response` | query | string | - | - | comma separated list of endpoints within this namespace, 20 items max |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `_id` | string | 5256c89f19c2956ff6046d47 |
| `air_date` | string | 2011-04-17 |
| `episodes` | array<object> | - |
| `episodes[].air_date` | string | 2011-04-17 |
| `episodes[].episode_number` | integer | 1 |
| `episodes[].episode_type` | string | standard |
| `episodes[].id` | integer | 63056 |
| `episodes[].name` | string | Winter Is Coming |
| `episodes[].overview` | string | Jon Arryn, the Hand of the King, is dead. K… |
| `episodes[].production_code` | string | 101 |
| `episodes[].runtime` | integer | 62 |
| `episodes[].season_number` | integer | 1 |
| `episodes[].show_id` | integer | 1399 |
| `episodes[].still_path` | string | /9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg |
| `episodes[].vote_average` | number | 8.1 |
| `episodes[].vote_count` | integer | 396 |
| `episodes[].crew` | array<object> | - |
| `episodes[].crew[].department` | string | Directing |
| `episodes[].crew[].job` | string | Director |
| `episodes[].crew[].credit_id` | string | 5256c8a219c2956ff6046e77 |
| `episodes[].crew[].adult` | boolean | false |
| `episodes[].crew[].gender` | integer | 2 |
| `episodes[].crew[].id` | integer | 44797 |
| `episodes[].crew[].known_for_department` | string | Directing |
| `episodes[].crew[].name` | string | Tim Van Patten |
| `episodes[].crew[].original_name` | string | Tim Van Patten |
| `episodes[].crew[].popularity` | number | 0.8004 |
| `episodes[].crew[].profile_path` | string | /vwcARZBg4PEzOwnPsXdjRWeUVrZ.jpg |
| `episodes[].guest_stars` | array<object> | - |
| `episodes[].guest_stars[].character` | string | Benjen Stark |
| `episodes[].guest_stars[].credit_id` | string | 5256c8b919c2956ff604836a |
| `episodes[].guest_stars[].order` | integer | 61 |
| `episodes[].guest_stars[].adult` | boolean | false |
| `episodes[].guest_stars[].gender` | integer | 2 |
| `episodes[].guest_stars[].id` | integer | 119783 |
| `episodes[].guest_stars[].known_for_department` | string | Acting |
| `episodes[].guest_stars[].name` | string | Joseph Mawle |
| `episodes[].guest_stars[].original_name` | string | Joseph Mawle |
| `episodes[].guest_stars[].popularity` | number | 0.8932 |
| `episodes[].guest_stars[].profile_path` | string | /1Ocb9v3h54beGVoJMm4w50UQhLf.jpg |
| `name` | string | Season 1 |
| `networks` | array<object> | - |
| `networks[].id` | integer | 49 |
| `networks[].logo_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `networks[].name` | string | HBO |
| `networks[].origin_country` | string | US |
| `overview` | string | Trouble is brewing in the Seven Kingdoms of… |
| `id` | integer | 3624 |
| `poster_path` | string | /wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg |
| `season_number` | integer | 1 |
| `vote_average` | number | 8.4 |

<details><summary>Example response</summary>

```json
{
  "_id": "5256c89f19c2956ff6046d47",
  "air_date": "2011-04-17",
  "episodes": [
    {
      "air_date": "2011-04-17",
      "episode_number": 1,
      "episode_type": "standard",
      "id": 63056,
      "name": "Winter Is Coming",
      "overview": "Jon Arryn, the Hand of the King, is dead. King Robert Baratheon plans to ask his oldest friend, Eddard Stark, to take Jon's place. Across the sea, Viserys Targaryen plans to wed his sister to a nomadic warlord in exchange for an army.",
      "production_code": "101",
      "runtime": 62,
      "season_number": 1,
      "show_id": 1399,
      "still_path": "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
      "vote_average": 8.1,
      "vote_count": 396,
      "crew": [
        {
          "department": "Directing",
          "job": "Director",
          "credit_id": "5256c8a219c2956ff6046e77",
          "adult": false,
          "gender": 2,
          "id": 44797,
          "known_for_department": "Directing",
          "name": "Tim Van Patten",
          "original_name": "Tim Van Patten",
          "popularity": 0.8004,
          "profile_path": "/vwcARZBg4PEzOwnPsXdjRWeUVrZ.jpg"
        },
        "… 4 more items"
      ],
      "guest_stars": [
        {
          "character": "Benjen Stark",
          "credit_id": "5256c8b919c2956ff604836a",
          "order": 61,
          "adult": false,
          "gender": 2,
          "id": 119783,
          "known_for_department": "Acting",
          "name": "Joseph Mawle",
          "original_name": "Joseph Mawle",
          "popularity": 0.8932,
          "profile_path": "/1Ocb9v3h54beGVoJMm4w50UQhLf.jpg"
        },
        "… 21 more items"
      ]
    },
    "… 9 more items"
  ],
  "name": "Season 1",
  "networks": [
    {
      "id": 49,
      "logo_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
      "name": "HBO",
      "origin_country": "US"
    }
  ],
  "overview": "Trouble is brewing in the Seven Kingdoms of Westeros. For the driven inhabitants of this visionary world, control of Westeros' Iron Throne holds the lure of great power. But in a land where the seasons can last a lifetime, winter is comi...",
  "id": 3624,
  "poster_path": "/wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg",
  "season_number": 1,
  "vote_average": 8.4
}
```

</details>


#### TV — Account States

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/account_states
```

Get the rating, watchlist and favourite status.

Operation id: `tv-season-account-states`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |
| `guest_session_id` | query | string | - | - | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `results` | array<object> | - |
| `results[].id` | integer | 63056 |
| `results[].episode_number` | integer | 1 |
| `results[].rated` | object | - |
| `results[].rated.value` | integer | 9 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "results": [
    {
      "id": 63056,
      "episode_number": 1,
      "rated": {
        "value": 9
      }
    },
    {
      "id": 63057,
      "episode_number": 2,
      "rated": false
    },
    "… 8 more items"
  ]
}
```

</details>


#### TV — Aggregate Credits

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/aggregate_credits
```

Get the aggregate credits (cast and crew) that have been added to a TV season.

Operation id: `tv-season-aggregate-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 2 |
| `cast[].id` | integer | 22970 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Peter Dinklage |
| `cast[].original_name` | string | Peter Dinklage |
| `cast[].popularity` | number | 30.6 |
| `cast[].profile_path` | string | /lRsRgnksAhBRXwAB68MFjmTtLrk.jpg |
| `cast[].roles` | array<object> | - |
| `cast[].roles[].credit_id` | string | 5256c8b219c2956ff6047cd8 |
| `cast[].roles[].character` | string | Tyrion Lannister |
| `cast[].roles[].episode_count` | integer | 10 |
| `cast[].total_episode_count` | integer | 10 |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 1 |
| `crew[].id` | integer | 9153 |
| `crew[].known_for_department` | string | Art |
| `crew[].name` | string | Gemma Jackson |
| `crew[].original_name` | string | Gemma Jackson |
| `crew[].popularity` | number | 0.995 |
| `crew[].profile_path` | object | - |
| `crew[].jobs` | array<object> | - |
| `crew[].jobs[].credit_id` | string | 54eee8b8c3a3686d5e005430 |
| `crew[].jobs[].job` | string | Production Design |
| `crew[].jobs[].episode_count` | integer | 10 |
| `crew[].department` | string | Art |
| `crew[].total_episode_count` | integer | 10 |
| `id` | integer | 3624 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 22970,
      "known_for_department": "Acting",
      "name": "Peter Dinklage",
      "original_name": "Peter Dinklage",
      "popularity": 30.6,
      "profile_path": "/lRsRgnksAhBRXwAB68MFjmTtLrk.jpg",
      "roles": [
        {
          "credit_id": "5256c8b219c2956ff6047cd8",
          "character": "Tyrion Lannister",
          "episode_count": 10
        }
      ],
      "total_episode_count": 10,
      "order": 0
    },
    {
      "adult": false,
      "gender": 2,
      "id": 48,
      "known_for_department": "Acting",
      "name": "Sean Bean",
      "original_name": "Sean Bean",
      "popularity": 34.931,
      "profile_path": "/kTjiABk3TJ3yI0Cto5RsvyT6V3o.jpg",
      "roles": [
        {
          "credit_id": "58c7134792514179d20011a9",
          "character": "Ned Stark",
          "episode_count": 10
        }
      ],
      "total_episode_count": 10,
      "order": 1
    },
    "… 106 more items"
  ],
  "crew": [
    {
      "adult": false,
      "gender": 1,
      "id": 9153,
      "known_for_department": "Art",
      "name": "Gemma Jackson",
      "original_name": "Gemma Jackson",
      "popularity": 0.995,
      "profile_path": null,
      "jobs": [
        {
          "credit_id": "54eee8b8c3a3686d5e005430",
          "job": "Production Design",
          "episode_count": 10
        }
      ],
      "department": "Art",
      "total_episode_count": 10
    },
    {
      "adult": false,
      "gender": 2,
      "id": 8410,
      "known_for_department": "Art",
      "name": "Richard Roberts",
      "original_name": "Richard Roberts",
      "popularity": 1.4,
      "profile_path": null,
      "jobs": [
        {
          "credit_id": "5c6d16640e0a262c999fc3c9",
          "job": "Set Decoration",
          "episode_count": 10
        }
      ],
      "department": "Art",
      "total_episode_count": 10
    },
    "… 29 more items"
  ],
  "id": 3624
}
```

</details>


#### TV — Credits

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/credits
```

Operation id: `tv-season-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 2 |
| `cast[].id` | integer | 22970 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Peter Dinklage |
| `cast[].original_name` | string | Peter Dinklage |
| `cast[].popularity` | number | 30.6 |
| `cast[].profile_path` | string | /lRsRgnksAhBRXwAB68MFjmTtLrk.jpg |
| `cast[].character` | string | Tyrion Lannister |
| `cast[].credit_id` | string | 5256c8b219c2956ff6047cd8 |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 0 |
| `crew[].id` | integer | 1223796 |
| `crew[].known_for_department` | string | Production |
| `crew[].name` | string | Frank Doelger |
| `crew[].original_name` | string | Frank Doelger |
| `crew[].popularity` | number | 0.694 |
| `crew[].profile_path` | object | - |
| `crew[].credit_id` | string | 5256c8c419c2956ff604867c |
| `crew[].department` | string | Production |
| `crew[].job` | string | Producer |
| `id` | integer | 3624 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 22970,
      "known_for_department": "Acting",
      "name": "Peter Dinklage",
      "original_name": "Peter Dinklage",
      "popularity": 30.6,
      "profile_path": "/lRsRgnksAhBRXwAB68MFjmTtLrk.jpg",
      "character": "Tyrion Lannister",
      "credit_id": "5256c8b219c2956ff6047cd8",
      "order": 0
    },
    {
      "adult": false,
      "gender": 2,
      "id": 48,
      "known_for_department": "Acting",
      "name": "Sean Bean",
      "original_name": "Sean Bean",
      "popularity": 34.931,
      "profile_path": "/kTjiABk3TJ3yI0Cto5RsvyT6V3o.jpg",
      "character": "Ned Stark",
      "credit_id": "58c7134792514179d20011a9",
      "order": 1
    },
    "… 16 more items"
  ],
  "crew": [
    {
      "adult": false,
      "gender": 0,
      "id": 1223796,
      "known_for_department": "Production",
      "name": "Frank Doelger",
      "original_name": "Frank Doelger",
      "popularity": 0.694,
      "profile_path": null,
      "credit_id": "5256c8c419c2956ff604867c",
      "department": "Production",
      "job": "Producer"
    },
    {
      "adult": false,
      "gender": 2,
      "id": 8410,
      "known_for_department": "Art",
      "name": "Richard Roberts",
      "original_name": "Richard Roberts",
      "popularity": 1.4,
      "profile_path": null,
      "credit_id": "5c6d16640e0a262c999fc3c9",
      "department": "Art",
      "job": "Set Decoration"
    },
    "… 15 more items"
  ],
  "id": 3624
}
```

</details>


#### TV — Details

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}
```

Query the details of a TV episode.

Operation id: `tv-episode-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |
| `append_to_response` | query | string | - | - | comma separated list of endpoints within this namespace, 20 items max |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `air_date` | string | 2011-04-17 |
| `crew` | array<object> | - |
| `crew[].department` | string | Directing |
| `crew[].job` | string | Director |
| `crew[].credit_id` | string | 5256c8a219c2956ff6046e77 |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 2 |
| `crew[].id` | integer | 44797 |
| `crew[].known_for_department` | string | Directing |
| `crew[].name` | string | Timothy Van Patten |
| `crew[].original_name` | string | Timothy Van Patten |
| `crew[].popularity` | number | 7.775 |
| `crew[].profile_path` | string | /MzSOFrd99HRdr6pkSRSctk3kBR.jpg |
| `episode_number` | integer | 1 |
| `guest_stars` | array<object> | - |
| `guest_stars[].character` | string | Benjen Stark |
| `guest_stars[].credit_id` | string | 5256c8b919c2956ff604836a |
| `guest_stars[].order` | integer | 62 |
| `guest_stars[].adult` | boolean | false |
| `guest_stars[].gender` | integer | 2 |
| `guest_stars[].id` | integer | 119783 |
| `guest_stars[].known_for_department` | string | Acting |
| `guest_stars[].name` | string | Joseph Mawle |
| `guest_stars[].original_name` | string | Joseph Mawle |
| `guest_stars[].popularity` | number | 6.758 |
| `guest_stars[].profile_path` | string | /1Ocb9v3h54beGVoJMm4w50UQhLf.jpg |
| `name` | string | Winter Is Coming |
| `overview` | string | Jon Arryn, the Hand of the King, is dead. K… |
| `id` | integer | 63056 |
| `production_code` | string | 101 |
| `runtime` | integer | 62 |
| `season_number` | integer | 1 |
| `still_path` | string | /9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg |
| `vote_average` | number | 7.8 |
| `vote_count` | integer | 286 |

<details><summary>Example response</summary>

```json
{
  "air_date": "2011-04-17",
  "crew": [
    {
      "department": "Directing",
      "job": "Director",
      "credit_id": "5256c8a219c2956ff6046e77",
      "adult": false,
      "gender": 2,
      "id": 44797,
      "known_for_department": "Directing",
      "name": "Timothy Van Patten",
      "original_name": "Timothy Van Patten",
      "popularity": 7.775,
      "profile_path": "/MzSOFrd99HRdr6pkSRSctk3kBR.jpg"
    },
    {
      "job": "Director of Photography",
      "department": "Camera",
      "credit_id": "54eef2429251417974005cb6",
      "adult": false,
      "gender": 2,
      "id": 1318704,
      "known_for_department": "Directing",
      "name": "Alik Sakharov",
      "original_name": "Alik Sakharov",
      "popularity": 1.347,
      "profile_path": null
    },
    "… 3 more items"
  ],
  "episode_number": 1,
  "guest_stars": [
    {
      "character": "Benjen Stark",
      "credit_id": "5256c8b919c2956ff604836a",
      "order": 62,
      "adult": false,
      "gender": 2,
      "id": 119783,
      "known_for_department": "Acting",
      "name": "Joseph Mawle",
      "original_name": "Joseph Mawle",
      "popularity": 6.758,
      "profile_path": "/1Ocb9v3h54beGVoJMm4w50UQhLf.jpg"
    },
    {
      "character": "Khal Drogo",
      "credit_id": "5256c8a219c2956ff6046f40",
      "order": 63,
      "adult": false,
      "gender": 2,
      "id": 117642,
      "known_for_department": "Acting",
      "name": "Jason Momoa",
      "original_name": "Jason Momoa",
      "popularity": 72.117,
      "profile_path": "/6dEFBpZH8C8OijsynkSajQT99Pb.jpg"
    },
    "… 19 more items"
  ],
  "name": "Winter Is Coming",
  "overview": "Jon Arryn, the Hand of the King, is dead. King Robert Baratheon plans to ask his oldest friend, Eddard Stark, to take Jon's place. Across the sea, Viserys Targaryen plans to wed his sister to a nomadic warlord in exchange for an army.",
  "id": 63056,
  "production_code": "101",
  "runtime": 62,
  "season_number": 1,
  "still_path": "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
  "vote_average": 7.8,
  "vote_count": 286
}
```

</details>


#### TV — Account States

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/account_states
```

Get the rating, watchlist and favourite status.

Operation id: `tv-episode-account-states`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |
| `guest_session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 550 |
| `favorite` | boolean | true |
| `rated` | object | - |
| `rated.value` | integer | 9 |
| `watchlist` | boolean | false |

<details><summary>Example response</summary>

```json
{
  "id": 550,
  "favorite": true,
  "rated": {
    "value": 9.0
  },
  "watchlist": false
}
```

</details>


#### TV — Credits

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/credits
```

Operation id: `tv-episode-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].gender` | integer | 2 |
| `cast[].id` | integer | 22970 |
| `cast[].known_for_department` | string | Acting |
| `cast[].name` | string | Peter Dinklage |
| `cast[].original_name` | string | Peter Dinklage |
| `cast[].popularity` | number | 30.6 |
| `cast[].profile_path` | string | /lRsRgnksAhBRXwAB68MFjmTtLrk.jpg |
| `cast[].character` | string | Tyrion Lannister |
| `cast[].credit_id` | string | 5256c8b219c2956ff6047cd8 |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].department` | string | Directing |
| `crew[].job` | string | Director |
| `crew[].credit_id` | string | 5256c8a219c2956ff6046e77 |
| `crew[].adult` | boolean | false |
| `crew[].gender` | integer | 2 |
| `crew[].id` | integer | 44797 |
| `crew[].known_for_department` | string | Directing |
| `crew[].name` | string | Timothy Van Patten |
| `crew[].original_name` | string | Timothy Van Patten |
| `crew[].popularity` | number | 8.292 |
| `crew[].profile_path` | string | /MzSOFrd99HRdr6pkSRSctk3kBR.jpg |
| `guest_stars` | array<object> | - |
| `guest_stars[].character` | string | Benjen Stark |
| `guest_stars[].credit_id` | string | 5256c8b919c2956ff604836a |
| `guest_stars[].order` | integer | 62 |
| `guest_stars[].adult` | boolean | false |
| `guest_stars[].gender` | integer | 2 |
| `guest_stars[].id` | integer | 119783 |
| `guest_stars[].known_for_department` | string | Acting |
| `guest_stars[].name` | string | Joseph Mawle |
| `guest_stars[].original_name` | string | Joseph Mawle |
| `guest_stars[].popularity` | number | 8.559 |
| `guest_stars[].profile_path` | string | /1Ocb9v3h54beGVoJMm4w50UQhLf.jpg |
| `id` | integer | 63056 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 22970,
      "known_for_department": "Acting",
      "name": "Peter Dinklage",
      "original_name": "Peter Dinklage",
      "popularity": 30.6,
      "profile_path": "/lRsRgnksAhBRXwAB68MFjmTtLrk.jpg",
      "character": "Tyrion Lannister",
      "credit_id": "5256c8b219c2956ff6047cd8",
      "order": 0
    },
    {
      "adult": false,
      "gender": 2,
      "id": 48,
      "known_for_department": "Acting",
      "name": "Sean Bean",
      "original_name": "Sean Bean",
      "popularity": 34.931,
      "profile_path": "/kTjiABk3TJ3yI0Cto5RsvyT6V3o.jpg",
      "character": "Ned Stark",
      "credit_id": "58c7134792514179d20011a9",
      "order": 1
    },
    "… 16 more items"
  ],
  "crew": [
    {
      "department": "Directing",
      "job": "Director",
      "credit_id": "5256c8a219c2956ff6046e77",
      "adult": false,
      "gender": 2,
      "id": 44797,
      "known_for_department": "Directing",
      "name": "Timothy Van Patten",
      "original_name": "Timothy Van Patten",
      "popularity": 8.292,
      "profile_path": "/MzSOFrd99HRdr6pkSRSctk3kBR.jpg"
    },
    {
      "job": "Director of Photography",
      "department": "Camera",
      "credit_id": "54eef2429251417974005cb6",
      "adult": false,
      "gender": 2,
      "id": 1318704,
      "known_for_department": "Directing",
      "name": "Alik Sakharov",
      "original_name": "Alik Sakharov",
      "popularity": 1.579,
      "profile_path": null
    },
    "… 3 more items"
  ],
  "guest_stars": [
    {
      "character": "Benjen Stark",
      "credit_id": "5256c8b919c2956ff604836a",
      "order": 62,
      "adult": false,
      "gender": 2,
      "id": 119783,
      "known_for_department": "Acting",
      "name": "Joseph Mawle",
      "original_name": "Joseph Mawle",
      "popularity": 8.559,
      "profile_path": "/1Ocb9v3h54beGVoJMm4w50UQhLf.jpg"
    },
    {
      "character": "Khal Drogo",
      "credit_id": "5256c8a219c2956ff6046f40",
      "order": 63,
      "adult": false,
      "gender": 2,
      "id": 117642,
      "known_for_department": "Acting",
      "name": "Jason Momoa",
      "original_name": "Jason Momoa",
      "popularity": 39.109,
      "profile_path": "/tsfL6u1WW6zHYS1lhWGPHuk6JON.jpg"
    },
    "… 19 more items"
  ],
  "id": 63056
}
```

</details>


#### TV — External IDs

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/external_ids
```

Get a list of external IDs that have been added to a TV episode.

Operation id: `tv-episode-external-ids`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | string | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 63056 |
| `imdb_id` | string | tt1480055 |
| `freebase_mid` | string | /m/0gmc6ph |
| `freebase_id` | string | /en/winter_is_coming |
| `tvdb_id` | integer | 3254641 |
| `tvrage_id` | integer | 1065008299 |
| `wikidata_id` | string | Q2614622 |

<details><summary>Example response</summary>

```json
{
  "id": 63056,
  "imdb_id": "tt1480055",
  "freebase_mid": "/m/0gmc6ph",
  "freebase_id": "/en/winter_is_coming",
  "tvdb_id": 3254641,
  "tvrage_id": 1065008299,
  "wikidata_id": "Q2614622"
}
```

</details>


#### TV — Images

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/images
```

Get the images that belong to a TV episode.

Operation id: `tv-episode-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_image_language` | query | string | - | - | specify a comma separated list of ISO-639-1 values to query, for example: `en-US,null` |
| `language` | query | string | - | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 63056 |
| `stills` | array<object> | - |
| `stills[].aspect_ratio` | number | 1.778 |
| `stills[].height` | integer | 1080 |
| `stills[].iso_639_1` | object | - |
| `stills[].file_path` | string | /9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg |
| `stills[].vote_average` | number | 5.454 |
| `stills[].vote_count` | integer | 3 |
| `stills[].width` | integer | 1920 |

<details><summary>Example response</summary>

```json
{
  "id": 63056,
  "stills": [
    {
      "aspect_ratio": 1.778,
      "height": 1080,
      "iso_639_1": null,
      "file_path": "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
      "vote_average": 5.454,
      "vote_count": 3,
      "width": 1920
    },
    {
      "aspect_ratio": 1.778,
      "height": 1080,
      "iso_639_1": null,
      "file_path": "/xIfvIM7YgkADTrqp23rm3CLaOVQ.jpg",
      "vote_average": 5.322,
      "vote_count": 5,
      "width": 1920
    },
    "… 13 more items"
  ]
}
```

</details>


#### TV — Add Rating

```
POST https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating
```

Rate a TV episode and save it to your rated list.

Operation id: `tv-episode-add-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |
| `Content-Type` | header | string | yes | `application/json;charset=utf-8` | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Request body** (`application/json`)

```json
{
  "value": 8.5
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

</details>


#### TV — Delete Rating

```
DELETE https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating
```

Delete your rating on a TV episode.

Operation id: `tv-episode-delete-rating`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `Content-Type` | header | string | - | `application/json;charset=utf-8` | - |
| `guest_session_id` | query | string | - | - | - |
| `session_id` | query | string | - | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 13 |
| `status_message` | string | The item/record was deleted successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 13,
  "status_message": "The item/record was deleted successfully."
}
```

</details>


#### TV — Translations

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/translations
```

Get the translations that have been added to a TV episode.

Operation id: `tv-episode-translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 63056 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | SA |
| `translations[].iso_639_1` | string | ar |
| `translations[].name` | string | العربية |
| `translations[].english_name` | string | Arabic |
| `translations[].data` | object | - |
| `translations[].data.name` | string | - |
| `translations[].data.overview` | string | خلف باب واسع من الجليد في شمالي وستيروس هنا… |

<details><summary>Example response</summary>

```json
{
  "id": 63056,
  "translations": [
    {
      "iso_3166_1": "SA",
      "iso_639_1": "ar",
      "name": "العربية",
      "english_name": "Arabic",
      "data": {
        "name": "",
        "overview": "خلف باب واسع من الجليد في شمالي وستيروس هناك شيء يحدث. تتلقى عائلة ستارك التي من وينترفيل زيارة من العائلة المالكة، بينما يشكل أمير عائلة تارغارين المنفي تحالفاً جديداً للسيطرة على العرش من جديد."
      }
    },
    {
      "iso_3166_1": "BY",
      "iso_639_1": "be",
      "name": "беларуская мова",
      "english_name": "Belarusian",
      "data": {
        "name": "",
        "overview": ""
      }
    },
    "… 51 more items"
  ]
}
```

</details>


#### TV — Videos

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/episode/{episode_number}/videos
```

Get the videos that belong to a TV episode.

Operation id: `tv-episode-videos`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_video_language` | query | string | - | - | filter the list results by language, supports more than one value by using a comma |
| `language` | query | string | - | `en-US` | - |
| `season_number` | path | integer (int32) | yes | - | - |
| `episode_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `results` | array<object> | - |
| `results[].iso_639_1` | string | en |
| `results[].iso_3166_1` | string | US |
| `results[].name` | string | Game Of Thrones - Season 1 Recap - Official… |
| `results[].key` | string | e0Y8KpQpW8c |
| `results[].site` | string | YouTube |
| `results[].size` | integer | 1080 |
| `results[].type` | string | Recap |
| `results[].official` | boolean | true |
| `results[].published_at` | string | 2015-05-19T16:31:23.000Z |
| `results[].id` | string | 5ce71a920e0a265ac0cfe497 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "results": [
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Game Of Thrones - Season 1 Recap - Official HBO UK",
      "key": "e0Y8KpQpW8c",
      "site": "YouTube",
      "size": 1080,
      "type": "Recap",
      "official": true,
      "published_at": "2015-05-19T16:31:23.000Z",
      "id": "5ce71a920e0a265ac0cfe497"
    },
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Game of Thrones: Pledge Your Allegiance - House Stark (HBO)",
      "key": "9cqABojhqr4",
      "published_at": "2012-03-19T20:01:10.000Z",
      "site": "YouTube",
      "size": 720,
      "type": "Featurette",
      "official": true,
      "id": "534921db0e0a266b3f0013bb"
    },
    "… 14 more items"
  ]
}
```

</details>


#### TV — External IDs

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/external_ids
```

Get a list of external IDs that have been added to a TV season.

Operation id: `tv-season-external-ids`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `freebase_mid` | string | /m/0gmd1gd |
| `freebase_id` | string | /m/0gmd1gd |
| `tvdb_id` | integer | 364731 |
| `tvrage_id` | object | - |
| `wikidata_id` | string | Q1658029 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "freebase_mid": "/m/0gmd1gd",
  "freebase_id": "/m/0gmd1gd",
  "tvdb_id": 364731,
  "tvrage_id": null,
  "wikidata_id": "Q1658029"
}
```

</details>


#### TV — Images

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/images
```

Get the images that belong to a TV season.

Operation id: `tv-season-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_image_language` | query | string | - | - | specify a comma separated list of ISO-639-1 values to query, for example: `en-US,null` |
| `language` | query | string | - | - | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `posters` | array<object> | - |
| `posters[].aspect_ratio` | number | 0.667 |
| `posters[].height` | integer | 1500 |
| `posters[].iso_639_1` | string | en |
| `posters[].file_path` | string | /wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg |
| `posters[].vote_average` | number | 5.514 |
| `posters[].vote_count` | integer | 18 |
| `posters[].width` | integer | 1000 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "posters": [
    {
      "aspect_ratio": 0.667,
      "height": 1500,
      "iso_639_1": "en",
      "file_path": "/wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg",
      "vote_average": 5.514,
      "vote_count": 18,
      "width": 1000
    },
    {
      "aspect_ratio": 0.667,
      "height": 1500,
      "iso_639_1": "pt",
      "file_path": "/wNOdNgqoySuWbQk3F0QZjPKp8X7.jpg",
      "vote_average": 5.384,
      "vote_count": 2,
      "width": 1000
    },
    "… 60 more items"
  ]
}
```

</details>


#### TV — Translations

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/translations
```

Get the translations for a TV season.

Operation id: `tv-season-translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | SA |
| `translations[].iso_639_1` | string | ar |
| `translations[].name` | string | العربية |
| `translations[].english_name` | string | Arabic |
| `translations[].data` | object | - |
| `translations[].data.name` | string | - |
| `translations[].data.overview` | string | سلسلة درامية مبنية على سلسلة روايات لـ جورج… |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "translations": [
    {
      "iso_3166_1": "SA",
      "iso_639_1": "ar",
      "name": "العربية",
      "english_name": "Arabic",
      "data": {
        "name": "",
        "overview": "سلسلة درامية مبنية على سلسلة روايات لـ جورج آر آر مارتن بعنوان \"إيه سونغ أوف آيس أن فاير\" والتي حققت مبيعات كبيرة وتتمحور حول الصراعات التي كانت تحدث في العصور الوسطى بين العائلات النبيلة للسيطرة على عرش وستيروس."
      }
    },
    {
      "iso_3166_1": "BY",
      "iso_639_1": "be",
      "name": "беларуская мова",
      "english_name": "Belarusian",
      "data": {
        "name": "",
        "overview": ""
      }
    },
    "… 51 more items"
  ]
}
```

</details>


#### TV — Videos

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/videos
```

Get the videos that belong to a TV season.

Operation id: `tv-season-videos`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_video_language` | query | string | - | - | filter the list results by language, supports more than one value by using a comma |
| `language` | query | string | - | `en-US` | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `results` | array<object> | - |
| `results[].iso_639_1` | string | en |
| `results[].iso_3166_1` | string | US |
| `results[].name` | string | Game Of Thrones - Season 1 Recap - Official… |
| `results[].key` | string | e0Y8KpQpW8c |
| `results[].site` | string | YouTube |
| `results[].size` | integer | 1080 |
| `results[].type` | string | Recap |
| `results[].official` | boolean | true |
| `results[].published_at` | string | 2015-05-19T16:31:23.000Z |
| `results[].id` | string | 5ce71a920e0a265ac0cfe497 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "results": [
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Game Of Thrones - Season 1 Recap - Official HBO UK",
      "key": "e0Y8KpQpW8c",
      "site": "YouTube",
      "size": 1080,
      "type": "Recap",
      "official": true,
      "published_at": "2015-05-19T16:31:23.000Z",
      "id": "5ce71a920e0a265ac0cfe497"
    },
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Game of Thrones: Pledge Your Allegiance - House Stark (HBO)",
      "key": "9cqABojhqr4",
      "published_at": "2012-03-19T20:01:10.000Z",
      "site": "YouTube",
      "size": 720,
      "type": "Featurette",
      "official": true,
      "id": "534921db0e0a266b3f0013bb"
    },
    "… 14 more items"
  ]
}
```

</details>


#### TV — Watch Providers

```
GET https://api.themoviedb.org/3/tv/{series_id}/season/{season_number}/watch/providers
```

Get the list of streaming providers we have for a TV season.

Operation id: `tv-season-watch-providers`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `season_number` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 3624 |
| `results` | object | - |
| `results.AD` | object | - |
| `results.AD.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AD.flatrate` | array<object> | - |
| `results.AD.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.AD.flatrate[].provider_id` | integer | 1899 |
| `results.AD.flatrate[].provider_name` | string | HBO Max |
| `results.AD.flatrate[].display_priority` | integer | 24 |
| `results.AE` | object | - |
| `results.AE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AE.flatrate` | array<object> | - |
| `results.AE.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.AE.flatrate[].provider_id` | integer | 629 |
| `results.AE.flatrate[].provider_name` | string | OSN+ |
| `results.AE.flatrate[].display_priority` | integer | 7 |
| `results.AG` | object | - |
| `results.AG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AG.flatrate` | array<object> | - |
| `results.AG.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.AG.flatrate[].provider_id` | integer | 1899 |
| `results.AG.flatrate[].provider_name` | string | HBO Max |
| `results.AG.flatrate[].display_priority` | integer | 11 |
| `results.AR` | object | - |
| `results.AR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AR.flatrate` | array<object> | - |
| `results.AR.flatrate[].logo_path` | string | /nr5UBW4IGKgBwmhpTMOfcvnX2vX.jpg |
| `results.AR.flatrate[].provider_id` | integer | 467 |
| `results.AR.flatrate[].provider_name` | string | DIRECTV GO |
| `results.AR.flatrate[].display_priority` | integer | 12 |
| `results.AT` | object | - |
| `results.AT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AT.buy` | array<object> | - |
| `results.AT.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.AT.buy[].provider_id` | integer | 10 |
| `results.AT.buy[].provider_name` | string | Amazon Video |
| `results.AT.buy[].display_priority` | integer | 3 |
| `results.AT.flatrate` | array<object> | - |
| `results.AT.flatrate[].logo_path` | string | /kAZkQcIxMxTmlwdgSB05fqtymp0.jpg |
| `results.AT.flatrate[].provider_id` | integer | 29 |
| `results.AT.flatrate[].provider_name` | string | Sky Go |
| `results.AT.flatrate[].display_priority` | integer | 8 |
| `results.AU` | object | - |
| `results.AU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AU.flatrate` | array<object> | - |
| `results.AU.flatrate[].logo_path` | string | /fejdSG7TwNQ5E0p6u7A6LVs280R.jpg |
| `results.AU.flatrate[].provider_id` | integer | 134 |
| `results.AU.flatrate[].provider_name` | string | Foxtel Now |
| `results.AU.flatrate[].display_priority` | integer | 8 |
| `results.AU.buy` | array<object> | - |
| `results.AU.buy[].logo_path` | string | /oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg |
| `results.AU.buy[].provider_id` | integer | 2 |
| `results.AU.buy[].provider_name` | string | Apple TV |
| `results.AU.buy[].display_priority` | integer | 11 |
| `results.BA` | object | - |
| `results.BA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BA.flatrate` | array<object> | - |
| `results.BA.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BA.flatrate[].provider_id` | integer | 1899 |
| `results.BA.flatrate[].provider_name` | string | HBO Max |
| `results.BA.flatrate[].display_priority` | integer | 27 |
| `results.BB` | object | - |
| `results.BB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BB.flatrate` | array<object> | - |
| `results.BB.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BB.flatrate[].provider_id` | integer | 1899 |
| `results.BB.flatrate[].provider_name` | string | HBO Max |
| `results.BB.flatrate[].display_priority` | integer | 23 |
| `results.BE` | object | - |
| `results.BE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BE.buy` | array<object> | - |
| `results.BE.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.BE.buy[].provider_id` | integer | 10 |
| `results.BE.buy[].provider_name` | string | Amazon Video |
| `results.BE.buy[].display_priority` | integer | 33 |
| `results.BE.flatrate` | array<object> | - |
| `results.BE.flatrate[].logo_path` | string | /kwftIxtjuCAROIcdd53UEjzSmca.jpg |
| `results.BE.flatrate[].provider_id` | integer | 1857 |
| `results.BE.flatrate[].provider_name` | string | Telenet |
| `results.BE.flatrate[].display_priority` | integer | 31 |
| `results.BG` | object | - |
| `results.BG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BG.flatrate` | array<object> | - |
| `results.BG.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BG.flatrate[].provider_id` | integer | 1899 |
| `results.BG.flatrate[].provider_name` | string | HBO Max |
| `results.BG.flatrate[].display_priority` | integer | 21 |
| `results.BH` | object | - |
| `results.BH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BH.flatrate` | array<object> | - |
| `results.BH.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.BH.flatrate[].provider_id` | integer | 629 |
| `results.BH.flatrate[].provider_name` | string | OSN+ |
| `results.BH.flatrate[].display_priority` | integer | 29 |
| `results.BO` | object | - |
| `results.BO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BO.flatrate` | array<object> | - |
| `results.BO.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BO.flatrate[].provider_id` | integer | 1899 |
| `results.BO.flatrate[].provider_name` | string | HBO Max |
| `results.BO.flatrate[].display_priority` | integer | 30 |
| `results.BR` | object | - |
| `results.BR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BR.flatrate` | array<object> | - |
| `results.BR.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BR.flatrate[].provider_id` | integer | 1899 |
| `results.BR.flatrate[].provider_name` | string | HBO Max |
| `results.BR.flatrate[].display_priority` | integer | 6 |
| `results.BS` | object | - |
| `results.BS.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BS.flatrate` | array<object> | - |
| `results.BS.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BS.flatrate[].provider_id` | integer | 1899 |
| `results.BS.flatrate[].provider_name` | string | HBO Max |
| `results.BS.flatrate[].display_priority` | integer | 24 |
| `results.BZ` | object | - |
| `results.BZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BZ.flatrate` | array<object> | - |
| `results.BZ.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.BZ.flatrate[].provider_id` | integer | 1899 |
| `results.BZ.flatrate[].provider_name` | string | HBO Max |
| `results.BZ.flatrate[].display_priority` | integer | 9 |
| `results.CA` | object | - |
| `results.CA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CA.flatrate` | array<object> | - |
| `results.CA.flatrate[].logo_path` | string | /ewOptMVIYcOadMGGJz8DJueH2bH.jpg |
| `results.CA.flatrate[].provider_id` | integer | 230 |
| `results.CA.flatrate[].provider_name` | string | Crave |
| `results.CA.flatrate[].display_priority` | integer | 4 |
| `results.CA.buy` | array<object> | - |
| `results.CA.buy[].logo_path` | string | /oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg |
| `results.CA.buy[].provider_id` | integer | 2 |
| `results.CA.buy[].provider_name` | string | Apple TV |
| `results.CA.buy[].display_priority` | integer | 5 |
| `results.CH` | object | - |
| `results.CH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CH.buy` | array<object> | - |
| `results.CH.buy[].logo_path` | string | /8z7rC8uIDaTM91X0ZfkRf04ydj2.jpg |
| `results.CH.buy[].provider_id` | integer | 3 |
| `results.CH.buy[].provider_name` | string | Google Play Movies |
| `results.CH.buy[].display_priority` | integer | 5 |
| `results.CH.flatrate` | array<object> | - |
| `results.CH.flatrate[].logo_path` | string | /ytApMa9fThUQUFTn696AeNBrB8f.jpg |
| `results.CH.flatrate[].provider_id` | integer | 210 |
| `results.CH.flatrate[].provider_name` | string | Sky |
| `results.CH.flatrate[].display_priority` | integer | 6 |
| `results.CI` | object | - |
| `results.CI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CI.flatrate` | array<object> | - |
| `results.CI.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.CI.flatrate[].provider_id` | integer | 55 |
| `results.CI.flatrate[].provider_name` | string | ShowMax |
| `results.CI.flatrate[].display_priority` | integer | 19 |
| `results.CL` | object | - |
| `results.CL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CL.flatrate` | array<object> | - |
| `results.CL.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.CL.flatrate[].provider_id` | integer | 1899 |
| `results.CL.flatrate[].provider_name` | string | HBO Max |
| `results.CL.flatrate[].display_priority` | integer | 7 |
| `results.CM` | object | - |
| `results.CM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CM.flatrate` | array<object> | - |
| `results.CM.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.CM.flatrate[].provider_id` | integer | 55 |
| `results.CM.flatrate[].provider_name` | string | ShowMax |
| `results.CM.flatrate[].display_priority` | integer | 0 |
| `results.CO` | object | - |
| `results.CO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CO.flatrate` | array<object> | - |
| `results.CO.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.CO.flatrate[].provider_id` | integer | 1899 |
| `results.CO.flatrate[].provider_name` | string | HBO Max |
| `results.CO.flatrate[].display_priority` | integer | 6 |
| `results.CR` | object | - |
| `results.CR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CR.flatrate` | array<object> | - |
| `results.CR.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.CR.flatrate[].provider_id` | integer | 1899 |
| `results.CR.flatrate[].provider_name` | string | HBO Max |
| `results.CR.flatrate[].display_priority` | integer | 4 |
| `results.CZ` | object | - |
| `results.CZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CZ.flatrate` | array<object> | - |
| `results.CZ.flatrate[].logo_path` | string | /489t5n9o1KhH7voGNQkrXT7vBKV.jpg |
| `results.CZ.flatrate[].provider_id` | integer | 1939 |
| `results.CZ.flatrate[].provider_name` | string | Lepsi TV |
| `results.CZ.flatrate[].display_priority` | integer | 26 |
| `results.DE` | object | - |
| `results.DE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DE.flatrate` | array<object> | - |
| `results.DE.flatrate[].logo_path` | string | /kAZkQcIxMxTmlwdgSB05fqtymp0.jpg |
| `results.DE.flatrate[].provider_id` | integer | 29 |
| `results.DE.flatrate[].provider_name` | string | Sky Go |
| `results.DE.flatrate[].display_priority` | integer | 8 |
| `results.DE.buy` | array<object> | - |
| `results.DE.buy[].logo_path` | string | /oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg |
| `results.DE.buy[].provider_id` | integer | 2 |
| `results.DE.buy[].provider_name` | string | Apple TV |
| `results.DE.buy[].display_priority` | integer | 4 |
| `results.DK` | object | - |
| `results.DK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DK.flatrate` | array<object> | - |
| `results.DK.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.DK.flatrate[].provider_id` | integer | 1899 |
| `results.DK.flatrate[].provider_name` | string | HBO Max |
| `results.DK.flatrate[].display_priority` | integer | 39 |
| `results.DO` | object | - |
| `results.DO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DO.flatrate` | array<object> | - |
| `results.DO.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.DO.flatrate[].provider_id` | integer | 1899 |
| `results.DO.flatrate[].provider_name` | string | HBO Max |
| `results.DO.flatrate[].display_priority` | integer | 30 |
| `results.EC` | object | - |
| `results.EC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.EC.flatrate` | array<object> | - |
| `results.EC.flatrate[].logo_path` | string | /tRNA2CRgA4XHvd7Mx9dH3sFtDVb.jpg |
| `results.EC.flatrate[].provider_id` | integer | 339 |
| `results.EC.flatrate[].provider_name` | string | MovistarTV |
| `results.EC.flatrate[].display_priority` | integer | 4 |
| `results.EG` | object | - |
| `results.EG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.EG.flatrate` | array<object> | - |
| `results.EG.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.EG.flatrate[].provider_id` | integer | 629 |
| `results.EG.flatrate[].provider_name` | string | OSN+ |
| `results.EG.flatrate[].display_priority` | integer | 20 |
| `results.ES` | object | - |
| `results.ES.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ES.flatrate` | array<object> | - |
| `results.ES.flatrate[].logo_path` | string | /f6TRLB3H4jDpFEZ0z2KWSSvu1SB.jpg |
| `results.ES.flatrate[].provider_id` | integer | 149 |
| `results.ES.flatrate[].provider_name` | string | Movistar Plus+ Ficción Total |
| `results.ES.flatrate[].display_priority` | integer | 6 |
| `results.ES.buy` | array<object> | - |
| `results.ES.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.ES.buy[].provider_id` | integer | 10 |
| `results.ES.buy[].provider_name` | string | Amazon Video |
| `results.ES.buy[].display_priority` | integer | 32 |
| `results.FI` | object | - |
| `results.FI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.FI.flatrate` | array<object> | - |
| `results.FI.flatrate[].logo_path` | string | /eglAxQEXSO13p6gNf3HKymrIu7y.jpg |
| `results.FI.flatrate[].provider_id` | integer | 540 |
| `results.FI.flatrate[].provider_name` | string | Elisa Viihde |
| `results.FI.flatrate[].display_priority` | integer | 18 |
| `results.FI.buy` | array<object> | - |
| `results.FI.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.FI.buy[].provider_id` | integer | 10 |
| `results.FI.buy[].provider_name` | string | Amazon Video |
| `results.FI.buy[].display_priority` | integer | 54 |
| `results.FR` | object | - |
| `results.FR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.FR.buy` | array<object> | - |
| `results.FR.buy[].logo_path` | string | /oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg |
| `results.FR.buy[].provider_id` | integer | 2 |
| `results.FR.buy[].provider_name` | string | Apple TV |
| `results.FR.buy[].display_priority` | integer | 4 |
| `results.FR.flatrate` | array<object> | - |
| `results.FR.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.FR.flatrate[].provider_id` | integer | 1899 |
| `results.FR.flatrate[].provider_name` | string | HBO Max |
| `results.FR.flatrate[].display_priority` | integer | 78 |
| `results.GB` | object | - |
| `results.GB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GB.buy` | array<object> | - |
| `results.GB.buy[].logo_path` | string | /oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg |
| `results.GB.buy[].provider_id` | integer | 2 |
| `results.GB.buy[].provider_name` | string | Apple TV |
| `results.GB.buy[].display_priority` | integer | 4 |
| `results.GB.flatrate` | array<object> | - |
| `results.GB.flatrate[].logo_path` | string | /g0E9h3JAeIwmdvxlT73jiEuxdNj.jpg |
| `results.GB.flatrate[].provider_id` | integer | 39 |
| `results.GB.flatrate[].provider_name` | string | Now TV |
| `results.GB.flatrate[].display_priority` | integer | 43 |
| `results.GG` | object | - |
| `results.GG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GG.buy` | array<object> | - |
| `results.GG.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.GG.buy[].provider_id` | integer | 10 |
| `results.GG.buy[].provider_name` | string | Amazon Video |
| `results.GG.buy[].display_priority` | integer | 84 |
| `results.GQ` | object | - |
| `results.GQ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GQ.flatrate` | array<object> | - |
| `results.GQ.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.GQ.flatrate[].provider_id` | integer | 55 |
| `results.GQ.flatrate[].provider_name` | string | ShowMax |
| `results.GQ.flatrate[].display_priority` | integer | 7 |
| `results.GT` | object | - |
| `results.GT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GT.flatrate` | array<object> | - |
| `results.GT.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.GT.flatrate[].provider_id` | integer | 1899 |
| `results.GT.flatrate[].provider_name` | string | HBO Max |
| `results.GT.flatrate[].display_priority` | integer | 4 |
| `results.GY` | object | - |
| `results.GY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GY.flatrate` | array<object> | - |
| `results.GY.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.GY.flatrate[].provider_id` | integer | 1899 |
| `results.GY.flatrate[].provider_name` | string | HBO Max |
| `results.GY.flatrate[].display_priority` | integer | 3 |
| `results.HK` | object | - |
| `results.HK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HK.flatrate` | array<object> | - |
| `results.HK.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.HK.flatrate[].provider_id` | integer | 1899 |
| `results.HK.flatrate[].provider_name` | string | HBO Max |
| `results.HK.flatrate[].display_priority` | integer | 38 |
| `results.HN` | object | - |
| `results.HN.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HN.flatrate` | array<object> | - |
| `results.HN.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.HN.flatrate[].provider_id` | integer | 1899 |
| `results.HN.flatrate[].provider_name` | string | HBO Max |
| `results.HN.flatrate[].display_priority` | integer | 30 |
| `results.HR` | object | - |
| `results.HR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HR.flatrate` | array<object> | - |
| `results.HR.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.HR.flatrate[].provider_id` | integer | 1899 |
| `results.HR.flatrate[].provider_name` | string | HBO Max |
| `results.HR.flatrate[].display_priority` | integer | 33 |
| `results.HU` | object | - |
| `results.HU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HU.flatrate` | array<object> | - |
| `results.HU.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.HU.flatrate[].provider_id` | integer | 1899 |
| `results.HU.flatrate[].provider_name` | string | HBO Max |
| `results.HU.flatrate[].display_priority` | integer | 28 |
| `results.ID` | object | - |
| `results.ID.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ID.flatrate` | array<object> | - |
| `results.ID.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.ID.flatrate[].provider_id` | integer | 1899 |
| `results.ID.flatrate[].provider_name` | string | HBO Max |
| `results.ID.flatrate[].display_priority` | integer | 39 |
| `results.IE` | object | - |
| `results.IE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IE.flatrate` | array<object> | - |
| `results.IE.flatrate[].logo_path` | string | /g0E9h3JAeIwmdvxlT73jiEuxdNj.jpg |
| `results.IE.flatrate[].provider_id` | integer | 39 |
| `results.IE.flatrate[].provider_name` | string | Now TV |
| `results.IE.flatrate[].display_priority` | integer | 10 |
| `results.IE.buy` | array<object> | - |
| `results.IE.buy[].logo_path` | string | /6AKbY2ayaEuH4zKg2prqoVQ9iaY.jpg |
| `results.IE.buy[].provider_id` | integer | 130 |
| `results.IE.buy[].provider_name` | string | Sky Store |
| `results.IE.buy[].display_priority` | integer | 9 |
| `results.IN` | object | - |
| `results.IN.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IN.flatrate` | array<object> | - |
| `results.IN.flatrate[].logo_path` | string | /kVqjgpcwvDJOhCupjcLzwwtOp52.jpg |
| `results.IN.flatrate[].provider_id` | integer | 2336 |
| `results.IN.flatrate[].provider_name` | string | JioHotstar |
| `results.IN.flatrate[].display_priority` | integer | 5 |
| `results.IQ` | object | - |
| `results.IQ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IQ.flatrate` | array<object> | - |
| `results.IQ.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.IQ.flatrate[].provider_id` | integer | 629 |
| `results.IQ.flatrate[].provider_name` | string | OSN+ |
| `results.IQ.flatrate[].display_priority` | integer | 7 |
| `results.IT` | object | - |
| `results.IT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IT.buy` | array<object> | - |
| `results.IT.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.IT.buy[].provider_id` | integer | 10 |
| `results.IT.buy[].provider_name` | string | Amazon Video |
| `results.IT.buy[].display_priority` | integer | 33 |
| `results.IT.flatrate` | array<object> | - |
| `results.IT.flatrate[].logo_path` | string | /g0E9h3JAeIwmdvxlT73jiEuxdNj.jpg |
| `results.IT.flatrate[].provider_id` | integer | 39 |
| `results.IT.flatrate[].provider_name` | string | Now TV |
| `results.IT.flatrate[].display_priority` | integer | 10 |
| `results.JM` | object | - |
| `results.JM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.JM.flatrate` | array<object> | - |
| `results.JM.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.JM.flatrate[].provider_id` | integer | 1899 |
| `results.JM.flatrate[].provider_name` | string | HBO Max |
| `results.JM.flatrate[].display_priority` | integer | 22 |
| `results.JO` | object | - |
| `results.JO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.JO.flatrate` | array<object> | - |
| `results.JO.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.JO.flatrate[].provider_id` | integer | 629 |
| `results.JO.flatrate[].provider_name` | string | OSN+ |
| `results.JO.flatrate[].display_priority` | integer | 31 |
| `results.JP` | object | - |
| `results.JP.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.JP.flatrate` | array<object> | - |
| `results.JP.flatrate[].logo_path` | string | /a5T7vNaGvoeckYO6rQkHolvyYf4.jpg |
| `results.JP.flatrate[].provider_id` | integer | 84 |
| `results.JP.flatrate[].provider_name` | string | U-NEXT |
| `results.JP.flatrate[].display_priority` | integer | 2 |
| `results.JP.buy` | array<object> | - |
| `results.JP.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.JP.buy[].provider_id` | integer | 10 |
| `results.JP.buy[].provider_name` | string | Amazon Video |
| `results.JP.buy[].display_priority` | integer | 5 |
| `results.JP.rent` | array<object> | - |
| `results.JP.rent[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.JP.rent[].provider_id` | integer | 10 |
| `results.JP.rent[].provider_name` | string | Amazon Video |
| `results.JP.rent[].display_priority` | integer | 5 |
| `results.KE` | object | - |
| `results.KE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.KE.flatrate` | array<object> | - |
| `results.KE.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.KE.flatrate[].provider_id` | integer | 55 |
| `results.KE.flatrate[].provider_name` | string | ShowMax |
| `results.KE.flatrate[].display_priority` | integer | 7 |
| `results.LB` | object | - |
| `results.LB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.LB.flatrate` | array<object> | - |
| `results.LB.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.LB.flatrate[].provider_id` | integer | 629 |
| `results.LB.flatrate[].provider_name` | string | OSN+ |
| `results.LB.flatrate[].display_priority` | integer | 7 |
| `results.LC` | object | - |
| `results.LC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.LC.flatrate` | array<object> | - |
| `results.LC.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.LC.flatrate[].provider_id` | integer | 1899 |
| `results.LC.flatrate[].provider_name` | string | HBO Max |
| `results.LC.flatrate[].display_priority` | integer | 10 |
| `results.MC` | object | - |
| `results.MC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MC.flatrate` | array<object> | - |
| `results.MC.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.MC.flatrate[].provider_id` | integer | 1899 |
| `results.MC.flatrate[].provider_name` | string | HBO Max |
| `results.MC.flatrate[].display_priority` | integer | 25 |
| `results.MD` | object | - |
| `results.MD.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MD.flatrate` | array<object> | - |
| `results.MD.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.MD.flatrate[].provider_id` | integer | 1899 |
| `results.MD.flatrate[].provider_name` | string | HBO Max |
| `results.MD.flatrate[].display_priority` | integer | 25 |
| `results.ME` | object | - |
| `results.ME.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ME.flatrate` | array<object> | - |
| `results.ME.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.ME.flatrate[].provider_id` | integer | 1899 |
| `results.ME.flatrate[].provider_name` | string | HBO Max |
| `results.ME.flatrate[].display_priority` | integer | 6 |
| `results.MG` | object | - |
| `results.MG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MG.flatrate` | array<object> | - |
| `results.MG.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.MG.flatrate[].provider_id` | integer | 55 |
| `results.MG.flatrate[].provider_name` | string | ShowMax |
| `results.MG.flatrate[].display_priority` | integer | 0 |
| `results.MK` | object | - |
| `results.MK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MK.flatrate` | array<object> | - |
| `results.MK.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.MK.flatrate[].provider_id` | integer | 1899 |
| `results.MK.flatrate[].provider_name` | string | HBO Max |
| `results.MK.flatrate[].display_priority` | integer | 26 |
| `results.ML` | object | - |
| `results.ML.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ML.flatrate` | array<object> | - |
| `results.ML.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.ML.flatrate[].provider_id` | integer | 55 |
| `results.ML.flatrate[].provider_name` | string | ShowMax |
| `results.ML.flatrate[].display_priority` | integer | 0 |
| `results.MU` | object | - |
| `results.MU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MU.flatrate` | array<object> | - |
| `results.MU.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.MU.flatrate[].provider_id` | integer | 55 |
| `results.MU.flatrate[].provider_name` | string | ShowMax |
| `results.MU.flatrate[].display_priority` | integer | 5 |
| `results.MX` | object | - |
| `results.MX.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MX.flatrate` | array<object> | - |
| `results.MX.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.MX.flatrate[].provider_id` | integer | 1899 |
| `results.MX.flatrate[].provider_name` | string | HBO Max |
| `results.MX.flatrate[].display_priority` | integer | 5 |
| `results.MY` | object | - |
| `results.MY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MY.flatrate` | array<object> | - |
| `results.MY.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.MY.flatrate[].provider_id` | integer | 1899 |
| `results.MY.flatrate[].provider_name` | string | HBO Max |
| `results.MY.flatrate[].display_priority` | integer | 31 |
| `results.MZ` | object | - |
| `results.MZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MZ.flatrate` | array<object> | - |
| `results.MZ.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.MZ.flatrate[].provider_id` | integer | 55 |
| `results.MZ.flatrate[].provider_name` | string | ShowMax |
| `results.MZ.flatrate[].display_priority` | integer | 7 |
| `results.NE` | object | - |
| `results.NE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NE.flatrate` | array<object> | - |
| `results.NE.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.NE.flatrate[].provider_id` | integer | 55 |
| `results.NE.flatrate[].provider_name` | string | ShowMax |
| `results.NE.flatrate[].display_priority` | integer | 19 |
| `results.NG` | object | - |
| `results.NG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NG.flatrate` | array<object> | - |
| `results.NG.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.NG.flatrate[].provider_id` | integer | 55 |
| `results.NG.flatrate[].provider_name` | string | ShowMax |
| `results.NG.flatrate[].display_priority` | integer | 21 |
| `results.NI` | object | - |
| `results.NI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NI.flatrate` | array<object> | - |
| `results.NI.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.NI.flatrate[].provider_id` | integer | 1899 |
| `results.NI.flatrate[].provider_name` | string | HBO Max |
| `results.NI.flatrate[].display_priority` | integer | 13 |
| `results.NL` | object | - |
| `results.NL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NL.buy` | array<object> | - |
| `results.NL.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.NL.buy[].provider_id` | integer | 10 |
| `results.NL.buy[].provider_name` | string | Amazon Video |
| `results.NL.buy[].display_priority` | integer | 34 |
| `results.NL.flatrate` | array<object> | - |
| `results.NL.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.NL.flatrate[].provider_id` | integer | 1899 |
| `results.NL.flatrate[].provider_name` | string | HBO Max |
| `results.NL.flatrate[].display_priority` | integer | 48 |
| `results.NO` | object | - |
| `results.NO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NO.flatrate` | array<object> | - |
| `results.NO.flatrate[].logo_path` | string | /3ZigBD8WTEPcEHAvMWiJGUsv5u4.jpg |
| `results.NO.flatrate[].provider_id` | integer | 578 |
| `results.NO.flatrate[].provider_name` | string | Strim |
| `results.NO.flatrate[].display_priority` | integer | 27 |
| `results.NZ` | object | - |
| `results.NZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NZ.buy` | array<object> | - |
| `results.NZ.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.NZ.buy[].provider_id` | integer | 10 |
| `results.NZ.buy[].provider_name` | string | Amazon Video |
| `results.NZ.buy[].display_priority` | integer | 59 |
| `results.NZ.flatrate` | array<object> | - |
| `results.NZ.flatrate[].logo_path` | string | /iscLKFDwQlr0BAgVDBcuRapLiwC.jpg |
| `results.NZ.flatrate[].provider_id` | integer | 273 |
| `results.NZ.flatrate[].provider_name` | string | Neon TV |
| `results.NZ.flatrate[].display_priority` | integer | 3 |
| `results.OM` | object | - |
| `results.OM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.OM.flatrate` | array<object> | - |
| `results.OM.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.OM.flatrate[].provider_id` | integer | 629 |
| `results.OM.flatrate[].provider_name` | string | OSN+ |
| `results.OM.flatrate[].display_priority` | integer | 31 |
| `results.PA` | object | - |
| `results.PA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PA.flatrate` | array<object> | - |
| `results.PA.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.PA.flatrate[].provider_id` | integer | 1899 |
| `results.PA.flatrate[].provider_name` | string | HBO Max |
| `results.PA.flatrate[].display_priority` | integer | 30 |
| `results.PE` | object | - |
| `results.PE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PE.flatrate` | array<object> | - |
| `results.PE.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.PE.flatrate[].provider_id` | integer | 1899 |
| `results.PE.flatrate[].provider_name` | string | HBO Max |
| `results.PE.flatrate[].display_priority` | integer | 7 |
| `results.PH` | object | - |
| `results.PH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PH.flatrate` | array<object> | - |
| `results.PH.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.PH.flatrate[].provider_id` | integer | 1899 |
| `results.PH.flatrate[].provider_name` | string | HBO Max |
| `results.PH.flatrate[].display_priority` | integer | 32 |
| `results.PL` | object | - |
| `results.PL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PL.flatrate` | array<object> | - |
| `results.PL.flatrate[].logo_path` | string | /jhMNVBV2UocEGepRkr9oFPD7Gpb.jpg |
| `results.PL.flatrate[].provider_id` | integer | 505 |
| `results.PL.flatrate[].provider_name` | string | Player |
| `results.PL.flatrate[].display_priority` | integer | 10 |
| `results.PL.buy` | array<object> | - |
| `results.PL.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.PL.buy[].provider_id` | integer | 10 |
| `results.PL.buy[].provider_name` | string | Amazon Video |
| `results.PL.buy[].display_priority` | integer | 27 |
| `results.PT` | object | - |
| `results.PT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PT.buy` | array<object> | - |
| `results.PT.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.PT.buy[].provider_id` | integer | 10 |
| `results.PT.buy[].provider_name` | string | Amazon Video |
| `results.PT.buy[].display_priority` | integer | 43 |
| `results.PT.flatrate` | array<object> | - |
| `results.PT.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.PT.flatrate[].provider_id` | integer | 1899 |
| `results.PT.flatrate[].provider_name` | string | HBO Max |
| `results.PT.flatrate[].display_priority` | integer | 32 |
| `results.PY` | object | - |
| `results.PY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PY.flatrate` | array<object> | - |
| `results.PY.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.PY.flatrate[].provider_id` | integer | 1899 |
| `results.PY.flatrate[].provider_name` | string | HBO Max |
| `results.PY.flatrate[].display_priority` | integer | 29 |
| `results.QA` | object | - |
| `results.QA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.QA.flatrate` | array<object> | - |
| `results.QA.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.QA.flatrate[].provider_id` | integer | 629 |
| `results.QA.flatrate[].provider_name` | string | OSN+ |
| `results.QA.flatrate[].display_priority` | integer | 31 |
| `results.RO` | object | - |
| `results.RO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RO.flatrate` | array<object> | - |
| `results.RO.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.RO.flatrate[].provider_id` | integer | 1899 |
| `results.RO.flatrate[].provider_name` | string | HBO Max |
| `results.RO.flatrate[].display_priority` | integer | 23 |
| `results.RS` | object | - |
| `results.RS.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RS.flatrate` | array<object> | - |
| `results.RS.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.RS.flatrate[].provider_id` | integer | 1899 |
| `results.RS.flatrate[].provider_name` | string | HBO Max |
| `results.RS.flatrate[].display_priority` | integer | 28 |
| `results.RU` | object | - |
| `results.RU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RU.flatrate` | array<object> | - |
| `results.RU.flatrate[].logo_path` | string | /5z8dpQN27kybhn21EVLZcJPpMEo.jpg |
| `results.RU.flatrate[].provider_id` | integer | 115 |
| `results.RU.flatrate[].provider_name` | string | Okko |
| `results.RU.flatrate[].display_priority` | integer | 0 |
| `results.SA` | object | - |
| `results.SA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SA.flatrate` | array<object> | - |
| `results.SA.flatrate[].logo_path` | string | /kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg |
| `results.SA.flatrate[].provider_id` | integer | 629 |
| `results.SA.flatrate[].provider_name` | string | OSN+ |
| `results.SA.flatrate[].display_priority` | integer | 20 |
| `results.SC` | object | - |
| `results.SC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SC.flatrate` | array<object> | - |
| `results.SC.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.SC.flatrate[].provider_id` | integer | 55 |
| `results.SC.flatrate[].provider_name` | string | ShowMax |
| `results.SC.flatrate[].display_priority` | integer | 6 |
| `results.SE` | object | - |
| `results.SE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SE.buy` | array<object> | - |
| `results.SE.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.SE.buy[].provider_id` | integer | 10 |
| `results.SE.buy[].provider_name` | string | Amazon Video |
| `results.SE.buy[].display_priority` | integer | 39 |
| `results.SE.flatrate` | array<object> | - |
| `results.SE.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.SE.flatrate[].provider_id` | integer | 1899 |
| `results.SE.flatrate[].provider_name` | string | HBO Max |
| `results.SE.flatrate[].display_priority` | integer | 44 |
| `results.SG` | object | - |
| `results.SG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SG.flatrate` | array<object> | - |
| `results.SG.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.SG.flatrate[].provider_id` | integer | 1899 |
| `results.SG.flatrate[].provider_name` | string | HBO Max |
| `results.SG.flatrate[].display_priority` | integer | 31 |
| `results.SI` | object | - |
| `results.SI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SI.flatrate` | array<object> | - |
| `results.SI.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.SI.flatrate[].provider_id` | integer | 1899 |
| `results.SI.flatrate[].provider_name` | string | HBO Max |
| `results.SI.flatrate[].display_priority` | integer | 28 |
| `results.SK` | object | - |
| `results.SK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SK.flatrate` | array<object> | - |
| `results.SK.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.SK.flatrate[].provider_id` | integer | 1899 |
| `results.SK.flatrate[].provider_name` | string | HBO Max |
| `results.SK.flatrate[].display_priority` | integer | 37 |
| `results.SN` | object | - |
| `results.SN.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SN.flatrate` | array<object> | - |
| `results.SN.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.SN.flatrate[].provider_id` | integer | 55 |
| `results.SN.flatrate[].provider_name` | string | ShowMax |
| `results.SN.flatrate[].display_priority` | integer | 7 |
| `results.SV` | object | - |
| `results.SV.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SV.flatrate` | array<object> | - |
| `results.SV.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.SV.flatrate[].provider_id` | integer | 1899 |
| `results.SV.flatrate[].provider_name` | string | HBO Max |
| `results.SV.flatrate[].display_priority` | integer | 30 |
| `results.TC` | object | - |
| `results.TC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TC.flatrate` | array<object> | - |
| `results.TC.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.TC.flatrate[].provider_id` | integer | 1899 |
| `results.TC.flatrate[].provider_name` | string | HBO Max |
| `results.TC.flatrate[].display_priority` | integer | 10 |
| `results.TD` | object | - |
| `results.TD.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TD.flatrate` | array<object> | - |
| `results.TD.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.TD.flatrate[].provider_id` | integer | 55 |
| `results.TD.flatrate[].provider_name` | string | ShowMax |
| `results.TD.flatrate[].display_priority` | integer | 0 |
| `results.TH` | object | - |
| `results.TH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TH.flatrate` | array<object> | - |
| `results.TH.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.TH.flatrate[].provider_id` | integer | 1899 |
| `results.TH.flatrate[].provider_name` | string | HBO Max |
| `results.TH.flatrate[].display_priority` | integer | 30 |
| `results.TR` | object | - |
| `results.TR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TR.flatrate` | array<object> | - |
| `results.TR.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.TR.flatrate[].provider_id` | integer | 1899 |
| `results.TR.flatrate[].provider_name` | string | HBO Max |
| `results.TR.flatrate[].display_priority` | integer | 29 |
| `results.TT` | object | - |
| `results.TT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TT.flatrate` | array<object> | - |
| `results.TT.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.TT.flatrate[].provider_id` | integer | 1899 |
| `results.TT.flatrate[].provider_name` | string | HBO Max |
| `results.TT.flatrate[].display_priority` | integer | 11 |
| `results.TW` | object | - |
| `results.TW.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TW.flatrate` | array<object> | - |
| `results.TW.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.TW.flatrate[].provider_id` | integer | 1899 |
| `results.TW.flatrate[].provider_name` | string | HBO Max |
| `results.TW.flatrate[].display_priority` | integer | 38 |
| `results.US` | object | - |
| `results.US.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.US.flatrate` | array<object> | - |
| `results.US.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.US.flatrate[].provider_id` | integer | 1899 |
| `results.US.flatrate[].provider_name` | string | HBO Max |
| `results.US.flatrate[].display_priority` | integer | 11 |
| `results.US.buy` | array<object> | - |
| `results.US.buy[].logo_path` | string | /seGSXajazLMCKGB5hnRCidtjay1.jpg |
| `results.US.buy[].provider_id` | integer | 10 |
| `results.US.buy[].provider_name` | string | Amazon Video |
| `results.US.buy[].display_priority` | integer | 6 |
| `results.UY` | object | - |
| `results.UY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.UY.flatrate` | array<object> | - |
| `results.UY.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.UY.flatrate[].provider_id` | integer | 1899 |
| `results.UY.flatrate[].provider_name` | string | HBO Max |
| `results.UY.flatrate[].display_priority` | integer | 3 |
| `results.VE` | object | - |
| `results.VE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.VE.flatrate` | array<object> | - |
| `results.VE.flatrate[].logo_path` | string | /jbe4gVSfRlbPTdESXhEKpornsfu.jpg |
| `results.VE.flatrate[].provider_id` | integer | 1899 |
| `results.VE.flatrate[].provider_name` | string | HBO Max |
| `results.VE.flatrate[].display_priority` | integer | 27 |
| `results.ZA` | object | - |
| `results.ZA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ZA.flatrate` | array<object> | - |
| `results.ZA.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.ZA.flatrate[].provider_id` | integer | 55 |
| `results.ZA.flatrate[].provider_name` | string | ShowMax |
| `results.ZA.flatrate[].display_priority` | integer | 4 |
| `results.ZM` | object | - |
| `results.ZM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ZM.flatrate` | array<object> | - |
| `results.ZM.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.ZM.flatrate[].provider_id` | integer | 55 |
| `results.ZM.flatrate[].provider_name` | string | ShowMax |
| `results.ZM.flatrate[].display_priority` | integer | 7 |
| `results.ZW` | object | - |
| `results.ZW.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ZW.flatrate` | array<object> | - |
| `results.ZW.flatrate[].logo_path` | string | /ilR8XFZOr3e3wETQ4ZXLjXQXrts.jpg |
| `results.ZW.flatrate[].provider_id` | integer | 55 |
| `results.ZW.flatrate[].provider_name` | string | ShowMax |
| `results.ZW.flatrate[].display_priority` | integer | 0 |

<details><summary>Example response</summary>

```json
{
  "id": 3624,
  "results": {
    "AD": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AD",
      "flatrate": [
        {
          "logo_path": "/jbe4gVSfRlbPTdESXhEKpornsfu.jpg",
          "provider_id": 1899,
          "provider_name": "HBO Max",
          "display_priority": 24
        }
      ]
    },
    "AE": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AE",
      "flatrate": [
        {
          "logo_path": "/kC6JTo59Gj6I4vJPyBAYGh0sKAE.jpg",
          "provider_id": 629,
          "provider_name": "OSN+",
          "display_priority": 7
        }
      ]
    },
    "AG": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AG",
      "flatrate": [
        {
          "logo_path": "/jbe4gVSfRlbPTdESXhEKpornsfu.jpg",
          "provider_id": 1899,
          "provider_name": "HBO Max",
          "display_priority": 11
        }
      ]
    },
    "AR": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AR",
      "flatrate": [
        {
          "logo_path": "/nr5UBW4IGKgBwmhpTMOfcvnX2vX.jpg",
          "provider_id": 467,
          "provider_name": "DIRECTV GO",
          "display_priority": 12
        },
        "… 1 more items"
      ]
    },
    "AT": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AT",
      "buy": [
        {
          "logo_path": "/seGSXajazLMCKGB5hnRCidtjay1.jpg",
          "provider_id": 10,
          "provider_name": "Amazon Video",
          "display_priority": 3
        },
        "… 2 more items"
      ],
      "flatrate": [
        {
          "logo_path": "/kAZkQcIxMxTmlwdgSB05fqtymp0.jpg",
          "provider_id": 29,
          "provider_name": "Sky Go",
          "display_priority": 8
        }
      ]
    },
    "AU": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AU",
      "flatrate": [
        {
          "logo_path": "/fejdSG7TwNQ5E0p6u7A6LVs280R.jpg",
          "provider_id": 134,
          "provider_name": "Foxtel Now",
          "display_priority": 8
        },
        "… 1 more items"
      ],
      "buy": [
        {
          "logo_path": "/oMYZg3cGAGp9ecKGlBgumcjDmnN.jpg",
          "provider_id": 2,
          "provider_name": "Apple TV",
          "display_priority": 11
        },
        "… 2 more items"
      ]
    },
    "BA": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=BA",
      "flatrate": [
        {
          "logo_path": "/jbe4gVSfRlbPTdESXhEKpornsfu.jpg",
    
… truncated
```

</details>


#### TV — Similar

```
GET https://api.themoviedb.org/3/tv/{series_id}/similar
```

Get the similar TV shows.

Operation id: `tv-series-similar`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /zcFSvWa34nDn2NcqOPuthyOIBWT.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 197063 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | ko |
| `results[].original_name` | string | 종이달 |
| `results[].overview` | string | A thriller drama about Yoo I-hwa, a stay-at… |
| `results[].popularity` | number | 12.299 |
| `results[].poster_path` | string | /xXWynVdMGyJXBUDvIN27AXM3iJJ.jpg |
| `results[].first_air_date` | string | 2023-04-10 |
| `results[].name` | string | Pale Moon |
| `results[].vote_average` | integer | 7 |
| `results[].vote_count` | integer | 2 |
| `total_pages` | integer | 82 |
| `total_results` | integer | 1639 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/zcFSvWa34nDn2NcqOPuthyOIBWT.jpg",
      "genre_ids": [
        18
      ],
      "id": 197063,
      "origin_country": [
        "KR"
      ],
      "original_language": "ko",
      "original_name": "종이달",
      "overview": "A thriller drama about Yoo I-hwa, a stay-at-home mom living her comfortable and contented life without desires, but to her husband's indifference. While working as a bank contract employee, she unexpectedly touches money from VIP clients...",
      "popularity": 12.299,
      "poster_path": "/xXWynVdMGyJXBUDvIN27AXM3iJJ.jpg",
      "first_air_date": "2023-04-10",
      "name": "Pale Moon",
      "vote_average": 7.0,
      "vote_count": 2
    },
    {
      "adult": false,
      "backdrop_path": null,
      "genre_ids": [
        10759,
        18
      ],
      "id": 197123,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "The White Darkness",
      "overview": "A spellbinding story of courage, love, family and the extremes of human capacity inspired by the true life account of Henry Worsley; a devoted husband and father, a former soldier, a man of deep honor and sacrifice, but also a man deeply...",
      "popularity": 0.61,
      "poster_path": null,
      "first_air_date": "",
      "name": "The White Darkness",
      "vote_average": 0.0,
      "vote_count": 0
    },
    "… 18 more items"
  ],
  "total_pages": 82,
  "total_results": 1639
}
```

</details>


#### TV — Translations

```
GET https://api.themoviedb.org/3/tv/{series_id}/translations
```

Get the translations that have been added to a TV show.

Operation id: `tv-series-translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | SA |
| `translations[].iso_639_1` | string | ar |
| `translations[].name` | string | العربية |
| `translations[].english_name` | string | Arabic |
| `translations[].data` | object | - |
| `translations[].data.name` | string | صراع العروش |
| `translations[].data.overview` | string | تتقاتل سبع عائلات نبيلة من أجل السيطرة على … |
| `translations[].data.homepage` | string | - |
| `translations[].data.tagline` | string | الشتاء قادم |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "translations": [
    {
      "iso_3166_1": "SA",
      "iso_639_1": "ar",
      "name": "العربية",
      "english_name": "Arabic",
      "data": {
        "name": "صراع العروش",
        "overview": "تتقاتل سبع عائلات نبيلة من أجل السيطرة على أرض - ويستيروس - الأسطورية. الاحتكاك بين العوائل يؤدي إلى حرب واسعة النطاق.  في حين يستيقظ الشر القديم في أقصى الشمال. وفي خضم الحرب، نظام عسكري مهمَل - حرس الليل - هم كل ما يقف بين عالم الإنسان...",
        "homepage": "",
        "tagline": "الشتاء قادم"
      }
    },
    {
      "iso_3166_1": "BY",
      "iso_639_1": "be",
      "name": "беларуская мова",
      "english_name": "Belarusian",
      "data": {
        "name": "Гульня тронаў",
        "overview": "Сюжэт серыяла заснаваны на фэнтэзійнай сазе Джорджа Марціна «Песьня лёду і полымя»; кожнаму сезону адпавядае адна кніга гепталогіі, з якіх пакуль напісана пяць кніг.",
        "homepage": "",
        "tagline": "Зіма блізка"
      }
    },
    "… 51 more items"
  ]
}
```

</details>


#### TV — Videos

```
GET https://api.themoviedb.org/3/tv/{series_id}/videos
```

Get the videos that belong to a TV show.

Operation id: `tv-series-videos`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |
| `include_video_language` | query | string | - | - | filter the list results by language, supports more than one value by using a comma |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `results` | array<object> | - |
| `results[].iso_639_1` | string | en |
| `results[].iso_3166_1` | string | US |
| `results[].name` | string | Inside Game of Thrones: A Story in Camera W… |
| `results[].key` | string | y2ZJ3lTaREY |
| `results[].site` | string | YouTube |
| `results[].size` | integer | 1080 |
| `results[].type` | string | Behind the Scenes |
| `results[].official` | boolean | true |
| `results[].published_at` | string | 2019-03-25T14:00:06.000Z |
| `results[].id` | string | 5c999b48c3a36863b73b9d42 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "results": [
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Inside Game of Thrones: A Story in Camera Work – BTS (HBO)",
      "key": "y2ZJ3lTaREY",
      "site": "YouTube",
      "size": 1080,
      "type": "Behind the Scenes",
      "official": true,
      "published_at": "2019-03-25T14:00:06.000Z",
      "id": "5c999b48c3a36863b73b9d42"
    },
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Inside Game of Thrones: A Story in Prosthetics – BTS (HBO)",
      "key": "f3MUpuRF6Ck",
      "site": "YouTube",
      "size": 1080,
      "type": "Behind the Scenes",
      "official": true,
      "published_at": "2019-03-11T14:00:03.000Z",
      "id": "5c92c2519251412b51773135"
    },
    "… 3 more items"
  ]
}
```

</details>


#### TV — Watch Providers

```
GET https://api.themoviedb.org/3/tv/{series_id}/watch/providers
```

Get the list of streaming providers we have for a TV show.

Operation id: `tv-series-watch-providers`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `series_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1399 |
| `results` | object | - |
| `results.AE` | object | - |
| `results.AE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AE.flatrate` | array<object> | - |
| `results.AE.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.AE.flatrate[].provider_id` | integer | 629 |
| `results.AE.flatrate[].provider_name` | string | OSN |
| `results.AE.flatrate[].display_priority` | integer | 11 |
| `results.AR` | object | - |
| `results.AR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AR.flatrate` | array<object> | - |
| `results.AR.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.AR.flatrate[].provider_id` | integer | 384 |
| `results.AR.flatrate[].provider_name` | string | HBO Max |
| `results.AR.flatrate[].display_priority` | integer | 5 |
| `results.AT` | object | - |
| `results.AT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AT.buy` | array<object> | - |
| `results.AT.buy[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.AT.buy[].provider_id` | integer | 10 |
| `results.AT.buy[].provider_name` | string | Amazon Video |
| `results.AT.buy[].display_priority` | integer | 3 |
| `results.AT.flatrate` | array<object> | - |
| `results.AT.flatrate[].logo_path` | string | /y0kyIFElN5sJAsmW8Txj69wzrD2.jpg |
| `results.AT.flatrate[].provider_id` | integer | 321 |
| `results.AT.flatrate[].provider_name` | string | Sky X |
| `results.AT.flatrate[].display_priority` | integer | 23 |
| `results.AU` | object | - |
| `results.AU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.AU.flatrate` | array<object> | - |
| `results.AU.flatrate[].logo_path` | string | /d3ixI1no0EpTj2i7u0Sd2DBXVlG.jpg |
| `results.AU.flatrate[].provider_id` | integer | 385 |
| `results.AU.flatrate[].provider_name` | string | BINGE |
| `results.AU.flatrate[].display_priority` | integer | 3 |
| `results.AU.buy` | array<object> | - |
| `results.AU.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.AU.buy[].provider_id` | integer | 2 |
| `results.AU.buy[].provider_name` | string | Apple TV |
| `results.AU.buy[].display_priority` | integer | 10 |
| `results.BA` | object | - |
| `results.BA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BA.flatrate` | array<object> | - |
| `results.BA.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BA.flatrate[].provider_id` | integer | 384 |
| `results.BA.flatrate[].provider_name` | string | HBO Max |
| `results.BA.flatrate[].display_priority` | integer | 28 |
| `results.BB` | object | - |
| `results.BB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BB.flatrate` | array<object> | - |
| `results.BB.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BB.flatrate[].provider_id` | integer | 384 |
| `results.BB.flatrate[].provider_name` | string | HBO Max |
| `results.BB.flatrate[].display_priority` | integer | 28 |
| `results.BE` | object | - |
| `results.BE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BE.flatrate` | array<object> | - |
| `results.BE.flatrate[].logo_path` | string | /pq8p1umEnJjdFAP1nFvNArTR61X.jpg |
| `results.BE.flatrate[].provider_id` | integer | 311 |
| `results.BE.flatrate[].provider_name` | string | Be TV Go |
| `results.BE.flatrate[].display_priority` | integer | 4 |
| `results.BG` | object | - |
| `results.BG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BG.flatrate` | array<object> | - |
| `results.BG.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BG.flatrate[].provider_id` | integer | 384 |
| `results.BG.flatrate[].provider_name` | string | HBO Max |
| `results.BG.flatrate[].display_priority` | integer | 15 |
| `results.BO` | object | - |
| `results.BO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BO.flatrate` | array<object> | - |
| `results.BO.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BO.flatrate[].provider_id` | integer | 384 |
| `results.BO.flatrate[].provider_name` | string | HBO Max |
| `results.BO.flatrate[].display_priority` | integer | 3 |
| `results.BR` | object | - |
| `results.BR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BR.flatrate` | array<object> | - |
| `results.BR.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BR.flatrate[].provider_id` | integer | 384 |
| `results.BR.flatrate[].provider_name` | string | HBO Max |
| `results.BR.flatrate[].display_priority` | integer | 7 |
| `results.BS` | object | - |
| `results.BS.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.BS.flatrate` | array<object> | - |
| `results.BS.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.BS.flatrate[].provider_id` | integer | 384 |
| `results.BS.flatrate[].provider_name` | string | HBO Max |
| `results.BS.flatrate[].display_priority` | integer | 28 |
| `results.CA` | object | - |
| `results.CA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CA.buy` | array<object> | - |
| `results.CA.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.CA.buy[].provider_id` | integer | 2 |
| `results.CA.buy[].provider_name` | string | Apple TV |
| `results.CA.buy[].display_priority` | integer | 6 |
| `results.CA.flatrate` | array<object> | - |
| `results.CA.flatrate[].logo_path` | string | /gJ3yVMWouaVj6iHd59TISJ1TlM5.jpg |
| `results.CA.flatrate[].provider_id` | integer | 230 |
| `results.CA.flatrate[].provider_name` | string | Crave |
| `results.CA.flatrate[].display_priority` | integer | 4 |
| `results.CH` | object | - |
| `results.CH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CH.flatrate` | array<object> | - |
| `results.CH.flatrate[].logo_path` | string | /sHP8XLo4Ac4WMbziRyAdRQdb76q.jpg |
| `results.CH.flatrate[].provider_id` | integer | 210 |
| `results.CH.flatrate[].provider_name` | string | Sky |
| `results.CH.flatrate[].display_priority` | integer | 7 |
| `results.CH.buy` | array<object> | - |
| `results.CH.buy[].logo_path` | string | /tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg |
| `results.CH.buy[].provider_id` | integer | 3 |
| `results.CH.buy[].provider_name` | string | Google Play Movies |
| `results.CH.buy[].display_priority` | integer | 5 |
| `results.CI` | object | - |
| `results.CI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CI.flatrate` | array<object> | - |
| `results.CI.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.CI.flatrate[].provider_id` | integer | 55 |
| `results.CI.flatrate[].provider_name` | string | ShowMax |
| `results.CI.flatrate[].display_priority` | integer | 25 |
| `results.CL` | object | - |
| `results.CL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CL.flatrate` | array<object> | - |
| `results.CL.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.CL.flatrate[].provider_id` | integer | 384 |
| `results.CL.flatrate[].provider_name` | string | HBO Max |
| `results.CL.flatrate[].display_priority` | integer | 7 |
| `results.CO` | object | - |
| `results.CO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CO.flatrate` | array<object> | - |
| `results.CO.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.CO.flatrate[].provider_id` | integer | 384 |
| `results.CO.flatrate[].provider_name` | string | HBO Max |
| `results.CO.flatrate[].display_priority` | integer | 6 |
| `results.CR` | object | - |
| `results.CR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CR.flatrate` | array<object> | - |
| `results.CR.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.CR.flatrate[].provider_id` | integer | 384 |
| `results.CR.flatrate[].provider_name` | string | HBO Max |
| `results.CR.flatrate[].display_priority` | integer | 4 |
| `results.CZ` | object | - |
| `results.CZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.CZ.flatrate` | array<object> | - |
| `results.CZ.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.CZ.flatrate[].provider_id` | integer | 384 |
| `results.CZ.flatrate[].provider_name` | string | HBO Max |
| `results.CZ.flatrate[].display_priority` | integer | 22 |
| `results.DE` | object | - |
| `results.DE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DE.buy` | array<object> | - |
| `results.DE.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.DE.buy[].provider_id` | integer | 2 |
| `results.DE.buy[].provider_name` | string | Apple TV |
| `results.DE.buy[].display_priority` | integer | 4 |
| `results.DE.flatrate` | array<object> | - |
| `results.DE.flatrate[].logo_path` | string | /MiVcYLkztM6qqLeVSYWHFCUcXx.jpg |
| `results.DE.flatrate[].provider_id` | integer | 30 |
| `results.DE.flatrate[].provider_name` | string | WOW |
| `results.DE.flatrate[].display_priority` | integer | 5 |
| `results.DK` | object | - |
| `results.DK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DK.flatrate` | array<object> | - |
| `results.DK.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.DK.flatrate[].provider_id` | integer | 384 |
| `results.DK.flatrate[].provider_name` | string | HBO Max |
| `results.DK.flatrate[].display_priority` | integer | 3 |
| `results.DO` | object | - |
| `results.DO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DO.flatrate` | array<object> | - |
| `results.DO.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.DO.flatrate[].provider_id` | integer | 384 |
| `results.DO.flatrate[].provider_name` | string | HBO Max |
| `results.DO.flatrate[].display_priority` | integer | 28 |
| `results.DZ` | object | - |
| `results.DZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.DZ.flatrate` | array<object> | - |
| `results.DZ.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.DZ.flatrate[].provider_id` | integer | 55 |
| `results.DZ.flatrate[].provider_name` | string | ShowMax |
| `results.DZ.flatrate[].display_priority` | integer | 27 |
| `results.EC` | object | - |
| `results.EC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.EC.flatrate` | array<object> | - |
| `results.EC.flatrate[].logo_path` | string | /cDzkhgvozSr4GW2aRdV22uDuFpw.jpg |
| `results.EC.flatrate[].provider_id` | integer | 339 |
| `results.EC.flatrate[].provider_name` | string | Movistar Play |
| `results.EC.flatrate[].display_priority` | integer | 4 |
| `results.EG` | object | - |
| `results.EG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.EG.flatrate` | array<object> | - |
| `results.EG.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.EG.flatrate[].provider_id` | integer | 629 |
| `results.EG.flatrate[].provider_name` | string | OSN |
| `results.EG.flatrate[].display_priority` | integer | 27 |
| `results.ES` | object | - |
| `results.ES.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ES.flatrate` | array<object> | - |
| `results.ES.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.ES.flatrate[].provider_id` | integer | 384 |
| `results.ES.flatrate[].provider_name` | string | HBO Max |
| `results.ES.flatrate[].display_priority` | integer | 9 |
| `results.FI` | object | - |
| `results.FI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.FI.buy` | array<object> | - |
| `results.FI.buy[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.FI.buy[].provider_id` | integer | 68 |
| `results.FI.buy[].provider_name` | string | Microsoft Store |
| `results.FI.buy[].display_priority` | integer | 12 |
| `results.FI.flatrate` | array<object> | - |
| `results.FI.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.FI.flatrate[].provider_id` | integer | 384 |
| `results.FI.flatrate[].provider_name` | string | HBO Max |
| `results.FI.flatrate[].display_priority` | integer | 4 |
| `results.FR` | object | - |
| `results.FR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.FR.buy` | array<object> | - |
| `results.FR.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.FR.buy[].provider_id` | integer | 2 |
| `results.FR.buy[].provider_name` | string | Apple TV |
| `results.FR.buy[].display_priority` | integer | 5 |
| `results.FR.flatrate` | array<object> | - |
| `results.FR.flatrate[].logo_path` | string | /loOaayvNiLnD0zKl70TO2L5vlAL.jpg |
| `results.FR.flatrate[].provider_id` | integer | 1870 |
| `results.FR.flatrate[].provider_name` | string | Pass Warner Amazon Channel |
| `results.FR.flatrate[].display_priority` | integer | 95 |
| `results.GB` | object | - |
| `results.GB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GB.flatrate` | array<object> | - |
| `results.GB.flatrate[].logo_path` | string | /fBHHXKC34ffxAsQvDe0ZJbvmTEQ.jpg |
| `results.GB.flatrate[].provider_id` | integer | 29 |
| `results.GB.flatrate[].provider_name` | string | Sky Go |
| `results.GB.flatrate[].display_priority` | integer | 9 |
| `results.GB.buy` | array<object> | - |
| `results.GB.buy[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.GB.buy[].provider_id` | integer | 10 |
| `results.GB.buy[].provider_name` | string | Amazon Video |
| `results.GB.buy[].display_priority` | integer | 4 |
| `results.GF` | object | - |
| `results.GF.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GF.flatrate` | array<object> | - |
| `results.GF.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.GF.flatrate[].provider_id` | integer | 384 |
| `results.GF.flatrate[].provider_name` | string | HBO Max |
| `results.GF.flatrate[].display_priority` | integer | 30 |
| `results.GH` | object | - |
| `results.GH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GH.flatrate` | array<object> | - |
| `results.GH.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.GH.flatrate[].provider_id` | integer | 55 |
| `results.GH.flatrate[].provider_name` | string | ShowMax |
| `results.GH.flatrate[].display_priority` | integer | 11 |
| `results.GQ` | object | - |
| `results.GQ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GQ.flatrate` | array<object> | - |
| `results.GQ.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.GQ.flatrate[].provider_id` | integer | 55 |
| `results.GQ.flatrate[].provider_name` | string | ShowMax |
| `results.GQ.flatrate[].display_priority` | integer | 11 |
| `results.GT` | object | - |
| `results.GT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.GT.flatrate` | array<object> | - |
| `results.GT.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.GT.flatrate[].provider_id` | integer | 384 |
| `results.GT.flatrate[].provider_name` | string | HBO Max |
| `results.GT.flatrate[].display_priority` | integer | 4 |
| `results.HK` | object | - |
| `results.HK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HK.flatrate` | array<object> | - |
| `results.HK.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.HK.flatrate[].provider_id` | integer | 425 |
| `results.HK.flatrate[].provider_name` | string | HBO Go |
| `results.HK.flatrate[].display_priority` | integer | 40 |
| `results.HN` | object | - |
| `results.HN.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HN.flatrate` | array<object> | - |
| `results.HN.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.HN.flatrate[].provider_id` | integer | 384 |
| `results.HN.flatrate[].provider_name` | string | HBO Max |
| `results.HN.flatrate[].display_priority` | integer | 4 |
| `results.HR` | object | - |
| `results.HR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HR.flatrate` | array<object> | - |
| `results.HR.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.HR.flatrate[].provider_id` | integer | 384 |
| `results.HR.flatrate[].provider_name` | string | HBO Max |
| `results.HR.flatrate[].display_priority` | integer | 34 |
| `results.HU` | object | - |
| `results.HU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.HU.flatrate` | array<object> | - |
| `results.HU.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.HU.flatrate[].provider_id` | integer | 384 |
| `results.HU.flatrate[].provider_name` | string | HBO Max |
| `results.HU.flatrate[].display_priority` | integer | 22 |
| `results.ID` | object | - |
| `results.ID.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ID.flatrate` | array<object> | - |
| `results.ID.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.ID.flatrate[].provider_id` | integer | 425 |
| `results.ID.flatrate[].provider_name` | string | HBO Go |
| `results.ID.flatrate[].display_priority` | integer | 14 |
| `results.IE` | object | - |
| `results.IE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IE.flatrate` | array<object> | - |
| `results.IE.flatrate[].logo_path` | string | /fBHHXKC34ffxAsQvDe0ZJbvmTEQ.jpg |
| `results.IE.flatrate[].provider_id` | integer | 29 |
| `results.IE.flatrate[].provider_name` | string | Sky Go |
| `results.IE.flatrate[].display_priority` | integer | 8 |
| `results.IE.buy` | array<object> | - |
| `results.IE.buy[].logo_path` | string | /2pCbao1J9s0DMak2KKnEzmzHni8.jpg |
| `results.IE.buy[].provider_id` | integer | 130 |
| `results.IE.buy[].provider_name` | string | Sky Store |
| `results.IE.buy[].display_priority` | integer | 9 |
| `results.IL` | object | - |
| `results.IL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IL.flatrate` | array<object> | - |
| `results.IL.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.IL.flatrate[].provider_id` | integer | 629 |
| `results.IL.flatrate[].provider_name` | string | OSN |
| `results.IL.flatrate[].display_priority` | integer | 13 |
| `results.IQ` | object | - |
| `results.IQ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IQ.flatrate` | array<object> | - |
| `results.IQ.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.IQ.flatrate[].provider_id` | integer | 629 |
| `results.IQ.flatrate[].provider_name` | string | OSN |
| `results.IQ.flatrate[].display_priority` | integer | 12 |
| `results.IT` | object | - |
| `results.IT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.IT.buy` | array<object> | - |
| `results.IT.buy[].logo_path` | string | /cksgBjTHV3rzAVaO2zUyS1mH4Ke.jpg |
| `results.IT.buy[].provider_id` | integer | 40 |
| `results.IT.buy[].provider_name` | string | Chili |
| `results.IT.buy[].display_priority` | integer | 11 |
| `results.IT.flatrate` | array<object> | - |
| `results.IT.flatrate[].logo_path` | string | /fBHHXKC34ffxAsQvDe0ZJbvmTEQ.jpg |
| `results.IT.flatrate[].provider_id` | integer | 29 |
| `results.IT.flatrate[].provider_name` | string | Sky Go |
| `results.IT.flatrate[].display_priority` | integer | 8 |
| `results.JM` | object | - |
| `results.JM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.JM.flatrate` | array<object> | - |
| `results.JM.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.JM.flatrate[].provider_id` | integer | 384 |
| `results.JM.flatrate[].provider_name` | string | HBO Max |
| `results.JM.flatrate[].display_priority` | integer | 27 |
| `results.JP` | object | - |
| `results.JP.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.JP.flatrate` | array<object> | - |
| `results.JP.flatrate[].logo_path` | string | /npg1OiBidQSndMsBZwgEPOYU6Jq.jpg |
| `results.JP.flatrate[].provider_id` | integer | 84 |
| `results.JP.flatrate[].provider_name` | string | U-NEXT |
| `results.JP.flatrate[].display_priority` | integer | 4 |
| `results.JP.buy` | array<object> | - |
| `results.JP.buy[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.JP.buy[].provider_id` | integer | 10 |
| `results.JP.buy[].provider_name` | string | Amazon Video |
| `results.JP.buy[].display_priority` | integer | 6 |
| `results.JP.rent` | array<object> | - |
| `results.JP.rent[].logo_path` | string | /5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg |
| `results.JP.rent[].provider_id` | integer | 10 |
| `results.JP.rent[].provider_name` | string | Amazon Video |
| `results.JP.rent[].display_priority` | integer | 6 |
| `results.KE` | object | - |
| `results.KE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.KE.flatrate` | array<object> | - |
| `results.KE.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.KE.flatrate[].provider_id` | integer | 55 |
| `results.KE.flatrate[].provider_name` | string | ShowMax |
| `results.KE.flatrate[].display_priority` | integer | 10 |
| `results.KR` | object | - |
| `results.KR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.KR.flatrate` | array<object> | - |
| `results.KR.flatrate[].logo_path` | string | /2ioan5BX5L9tz4fIGU93blTeFhv.jpg |
| `results.KR.flatrate[].provider_id` | integer | 356 |
| `results.KR.flatrate[].provider_name` | string | wavve |
| `results.KR.flatrate[].display_priority` | integer | 3 |
| `results.LB` | object | - |
| `results.LB.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.LB.flatrate` | array<object> | - |
| `results.LB.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.LB.flatrate[].provider_id` | integer | 629 |
| `results.LB.flatrate[].provider_name` | string | OSN |
| `results.LB.flatrate[].display_priority` | integer | 13 |
| `results.LT` | object | - |
| `results.LT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.LT.flatrate` | array<object> | - |
| `results.LT.flatrate[].logo_path` | string | /xTVM8uXT9QocigQ07LE7Irc65W2.jpg |
| `results.LT.flatrate[].provider_id` | integer | 553 |
| `results.LT.flatrate[].provider_name` | string | Telia Play |
| `results.LT.flatrate[].display_priority` | integer | 15 |
| `results.LY` | object | - |
| `results.LY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.LY.flatrate` | array<object> | - |
| `results.LY.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.LY.flatrate[].provider_id` | integer | 55 |
| `results.LY.flatrate[].provider_name` | string | ShowMax |
| `results.LY.flatrate[].display_priority` | integer | 27 |
| `results.MD` | object | - |
| `results.MD.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MD.flatrate` | array<object> | - |
| `results.MD.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.MD.flatrate[].provider_id` | integer | 384 |
| `results.MD.flatrate[].provider_name` | string | HBO Max |
| `results.MD.flatrate[].display_priority` | integer | 26 |
| `results.MK` | object | - |
| `results.MK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MK.flatrate` | array<object> | - |
| `results.MK.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.MK.flatrate[].provider_id` | integer | 384 |
| `results.MK.flatrate[].provider_name` | string | HBO Max |
| `results.MK.flatrate[].display_priority` | integer | 29 |
| `results.MU` | object | - |
| `results.MU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MU.flatrate` | array<object> | - |
| `results.MU.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.MU.flatrate[].provider_id` | integer | 55 |
| `results.MU.flatrate[].provider_name` | string | ShowMax |
| `results.MU.flatrate[].display_priority` | integer | 8 |
| `results.MX` | object | - |
| `results.MX.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MX.flatrate` | array<object> | - |
| `results.MX.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.MX.flatrate[].provider_id` | integer | 384 |
| `results.MX.flatrate[].provider_name` | string | HBO Max |
| `results.MX.flatrate[].display_priority` | integer | 11 |
| `results.MY` | object | - |
| `results.MY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MY.flatrate` | array<object> | - |
| `results.MY.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.MY.flatrate[].provider_id` | integer | 425 |
| `results.MY.flatrate[].provider_name` | string | HBO Go |
| `results.MY.flatrate[].display_priority` | integer | 14 |
| `results.MZ` | object | - |
| `results.MZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.MZ.flatrate` | array<object> | - |
| `results.MZ.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.MZ.flatrate[].provider_id` | integer | 55 |
| `results.MZ.flatrate[].provider_name` | string | ShowMax |
| `results.MZ.flatrate[].display_priority` | integer | 10 |
| `results.NE` | object | - |
| `results.NE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NE.flatrate` | array<object> | - |
| `results.NE.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.NE.flatrate[].provider_id` | integer | 55 |
| `results.NE.flatrate[].provider_name` | string | ShowMax |
| `results.NE.flatrate[].display_priority` | integer | 25 |
| `results.NG` | object | - |
| `results.NG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NG.flatrate` | array<object> | - |
| `results.NG.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.NG.flatrate[].provider_id` | integer | 55 |
| `results.NG.flatrate[].provider_name` | string | ShowMax |
| `results.NG.flatrate[].display_priority` | integer | 27 |
| `results.NL` | object | - |
| `results.NL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NL.flatrate` | array<object> | - |
| `results.NL.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.NL.flatrate[].provider_id` | integer | 384 |
| `results.NL.flatrate[].provider_name` | string | HBO Max |
| `results.NL.flatrate[].display_priority` | integer | 47 |
| `results.NL.buy` | array<object> | - |
| `results.NL.buy[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.NL.buy[].provider_id` | integer | 68 |
| `results.NL.buy[].provider_name` | string | Microsoft Store |
| `results.NL.buy[].display_priority` | integer | 12 |
| `results.NO` | object | - |
| `results.NO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NO.flatrate` | array<object> | - |
| `results.NO.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.NO.flatrate[].provider_id` | integer | 384 |
| `results.NO.flatrate[].provider_name` | string | HBO Max |
| `results.NO.flatrate[].display_priority` | integer | 5 |
| `results.NO.buy` | array<object> | - |
| `results.NO.buy[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.NO.buy[].provider_id` | integer | 68 |
| `results.NO.buy[].provider_name` | string | Microsoft Store |
| `results.NO.buy[].display_priority` | integer | 9 |
| `results.NZ` | object | - |
| `results.NZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.NZ.flatrate` | array<object> | - |
| `results.NZ.flatrate[].logo_path` | string | /od4YNSSLgOP3p8EtQTnEYfrPa77.jpg |
| `results.NZ.flatrate[].provider_id` | integer | 273 |
| `results.NZ.flatrate[].provider_name` | string | Neon TV |
| `results.NZ.flatrate[].display_priority` | integer | 2 |
| `results.PA` | object | - |
| `results.PA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PA.flatrate` | array<object> | - |
| `results.PA.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.PA.flatrate[].provider_id` | integer | 384 |
| `results.PA.flatrate[].provider_name` | string | HBO Max |
| `results.PA.flatrate[].display_priority` | integer | 27 |
| `results.PE` | object | - |
| `results.PE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PE.flatrate` | array<object> | - |
| `results.PE.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.PE.flatrate[].provider_id` | integer | 384 |
| `results.PE.flatrate[].provider_name` | string | HBO Max |
| `results.PE.flatrate[].display_priority` | integer | 8 |
| `results.PH` | object | - |
| `results.PH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PH.flatrate` | array<object> | - |
| `results.PH.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.PH.flatrate[].provider_id` | integer | 425 |
| `results.PH.flatrate[].provider_name` | string | HBO Go |
| `results.PH.flatrate[].display_priority` | integer | 12 |
| `results.PL` | object | - |
| `results.PL.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PL.flatrate` | array<object> | - |
| `results.PL.flatrate[].logo_path` | string | /l5Wxbsgral716BOtZsGyPVNn8GC.jpg |
| `results.PL.flatrate[].provider_id` | integer | 250 |
| `results.PL.flatrate[].provider_name` | string | Horizon |
| `results.PL.flatrate[].display_priority` | integer | 7 |
| `results.PL.rent` | array<object> | - |
| `results.PL.rent[].logo_path` | string | /bZNXgd8fwVTD68aAGlElkpAtu7b.jpg |
| `results.PL.rent[].provider_id` | integer | 549 |
| `results.PL.rent[].provider_name` | string | IPLA |
| `results.PL.rent[].display_priority` | integer | 17 |
| `results.PS` | object | - |
| `results.PS.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PS.flatrate` | array<object> | - |
| `results.PS.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.PS.flatrate[].provider_id` | integer | 629 |
| `results.PS.flatrate[].provider_name` | string | OSN |
| `results.PS.flatrate[].display_priority` | integer | 12 |
| `results.PT` | object | - |
| `results.PT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PT.flatrate` | array<object> | - |
| `results.PT.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.PT.flatrate[].provider_id` | integer | 384 |
| `results.PT.flatrate[].provider_name` | string | HBO Max |
| `results.PT.flatrate[].display_priority` | integer | 28 |
| `results.PY` | object | - |
| `results.PY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.PY.flatrate` | array<object> | - |
| `results.PY.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.PY.flatrate[].provider_id` | integer | 384 |
| `results.PY.flatrate[].provider_name` | string | HBO Max |
| `results.PY.flatrate[].display_priority` | integer | 3 |
| `results.RO` | object | - |
| `results.RO.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RO.flatrate` | array<object> | - |
| `results.RO.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.RO.flatrate[].provider_id` | integer | 384 |
| `results.RO.flatrate[].provider_name` | string | HBO Max |
| `results.RO.flatrate[].display_priority` | integer | 17 |
| `results.RS` | object | - |
| `results.RS.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RS.flatrate` | array<object> | - |
| `results.RS.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.RS.flatrate[].provider_id` | integer | 384 |
| `results.RS.flatrate[].provider_name` | string | HBO Max |
| `results.RS.flatrate[].display_priority` | integer | 32 |
| `results.RU` | object | - |
| `results.RU.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.RU.flatrate` | array<object> | - |
| `results.RU.flatrate[].logo_path` | string | /w1T8s7FqakcfucR8cgOvbe6UeXN.jpg |
| `results.RU.flatrate[].provider_id` | integer | 115 |
| `results.RU.flatrate[].provider_name` | string | Okko |
| `results.RU.flatrate[].display_priority` | integer | 0 |
| `results.RU.ads` | array<object> | - |
| `results.RU.ads[].logo_path` | string | /3jJtMOIwtvcrCyeRMUvv4wsfhJk.jpg |
| `results.RU.ads[].provider_id` | integer | 577 |
| `results.RU.ads[].provider_name` | string | TvIgle |
| `results.RU.ads[].display_priority` | integer | 22 |
| `results.SA` | object | - |
| `results.SA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SA.flatrate` | array<object> | - |
| `results.SA.flatrate[].logo_path` | string | /xEPXbwbfABzPrUTWbgtDFH1NOa.jpg |
| `results.SA.flatrate[].provider_id` | integer | 629 |
| `results.SA.flatrate[].provider_name` | string | OSN |
| `results.SA.flatrate[].display_priority` | integer | 25 |
| `results.SC` | object | - |
| `results.SC.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SC.flatrate` | array<object> | - |
| `results.SC.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.SC.flatrate[].provider_id` | integer | 55 |
| `results.SC.flatrate[].provider_name` | string | ShowMax |
| `results.SC.flatrate[].display_priority` | integer | 9 |
| `results.SE` | object | - |
| `results.SE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SE.flatrate` | array<object> | - |
| `results.SE.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.SE.flatrate[].provider_id` | integer | 384 |
| `results.SE.flatrate[].provider_name` | string | HBO Max |
| `results.SE.flatrate[].display_priority` | integer | 4 |
| `results.SE.buy` | array<object> | - |
| `results.SE.buy[].logo_path` | string | /shq88b09gTBYC4hA7K7MUL8Q4zP.jpg |
| `results.SE.buy[].provider_id` | integer | 68 |
| `results.SE.buy[].provider_name` | string | Microsoft Store |
| `results.SE.buy[].display_priority` | integer | 7 |
| `results.SG` | object | - |
| `results.SG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SG.flatrate` | array<object> | - |
| `results.SG.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.SG.flatrate[].provider_id` | integer | 425 |
| `results.SG.flatrate[].provider_name` | string | HBO Go |
| `results.SG.flatrate[].display_priority` | integer | 13 |
| `results.SI` | object | - |
| `results.SI.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SI.flatrate` | array<object> | - |
| `results.SI.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.SI.flatrate[].provider_id` | integer | 384 |
| `results.SI.flatrate[].provider_name` | string | HBO Max |
| `results.SI.flatrate[].display_priority` | integer | 29 |
| `results.SK` | object | - |
| `results.SK.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SK.flatrate` | array<object> | - |
| `results.SK.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.SK.flatrate[].provider_id` | integer | 384 |
| `results.SK.flatrate[].provider_name` | string | HBO Max |
| `results.SK.flatrate[].display_priority` | integer | 37 |
| `results.SN` | object | - |
| `results.SN.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SN.flatrate` | array<object> | - |
| `results.SN.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.SN.flatrate[].provider_id` | integer | 55 |
| `results.SN.flatrate[].provider_name` | string | ShowMax |
| `results.SN.flatrate[].display_priority` | integer | 11 |
| `results.SV` | object | - |
| `results.SV.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.SV.flatrate` | array<object> | - |
| `results.SV.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.SV.flatrate[].provider_id` | integer | 384 |
| `results.SV.flatrate[].provider_name` | string | HBO Max |
| `results.SV.flatrate[].display_priority` | integer | 25 |
| `results.TH` | object | - |
| `results.TH.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TH.flatrate` | array<object> | - |
| `results.TH.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.TH.flatrate[].provider_id` | integer | 425 |
| `results.TH.flatrate[].provider_name` | string | HBO Go |
| `results.TH.flatrate[].display_priority` | integer | 12 |
| `results.TR` | object | - |
| `results.TR.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TR.flatrate` | array<object> | - |
| `results.TR.flatrate[].logo_path` | string | /z3XAGCCbDD3KTZFvc96Ytr3XR56.jpg |
| `results.TR.flatrate[].provider_id` | integer | 341 |
| `results.TR.flatrate[].provider_name` | string | blutv |
| `results.TR.flatrate[].display_priority` | integer | 2 |
| `results.TT` | object | - |
| `results.TT.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TT.flatrate` | array<object> | - |
| `results.TT.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.TT.flatrate[].provider_id` | integer | 384 |
| `results.TT.flatrate[].provider_name` | string | HBO Max |
| `results.TT.flatrate[].display_priority` | integer | 11 |
| `results.TW` | object | - |
| `results.TW.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TW.flatrate` | array<object> | - |
| `results.TW.flatrate[].logo_path` | string | /bxdNcDbk1ohVeOMmM3eusAAiTLw.jpg |
| `results.TW.flatrate[].provider_id` | integer | 425 |
| `results.TW.flatrate[].provider_name` | string | HBO Go |
| `results.TW.flatrate[].display_priority` | integer | 40 |
| `results.TZ` | object | - |
| `results.TZ.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.TZ.flatrate` | array<object> | - |
| `results.TZ.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.TZ.flatrate[].provider_id` | integer | 55 |
| `results.TZ.flatrate[].provider_name` | string | ShowMax |
| `results.TZ.flatrate[].display_priority` | integer | 9 |
| `results.UG` | object | - |
| `results.UG.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.UG.flatrate` | array<object> | - |
| `results.UG.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.UG.flatrate[].provider_id` | integer | 55 |
| `results.UG.flatrate[].provider_name` | string | ShowMax |
| `results.UG.flatrate[].display_priority` | integer | 10 |
| `results.US` | object | - |
| `results.US.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.US.free` | array<object> | - |
| `results.US.free[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.US.free[].provider_id` | integer | 384 |
| `results.US.free[].provider_name` | string | HBO Max |
| `results.US.free[].display_priority` | integer | 7 |
| `results.US.buy` | array<object> | - |
| `results.US.buy[].logo_path` | string | /peURlLlr8jggOwK53fJ5wdQl05y.jpg |
| `results.US.buy[].provider_id` | integer | 2 |
| `results.US.buy[].provider_name` | string | Apple TV |
| `results.US.buy[].display_priority` | integer | 4 |
| `results.US.flatrate` | array<object> | - |
| `results.US.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.US.flatrate[].provider_id` | integer | 384 |
| `results.US.flatrate[].provider_name` | string | HBO Max |
| `results.US.flatrate[].display_priority` | integer | 7 |
| `results.UY` | object | - |
| `results.UY.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.UY.flatrate` | array<object> | - |
| `results.UY.flatrate[].logo_path` | string | /kV8XFGI5OLJKl72dI8DtnKplfFr.jpg |
| `results.UY.flatrate[].provider_id` | integer | 467 |
| `results.UY.flatrate[].provider_name` | string | DIRECTV GO |
| `results.UY.flatrate[].display_priority` | integer | 9 |
| `results.VE` | object | - |
| `results.VE.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.VE.flatrate` | array<object> | - |
| `results.VE.flatrate[].logo_path` | string | /Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg |
| `results.VE.flatrate[].provider_id` | integer | 384 |
| `results.VE.flatrate[].provider_name` | string | HBO Max |
| `results.VE.flatrate[].display_priority` | integer | 8 |
| `results.ZA` | object | - |
| `results.ZA.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ZA.flatrate` | array<object> | - |
| `results.ZA.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.ZA.flatrate[].provider_id` | integer | 55 |
| `results.ZA.flatrate[].provider_name` | string | ShowMax |
| `results.ZA.flatrate[].display_priority` | integer | 4 |
| `results.ZM` | object | - |
| `results.ZM.link` | string | https://www.themoviedb.org/tv/1399-game-of-… |
| `results.ZM.flatrate` | array<object> | - |
| `results.ZM.flatrate[].logo_path` | string | /okiQZMXnqwv0aD3QDYmu5DBNLce.jpg |
| `results.ZM.flatrate[].provider_id` | integer | 55 |
| `results.ZM.flatrate[].provider_name` | string | ShowMax |
| `results.ZM.flatrate[].display_priority` | integer | 10 |

<details><summary>Example response</summary>

```json
{
  "id": 1399,
  "results": {
    "AE": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AE",
      "flatrate": [
        {
          "logo_path": "/xEPXbwbfABzPrUTWbgtDFH1NOa.jpg",
          "provider_id": 629,
          "provider_name": "OSN",
          "display_priority": 11
        }
      ]
    },
    "AR": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AR",
      "flatrate": [
        {
          "logo_path": "/Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg",
          "provider_id": 384,
          "provider_name": "HBO Max",
          "display_priority": 5
        },
        "… 2 more items"
      ]
    },
    "AT": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AT",
      "buy": [
        {
          "logo_path": "/5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg",
          "provider_id": 10,
          "provider_name": "Amazon Video",
          "display_priority": 3
        },
        "… 5 more items"
      ],
      "flatrate": [
        {
          "logo_path": "/y0kyIFElN5sJAsmW8Txj69wzrD2.jpg",
          "provider_id": 321,
          "provider_name": "Sky X",
          "display_priority": 23
        },
        "… 1 more items"
      ]
    },
    "AU": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=AU",
      "flatrate": [
        {
          "logo_path": "/d3ixI1no0EpTj2i7u0Sd2DBXVlG.jpg",
          "provider_id": 385,
          "provider_name": "BINGE",
          "display_priority": 3
        },
        "… 1 more items"
      ],
      "buy": [
        {
          "logo_path": "/peURlLlr8jggOwK53fJ5wdQl05y.jpg",
          "provider_id": 2,
          "provider_name": "Apple TV",
          "display_priority": 10
        },
        "… 3 more items"
      ]
    },
    "BA": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=BA",
      "flatrate": [
        {
          "logo_path": "/Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg",
          "provider_id": 384,
          "provider_name": "HBO Max",
          "display_priority": 28
        }
      ]
    },
    "BB": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=BB",
      "flatrate": [
        {
          "logo_path": "/Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg",
          "provider_id": 384,
          "provider_name": "HBO Max",
          "display_priority": 28
        }
      ]
    },
    "BE": {
      "link": "https://www.themoviedb.org/tv/1399-game-of-thrones/watch?locale=BE",
      "flatrate": [
        {
          "logo_path": "/pq8p1umEnJjdFAP1nFvNArT
… truncated
```

</details>



### Networks

#### Networks — Details

```
GET https://api.themoviedb.org/3/network/{network_id}
```

Operation id: `network-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `network_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `headquarters` | string | New York City, New York |
| `homepage` | string | https://www.hbo.com |
| `id` | integer | 49 |
| `logo_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `name` | string | HBO |
| `origin_country` | string | US |

<details><summary>Example response</summary>

```json
{
  "headquarters": "New York City, New York",
  "homepage": "https://www.hbo.com",
  "id": 49,
  "logo_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
  "name": "HBO",
  "origin_country": "US"
}
```

</details>


#### Networks — Alternative Names

```
GET https://api.themoviedb.org/3/network/{network_id}/alternative_names
```

Get the alternative names of a network.

Operation id: `details-copy`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `network_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 49 |
| `results` | array<object> | - |
| `results[].name` | string | Home Box Office |
| `results[].type` | string | - |

<details><summary>Example response</summary>

```json
{
  "id": 49,
  "results": [
    {
      "name": "Home Box Office",
      "type": ""
    },
    {
      "name": "HBO HD",
      "type": ""
    }
  ]
}
```

</details>


#### Networks — Images

```
GET https://api.themoviedb.org/3/network/{network_id}/images
```

Get the TV network logos by id.

Operation id: `alternative-names-copy`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `network_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 49 |
| `logos` | array<object> | - |
| `logos[].aspect_ratio` | number | 2.425287356321839 |
| `logos[].file_path` | string | /tuomPhY2UtuPTqqFnKMVHvSb724.png |
| `logos[].height` | integer | 174 |
| `logos[].id` | string | 5a7a67a40e0a26020a000091 |
| `logos[].file_type` | string | .svg |
| `logos[].vote_average` | number | 5.318 |
| `logos[].vote_count` | integer | 3 |
| `logos[].width` | integer | 422 |

<details><summary>Example response</summary>

```json
{
  "id": 49,
  "logos": [
    {
      "aspect_ratio": 2.425287356321839,
      "file_path": "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
      "height": 174,
      "id": "5a7a67a40e0a26020a000091",
      "file_type": ".svg",
      "vote_average": 5.318,
      "vote_count": 3,
      "width": 422
    },
    {
      "aspect_ratio": 2.424242424242424,
      "file_path": "/hizvY65SpyF3BPY2qsBZMgUOxjs.png",
      "height": 495,
      "id": "63e7979663aad200858726da",
      "file_type": ".png",
      "vote_average": 5.312,
      "vote_count": 1,
      "width": 1200
    }
  ]
}
```

</details>



### People

#### People — People List

```
GET https://api.themoviedb.org/3/person/changes
```

Operation id: `changes-people-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `end_date` | query | string (date) | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string (date) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `results` | array<object> | - |
| `results[].id` | integer | 4037513 |
| `results[].adult` | boolean | false |
| `page` | integer | 1 |
| `total_pages` | integer | 53 |
| `total_results` | integer | 5292 |

<details><summary>Example response</summary>

```json
{
  "results": [
    {
      "id": 4037513,
      "adult": false
    },
    {
      "id": 2280390,
      "adult": false
    },
    "… 98 more items"
  ],
  "page": 1,
  "total_pages": 53,
  "total_results": 5292
}
```

</details>


#### People — Latest

```
GET https://api.themoviedb.org/3/person/latest
```

Get the newest created person. This is a live response and will continuously change.

Operation id: `person-latest-id`

**Parameters:** none.

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `also_known_as` | array<object> | - |
| `biography` | string | - |
| `birthday` | object | - |
| `deathday` | object | - |
| `gender` | integer | 0 |
| `homepage` | object | - |
| `id` | integer | 4064343 |
| `imdb_id` | object | - |
| `known_for_department` | object | - |
| `name` | string | Ángel Cruz |
| `place_of_birth` | object | - |
| `popularity` | integer | 0 |
| `profile_path` | object | - |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "also_known_as": [],
  "biography": "",
  "birthday": null,
  "deathday": null,
  "gender": 0,
  "homepage": null,
  "id": 4064343,
  "imdb_id": null,
  "known_for_department": null,
  "name": "Ángel Cruz",
  "place_of_birth": null,
  "popularity": 0.0,
  "profile_path": null
}
```

</details>


#### People — Popular

```
GET https://api.themoviedb.org/3/person/popular
```

Get a list of people ordered by popularity.

Operation id: `person-popular-list`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].gender` | integer | 1 |
| `results[].id` | integer | 224513 |
| `results[].known_for` | array<object> | - |
| `results[].known_for[].adult` | boolean | false |
| `results[].known_for[].backdrop_path` | string | /ilRyazdMJwN05exqhwK4tMKBYZs.jpg |
| `results[].known_for[].genre_ids` | array<integer> | - |
| `results[].known_for[].id` | integer | 335984 |
| `results[].known_for[].media_type` | string | movie |
| `results[].known_for[].original_language` | string | en |
| `results[].known_for[].original_title` | string | Blade Runner 2049 |
| `results[].known_for[].overview` | string | Thirty years after the events of the first … |
| `results[].known_for[].poster_path` | string | /gajva2L0rPYkEWjzgFlBXCAVBE5.jpg |
| `results[].known_for[].release_date` | string | 2017-10-04 |
| `results[].known_for[].title` | string | Blade Runner 2049 |
| `results[].known_for[].video` | boolean | false |
| `results[].known_for[].vote_average` | number | 7.5 |
| `results[].known_for[].vote_count` | integer | 11771 |
| `results[].known_for_department` | string | Acting |
| `results[].name` | string | Ana de Armas |
| `results[].popularity` | number | 343.33 |
| `results[].profile_path` | string | /3vxvsmYLTf4jnr163SUlBIw51ee.jpg |
| `total_pages` | integer | 500 |
| `total_results` | integer | 10000 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "gender": 1,
      "id": 224513,
      "known_for": [
        {
          "adult": false,
          "backdrop_path": "/ilRyazdMJwN05exqhwK4tMKBYZs.jpg",
          "genre_ids": [
            878,
            18
          ],
          "id": 335984,
          "media_type": "movie",
          "original_language": "en",
          "original_title": "Blade Runner 2049",
          "overview": "Thirty years after the events of the first film, a new blade runner, LAPD Officer K, unearths a long-buried secret that has the potential to plunge what's left of society into chaos. K's discovery leads him on a quest to find Rick Deckar...",
          "poster_path": "/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
          "release_date": "2017-10-04",
          "title": "Blade Runner 2049",
          "video": false,
          "vote_average": 7.5,
          "vote_count": 11771
        },
        "… 2 more items"
      ],
      "known_for_department": "Acting",
      "name": "Ana de Armas",
      "popularity": 343.33,
      "profile_path": "/3vxvsmYLTf4jnr163SUlBIw51ee.jpg"
    },
    "… 19 more items"
  ],
  "total_pages": 500,
  "total_results": 10000
}
```

</details>


#### People — Details

```
GET https://api.themoviedb.org/3/person/{person_id}
```

Query the top level details of a person.

Operation id: `person-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |
| `append_to_response` | query | string | - | - | comma separated list of endpoints within this namespace, 20 items max |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `adult` | boolean | false |
| `also_known_as` | array<string> | - |
| `biography` | string | Thomas Jeffrey Hanks (born July 9, 1956) is… |
| `birthday` | string | 1956-07-09 |
| `deathday` | object | - |
| `gender` | integer | 2 |
| `homepage` | object | - |
| `id` | integer | 31 |
| `imdb_id` | string | nm0000158 |
| `known_for_department` | string | Acting |
| `name` | string | Tom Hanks |
| `place_of_birth` | string | Concord, California, USA |
| `popularity` | number | 82.989 |
| `profile_path` | string | /xndWFsBlClOJFRdhSt4NBwiPq2o.jpg |

<details><summary>Example response</summary>

```json
{
  "adult": false,
  "also_known_as": [
    "Thomas Jeffrey Hanks",
    "Том Хэнкс",
    "توم هانكس",
    "トム・ハンクス",
    "톰 행크스",
    "ทอม แฮงส์",
    "汤姆·汉克斯",
    "Том Генкс",
    "… 7 more items"
  ],
  "biography": "Thomas Jeffrey Hanks (born July 9, 1956) is an American actor and filmmaker. Known for both his comedic and dramatic roles, Hanks is one of the most popular and recognizable film stars worldwide, and is widely regarded as an American cul...",
  "birthday": "1956-07-09",
  "deathday": null,
  "gender": 2,
  "homepage": null,
  "id": 31,
  "imdb_id": "nm0000158",
  "known_for_department": "Acting",
  "name": "Tom Hanks",
  "place_of_birth": "Concord, California, USA",
  "popularity": 82.989,
  "profile_path": "/xndWFsBlClOJFRdhSt4NBwiPq2o.jpg"
}
```

</details>


#### People — Changes

```
GET https://api.themoviedb.org/3/person/{person_id}/changes
```

Get the recent changes for a person.

Operation id: `person-changes`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |
| `end_date` | query | string (date) | - | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `start_date` | query | string (date) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `changes` | array<object> | - |
| `changes[].key` | string | biography |
| `changes[].items` | array<object> | - |
| `changes[].items[].id` | string | 640469b113654500ba4e859a |
| `changes[].items[].action` | string | added |
| `changes[].items[].time` | string | 2023-03-05 10:06:41 UTC |
| `changes[].items[].iso_639_1` | string | ca |
| `changes[].items[].iso_3166_1` | string | ES |
| `changes[].items[].value` | string | Thomas "Tom" Jeffrey Hanks (Concord, Califò… |

<details><summary>Example response</summary>

```json
{
  "changes": [
    {
      "key": "biography",
      "items": [
        {
          "id": "640469b113654500ba4e859a",
          "action": "added",
          "time": "2023-03-05 10:06:41 UTC",
          "iso_639_1": "ca",
          "iso_3166_1": "ES",
          "value": "Thomas \"Tom\" Jeffrey Hanks (Concord, Califòrnia, 9 de juliol de 1956) és un actor de cinema i productor estatunidenc, guanyador dues vegades de l'Oscar al millor actor i considerat un dels més versàtils i talentosos del cinema actual.\n\nH..."
        }
      ]
    },
    {
      "key": "translations",
      "items": [
        {
          "id": "640469a7e61e6d00963c33bb",
          "action": "added",
          "time": "2023-03-05 10:06:31 UTC",
          "iso_639_1": "",
          "iso_3166_1": "",
          "value": "ca-ES"
        }
      ]
    }
  ]
}
```

</details>


#### People — Combined Credits

```
GET https://api.themoviedb.org/3/person/{person_id}/combined_credits
```

Get the combined movie and TV credits that belong to a person.

Operation id: `person-combined-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | string | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].backdrop_path` | string | /3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg |
| `cast[].genre_ids` | array<integer> | - |
| `cast[].id` | integer | 13 |
| `cast[].original_language` | string | en |
| `cast[].original_title` | string | Forrest Gump |
| `cast[].overview` | string | A man with a low IQ has accomplished great … |
| `cast[].popularity` | number | 62.225 |
| `cast[].poster_path` | string | /arw2vcBveWOVZr6pxd9XTd1TdQa.jpg |
| `cast[].release_date` | string | 1994-06-23 |
| `cast[].title` | string | Forrest Gump |
| `cast[].video` | boolean | false |
| `cast[].vote_average` | number | 8.481 |
| `cast[].vote_count` | integer | 24535 |
| `cast[].character` | string | Forrest Gump |
| `cast[].credit_id` | string | 52fe420ec3a36847f800074f |
| `cast[].order` | integer | 0 |
| `cast[].media_type` | string | movie |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].backdrop_path` | string | /tx3uj8GPWf5pzb0gWATJ4bokNHI.jpg |
| `crew[].genre_ids` | array<integer> | - |
| `crew[].id` | integer | 87061 |
| `crew[].original_language` | string | fr |
| `crew[].original_title` | string | Le Voyage extraordinaire |
| `crew[].overview` | string | An account of the extraordinary life of fil… |
| `crew[].popularity` | number | 6.007 |
| `crew[].poster_path` | string | /zHNNT9gfiGsuadR6x38KYOp6ekq.jpg |
| `crew[].release_date` | string | 2011-12-08 |
| `crew[].title` | string | The Extraordinary Voyage |
| `crew[].video` | boolean | false |
| `crew[].vote_average` | number | 7.6 |
| `crew[].vote_count` | integer | 47 |
| `crew[].credit_id` | string | 5d818a63d34eb3002c4f8fea |
| `crew[].department` | string | Crew |
| `crew[].job` | string | Thanks |
| `crew[].media_type` | string | movie |
| `id` | integer | 31 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "backdrop_path": "/3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg",
      "genre_ids": [
        35,
        18,
        10749
      ],
      "id": 13,
      "original_language": "en",
      "original_title": "Forrest Gump",
      "overview": "A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do. But despite all he has achieved, his one true love eludes him.",
      "popularity": 62.225,
      "poster_path": "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
      "release_date": "1994-06-23",
      "title": "Forrest Gump",
      "video": false,
      "vote_average": 8.481,
      "vote_count": 24535,
      "character": "Forrest Gump",
      "credit_id": "52fe420ec3a36847f800074f",
      "order": 0,
      "media_type": "movie"
    },
    "… 216 more items"
  ],
  "crew": [
    {
      "adult": false,
      "backdrop_path": "/tx3uj8GPWf5pzb0gWATJ4bokNHI.jpg",
      "genre_ids": [
        99
      ],
      "id": 87061,
      "original_language": "fr",
      "original_title": "Le Voyage extraordinaire",
      "overview": "An account of the extraordinary life of film pioneer Georges Méliès (1861-1938) and the amazing story of the copy in color of his masterpiece “A Trip to the Moon” (1902), unexpectedly found in Spain and restored thanks to the heroic effo...",
      "popularity": 6.007,
      "poster_path": "/zHNNT9gfiGsuadR6x38KYOp6ekq.jpg",
      "release_date": "2011-12-08",
      "title": "The Extraordinary Voyage",
      "video": false,
      "vote_average": 7.6,
      "vote_count": 47,
      "credit_id": "5d818a63d34eb3002c4f8fea",
      "department": "Crew",
      "job": "Thanks",
      "media_type": "movie"
    },
    "… 68 more items"
  ],
  "id": 31
}
```

</details>


#### People — External IDs

```
GET https://api.themoviedb.org/3/person/{person_id}/external_ids
```

Get the external ID's that belong to a person.

Operation id: `person-external-ids`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 31 |
| `freebase_mid` | string | /m/0bxtg |
| `freebase_id` | string | /en/tom_hanks |
| `imdb_id` | string | nm0000158 |
| `tvrage_id` | integer | 14293 |
| `wikidata_id` | string | Q2263 |
| `facebook_id` | string | TomHanks |
| `instagram_id` | string | tomhanks |
| `tiktok_id` | string | tomhanks |
| `twitter_id` | string | tomhanks |
| `youtube_id` | object | - |

<details><summary>Example response</summary>

```json
{
  "id": 31,
  "freebase_mid": "/m/0bxtg",
  "freebase_id": "/en/tom_hanks",
  "imdb_id": "nm0000158",
  "tvrage_id": 14293,
  "wikidata_id": "Q2263",
  "facebook_id": "TomHanks",
  "instagram_id": "tomhanks",
  "tiktok_id": "tomhanks",
  "twitter_id": "tomhanks",
  "youtube_id": null
}
```

</details>


#### People — Images

```
GET https://api.themoviedb.org/3/person/{person_id}/images
```

Get the profile images that belong to a person.

Operation id: `person-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 287 |
| `profiles` | array<object> | - |
| `profiles[].aspect_ratio` | number | 0.666 |
| `profiles[].height` | integer | 980 |
| `profiles[].iso_639_1` | object | - |
| `profiles[].file_path` | string | /cckcYc2v0yh1tc9QjRelptcOBko.jpg |
| `profiles[].vote_average` | number | 5.288 |
| `profiles[].vote_count` | integer | 89 |
| `profiles[].width` | integer | 653 |

<details><summary>Example response</summary>

```json
{
  "id": 287,
  "profiles": [
    {
      "aspect_ratio": 0.666,
      "height": 980,
      "iso_639_1": null,
      "file_path": "/cckcYc2v0yh1tc9QjRelptcOBko.jpg",
      "vote_average": 5.288,
      "vote_count": 89,
      "width": 653
    },
    {
      "aspect_ratio": 0.667,
      "height": 3000,
      "iso_639_1": null,
      "file_path": "/eAOtKAc4p2C3DV8TGJQJzw8DeRv.jpg",
      "vote_average": 5.242,
      "vote_count": 40,
      "width": 2000
    },
    "… 12 more items"
  ]
}
```

</details>


#### People — Movie Credits

```
GET https://api.themoviedb.org/3/person/{person_id}/movie_credits
```

Get the movie credits for a person.

Operation id: `person-movie-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].backdrop_path` | string | /3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg |
| `cast[].genre_ids` | array<integer> | - |
| `cast[].id` | integer | 13 |
| `cast[].original_language` | string | en |
| `cast[].original_title` | string | Forrest Gump |
| `cast[].overview` | string | A man with a low IQ has accomplished great … |
| `cast[].popularity` | number | 62.225 |
| `cast[].poster_path` | string | /arw2vcBveWOVZr6pxd9XTd1TdQa.jpg |
| `cast[].release_date` | string | 1994-06-23 |
| `cast[].title` | string | Forrest Gump |
| `cast[].video` | boolean | false |
| `cast[].vote_average` | number | 8.481 |
| `cast[].vote_count` | integer | 24535 |
| `cast[].character` | string | Forrest Gump |
| `cast[].credit_id` | string | 52fe420ec3a36847f800074f |
| `cast[].order` | integer | 0 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].backdrop_path` | string | /tx3uj8GPWf5pzb0gWATJ4bokNHI.jpg |
| `crew[].genre_ids` | array<integer> | - |
| `crew[].id` | integer | 87061 |
| `crew[].original_language` | string | fr |
| `crew[].original_title` | string | Le Voyage extraordinaire |
| `crew[].overview` | string | An account of the extraordinary life of fil… |
| `crew[].popularity` | number | 6.007 |
| `crew[].poster_path` | string | /zHNNT9gfiGsuadR6x38KYOp6ekq.jpg |
| `crew[].release_date` | string | 2011-12-08 |
| `crew[].title` | string | The Extraordinary Voyage |
| `crew[].video` | boolean | false |
| `crew[].vote_average` | number | 7.6 |
| `crew[].vote_count` | integer | 47 |
| `crew[].credit_id` | string | 5d818a63d34eb3002c4f8fea |
| `crew[].department` | string | Crew |
| `crew[].job` | string | Thanks |
| `id` | integer | 31 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "backdrop_path": "/3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg",
      "genre_ids": [
        35,
        18,
        10749
      ],
      "id": 13,
      "original_language": "en",
      "original_title": "Forrest Gump",
      "overview": "A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do. But despite all he has achieved, his one true love eludes him.",
      "popularity": 62.225,
      "poster_path": "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
      "release_date": "1994-06-23",
      "title": "Forrest Gump",
      "video": false,
      "vote_average": 8.481,
      "vote_count": 24535,
      "character": "Forrest Gump",
      "credit_id": "52fe420ec3a36847f800074f",
      "order": 0
    },
    "… 149 more items"
  ],
  "crew": [
    {
      "adult": false,
      "backdrop_path": "/tx3uj8GPWf5pzb0gWATJ4bokNHI.jpg",
      "genre_ids": [
        99
      ],
      "id": 87061,
      "original_language": "fr",
      "original_title": "Le Voyage extraordinaire",
      "overview": "An account of the extraordinary life of film pioneer Georges Méliès (1861-1938) and the amazing story of the copy in color of his masterpiece “A Trip to the Moon” (1902), unexpectedly found in Spain and restored thanks to the heroic effo...",
      "popularity": 6.007,
      "poster_path": "/zHNNT9gfiGsuadR6x38KYOp6ekq.jpg",
      "release_date": "2011-12-08",
      "title": "The Extraordinary Voyage",
      "video": false,
      "vote_average": 7.6,
      "vote_count": 47,
      "credit_id": "5d818a63d34eb3002c4f8fea",
      "department": "Crew",
      "job": "Thanks"
    },
    "… 40 more items"
  ],
  "id": 31
}
```

</details>


#### People — Tagged Images

```
GET https://api.themoviedb.org/3/person/{person_id}/tagged_images
```

Get the tagged images for a person.

Operation id: `person-tagged-images`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 31 |
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].aspect_ratio` | number | 0.6666666666666666 |
| `results[].file_path` | string | /1wY4psJ5NVEhCuOYROwLH2XExM2.jpg |
| `results[].height` | integer | 1500 |
| `results[].id` | string | 5b235d740e0a265b5d0031d9 |
| `results[].iso_639_1` | string | en |
| `results[].vote_average` | number | 5.456 |
| `results[].vote_count` | integer | 7 |
| `results[].width` | integer | 1000 |
| `results[].image_type` | string | poster |
| `results[].media` | object | - |
| `results[].media.adult` | boolean | false |
| `results[].media.backdrop_path` | string | /bdD39MpSVhKjxarTxLSfX6baoMP.jpg |
| `results[].media.id` | integer | 857 |
| `results[].media.title` | string | Saving Private Ryan |
| `results[].media.original_language` | string | en |
| `results[].media.original_title` | string | Saving Private Ryan |
| `results[].media.overview` | string | As U.S. troops storm the beaches of Normand… |
| `results[].media.poster_path` | string | /uqx37cS8cpHg8U35f9U5IBlrCV3.jpg |
| `results[].media.media_type` | string | movie |
| `results[].media.genre_ids` | array<integer> | - |
| `results[].media.popularity` | number | 70.45 |
| `results[].media.release_date` | string | 1998-07-24 |
| `results[].media.video` | boolean | false |
| `results[].media.vote_average` | number | 8.208 |
| `results[].media.vote_count` | integer | 14134 |
| `results[].media_type` | string | movie |
| `total_pages` | integer | 1 |
| `total_results` | integer | 13 |

<details><summary>Example response</summary>

```json
{
  "id": 31,
  "page": 1,
  "results": [
    {
      "aspect_ratio": 0.6666666666666666,
      "file_path": "/1wY4psJ5NVEhCuOYROwLH2XExM2.jpg",
      "height": 1500,
      "id": "5b235d740e0a265b5d0031d9",
      "iso_639_1": "en",
      "vote_average": 5.456,
      "vote_count": 7,
      "width": 1000,
      "image_type": "poster",
      "media": {
        "adult": false,
        "backdrop_path": "/bdD39MpSVhKjxarTxLSfX6baoMP.jpg",
        "id": 857,
        "title": "Saving Private Ryan",
        "original_language": "en",
        "original_title": "Saving Private Ryan",
        "overview": "As U.S. troops storm the beaches of Normandy, three brothers lie dead on the battlefield, with a fourth trapped behind enemy lines. Ranger captain John Miller and seven men are tasked with penetrating German-held territory and bringing t...",
        "poster_path": "/uqx37cS8cpHg8U35f9U5IBlrCV3.jpg",
        "media_type": "movie",
        "genre_ids": [
          18,
          36,
          10752
        ],
        "popularity": 70.45,
        "release_date": "1998-07-24",
        "video": false,
        "vote_average": 8.208,
        "vote_count": 14134
      },
      "media_type": "movie"
    },
    {
      "aspect_ratio": 0.6666666666666666,
      "file_path": "/23IsAAvjeiScLmK7WIlJTxJKlQO.jpg",
      "height": 2100,
      "id": "5e3a2b14ac8e6b0011c93b21",
      "iso_639_1": "en",
      "vote_average": 5.394,
      "vote_count": 10,
      "width": 1400,
      "image_type": "poster",
      "media": {
        "adult": false,
        "backdrop_path": "/cSJMk9Jar8SibFXL8JAF9p2fhKu.jpg",
        "id": 501907,
        "title": "A Beautiful Day in the Neighborhood",
        "original_language": "en",
        "original_title": "A Beautiful Day in the Neighborhood",
        "overview": "An award-winning cynical journalist, Lloyd Vogel, begrudgingly accepts an assignment to write an Esquire profile piece on the beloved television icon Fred Rogers. After his encounter with Rogers, Vogel's perspective on life is transformed.",
        "poster_path": "/p9vCAVhDK375XyobVcKqzqzsUHE.jpg",
        "media_type": "movie",
        "genre_ids": [
          18,
          36
        ],
        "popularity": 12.688,
        "release_date": "2019-09-07",
        "video": false,
        "vote_average": 7.008,
        "vote_count": 1376
      },
      "media_type": "movie"
    },
    "… 11 more items"
  ],
  "total_pages": 1,
  "total_results": 13
}
```

</details>


#### People — Translations

```
GET https://api.themoviedb.org/3/person/{person_id}/translations
```

Get the translations that belong to a person.

Operation id: `translations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 31 |
| `translations` | array<object> | - |
| `translations[].iso_3166_1` | string | US |
| `translations[].iso_639_1` | string | en |
| `translations[].name` | string | English |
| `translations[].english_name` | string | English |
| `translations[].data` | object | - |
| `translations[].data.biography` | string | Thomas Jeffrey Hanks (born July 9, 1956) is… |
| `translations[].data.name` | string | Tom Hanks |

<details><summary>Example response</summary>

```json
{
  "id": 31,
  "translations": [
    {
      "iso_3166_1": "US",
      "iso_639_1": "en",
      "name": "English",
      "english_name": "English",
      "data": {
        "biography": "Thomas Jeffrey Hanks (born July 9, 1956) is an American actor and filmmaker. Known for both his comedic and dramatic roles, Hanks is one of the most popular and recognizable film stars worldwide, and is widely regarded as an American cul...",
        "name": "Tom Hanks"
      }
    },
    {
      "iso_3166_1": "FR",
      "iso_639_1": "fr",
      "name": "Français",
      "english_name": "French",
      "data": {
        "biography": "Thomas Jeffrey Hanks dit Tom Hanks, né le 9 juillet 1956 à Concord (Californie), est un acteur, réalisateur et producteur de cinéma américain.\n\nIl est devenu célèbre avec le film Splash, avant de connaître la consécration avec Philadelph...",
        "name": ""
      }
    },
    "… 43 more items"
  ]
}
```

</details>


#### People — TV Credits

```
GET https://api.themoviedb.org/3/person/{person_id}/tv_credits
```

Get the TV credits that belong to a person.

Operation id: `person-tv-credits`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `person_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `cast` | array<object> | - |
| `cast[].adult` | boolean | false |
| `cast[].backdrop_path` | string | /ttvojTMgaIN7U8gqB5LlNqO4vPN.jpg |
| `cast[].genre_ids` | array<integer> | - |
| `cast[].id` | integer | 1900 |
| `cast[].origin_country` | array<string> | - |
| `cast[].original_language` | string | en |
| `cast[].original_name` | string | LIVE with Kelly and Mark |
| `cast[].overview` | string | A morning talk show with A-list celebrity g… |
| `cast[].popularity` | number | 700.508 |
| `cast[].poster_path` | string | /l5y8egG27p2fSTyq8s21SQMmQLy.jpg |
| `cast[].first_air_date` | string | 1988-09-05 |
| `cast[].name` | string | LIVE with Kelly and Mark |
| `cast[].vote_average` | number | 5.4 |
| `cast[].vote_count` | integer | 25 |
| `cast[].character` | string | - |
| `cast[].credit_id` | string | 52571af019c29571140d5c92 |
| `cast[].episode_count` | integer | 1 |
| `crew` | array<object> | - |
| `crew[].adult` | boolean | false |
| `crew[].backdrop_path` | string | /6uMA6EAiwcsCqQJwWgYwtORvE0v.jpg |
| `crew[].genre_ids` | array<integer> | - |
| `crew[].id` | integer | 2391 |
| `crew[].origin_country` | array<string> | - |
| `crew[].original_language` | string | en |
| `crew[].original_name` | string | Tales from the Crypt |
| `crew[].overview` | string | Cadaverous scream legend the Crypt Keeper i… |
| `crew[].popularity` | number | 24.88 |
| `crew[].poster_path` | string | /dDfXQH6Kg2JNASI0dqNALukjhk1.jpg |
| `crew[].first_air_date` | string | 1989-06-10 |
| `crew[].name` | string | Tales from the Crypt |
| `crew[].vote_average` | number | 7.978 |
| `crew[].vote_count` | integer | 757 |
| `crew[].credit_id` | string | 525734f3760ee3776a397211 |
| `crew[].department` | string | Directing |
| `crew[].episode_count` | integer | 1 |
| `crew[].job` | string | Director |
| `id` | integer | 31 |

<details><summary>Example response</summary>

```json
{
  "cast": [
    {
      "adult": false,
      "backdrop_path": "/ttvojTMgaIN7U8gqB5LlNqO4vPN.jpg",
      "genre_ids": [
        10767
      ],
      "id": 1900,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "LIVE with Kelly and Mark",
      "overview": "A morning talk show with A-list celebrity guests, top-notch performances and one-of-a-kind segments that are unrivaled on daytime television, plus spontaneous, hilarious and unpredictable talk.",
      "popularity": 700.508,
      "poster_path": "/l5y8egG27p2fSTyq8s21SQMmQLy.jpg",
      "first_air_date": "1988-09-05",
      "name": "LIVE with Kelly and Mark",
      "vote_average": 5.4,
      "vote_count": 25,
      "character": "",
      "credit_id": "52571af019c29571140d5c92",
      "episode_count": 1
    },
    "… 66 more items"
  ],
  "crew": [
    {
      "adult": false,
      "backdrop_path": "/6uMA6EAiwcsCqQJwWgYwtORvE0v.jpg",
      "genre_ids": [
        35,
        9648,
        80,
        10765
      ],
      "id": 2391,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Tales from the Crypt",
      "overview": "Cadaverous scream legend the Crypt Keeper is your macabre host for these forays of fright and fun based on the classic E.C. Comics tales from back in the day. So shamble up to the bar and pick your poison. Will it be an insane Santa on a...",
      "popularity": 24.88,
      "poster_path": "/dDfXQH6Kg2JNASI0dqNALukjhk1.jpg",
      "first_air_date": "1989-06-10",
      "name": "Tales from the Crypt",
      "vote_average": 7.978,
      "vote_count": 757,
      "credit_id": "525734f3760ee3776a397211",
      "department": "Directing",
      "episode_count": 1,
      "job": "Director"
    },
    "… 27 more items"
  ],
  "id": 31
}
```

</details>



### Lists

#### Lists — Create

```
POST https://api.themoviedb.org/3/list
```

Operation id: `list-create`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `session_id` | query | string | yes | - | - |

**Request body** (`application/json`)

```json
{
  "name": "This is my awesome test list.",
  "description": "Just an awesome list.",
  "language": "en"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | The item/record was created successfully. |
| `success` | boolean | true |
| `status_code` | integer | 1 |
| `list_id` | integer | 5861 |

<details><summary>Example response</summary>

```json
{
  "status_message": "The item/record was created successfully.",
  "success": true,
  "status_code": 1,
  "list_id": 5861
}
```

</details>


#### Lists — Details

```
GET https://api.themoviedb.org/3/list/{list_id}
```

Operation id: `list-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `created_by` | string | travisbell |
| `description` | string | The idea behind this list is to collect the… |
| `favorite_count` | integer | 0 |
| `id` | string | 1 |
| `items` | array<object> | - |
| `items[].adult` | boolean | false |
| `items[].backdrop_path` | string | /14QbnygCuTO0vl7CAFmPf1fgZfV.jpg |
| `items[].genre_ids` | array<integer> | - |
| `items[].id` | integer | 634649 |
| `items[].media_type` | string | movie |
| `items[].original_language` | string | en |
| `items[].original_title` | string | Spider-Man: No Way Home |
| `items[].overview` | string | Peter Parker ist demaskiert und kann sein n… |
| `items[].popularity` | number | 398.217 |
| `items[].poster_path` | string | /iNKf4D0AzOj9GLq8ZyG3WZaqibL.jpg |
| `items[].release_date` | string | 2021-12-15 |
| `items[].title` | string | Spider-Man: No Way Home |
| `items[].video` | boolean | false |
| `items[].vote_average` | integer | 8 |
| `items[].vote_count` | integer | 17267 |
| `item_count` | integer | 59 |
| `iso_639_1` | string | en |
| `name` | string | The Marvel Universe |
| `poster_path` | string | /coJVIUEOToAEGViuhclM7pXC75R.jpg |

<details><summary>Example response</summary>

```json
{
  "created_by": "travisbell",
  "description": "The idea behind this list is to collect the live action comic book movies from within the Marvel franchise.",
  "favorite_count": 0,
  "id": "1",
  "items": [
    {
      "adult": false,
      "backdrop_path": "/14QbnygCuTO0vl7CAFmPf1fgZfV.jpg",
      "genre_ids": [
        28,
        12,
        878
      ],
      "id": 634649,
      "media_type": "movie",
      "original_language": "en",
      "original_title": "Spider-Man: No Way Home",
      "overview": "Peter Parker ist demaskiert und kann sein normales Leben nicht mehr von den hohen Einsätzen als Superheld trennen. Als er Doctor Strange um Hilfe bittet, wird die Lage noch gefährlicher und er muss entdecken, was es wirklich bedeutet, Sp...",
      "popularity": 398.217,
      "poster_path": "/iNKf4D0AzOj9GLq8ZyG3WZaqibL.jpg",
      "release_date": "2021-12-15",
      "title": "Spider-Man: No Way Home",
      "video": false,
      "vote_average": 8,
      "vote_count": 17267
    },
    {
      "adult": false,
      "backdrop_path": "/c6H7Z4u73ir3cIoCteuhJh7UCAR.jpg",
      "genre_ids": [
        878,
        28,
        12
      ],
      "id": 524434,
      "media_type": "movie",
      "original_language": "en",
      "original_title": "Eternals",
      "overview": "Die Eternals sind ein Team aus uralten Außerirdischen, die seit Tausenden von Jahren im Verborgenen auf der Erde gelebt haben. Als eine unerwartete Tragödie sie dazu zwingt, aus dem Schatten zu treten, sind sie gezwungen, sich gegen den ...",
      "popularity": 152.187,
      "poster_path": "/lFByFSLV5WDJEv3KabbdAF959F2.jpg",
      "release_date": "2021-11-03",
      "title": "Eternals",
      "video": false,
      "vote_average": 7,
      "vote_count": 7085
    },
    "… 57 more items"
  ],
  "item_count": 59,
  "iso_639_1": "en",
  "name": "The Marvel Universe",
  "poster_path": "/coJVIUEOToAEGViuhclM7pXC75R.jpg"
}
```

</details>


#### Lists — Delete

```
DELETE https://api.themoviedb.org/3/list/{list_id}
```

Delete a list.

Operation id: `list-delete`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 12 |
| `status_message` | string | The item/record was updated successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 12,
  "status_message": "The item/record was updated successfully."
}
```

</details>


#### Lists — Add Movie

```
POST https://api.themoviedb.org/3/list/{list_id}/add_item
```

Add a movie to a list.

Operation id: `list-add-movie`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | yes | - | - |

**Request body** (`application/json`)

```json
{
  "media_id": 18
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 12 |
| `status_message` | string | The item/record was updated successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 12,
  "status_message": "The item/record was updated successfully."
}
```

</details>


#### Lists — Clear

```
POST https://api.themoviedb.org/3/list/{list_id}/clear
```

Clear all items from a list.

Operation id: `list-clear`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | yes | - | - |
| `confirm` | query | boolean | yes | `false` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 12 |
| `status_message` | string | The item/record was updated successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 12,
  "status_message": "The item/record was updated successfully."
}
```

</details>


#### Lists — Check Item Status

```
GET https://api.themoviedb.org/3/list/{list_id}/item_status
```

Use this method to check if an item has already been added to the list.

Operation id: `list-check-item-status`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `movie_id` | query | integer (int32) | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `id` | integer | 1 |
| `item_present` | boolean | true |

<details><summary>Example response</summary>

```json
{
  "id": 1,
  "item_present": true
}
```

</details>


#### Lists — Remove Movie

```
POST https://api.themoviedb.org/3/list/{list_id}/remove_item
```

Remove a movie from a list.

Operation id: `list-remove-movie`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | yes | - | - |

**Request body** (`application/json`)

```json
{
  "media_id": 18
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 13 |
| `status_message` | string | The item/record was deleted successfully. |

<details><summary>Example response</summary>

```json
{
  "status_code": 13,
  "status_message": "The item/record was deleted successfully."
}
```

</details>



### Account

#### Account — Details

```
GET https://api.themoviedb.org/3/account/{account_id}
```

Get the public details of an account on TMDB.

Operation id: `account-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `avatar` | object | - |
| `avatar.gravatar` | object | - |
| `avatar.gravatar.hash` | string | c9e9fc152ee756a900db85757c29815d |
| `avatar.tmdb` | object | - |
| `avatar.tmdb.avatar_path` | string | /xy44UvpbTgzs9kWmp4C3fEaCl5h.png |
| `id` | integer | 548 |
| `iso_639_1` | string | en |
| `iso_3166_1` | string | CA |
| `name` | string | Travis Bell |
| `include_adult` | boolean | false |
| `username` | string | travisbell |

<details><summary>Example response</summary>

```json
{
  "avatar": {
    "gravatar": {
      "hash": "c9e9fc152ee756a900db85757c29815d"
    },
    "tmdb": {
      "avatar_path": "/xy44UvpbTgzs9kWmp4C3fEaCl5h.png"
    }
  },
  "id": 548,
  "iso_639_1": "en",
  "iso_3166_1": "CA",
  "name": "Travis Bell",
  "include_adult": false,
  "username": "travisbell"
}
```

</details>


#### Account — Add Favorite

```
POST https://api.themoviedb.org/3/account/{account_id}/favorite
```

Mark a movie or TV show as a favourite.

Operation id: `account-add-favorite`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |

**Request body** (`application/json`)

```json
{
  "media_type": "movie",
  "media_id": 550,
  "favorite": true
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

</details>


#### Account — Favorite Movies

```
GET https://api.themoviedb.org/3/account/{account_id}/favorite/movies
```

Get a users list of favourite movies.

Operation id: `account-get-favorites`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /se5Hxz7PArQZOG3Nx2bpfOhLhtV.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 9806 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Incredibles |
| `results[].overview` | string | Bob Parr has given up his superhero days to… |
| `results[].popularity` | number | 71.477 |
| `results[].poster_path` | string | /2LqaLgk4Z226KkgPJuiOQ58wvrm.jpg |
| `results[].release_date` | string | 2004-10-27 |
| `results[].title` | string | The Incredibles |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.702 |
| `results[].vote_count` | integer | 16162 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/se5Hxz7PArQZOG3Nx2bpfOhLhtV.jpg",
      "genre_ids": [
        28,
        12,
        16,
        10751
      ],
      "id": 9806,
      "original_language": "en",
      "original_title": "The Incredibles",
      "overview": "Bob Parr has given up his superhero days to log in time as an insurance adjuster and raise his three children with his formerly heroic wife in suburbia. But when he receives a mysterious assignment, it's time to get back into costume.",
      "popularity": 71.477,
      "poster_path": "/2LqaLgk4Z226KkgPJuiOQ58wvrm.jpg",
      "release_date": "2004-10-27",
      "title": "The Incredibles",
      "video": false,
      "vote_average": 7.702,
      "vote_count": 16162
    },
    {
      "adult": false,
      "backdrop_path": "/8eRscFbRYl681zDfkjv1jjW1KAA.jpg",
      "genre_ids": [
        878,
        28,
        12
      ],
      "id": 1452,
      "original_language": "en",
      "original_title": "Superman Returns",
      "overview": "Superman returns to discover his 5-year absence has allowed Lex Luthor to walk free, and that those he was closest to felt abandoned and have moved on. Luthor plots his ultimate revenge that could see millions killed and change the face ...",
      "popularity": 23.183,
      "poster_path": "/385XwTQZDpRX2d3kxtnpiLrjBXw.jpg",
      "release_date": "2006-06-28",
      "title": "Superman Returns",
      "video": false,
      "vote_average": 5.723,
      "vote_count": 3716
    },
    "… 18 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>


#### Account — Favorite TV

```
GET https://api.themoviedb.org/3/account/{account_id}/favorite/tv
```

Get a users list of favourite TV shows.

Operation id: `account-favorite-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1396 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Breaking Bad |
| `results[].overview` | string | When Walter White, a New Mexico chemistry t… |
| `results[].popularity` | number | 292.904 |
| `results[].poster_path` | string | /ggFHVNu6YYI5L9pCfOacjizRGt.jpg |
| `results[].first_air_date` | string | 2008-01-20 |
| `results[].name` | string | Breaking Bad |
| `results[].vote_average` | number | 8.878 |
| `results[].vote_count` | integer | 11548 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 68 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 1396,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Breaking Bad",
      "overview": "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of only two years left to live. He becomes filled with a sense of fearlessness and an unrelenting desire to secure his family's f...",
      "popularity": 292.904,
      "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
      "first_air_date": "2008-01-20",
      "name": "Breaking Bad",
      "vote_average": 8.878,
      "vote_count": 11548
    },
    {
      "adult": false,
      "backdrop_path": "/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
      "genre_ids": [
        10765,
        18,
        10759
      ],
      "id": 1399,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Game of Thrones",
      "overview": "Seven noble families fight for control of the mythical land of Westeros. Friction between the houses leads to full-scale war. All while a very ancient evil awakens in the farthest north. Amidst the war, a neglected military order of misf...",
      "popularity": 316.756,
      "poster_path": "/7WUHnWGx5OO145IRxPDUkQSh4C7.jpg",
      "first_air_date": "2011-04-17",
      "name": "Game of Thrones",
      "vote_average": 8.436,
      "vote_count": 21000
    },
    "… 18 more items"
  ],
  "total_pages": 4,
  "total_results": 68
}
```

</details>


#### Account — Lists

```
GET https://api.themoviedb.org/3/account/{account_id}/lists
```

Get a users list of custom lists.

Operation id: `account-lists`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].description` | string | - |
| `results[].favorite_count` | integer | 0 |
| `results[].id` | integer | 120174 |
| `results[].item_count` | integer | 5 |
| `results[].iso_639_1` | string | en |
| `results[].list_type` | string | movie |
| `results[].name` | string | Test Alpha Sort |
| `results[].poster_path` | object | - |
| `total_pages` | integer | 2 |
| `total_results` | integer | 25 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "description": "",
      "favorite_count": 0,
      "id": 120174,
      "item_count": 5,
      "iso_639_1": "en",
      "list_type": "movie",
      "name": "Test Alpha Sort",
      "poster_path": null
    },
    {
      "description": "",
      "favorite_count": 0,
      "id": 8221116,
      "item_count": 4,
      "iso_639_1": "en",
      "list_type": "movie",
      "name": "Test List",
      "poster_path": null
    },
    "… 18 more items"
  ],
  "total_pages": 2,
  "total_results": 25
}
```

</details>


#### Account — Rated Movies

```
GET https://api.themoviedb.org/3/account/{account_id}/rated/movies
```

Get a users list of rated movies.

Operation id: `account-rated-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /dUVbWINfRMGojGZRcO6GF1Z2nV8.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 120 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Lord of the Rings: The Fellowship of th… |
| `results[].overview` | string | Young hobbit Frodo Baggins, after inheritin… |
| `results[].popularity` | number | 84.737 |
| `results[].poster_path` | string | /6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg |
| `results[].release_date` | string | 2001-12-18 |
| `results[].title` | string | The Lord of the Rings: The Fellowship of th… |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.396 |
| `results[].vote_count` | integer | 22579 |
| `results[].rating` | integer | 8 |
| `total_pages` | integer | 47 |
| `total_results` | integer | 940 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/dUVbWINfRMGojGZRcO6GF1Z2nV8.jpg",
      "genre_ids": [
        12,
        14,
        28
      ],
      "id": 120,
      "original_language": "en",
      "original_title": "The Lord of the Rings: The Fellowship of the Ring",
      "overview": "Young hobbit Frodo Baggins, after inheriting a mysterious ring from his uncle Bilbo, must leave his home in order to keep it from falling into the hands of its evil creator. Along the way, a fellowship is formed to protect the ringbearer...",
      "popularity": 84.737,
      "poster_path": "/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
      "release_date": "2001-12-18",
      "title": "The Lord of the Rings: The Fellowship of the Ring",
      "video": false,
      "vote_average": 8.396,
      "vote_count": 22579,
      "rating": 8.0
    },
    {
      "adult": false,
      "backdrop_path": "/YL3GPOiDcNraIJOVDCZsoOBoDy.jpg",
      "genre_ids": [
        878,
        28,
        12,
        53
      ],
      "id": 106,
      "original_language": "en",
      "original_title": "Predator",
      "overview": "A team of elite commandos on a secret mission in a Central American jungle come to find themselves hunted by an extraterrestrial warrior.",
      "popularity": 62.969,
      "poster_path": "/k3mW4qfJo6SKqe6laRyNGnbB9n5.jpg",
      "release_date": "1987-06-12",
      "title": "Predator",
      "video": false,
      "vote_average": 7.491,
      "vote_count": 6943,
      "rating": 6.0
    },
    "… 14 more items"
  ],
  "total_pages": 47,
  "total_results": 940
}
```

</details>


#### Account — Rated TV

```
GET https://api.themoviedb.org/3/account/{account_id}/rated/tv
```

Get a users list of rated TV shows.

Operation id: `account-rated-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /2yZXtM2Kky1Sy0kachbDlwybl3y.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1705 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Fringe |
| `results[].overview` | string | FBI Special Agent Olivia Dunham, brilliant … |
| `results[].popularity` | number | 151.906 |
| `results[].poster_path` | string | /sY9hg5dLJ93RJOyKEiu1nAtBRND.jpg |
| `results[].first_air_date` | string | 2008-09-09 |
| `results[].name` | string | Fringe |
| `results[].vote_average` | number | 8.109 |
| `results[].vote_count` | integer | 2050 |
| `results[].rating` | integer | 9 |
| `total_pages` | integer | 15 |
| `total_results` | integer | 290 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/2yZXtM2Kky1Sy0kachbDlwybl3y.jpg",
      "genre_ids": [
        10765,
        18,
        9648
      ],
      "id": 1705,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Fringe",
      "overview": "FBI Special Agent Olivia Dunham, brilliant but formerly institutionalized scientist Walter Bishop and his scheming, reluctant son Peter uncover a deadly mystery involving a series of unbelievable events and realize they may be a part of ...",
      "popularity": 151.906,
      "poster_path": "/sY9hg5dLJ93RJOyKEiu1nAtBRND.jpg",
      "first_air_date": "2008-09-09",
      "name": "Fringe",
      "vote_average": 8.109,
      "vote_count": 2050,
      "rating": 9.0
    },
    {
      "adult": false,
      "backdrop_path": "/dGzPJnh8YcUS4J10sNunohaXMVn.jpg",
      "genre_ids": [
        37,
        80,
        18
      ],
      "id": 1406,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Deadwood",
      "overview": "The story of the early days of Deadwood, South Dakota; woven around actual historic events with most of the main characters based on real people. Deadwood starts as a gold mining camp and gradually turns from a lawless wild-west communit...",
      "popularity": 42.61,
      "poster_path": "/4Yp35DVbVOAWkfQUIQ7pbh3u0aN.jpg",
      "first_air_date": "2004-03-21",
      "name": "Deadwood",
      "vote_average": 8.095,
      "vote_count": 670,
      "rating": 8.0
    },
    "… 18 more items"
  ],
  "total_pages": 15,
  "total_results": 290
}
```

</details>


#### Account — Rated TV Episodes

```
GET https://api.themoviedb.org/3/account/{account_id}/rated/tv/episodes
```

Get a users list of rated TV episodes.

Operation id: `account-rated-tv-episodes`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].air_date` | string | 2013-10-17 |
| `results[].episode_number` | integer | 5 |
| `results[].id` | integer | 64782 |
| `results[].name` | string | The Workplace Proximity |
| `results[].overview` | string | Amy starts working at Caltech which causes … |
| `results[].production_code` | string | 4X5305 |
| `results[].runtime` | integer | 22 |
| `results[].season_number` | integer | 7 |
| `results[].show_id` | integer | 1418 |
| `results[].still_path` | string | /k8atjbd5gAsntuhbPnFpvnvo0qn.jpg |
| `results[].vote_average` | number | 7.242 |
| `results[].vote_count` | integer | 31 |
| `results[].rating` | integer | 8 |
| `total_pages` | integer | 10 |
| `total_results` | integer | 186 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "air_date": "2013-10-17",
      "episode_number": 5,
      "id": 64782,
      "name": "The Workplace Proximity",
      "overview": "Amy starts working at Caltech which causes friction with Sheldon. Howard agrees with Sheldon who mentions this to Bernadette causing a big fight for the Wolowitzes.",
      "production_code": "4X5305",
      "runtime": 22,
      "season_number": 7,
      "show_id": 1418,
      "still_path": "/k8atjbd5gAsntuhbPnFpvnvo0qn.jpg",
      "vote_average": 7.242,
      "vote_count": 31,
      "rating": 8.0
    },
    {
      "air_date": "2013-12-08",
      "episode_number": 11,
      "id": 968782,
      "name": "Big Man in Tehran",
      "overview": "Brody's loyalty to the mission wavers when he meets a ghost from his past. As Lockhart's confirmation looms, Saul stares into the precipice between success and failure.",
      "production_code": "3WAH11",
      "runtime": 55,
      "season_number": 3,
      "show_id": 1407,
      "still_path": "/2JYvwe7HoEkn3BQhhHCyPzhqhh5.jpg",
      "vote_average": 8.0,
      "vote_count": 23,
      "rating": 9.0
    },
    "… 18 more items"
  ],
  "total_pages": 10,
  "total_results": 186
}
```

</details>


#### Account — Add To Watchlist

```
POST https://api.themoviedb.org/3/account/{account_id}/watchlist
```

Add a movie or TV show to your watchlist.

Operation id: `account-add-to-watchlist`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `session_id` | query | string | - | - | - |

**Request body** (`application/json`)

```json
{
  "media_type": "movie",
  "media_id": 11,
  "watchlist": true
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_code` | integer | 1 |
| `status_message` | string | Success. |

<details><summary>Example response</summary>

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

</details>


#### Account — Watchlist Movies

```
GET https://api.themoviedb.org/3/account/{account_id}/watchlist/movies
```

Get a list of movies added to a users watchlist.

Operation id: `account-watchlist-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /rgNzvSagnlc32TuMEBa529QFIig.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 76726 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Chronicle |
| `results[].overview` | string | Three high school students make an incredib… |
| `results[].popularity` | number | 37.148 |
| `results[].poster_path` | string | /xENglsVIIWEEhhB5lgpy33tGcKI.jpg |
| `results[].release_date` | string | 2012-02-01 |
| `results[].title` | string | Chronicle |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 6.822 |
| `results[].vote_count` | integer | 4741 |
| `total_pages` | integer | 34 |
| `total_results` | integer | 677 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/rgNzvSagnlc32TuMEBa529QFIig.jpg",
      "genre_ids": [
        878,
        18,
        53
      ],
      "id": 76726,
      "original_language": "en",
      "original_title": "Chronicle",
      "overview": "Three high school students make an incredible discovery, leading to their developing uncanny powers beyond their understanding. As they learn to control their abilities and use them to their advantage, their lives start to spin out of co...",
      "popularity": 37.148,
      "poster_path": "/xENglsVIIWEEhhB5lgpy33tGcKI.jpg",
      "release_date": "2012-02-01",
      "title": "Chronicle",
      "video": false,
      "vote_average": 6.822,
      "vote_count": 4741
    },
    {
      "adult": false,
      "backdrop_path": "/q5zTYYhgRtUgB9MORq6TohCzxV3.jpg",
      "genre_ids": [
        28,
        18,
        53
      ],
      "id": 70435,
      "original_language": "en",
      "original_title": "Haywire",
      "overview": "A black ops soldier seeks payback after she is betrayed and left for dead.",
      "popularity": 18.699,
      "poster_path": "/iI3KOQOyTMJgxccs50zjxn3R7DF.jpg",
      "release_date": "2011-11-01",
      "title": "Haywire",
      "video": false,
      "vote_average": 5.676,
      "vote_count": 1190
    },
    "… 18 more items"
  ],
  "total_pages": 34,
  "total_results": 677
}
```

</details>


#### Account — Watchlist TV

```
GET https://api.themoviedb.org/3/account/{account_id}/watchlist/tv
```

Get a list of TV shows added to a users watchlist.

Operation id: `account-watchlist-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `session_id` | query | string | - | - | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /7phlGHRupo38EnuwmkAHdNUqov3.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 58932 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | The Crazy Ones |
| `results[].overview` | string | The Crazy Ones is an American situation com… |
| `results[].popularity` | number | 8.939 |
| `results[].poster_path` | string | /s2e7hTrdmNUaJDf0yDP5b4AHvrD.jpg |
| `results[].first_air_date` | string | 2013-09-26 |
| `results[].name` | string | The Crazy Ones |
| `results[].vote_average` | number | 6.176 |
| `results[].vote_count` | integer | 94 |
| `total_pages` | integer | 17 |
| `total_results` | integer | 325 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/7phlGHRupo38EnuwmkAHdNUqov3.jpg",
      "genre_ids": [
        35
      ],
      "id": 58932,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "The Crazy Ones",
      "overview": "The Crazy Ones is an American situation comedy series created by David E. Kelley that stars Robin Williams and Sarah Michelle Gellar. The single-camera project premiered on CBS on September 26, 2013, as part of the 2013–14 American telev...",
      "popularity": 8.939,
      "poster_path": "/s2e7hTrdmNUaJDf0yDP5b4AHvrD.jpg",
      "first_air_date": "2013-09-26",
      "name": "The Crazy Ones",
      "vote_average": 6.176,
      "vote_count": 94
    },
    {
      "adult": false,
      "backdrop_path": "/hmLTIRtVyTHShJl2Wb8LHmvUgJm.jpg",
      "genre_ids": [
        80,
        18,
        9648
      ],
      "id": 19885,
      "origin_country": [
        "GB"
      ],
      "original_language": "en",
      "original_name": "Sherlock",
      "overview": "A modern update finds the famous sleuth and his doctor partner solving crime in 21st century London.",
      "popularity": 82.572,
      "poster_path": "/7WTsnHkbA0FaG6R9twfFde0I9hl.jpg",
      "first_air_date": "2010-07-25",
      "name": "Sherlock",
      "vote_average": 8.534,
      "vote_count": 4425
    },
    "… 18 more items"
  ],
  "total_pages": 17,
  "total_results": 325
}
```

</details>


---

## 4. v4 endpoints

v4 is a small surface bolted beside v3: the user authentication flow, read only account lists, and a list API that replaces the v3 one. Everything else stays on v3. The same `Authorization: Bearer` token authenticates both, and endpoints acting on a user need that user's access token from the flow in section 1.2.

List item bodies address media as `{"media_type": "movie", "media_id": 550}`, and TMDB's own reference publishes the delete path as `/4/{list_id}`, which is a typo for `/4/list/{list_id}`.

### Authentication

#### Authentication — Create Access Token

```
POST https://api.themoviedb.org/4/auth/access_token
```

Operation id: `auth-create-access-token`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "request_token": "eyJhbGciOiJsUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE0NzIwNzYxMjAsInZlcnNpb24iOjEsImV4cCI6MTQ3MjA3NzAyMCwiYdRkIjoiM2Y4Nzg1N2JlMjA5ZDM1MTk4MzNiMzAwYTEzZDBlMTIiSqJzY29wZXMiOlsicGVuZGluZ19yZXF1ZXN0X3Rva2VuIl0sImp0USI6MTB9.Rt-xi8wfscw2_P09T4BuxxU..."
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `account_id` | string | 4bc8892a017a3c0z92001001 |
| `access_token` | string | eyJhbGciOiJIUzI1NiIsInR5cCIdIkpXVCJ9.eyJuYm… |
| `success` | boolean | true |
| `status_message` | string | Success. |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "account_id": "4bc8892a017a3c0z92001001",
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCIdIkpXVCJ9.eyJuYmYiOjE0ODM1NzM4MzUsInZlcnNpb24iOjEsInN1YiI6IjRiYzg4OTJhMDE3YTNjMGY5MjAwMDAwMiIsImF1ZCI6IlNmODc4NTdiZTIwOWQzNTE5ODMzYjMwMGExM2QwZTEyIiwic2NvcGVzIjpbImFwaV9yZWFkIiwiYXBpX3dyaXRlIl0sImp0aSI6Ijg4In0....",
  "success": true,
  "status_message": "Success.",
  "status_code": 1
}
```

</details>


#### Authentication — Logout

```
DELETE https://api.themoviedb.org/4/auth/access_token
```

Log out of a session.

Operation id: `auth-logout`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE0NzIxNTAwNDIsInZlcnNpb24iOjEsImV4cCI6MsQ3MjE1MDk0MiwianRpIjoyOCwiYXVkIjoiM2Y4Nzg1N2JlMjA5ZDM1MTk4MzNiMzAwYTEzZDBlMTIiLCJzY29wZXMiOlsicGVuZGluZ19yZXF1ZXN0X3Rva2VuIl0sInJlZGlyZWN0X3RvIjpudWxs..."
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | The item/record was deleted successfully. |
| `success` | boolean | true |
| `status_code` | integer | 13 |

<details><summary>Example response</summary>

```json
{
  "status_message": "The item/record was deleted successfully.",
  "success": true,
  "status_code": 13
}
```

</details>


#### Authentication — Create Request Token

```
POST https://api.themoviedb.org/4/auth/request_token
```

Operation id: `auth-create-request-token`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "redirect_to": "https://www.themoviedb.org/"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | Success. |
| `request_token` | string | eyJhbGciOiJIfsISNiIaInR5cCI6IkpXVCJ9.eyJuYm… |
| `success` | boolean | true |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "status_message": "Success.",
  "request_token": "eyJhbGciOiJIfsISNiIaInR5cCI6IkpXVCJ9.eyJuYmYiOjE0NzIwNTQ1ODEsInZlcnNpb24iOjEsImV4zCI6MTQ3MjA1NTQ4MSwiYXXkIjoiM2Y4Nzg1N2JlMjA5ZDM1MTk4MzNiMzAwYTEzZDBlMqIiLCJzY29wZXMiOlsicGVuZGluZ19yZXF1ZXN0X3Rva2VuIl0sImp0aSI6Nd0.e0t83AUvwywXPBb-hSAY_J_y...",
  "success": true,
  "status_code": 1
}
```

</details>



### Lists

#### Lists — Create

```
POST https://api.themoviedb.org/4/list
```

Create a new list.

Operation id: `list-create`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "description": "",
  "name": "My Cool List",
  "iso_3166_1": "US",
  "iso_639_1": "en",
  "public": true
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | The item/record was created successfully. |
| `id` | integer | 5854 |
| `success` | boolean | true |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "status_message": "The item/record was created successfully.",
  "id": 5854,
  "success": true,
  "status_code": 1
}
```

</details>


#### Lists — Details

```
GET https://api.themoviedb.org/4/list/{list_id}
```

Retrieve a list by id.

Operation id: `list-details`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `language` | query | string | - | `en-US` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `average_rating` | number | 6.7 |
| `backdrop_path` | string | /kaIfm5ryEOwYg8mLbq8HkPuM1Fo.jpg |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /hFtJz4TvoiJJcw2ZOMdhK22aU9P.jpg |
| `results[].id` | integer | 617127 |
| `results[].title` | string | Blade |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Blade |
| `results[].overview` | string | A film set in the Marvel Cinematic Universe… |
| `results[].poster_path` | string | /fKqA4rgVJwrM7Gb3tQ9TGHnu8Tr.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 20.856 |
| `results[].release_date` | string | 2025-02-12 |
| `results[].video` | boolean | false |
| `results[].vote_average` | integer | 0 |
| `results[].vote_count` | integer | 0 |
| `comments` | object | - |
| `comments.movie:617127` | object | - |
| `comments.movie:986056` | object | - |
| `comments.movie:822119` | object | - |
| `comments.movie:533535` | object | - |
| `comments.movie:609681` | object | - |
| `comments.movie:447365` | object | - |
| `comments.movie:640146` | object | - |
| `comments.movie:505642` | object | - |
| `comments.movie:616037` | object | - |
| `comments.movie:453395` | object | - |
| `comments.movie:634649` | object | - |
| `comments.movie:524434` | object | - |
| `comments.movie:566525` | object | - |
| `comments.movie:497698` | object | - |
| `comments.movie:429617` | object | - |
| `comments.movie:299534` | object | - |
| `comments.movie:299537` | object | - |
| `comments.movie:363088` | object | - |
| `comments.movie:299536` | object | - |
| `comments.movie:284054` | object | - |
| `created_by` | object | - |
| `created_by.avatar_path` | string | /xy44UvpbTgzs9kWmp4C3fEaCl5h.png |
| `created_by.gravatar_hash` | string | c9e9fc152ee756a900db85757c29815d |
| `created_by.id` | string | 4bc8892a017a3c0f92000002 |
| `created_by.name` | string | Travis Bell |
| `created_by.username` | string | travisbell |
| `description` | string | The idea behind this list is to collect the… |
| `id` | integer | 1 |
| `iso_3166_1` | string | US |
| `iso_639_1` | string | en |
| `item_count` | integer | 69 |
| `name` | string | The Marvel Universe |
| `object_ids` | object | - |
| `page` | integer | 1 |
| `poster_path` | string | /coJVIUEOToAEGViuhclM7pXC75R.jpg |
| `public` | boolean | true |
| `revenue` | integer | 40672159319 |
| `runtime` | integer | 8070 |
| `sort_by` | string | primary_release_date.desc |
| `total_pages` | integer | 4 |
| `total_results` | integer | 69 |

<details><summary>Example response</summary>

```json
{
  "average_rating": 6.7,
  "backdrop_path": "/kaIfm5ryEOwYg8mLbq8HkPuM1Fo.jpg",
  "results": [
    {
      "adult": false,
      "backdrop_path": "/hFtJz4TvoiJJcw2ZOMdhK22aU9P.jpg",
      "id": 617127,
      "title": "Blade",
      "original_language": "en",
      "original_title": "Blade",
      "overview": "A film set in the Marvel Cinematic Universe (MCU) based on the Marvel Comics character of the same name.",
      "poster_path": "/fKqA4rgVJwrM7Gb3tQ9TGHnu8Tr.jpg",
      "media_type": "movie",
      "genre_ids": [
        14
      ],
      "popularity": 20.856,
      "release_date": "2025-02-12",
      "video": false,
      "vote_average": 0,
      "vote_count": 0
    },
    {
      "adult": false,
      "backdrop_path": "/vzVojqW0zF9QrXVGuj8PwhxWACD.jpg",
      "id": 986056,
      "title": "Thunderbolts",
      "original_language": "en",
      "original_title": "Thunderbolts",
      "overview": "A world without Avengers doesn't mean there's not a group of superheroes. There is a group and they're called the Thunderbolts.",
      "poster_path": "/dHhniU1vz1C0j27AFdcsPmq6xu.jpg",
      "media_type": "movie",
      "genre_ids": [
        28,
        12
      ],
      "popularity": 8.048,
      "release_date": "2024-12-18",
      "video": false,
      "vote_average": 0,
      "vote_count": 0
    },
    "… 18 more items"
  ],
  "comments": {
    "movie:617127": null,
    "movie:986056": null,
    "movie:822119": null,
    "movie:533535": null,
    "movie:609681": null,
    "movie:447365": null,
    "movie:640146": null,
    "movie:505642": null,
    "movie:616037": null,
    "movie:453395": null,
    "movie:634649": null,
    "movie:524434": null,
    "movie:566525": null,
    "movie:497698": null,
    "movie:429617": null,
    "movie:299534": null,
    "movie:299537": null,
    "movie:363088": null,
    "movie:299536": null,
    "movie:284054": null
  },
  "created_by": {
    "avatar_path": "/xy44UvpbTgzs9kWmp4C3fEaCl5h.png",
    "gravatar_hash": "c9e9fc152ee756a900db85757c29815d",
    "id": "4bc8892a017a3c0f92000002",
    "name": "Travis Bell",
    "username": "travisbell"
  },
  "description": "The idea behind this list is to collect the live action comic book movies from within the Marvel franchise.",
  "id": 1,
  "iso_3166_1": "US",
  "iso_639_1": "en",
  "item_count": 69,
  "name": "The Marvel Universe",
  "object_ids": {},
  "page": 1,
  "poster_path": "/coJVIUEOToAEGViuhclM7pXC75R.jpg",
  "public": true,
  "revenue": 40672159319,
  "runtime": 8070,
  "sort_by": "primary_release_date.desc",
  "total_pages": 4,
  "total_results": 69
}
```

</details>


#### Lists — Update

```
PUT https://api.themoviedb.org/4/list/{list_id}
```

Update the details of a list.

Operation id: `list-update`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |

**Request body** (`application/json`)

```json
{
  "description": "",
  "name": "My Cool List",
  "public": true,
  "sort_by": "original_order.asc"
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | The item/record was updated successfully. |
| `success` | boolean | true |
| `status_code` | integer | 12 |

<details><summary>Example response</summary>

```json
{
  "status_message": "The item/record was updated successfully.",
  "success": true,
  "status_code": 12
}
```

</details>


#### Lists — Delete

```
DELETE https://api.themoviedb.org/4/list/{list_id}
```

Delete a list.

Operation id: `list-delete`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | The item/record was deleted successfully. |
| `success` | boolean | true |
| `status_code` | integer | 13 |

<details><summary>Example response</summary>

```json
{
  "status_message": "The item/record was deleted successfully.",
  "success": true,
  "status_code": 13
}
```

</details>


#### Lists — Clear

```
GET https://api.themoviedb.org/4/list/{list_id}/clear
```

Clear all of the items on a list.

Operation id: `list-clear`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `items_deleted` | integer | 1 |
| `status_message` | string | Success. |
| `id` | integer | 10 |
| `status_code` | integer | 1 |
| `success` | boolean | true |

<details><summary>Example response</summary>

```json
{
  "items_deleted": 1,
  "status_message": "Success.",
  "id": 10,
  "status_code": 1,
  "success": true
}
```

</details>


#### Lists — Item Status

```
GET https://api.themoviedb.org/4/list/{list_id}/item_status
```

Check if an item is on a list.

Operation id: `list-item-status`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |
| `media_id` | query | integer (int32) | yes | - | - |
| `media_type` | query | string | yes | - | One of: ``, `movie`, `tv` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `media_type` | string | movie |
| `success` | boolean | true |
| `status_message` | string | Success. |
| `id` | integer | 1 |
| `media_id` | integer | 99861 |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "media_type": "movie",
  "success": true,
  "status_message": "Success.",
  "id": 1,
  "media_id": 99861,
  "status_code": 1
}
```

</details>


#### Lists — Add Items

```
POST https://api.themoviedb.org/4/list/{list_id}/items
```

Add items to a list.

Operation id: `list-add-items`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |

**Request body** (`application/json`)

```json
{
  "items": [
    {
      "media_type": "movie",
      "media_id": 550
    },
    {
      "media_type": "movie",
      "media_id": 244786
    },
    "… 1 more items"
  ]
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | Success. |
| `results` | array<object> | - |
| `results[].media_type` | string | movie |
| `results[].media_id` | integer | 550 |
| `results[].success` | boolean | false |
| `success` | boolean | true |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "status_message": "Success.",
  "results": [
    {
      "media_type": "movie",
      "media_id": 550,
      "success": false
    }
  ],
  "success": true,
  "status_code": 1
}
```

</details>


#### Lists — Update Items

```
PUT https://api.themoviedb.org/4/list/{list_id}/items
```

Update an individual item on a list

Operation id: `list-update-items`

**Parameters:** none.

**Request body** (`application/json`)

```json
{
  "items": [
    {
      "media_type": "movie",
      "media_id": 194662,
      "comment": "Amazing movie!"
    },
    {
      "media_type": "movie",
      "media_id": 76203,
      "comment": "Wow."
    }
  ]
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | Success. |
| `results` | array<object> | - |
| `results[].media_type` | string | movie |
| `results[].media_id` | integer | 194662 |
| `results[].success` | boolean | true |
| `success` | boolean | true |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "status_message": "Success.",
  "results": [
    {
      "media_type": "movie",
      "media_id": 194662,
      "success": true
    }
  ],
  "success": true,
  "status_code": 1
}
```

</details>


#### Lists — Remove Items

```
DELETE https://api.themoviedb.org/4/list/{list_id}/items
```

Remove items from a list

Operation id: `list-remove-items`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `list_id` | path | integer (int32) | yes | - | - |

**Request body** (`application/json`)

```json
{
  "items": [
    {
      "media_type": "movie",
      "media_id": 194662
    },
    {
      "media_type": "movie",
      "media_id": 76203
    },
    "… 1 more items"
  ]
}
```

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `status_message` | string | Success. |
| `results` | array<object> | - |
| `results[].media_type` | string | movie |
| `results[].media_id` | integer | 194662 |
| `results[].success` | boolean | true |
| `success` | boolean | true |
| `status_code` | integer | 1 |

<details><summary>Example response</summary>

```json
{
  "status_message": "Success.",
  "results": [
    {
      "media_type": "movie",
      "media_id": 194662,
      "success": true
    }
  ],
  "success": true,
  "status_code": 1
}
```

</details>



### Account

#### Account — Lists

```
GET https://api.themoviedb.org/4/account/{account_object_id}/lists
```

Get the custom lists that a user has created.

Operation id: `account-lists`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].account_object_id` | string | 4bc8892a017a3c0f92000002 |
| `results[].adult` | integer | 0 |
| `results[].average_rating` | number | 7.90183 |
| `results[].created_at` | string | 2019-08-27 15:13:15 |
| `results[].description` | string | - |
| `results[].featured` | integer | 0 |
| `results[].id` | integer | 120174 |
| `results[].iso_3166_1` | string | US |
| `results[].iso_639_1` | string | en |
| `results[].name` | string | Test Alpha Sort |
| `results[].number_of_items` | integer | 6 |
| `results[].public` | integer | 0 |
| `results[].revenue` | string | 586453267 |
| `results[].runtime` | integer | 644 |
| `results[].sort_by` | integer | 7 |
| `results[].updated_at` | string | 2023-05-05 16:49:11 |
| `total_pages` | integer | 2 |
| `total_results` | integer | 25 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "account_object_id": "4bc8892a017a3c0f92000002",
      "adult": 0,
      "average_rating": 7.90183,
      "created_at": "2019-08-27 15:13:15",
      "description": "",
      "featured": 0,
      "id": 120174,
      "iso_3166_1": "US",
      "iso_639_1": "en",
      "name": "Test Alpha Sort",
      "number_of_items": 6,
      "public": 0,
      "revenue": "586453267",
      "runtime": 644,
      "sort_by": 7,
      "updated_at": "2023-05-05 16:49:11"
    },
    {
      "account_object_id": "4bc8892a017a3c0f92000002",
      "adult": 0,
      "average_rating": 0,
      "backdrop_path": "/ew5FcYiRhTYNJAkxoVPMNlCOdVn.jpg",
      "created_at": "2022-10-06 15:34:43",
      "description": "",
      "featured": 0,
      "id": 8221116,
      "iso_3166_1": "US",
      "iso_639_1": "en",
      "name": "Test List",
      "number_of_items": 4,
      "public": 0,
      "revenue": "0",
      "runtime": 0,
      "sort_by": 1,
      "updated_at": "2023-01-23 22:13:47"
    },
    "… 18 more items"
  ],
  "total_pages": 2,
  "total_results": 25
}
```

</details>


#### Account — Favorite Movies

```
GET https://api.themoviedb.org/4/account/{account_object_id}/movie/favorites
```

Get a users list of favourite movies.

Operation id: `account-favorite-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /se5Hxz7PArQZOG3Nx2bpfOhLhtV.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 9806 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Incredibles |
| `results[].overview` | string | Bob Parr has given up his superhero days to… |
| `results[].popularity` | number | 67.887 |
| `results[].poster_path` | string | /2LqaLgk4Z226KkgPJuiOQ58wvrm.jpg |
| `results[].release_date` | string | 2004-10-27 |
| `results[].title` | string | The Incredibles |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 7.702 |
| `results[].vote_count` | integer | 16188 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/se5Hxz7PArQZOG3Nx2bpfOhLhtV.jpg",
      "genre_ids": [
        28,
        12,
        16,
        10751
      ],
      "id": 9806,
      "original_language": "en",
      "original_title": "The Incredibles",
      "overview": "Bob Parr has given up his superhero days to log in time as an insurance adjuster and raise his three children with his formerly heroic wife in suburbia. But when he receives a mysterious assignment, it's time to get back into costume.",
      "popularity": 67.887,
      "poster_path": "/2LqaLgk4Z226KkgPJuiOQ58wvrm.jpg",
      "release_date": "2004-10-27",
      "title": "The Incredibles",
      "video": false,
      "vote_average": 7.702,
      "vote_count": 16188
    },
    {
      "adult": false,
      "backdrop_path": "/8eRscFbRYl681zDfkjv1jjW1KAA.jpg",
      "genre_ids": [
        878,
        28,
        12
      ],
      "id": 1452,
      "original_language": "en",
      "original_title": "Superman Returns",
      "overview": "Superman returns to discover his 5-year absence has allowed Lex Luthor to walk free, and that those he was closest to felt abandoned and have moved on. Luthor plots his ultimate revenge that could see millions killed and change the face ...",
      "popularity": 19.738,
      "poster_path": "/385XwTQZDpRX2d3kxtnpiLrjBXw.jpg",
      "release_date": "2006-06-28",
      "title": "Superman Returns",
      "video": false,
      "vote_average": 5.723,
      "vote_count": 3722
    },
    "… 18 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>


#### Account — Rated Movies

```
GET https://api.themoviedb.org/4/account/{account_object_id}/movie/rated
```

Get a users rated movies.

Operation id: `account-rated-movies`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /dUVbWINfRMGojGZRcO6GF1Z2nV8.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 120 |
| `results[].original_language` | string | en |
| `results[].original_title` | string | The Lord of the Rings: The Fellowship of th… |
| `results[].overview` | string | Young hobbit Frodo Baggins, after inheritin… |
| `results[].popularity` | number | 79.298 |
| `results[].poster_path` | string | /6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg |
| `results[].release_date` | string | 2001-12-18 |
| `results[].title` | string | The Lord of the Rings: The Fellowship of th… |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.4 |
| `results[].vote_count` | integer | 22626 |
| `results[].account_rating` | object | - |
| `results[].account_rating.created_at` | string | 2012-02-15T15:18:04.000Z |
| `results[].account_rating.value` | integer | 8 |
| `total_pages` | integer | 47 |
| `total_results` | integer | 940 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/dUVbWINfRMGojGZRcO6GF1Z2nV8.jpg",
      "genre_ids": [
        12,
        14,
        28
      ],
      "id": 120,
      "original_language": "en",
      "original_title": "The Lord of the Rings: The Fellowship of the Ring",
      "overview": "Young hobbit Frodo Baggins, after inheriting a mysterious ring from his uncle Bilbo, must leave his home in order to keep it from falling into the hands of its evil creator. Along the way, a fellowship is formed to protect the ringbearer...",
      "popularity": 79.298,
      "poster_path": "/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
      "release_date": "2001-12-18",
      "title": "The Lord of the Rings: The Fellowship of the Ring",
      "video": false,
      "vote_average": 8.4,
      "vote_count": 22626,
      "account_rating": {
        "created_at": "2012-02-15T15:18:04.000Z",
        "value": 8.0
      }
    },
    {
      "adult": false,
      "backdrop_path": "/YL3GPOiDcNraIJOVDCZsoOBoDy.jpg",
      "genre_ids": [
        878,
        28,
        12,
        53
      ],
      "id": 106,
      "original_language": "en",
      "original_title": "Predator",
      "overview": "A team of elite commandos on a secret mission in a Central American jungle come to find themselves hunted by an extraterrestrial warrior.",
      "popularity": 61.013,
      "poster_path": "/9XibNLfmUWCg0PPydmyoCl1KxvF.jpg",
      "release_date": "1987-06-12",
      "title": "Predator",
      "video": false,
      "vote_average": 7.491,
      "vote_count": 6958,
      "account_rating": {
        "created_at": "2012-02-15T15:18:04.000Z",
        "value": 6.0
      }
    },
    "… 15 more items"
  ],
  "total_pages": 47,
  "total_results": 940
}
```

</details>


#### Account — Recommended Movies

```
GET https://api.themoviedb.org/4/account/{account_object_id}/movie/recommendations
```

Get a users list of recommended movies.

Operation id: `account-movie-recommendations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /9sfVyE3sP2dkCwDyV7UlYP5TAAR.jpg |
| `results[].id` | integer | 823754 |
| `results[].title` | string | Bo Burnham: Inside |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Bo Burnham: Inside |
| `results[].overview` | string | Stuck in COVID-19 lockdown, US comedian and… |
| `results[].poster_path` | string | /ku1UvTWYvhFQbSesOD6zteY7bXT.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 11.904 |
| `results[].release_date` | string | 2021-07-22 |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.178 |
| `results[].vote_count` | integer | 352 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/9sfVyE3sP2dkCwDyV7UlYP5TAAR.jpg",
      "id": 823754,
      "title": "Bo Burnham: Inside",
      "original_language": "en",
      "original_title": "Bo Burnham: Inside",
      "overview": "Stuck in COVID-19 lockdown, US comedian and musician Bo Burnham attempts to stay sane and happy by writing, shooting and performing a one-man comedy special.",
      "poster_path": "/ku1UvTWYvhFQbSesOD6zteY7bXT.jpg",
      "media_type": "movie",
      "genre_ids": [
        35,
        18
      ],
      "popularity": 11.904,
      "release_date": "2021-07-22",
      "video": false,
      "vote_average": 8.178,
      "vote_count": 352
    },
    {
      "adult": false,
      "backdrop_path": "/4XKqxgjRJs3YtH5j1Eli5y6gtno.jpg",
      "id": 542178,
      "title": "The French Dispatch",
      "original_language": "en",
      "original_title": "The French Dispatch",
      "overview": "The staff of an American magazine based in France puts out its last issue, with stories featuring an artist sentenced to life imprisonment, student riots, and a kidnapping resolved by a chef.",
      "poster_path": "/pMTMSKBEe5sMg1ZxWCy9YWLuyln.jpg",
      "media_type": "movie",
      "genre_ids": [
        35,
        18,
        10749
      ],
      "popularity": 25.628,
      "release_date": "2021-10-21",
      "video": false,
      "vote_average": 7.152,
      "vote_count": 2015
    },
    "… 19 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>


#### Account — Watchlist Movies

```
GET https://api.themoviedb.org/4/account/{account_object_id}/movie/watchlist
```

Get a users movie watchlist.

Operation id: `account-movie-watchlist`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /9sfVyE3sP2dkCwDyV7UlYP5TAAR.jpg |
| `results[].id` | integer | 823754 |
| `results[].title` | string | Bo Burnham: Inside |
| `results[].original_language` | string | en |
| `results[].original_title` | string | Bo Burnham: Inside |
| `results[].overview` | string | Stuck in COVID-19 lockdown, US comedian and… |
| `results[].poster_path` | string | /ku1UvTWYvhFQbSesOD6zteY7bXT.jpg |
| `results[].media_type` | string | movie |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 11.904 |
| `results[].release_date` | string | 2021-07-22 |
| `results[].video` | boolean | false |
| `results[].vote_average` | number | 8.178 |
| `results[].vote_count` | integer | 352 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/9sfVyE3sP2dkCwDyV7UlYP5TAAR.jpg",
      "id": 823754,
      "title": "Bo Burnham: Inside",
      "original_language": "en",
      "original_title": "Bo Burnham: Inside",
      "overview": "Stuck in COVID-19 lockdown, US comedian and musician Bo Burnham attempts to stay sane and happy by writing, shooting and performing a one-man comedy special.",
      "poster_path": "/ku1UvTWYvhFQbSesOD6zteY7bXT.jpg",
      "media_type": "movie",
      "genre_ids": [
        35,
        18
      ],
      "popularity": 11.904,
      "release_date": "2021-07-22",
      "video": false,
      "vote_average": 8.178,
      "vote_count": 352
    },
    {
      "adult": false,
      "backdrop_path": "/4XKqxgjRJs3YtH5j1Eli5y6gtno.jpg",
      "id": 542178,
      "title": "The French Dispatch",
      "original_language": "en",
      "original_title": "The French Dispatch",
      "overview": "The staff of an American magazine based in France puts out its last issue, with stories featuring an artist sentenced to life imprisonment, student riots, and a kidnapping resolved by a chef.",
      "poster_path": "/pMTMSKBEe5sMg1ZxWCy9YWLuyln.jpg",
      "media_type": "movie",
      "genre_ids": [
        35,
        18,
        10749
      ],
      "popularity": 25.628,
      "release_date": "2021-10-21",
      "video": false,
      "vote_average": 7.152,
      "vote_count": 2015
    },
    "… 19 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>


#### Account — Favorite TV Shows

```
GET https://api.themoviedb.org/4/account/{account_object_id}/tv/favorites
```

Get a users list of favourite TV shows.

Operation id: `account-favorite-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1396 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Breaking Bad |
| `results[].overview` | string | When Walter White, a New Mexico chemistry t… |
| `results[].popularity` | number | 255.118 |
| `results[].poster_path` | string | /ggFHVNu6YYI5L9pCfOacjizRGt.jpg |
| `results[].first_air_date` | string | 2008-01-20 |
| `results[].name` | string | Breaking Bad |
| `results[].vote_average` | number | 8.879 |
| `results[].vote_count` | integer | 11625 |
| `total_pages` | integer | 4 |
| `total_results` | integer | 68 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
      "genre_ids": [
        18,
        80
      ],
      "id": 1396,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Breaking Bad",
      "overview": "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of only two years left to live. He becomes filled with a sense of fearlessness and an unrelenting desire to secure his family's f...",
      "popularity": 255.118,
      "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
      "first_air_date": "2008-01-20",
      "name": "Breaking Bad",
      "vote_average": 8.879,
      "vote_count": 11625
    },
    {
      "adult": false,
      "backdrop_path": "/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
      "genre_ids": [
        10765,
        18,
        10759
      ],
      "id": 1399,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Game of Thrones",
      "overview": "Seven noble families fight for control of the mythical land of Westeros. Friction between the houses leads to full-scale war. All while a very ancient evil awakens in the farthest north. Amidst the war, a neglected military order of misf...",
      "popularity": 269.397,
      "poster_path": "/7WUHnWGx5OO145IRxPDUkQSh4C7.jpg",
      "first_air_date": "2011-04-17",
      "name": "Game of Thrones",
      "vote_average": 8.437,
      "vote_count": 21053
    },
    "… 18 more items"
  ],
  "total_pages": 4,
  "total_results": 68
}
```

</details>


#### Account — Rated TV Shows

```
GET https://api.themoviedb.org/4/account/{account_object_id}/tv/rated
```

Get a users rated TV shows.

Operation id: `account-rated-tv`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /2yZXtM2Kky1Sy0kachbDlwybl3y.jpg |
| `results[].genre_ids` | array<integer> | - |
| `results[].id` | integer | 1705 |
| `results[].origin_country` | array<string> | - |
| `results[].original_language` | string | en |
| `results[].original_name` | string | Fringe |
| `results[].overview` | string | FBI Special Agent Olivia Dunham, brilliant … |
| `results[].popularity` | number | 145.5 |
| `results[].poster_path` | string | /sY9hg5dLJ93RJOyKEiu1nAtBRND.jpg |
| `results[].first_air_date` | string | 2008-09-09 |
| `results[].name` | string | Fringe |
| `results[].vote_average` | number | 8.11 |
| `results[].vote_count` | integer | 2053 |
| `results[].account_rating` | object | - |
| `results[].account_rating.created_at` | string | 2013-10-10T21:03:56.499Z |
| `results[].account_rating.value` | integer | 9 |
| `total_pages` | integer | 15 |
| `total_results` | integer | 291 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/2yZXtM2Kky1Sy0kachbDlwybl3y.jpg",
      "genre_ids": [
        10765,
        18,
        9648
      ],
      "id": 1705,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Fringe",
      "overview": "FBI Special Agent Olivia Dunham, brilliant but formerly institutionalized scientist Walter Bishop and his scheming, reluctant son Peter uncover a deadly mystery involving a series of unbelievable events and realize they may be a part of ...",
      "popularity": 145.5,
      "poster_path": "/sY9hg5dLJ93RJOyKEiu1nAtBRND.jpg",
      "first_air_date": "2008-09-09",
      "name": "Fringe",
      "vote_average": 8.11,
      "vote_count": 2053,
      "account_rating": {
        "created_at": "2013-10-10T21:03:56.499Z",
        "value": 9.0
      }
    },
    {
      "adult": false,
      "backdrop_path": "/dGzPJnh8YcUS4J10sNunohaXMVn.jpg",
      "genre_ids": [
        37,
        80,
        18
      ],
      "id": 1406,
      "origin_country": [
        "US"
      ],
      "original_language": "en",
      "original_name": "Deadwood",
      "overview": "The story of the early days of Deadwood, South Dakota; woven around actual historic events with most of the main characters based on real people. Deadwood starts as a gold mining camp and gradually turns from a lawless wild-west communit...",
      "popularity": 34.398,
      "poster_path": "/4Yp35DVbVOAWkfQUIQ7pbh3u0aN.jpg",
      "first_air_date": "2004-03-21",
      "name": "Deadwood",
      "vote_average": 8.094,
      "vote_count": 673,
      "account_rating": {
        "created_at": "2013-10-10T21:04:10.564Z",
        "value": 8.0
      }
    },
    "… 18 more items"
  ],
  "total_pages": 15,
  "total_results": 291
}
```

</details>


#### Account — Recommended TV Shows

```
GET https://api.themoviedb.org/4/account/{account_object_id}/tv/recommendations
```

Get a users list of recommended TV shows.

Operation id: `account-tv-recommendations`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /7bsHAsS1RDtslictkApeb7cedLL.jpg |
| `results[].id` | integer | 152483 |
| `results[].name` | string | The Boys Presents: Diabolical |
| `results[].original_language` | string | en |
| `results[].original_name` | string | The Boys Presents: Diabolical |
| `results[].overview` | string | From some of the most unhinged and maniacal… |
| `results[].poster_path` | string | /kZKfZWwFOAicgoKS2IO7oM1GuHZ.jpg |
| `results[].media_type` | string | tv |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 24.596 |
| `results[].first_air_date` | string | 2022-03-03 |
| `results[].vote_average` | number | 7.201 |
| `results[].vote_count` | integer | 214 |
| `results[].origin_country` | array<string> | - |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/7bsHAsS1RDtslictkApeb7cedLL.jpg",
      "id": 152483,
      "name": "The Boys Presents: Diabolical",
      "original_language": "en",
      "original_name": "The Boys Presents: Diabolical",
      "overview": "From some of the most unhinged and maniacal minds in Hollywood today comes this animated anthology series, a collection of irreverent and emotionally shocking animated short films. Each episode plunges elbow-deep into unseen crevices of ...",
      "poster_path": "/kZKfZWwFOAicgoKS2IO7oM1GuHZ.jpg",
      "media_type": "tv",
      "genre_ids": [
        16,
        10759
      ],
      "popularity": 24.596,
      "first_air_date": "2022-03-03",
      "vote_average": 7.201,
      "vote_count": 214,
      "origin_country": [
        "US"
      ]
    },
    {
      "adult": false,
      "backdrop_path": "/fg9psAEDJXX0svqSd3h6KNiWPBE.jpg",
      "id": 95480,
      "name": "Slow Horses",
      "original_language": "en",
      "original_name": "Slow Horses",
      "overview": "This quick-witted spy drama follows a dysfunctional team of MI5 agents—and their obnoxious boss, the notorious Jackson Lamb—as they navigate the espionage world’s smoke and mirrors to defend England from sinister forces.",
      "poster_path": "/8FM8KCgZQSbLLXcZew0MSj6so1k.jpg",
      "media_type": "tv",
      "genre_ids": [
        18,
        80
      ],
      "popularity": 90.931,
      "first_air_date": "2022-04-01",
      "vote_average": 7.7,
      "vote_count": 171,
      "origin_country": [
        "GB"
      ]
    },
    "… 19 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>


#### Account — Watchlist TV Shows

```
GET https://api.themoviedb.org/4/account/{account_object_id}/tv/watchlist
```

Get a users TV watchlist.

Operation id: `account-tv-watchlist`

**Parameters**

| Name | In | Type | Required | Default | Notes |
|---|---|---|---|---|---|
| `account_object_id` | path | string | yes | `<<account_object_id>>` | - |
| `page` | query | integer (int32) | - | `1` | - |
| `language` | query | string | - | `en-US` | - |
| `sort_by` | query | string | - | `created_at.asc` | One of: `created_at.asc`, `created_at.desc` |

**Response `200`**

| Field | Type | Example |
|---|---|---|
| `page` | integer | 1 |
| `results` | array<object> | - |
| `results[].adult` | boolean | false |
| `results[].backdrop_path` | string | /7bsHAsS1RDtslictkApeb7cedLL.jpg |
| `results[].id` | integer | 152483 |
| `results[].name` | string | The Boys Presents: Diabolical |
| `results[].original_language` | string | en |
| `results[].original_name` | string | The Boys Presents: Diabolical |
| `results[].overview` | string | From some of the most unhinged and maniacal… |
| `results[].poster_path` | string | /kZKfZWwFOAicgoKS2IO7oM1GuHZ.jpg |
| `results[].media_type` | string | tv |
| `results[].genre_ids` | array<integer> | - |
| `results[].popularity` | number | 24.596 |
| `results[].first_air_date` | string | 2022-03-03 |
| `results[].vote_average` | number | 7.201 |
| `results[].vote_count` | integer | 214 |
| `results[].origin_country` | array<string> | - |
| `total_pages` | integer | 4 |
| `total_results` | integer | 80 |

<details><summary>Example response</summary>

```json
{
  "page": 1,
  "results": [
    {
      "adult": false,
      "backdrop_path": "/7bsHAsS1RDtslictkApeb7cedLL.jpg",
      "id": 152483,
      "name": "The Boys Presents: Diabolical",
      "original_language": "en",
      "original_name": "The Boys Presents: Diabolical",
      "overview": "From some of the most unhinged and maniacal minds in Hollywood today comes this animated anthology series, a collection of irreverent and emotionally shocking animated short films. Each episode plunges elbow-deep into unseen crevices of ...",
      "poster_path": "/kZKfZWwFOAicgoKS2IO7oM1GuHZ.jpg",
      "media_type": "tv",
      "genre_ids": [
        16,
        10759
      ],
      "popularity": 24.596,
      "first_air_date": "2022-03-03",
      "vote_average": 7.201,
      "vote_count": 214,
      "origin_country": [
        "US"
      ]
    },
    {
      "adult": false,
      "backdrop_path": "/fg9psAEDJXX0svqSd3h6KNiWPBE.jpg",
      "id": 95480,
      "name": "Slow Horses",
      "original_language": "en",
      "original_name": "Slow Horses",
      "overview": "This quick-witted spy drama follows a dysfunctional team of MI5 agents—and their obnoxious boss, the notorious Jackson Lamb—as they navigate the espionage world’s smoke and mirrors to defend England from sinister forces.",
      "poster_path": "/8FM8KCgZQSbLLXcZew0MSj6so1k.jpg",
      "media_type": "tv",
      "genre_ids": [
        18,
        80
      ],
      "popularity": 90.931,
      "first_air_date": "2022-04-01",
      "vote_average": 7.7,
      "vote_count": 171,
      "origin_country": [
        "GB"
      ]
    },
    "… 19 more items"
  ],
  "total_pages": 4,
  "total_results": 80
}
```

</details>

