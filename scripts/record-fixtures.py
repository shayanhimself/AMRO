#!/usr/bin/env python3
"""Record the TMDB responses the JVM tests assert against.

Every fixture is a response body written exactly as TMDB sent it, so a test reads the shapes the
provider actually produces rather than the ones its author remembered.

The read access token comes from local.properties, which is not in the repository. The script
refuses to run without one rather than recording an unauthorised body over a real fixture.

The assembled fixtures under trending/overlap are never touched: they are built from
recorded rows so the deduplication test fails for the right reason on every run, and a recording
would only reproduce duplicate ids on a day TMDB happens to re-rank mid-sequence.
"""

import json
import pathlib
import sys
import urllib.error
import urllib.request

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
LOCAL_PROPERTIES = REPO_ROOT / "local.properties"
FIXTURES = REPO_ROOT / "core/network/src/test/resources/fixtures/tmdb"

TOKEN_KEY = "tmdb.readAccessToken"
BASE_URL = "https://api.themoviedb.org/3/"

TRENDING_PAGES = 6

# One movie per shape the detail screen has to render, keyed by the file it is recorded into.
MOVIE_IDS = {
    "released": 687163,
    "in-production": 1294189,
    "post-production": 1003596,
}

# An id TMDB does not issue, which is how a 404 body is recorded. There is no way to ask a healthy
# API for a 500, so that status is synthesised by the test engine and has no fixture.
UNKNOWN_MOVIE_ID = 1
INVALID_TOKEN = "not-a-token"


def read_token() -> str:
    if not LOCAL_PROPERTIES.exists():
        sys.exit(f"no {LOCAL_PROPERTIES.name}: nothing to authenticate with")
    for line in LOCAL_PROPERTIES.read_text().splitlines():
        key, separator, value = line.partition("=")
        if separator and key.strip() == TOKEN_KEY:
            token = value.strip()
            if token:
                return token
    sys.exit(f"no {TOKEN_KEY} in {LOCAL_PROPERTIES.name}: nothing to authenticate with")


def fetch(path: str, token: str) -> tuple[int, str]:
    """Reads one endpoint.

    An error status is a body to record rather than a failure, so it is returned like any other.
    """
    request = urllib.request.Request(
        BASE_URL + path,
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()


def write(relative: str, body: str) -> None:
    """Writes one body verbatim and reports whether it differs from what was there."""
    target = FIXTURES / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    before = target.read_text() if target.exists() else None
    target.write_text(body)
    if before is None:
        print(f"  new       {relative}")
    elif before != body:
        print(f"  changed   {relative}")
    else:
        print(f"  unchanged {relative}")


def expect(status: int, path: str, wanted: int) -> None:
    if status != wanted:
        sys.exit(f"{path} answered {status}, expected {wanted}: nothing recorded for it")


def main() -> None:
    token = read_token()
    print(f"recording into {FIXTURES.relative_to(REPO_ROOT)}")

    for page in range(1, TRENDING_PAGES + 1):
        path = f"trending/movie/week?page={page}"
        status, body = fetch(path, token)
        expect(status, path, 200)
        write(f"trending/page-{page}.json", body)

    status, body = fetch("genre/movie/list", token)
    expect(status, "genre/movie/list", 200)
    write("genres.json", body)

    for shape, movie_id in MOVIE_IDS.items():
        path = f"movie/{movie_id}"
        status, body = fetch(path, token)
        expect(status, path, 200)
        write(f"movie/{shape}.json", body)

    status, body = fetch(f"movie/{UNKNOWN_MOVIE_ID}", token)
    expect(status, f"movie/{UNKNOWN_MOVIE_ID}", 404)
    write("error/404.json", body)

    status, body = fetch("trending/movie/week", INVALID_TOKEN)
    expect(status, "trending/movie/week with an invalid token", 401)
    write("error/401.json", body)


if __name__ == "__main__":
    main()
