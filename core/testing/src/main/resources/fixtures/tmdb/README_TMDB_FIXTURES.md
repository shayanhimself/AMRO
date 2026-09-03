# TMDB fixtures

Recorded TMDB responses, each written exactly as the API sent it. A test reads them through the
`TmdbFixture` enum in `:core:testing`.

`scripts/record-fixtures.py` re-records everything here except the assembled pages. It reads the
token from `local.properties`, refuses to run without one, and prints which files changed.

## What each file is

| File | Is |
|---|---|
| `trending/page-1.json` to `page-6.json` | Six sequential pages of `/trending/movie/week`, one run |
| `trending/overlap/page-1.json`, `page-2.json` | Two pages assembled from those recordings, see below |
| `genres.json` | `/genre/movie/list`, the evidence the app's genre table is total over what TMDB sends |
| `movie/released.json` | A released movie with every field populated |
| `movie/in-production.json` | A movie in production: empty tagline, zero budget, revenue and votes |
| `movie/post-production.json` | A movie in post production: no financials, no votes |
| `error/401.json` | TMDB's body for an invalid token |
| `error/404.json` | TMDB's body for an id it does not issue |

The three movies are one per shape the detail screen renders. Each file is named for its shape
and the recorder holds the id behind it. A re-record checks nothing about that shape: if TMDB
updates one of them, the test that reads it fails and a different id has to be picked.

## The assembled pages

TMDB re-ranks its trending list between requests, so a movie sitting on a page boundary is served on
two sequential pages while another is missed. The walk that fetches a hundred distinct movies has
to drop the repeat, and that needs a fixture that repeats.

A recording only reproduces this on a day TMDB happens to re-rank mid-run, so the two pages under
`overlap/` are built instead: `overlap/page-2.json` opens with the last three rows of
`overlap/page-1.json`, and the rest of it is `trending/page-2.json`'s own rows. Every row is a real
recorded row. Forty rows, thirty-seven distinct.

The recorder leaves them alone, because a re-record would quietly disarm the test that reads them.

## What is not recorded

There is no 500. A healthy API cannot be asked for one, so that status is a value the test engine
returns with no body. That is the point every error fixture makes: the status decides, and an error
body never parses into a movie.
