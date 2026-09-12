# TMDB fixtures

Recorded TMDB responses, each written exactly as the API sent it. A test reads them through the
`TmdbFixture` enum beside them.

`scripts/record-fixtures.py` re-records everything here. It reads the token from
`local.properties`, refuses to run without one, and prints which files changed.

## What each file is

| File | Is |
|---|---|
| `discover/page-1.json` to `page-6.json` | Six sequential pages of `/discover/movie` under `popularity.desc`, one run |
| `genres.json` | `/genre/movie/list`, the evidence the app's genre table is total over what TMDB sends |
| `movie/released.json` | A released movie with every field populated |
| `movie/in-production.json` | A movie in production: empty tagline, zero budget, revenue and votes |
| `movie/post-production.json` | A movie in post production: no financials, no votes |
| `error/401.json` | TMDB's body for an invalid token |
| `error/404.json` | TMDB's body for an id it does not issue |

The three movies are one per shape the detail screen renders. Each file is named for its shape
and the recorder holds the id behind it. A re-record checks nothing about that shape: if TMDB
updates one of them, the test that reads it fails and a different id has to be picked.

The discover pages are recorded under the ordering the app opens on, so a walk over them is the
walk the app makes. Paging there is stable: page one repeated is identical and adjacent pages share
no movie, so no recording has to reproduce a repeat.

## What is not recorded

There is no 500. A healthy API cannot be asked for one, so that status is a value the test engine
returns with no body. That is the point every error fixture makes: the status decides, and an error
body never parses into a movie.
