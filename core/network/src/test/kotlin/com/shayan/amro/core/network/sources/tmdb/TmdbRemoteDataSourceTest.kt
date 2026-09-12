package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.sources.MovieId
import com.shayan.amro.core.network.sources.tmdb.fixture.TmdbFixture
import com.shayan.amro.core.network.sources.tmdb.fixture.TmdbRecording
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TOKEN = "test-read-access-token"

private const val DISCOVER_PATH_SEGMENT = "discover/movie"
private const val PAGE_PARAMETER = "page"
private const val SORT_BY_PARAMETER = "sort_by"
private const val WITH_GENRES_PARAMETER = "with_genres"
private const val FIRST_PAGE = 1

/** A whole number of provider pages, so a walk to it takes exactly that many requests. */
private const val WANTED = 60

/** How many provider pages [WANTED] movies take. */
private const val PAGES_FOR_WANTED = 3

/** A count between two page boundaries, which whole pages can only run past. */
private const val WANTED_MID_PAGE = 25

private const val EMPTY_RESULTS = """{"page":1,"results":[],"total_pages":0}"""
private const val NOT_JSON = "<html>a gateway said no</html>"

private const val KNOWN_MOVIE_ID = "687163"

class TmdbRemoteDataSourceTest {
    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `a walk concatenates pages in order until it holds the count`() =
        runTest {
            val result = pagingSource().getMovies(MovieSelection(), WANTED, after = null)

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(WANTED, page.movies.size)
            assertEquals(
                (FIRST_PAGE..PAGES_FOR_WANTED).map { it.toString() },
                requests.map { it.url.parameters[PAGE_PARAMETER] },
            )
        }

    @Test
    fun `a walk keeps every movie of the page that carried it past the count`() =
        runTest {
            val result = pagingSource().getMovies(MovieSelection(), WANTED_MID_PAGE, after = null)

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(TMDB_PAGE_SIZE * 2, page.movies.size)
        }

    @Test
    fun `a walk resumes on the page its cursor names`() =
        runTest {
            val resumed = pagingSource().getMovies(MovieSelection(), WANTED, after = Cursor("4"))

            assertIs<NetworkResult.Success<MoviePage>>(resumed)
            assertEquals(
                listOf("4", "5", "6"),
                requests.map { it.url.parameters[PAGE_PARAMETER] },
            )
        }

    @Test
    fun `a cursor past the last page TMDB answers ends pagination without a request`() =
        runTest {
            val result =
                pagingSource().getMovies(
                    selection = MovieSelection(),
                    count = WANTED,
                    after = Cursor((TMDB_LAST_PAGE + 1).toString()),
                )

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(emptyList(), page.movies)
            assertNull(page.next)
            assertEquals(emptyList(), requests)
        }

    @Test
    fun `an empty page ends pagination with what the walk already held`() =
        runTest {
            val result =
                pagingSource().getMovies(
                    MovieSelection(),
                    count = Int.MAX_VALUE,
                    after = null,
                )

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(TmdbFixture.Discover.entries.size * TMDB_PAGE_SIZE, page.movies.size)
            assertNull(page.next)
        }

    @Test
    fun `an empty first page is a list with nothing in it rather than a failure`() =
        runTest {
            val result =
                dataSource { respondJson(EMPTY_RESULTS) }
                    .getMovies(MovieSelection(), WANTED, after = null)

            val page = assertIs<NetworkResult.Success<MoviePage>>(result).value
            assertEquals(emptyList(), page.movies)
            assertNull(page.next)
        }

    @Test
    fun `a page that fails after the first fails the call rather than returning a part of it`() =
        runTest {
            val result =
                failingAfterFirstPageSource().getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `every endpoint resolves under the API root`() =
        runTest {
            pagingSource().getMovies(MovieSelection(), TMDB_PAGE_SIZE, after = null)

            val url = requests.first().url.toString()

            assertTrue(url.startsWith(TMDB_BASE_URL + DISCOVER_PATH_SEGMENT), url)
        }

    @Test
    fun `a selection reaches the request as the ordering and the genres it names`() =
        runTest {
            val selection = MovieSelection(genres = setOf(Genre.COMEDY, Genre.HORROR))

            pagingSource().getMovies(selection, TMDB_PAGE_SIZE, after = null)

            val parameters = requests.first().url.parameters
            assertEquals(FIRST_PAGE.toString(), parameters[PAGE_PARAMETER])
            assertEquals("popularity.desc", parameters[SORT_BY_PARAMETER])
            assertEquals("27|35", parameters[WITH_GENRES_PARAMETER])
        }

    @Test
    fun `an unfiltered selection sends no genre parameter`() =
        runTest {
            pagingSource().getMovies(MovieSelection(), TMDB_PAGE_SIZE, after = null)

            assertNull(requests.first().url.parameters[WITH_GENRES_PARAMETER])
        }

    @Test
    fun `a recorded detail becomes a movie detail`() =
        runTest {
            val result =
                dataSource { respondJson(TmdbFixture.Movie.RELEASED) }
                    .getMovieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID).qualified)

            assertIs<NetworkResult.Success<*>>(result)
            assertTrue(
                requests
                    .single()
                    .url
                    .toString()
                    .endsWith(KNOWN_MOVIE_ID),
            )
        }

    @Test
    fun `a dropped connection is no connectivity`() =
        runTest {
            val result =
                dataSource { throw IOException("dropped") }
                    .getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `an unresolved host is no connectivity rather than a server fault`() =
        runTest {
            val result =
                dataSource { throw UnresolvedAddressException() }
                    .getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a refused status fails a page even when the body would have parsed`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Discover.PAGE_1, HttpStatusCode.Unauthorized)
                }.getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `the recorded unauthorised body never becomes movies`() =
        runTest {
            val result =
                dataSource { respondJson(TmdbFixture.Error.UNAUTHORISED) }
                    .getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a server error status is a server failure`() =
        runTest {
            val result =
                dataSource {
                    respondJson(EMPTY_RESULTS, HttpStatusCode.InternalServerError)
                }.getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a refused status fails a detail even when the body would have parsed`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Movie.RELEASED, HttpStatusCode.NotFound)
                }.getMovieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID).qualified)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `the recorded not found body never becomes a movie detail`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Error.NOT_FOUND)
                }.getMovieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID).qualified)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a body that cannot be parsed is a server failure`() =
        runTest {
            val result =
                dataSource { respondJson(NOT_JSON) }
                    .getMovies(MovieSelection(), WANTED, after = null)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a cancelled call is rethrown rather than returned as a failure`() =
        runTest {
            assertFailsWith<CancellationException> {
                dataSource { throw CancellationException("navigated away") }
                    .getMovies(MovieSelection(), WANTED, after = null)
            }
        }

    @Test
    fun `every request carries the credential as a bearer header and never in the URL`() =
        runTest {
            dataSource { respondJson(TmdbFixture.Discover.PAGE_1) }
                .getMovies(MovieSelection(), TMDB_PAGE_SIZE, after = null)

            val request = requests.single()
            assertEquals("Bearer $TOKEN", request.headers[HttpHeaders.Authorization])
            assertFalse(request.url.toString().contains(TOKEN), "the credential is in the URL")
        }

    /** A source answering each successive request with the next recorded page, then with none. */
    private fun pagingSource() = sequenceSource(TmdbFixture.Discover.entries)

    /**
     * A source that serves [pages] in order and answers every request past them with an empty
     * page, which is how TMDB says it has no more to give.
     */
    private fun sequenceSource(pages: List<TmdbRecording>): TmdbRemoteDataSource {
        var served = 0
        return dataSource {
            val body = pages.getOrNull(served)?.json ?: EMPTY_RESULTS
            served++
            respondJson(body)
        }
    }

    /** A source that answers the first page and drops the connection on the one after it. */
    private fun failingAfterFirstPageSource(): TmdbRemoteDataSource {
        var request = 0
        return dataSource {
            request++
            if (request == FIRST_PAGE) {
                respondJson(TmdbFixture.Discover.PAGE_1)
            } else {
                throw IOException("dropped")
            }
        }
    }

    /**
     * A source whose every request is answered by [handler].
     */
    private fun dataSource(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): TmdbRemoteDataSource {
        val engine =
            MockEngine { request ->
                requests += request
                handler(request)
            }
        val client =
            tmdbHttpClient(
                config = TmdbConfig(readAccessToken = TOKEN),
                engine = engine,
            )
        return TmdbRemoteDataSource(TmdbApi(client))
    }

    /**
     * Answers one request with [body], the way TMDB would have.
     * Ktor's [MockEngine] calls this in place of a socket.
     *
     * @param body the response body, usually a recorded [TmdbRecording].
     * @param status the status to answer with. An error status carries a body too.
     */
    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    /**
     * Answers one request with what TMDB sent when [fixture] was recorded.
     *
     * @param status the status to answer under, which a recorded error body is paired with.
     */
    private fun MockRequestHandleScope.respondJson(
        fixture: TmdbRecording,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respondJson(fixture.json, status)
}
