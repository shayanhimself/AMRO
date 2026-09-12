#!/usr/bin/env python3
"""Fetch every page of TMDB's trending/movie/week endpoint and sort the
combined result set by popularity, descending.

Reads the read access token from local.properties, pages through the
endpoint (it caps at 500 pages / 10000 results), and writes the sorted
result set as JSON so a report can be built from it.
"""

import concurrent.futures
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

TMDB_TRENDING_MAX_PAGE = 500
REQUEST_TIMEOUT_SECONDS = 30
CONCURRENT_REQUESTS = 10

REPO_ROOT = Path(__file__).resolve().parent.parent
LOCAL_PROPERTIES_PATH = REPO_ROOT / "local.properties"


def read_access_token() -> str:
    """Reads the TMDB read access token out of local.properties."""
    text = LOCAL_PROPERTIES_PATH.read_text()
    match = re.search(r"^tmdb\.readAccessToken=(.+)$", text, re.MULTILINE)
    if not match:
        raise RuntimeError(f"tmdb.readAccessToken not found in {LOCAL_PROPERTIES_PATH}")
    return match.group(1).strip()


def fetch_page(token: str, page: int) -> dict:
    """Fetches one page of /trending/movie/week."""
    url = f"https://api.themoviedb.org/3/trending/movie/week?language=en-US&page={page}"
    request = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "accept": "application/json",
        },
    )
    with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
        return json.load(response)


def fetch_all_pages(token: str) -> list[dict]:
    """Fetches every page of trending movies, in page order."""
    pages: dict[int, dict] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as pool:
        futures = {
            pool.submit(fetch_page, token, page): page
            for page in range(1, TMDB_TRENDING_MAX_PAGE + 1)
        }
        for future in concurrent.futures.as_completed(futures):
            page = futures[future]
            try:
                pages[page] = future.result()
            except urllib.error.HTTPError as error:
                raise RuntimeError(f"page {page} failed: HTTP {error.code} {error.read()}") from error

    movies = []
    for page in sorted(pages):
        movies.extend(pages[page]["results"])
    return movies


def dedupe_by_id(movies: list[dict]) -> list[dict]:
    """Drops repeat entries for the same movie id, keeping the first seen.

    Trending's paging overlaps once total_pages runs into the hundreds, so
    the same movie can land on more than one page.
    """
    seen_ids: set[int] = set()
    unique_movies = []
    for movie in movies:
        if movie["id"] in seen_ids:
            continue
        seen_ids.add(movie["id"])
        unique_movies.append(movie)
    return unique_movies


def main() -> None:
    if len(sys.argv) != 2:
        print(f"usage: {sys.argv[0]} <output-json-path>", file=sys.stderr)
        sys.exit(1)
    output_path = Path(sys.argv[1])

    token = read_access_token()
    movies = dedupe_by_id(fetch_all_pages(token))
    movies.sort(key=lambda movie: movie["popularity"], reverse=True)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(movies, indent=2))
    print(f"{len(movies)} movies fetched and sorted by popularity into {output_path}")


if __name__ == "__main__":
    main()
