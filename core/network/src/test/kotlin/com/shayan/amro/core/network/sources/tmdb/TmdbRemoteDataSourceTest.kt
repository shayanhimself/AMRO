package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fixture.tmdb.TmdbFixture
import com.shayan.amro.core.testing.fixture.tmdb.TmdbRecording
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
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
import kotlin.test.assertTrue

private const val BASE_URL = "https://movies.example/3/"

/** Shaped like the credential it stands in for, so a leak into a URL or a log line is visible. */
private const val TOKEN = "test-read-access-token"

private const val TRENDING_PATH_SEGMENT = "trending/movie/week"
private const val PAGE_PARAMETER = "page"
private const val FIRST_PAGE = 1

/** Fewer than the recorded pages hold, so the walk stops on the count rather than on the pages. */
private const val WANTED = 100

/** How many recorded pages reaching [WANTED] distinct movies took on the day they were recorded. */
private const val PAGES_FOR_WANTED = 6

/** More than any one page holds, so a source repeating one page is stopped by the cap alone. */
private const val MORE_THAN_ANY_PAGE_HOLDS = 500

/** Rows across the two assembled pages, and how many of them are distinct. */
private const val OVERLAP_ROWS = 40
private const val OVERLAP_DISTINCT = 37

private const val EMPTY_RESULTS = """{"page":1,"results":[],"total_pages":0}"""
private const val NOT_JSON = "<html>a gateway said no</html>"

private const val KNOWN_MOVIE_ID = "687163"

class TmdbRemoteDataSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val logLines = mutableListOf<String>()

    @Test
    fun `a walk stops once it holds the movies that were asked for`() =
        runTest {
            val result = pagingSource().trending(WANTED)

            val movies = assertIs<NetworkResult.Success<List<Movie>>>(result).value

            assertEquals(WANTED, movies.size)
        }

    @Test
    fun `a movie served on two pages is held once`() =
        runTest {
            val result = overlapSource().trending(OVERLAP_ROWS)

            val movies = assertIs<NetworkResult.Success<List<Movie>>>(result).value
            val ids = movies.map { it.id }
            assertEquals(ids.distinct(), ids, "a repeated movie reached the caller twice")
            assertEquals(OVERLAP_DISTINCT, ids.size)
        }

    @Test
    fun `a walk asks for as many pages as reaching the count takes`() =
        runTest {
            pagingSource().trending(WANTED)

            assertEquals(PAGES_FOR_WANTED, requests.size)
            assertEquals(
                (1..PAGES_FOR_WANTED).map { it.toString() },
                requests.map { it.url.parameters[PAGE_PARAMETER] },
            )
        }

    @Test
    fun `a walk asks for no more than the page cap`() =
        runTest {
            val result = repeatingSource().trending(MORE_THAN_ANY_PAGE_HOLDS)

            assertEquals(TMDB_PAGE_CAP, requests.size)
            val movies = assertIs<NetworkResult.Success<List<Movie>>>(result).value
            assertEquals(
                TMDB_PAGE_SIZE,
                movies.size,
                "the same page over again is one page of movies",
            )
        }

    @Test
    fun `a page that fails after the first fails the walk rather than returning a part of it`() =
        runTest {
            val result = failingAfterFirstPageSource().trending(WANTED)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `the page number is the only query parameter`() =
        runTest {
            pagingSource().trending(TMDB_PAGE_SIZE)

            val url = requests.single().url
            assertTrue(url.toString().contains(TRENDING_PATH_SEGMENT), url.toString())
            assertEquals(FIRST_PAGE.toString(), url.parameters[PAGE_PARAMETER])
        }

    @Test
    fun `a recorded detail becomes a movie detail`() =
        runTest {
            val result =
                dataSource { respondJson(TmdbFixture.Movie.RELEASED) }
                    .movieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID))

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
            val result = dataSource { throw IOException("dropped") }.trending(WANTED)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `an unresolved host is no connectivity rather than a server fault`() =
        runTest {
            val result = dataSource { throw UnresolvedAddressException() }.trending(WANTED)

            assertEquals(DataError.NoConnectivity, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a refused status fails a page even when the body would have parsed`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Trending.PAGE_1, HttpStatusCode.Unauthorized)
                }.trending(WANTED)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `the recorded unauthorised body never becomes movies`() =
        runTest {
            val result = dataSource { respondJson(TmdbFixture.Error.UNAUTHORISED) }.trending(WANTED)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a server error status is a server failure`() =
        runTest {
            val result =
                dataSource {
                    respondJson(
                        EMPTY_RESULTS,
                        HttpStatusCode.InternalServerError,
                    )
                }.trending(WANTED)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a refused status fails a detail even when the body would have parsed`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Movie.RELEASED, HttpStatusCode.NotFound)
                }.movieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID))

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `the recorded not found body never becomes a movie detail`() =
        runTest {
            val result =
                dataSource {
                    respondJson(TmdbFixture.Error.NOT_FOUND)
                }.movieDetail(MovieId(TMDB_SOURCE, KNOWN_MOVIE_ID))

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a body that cannot be parsed is a server failure`() =
        runTest {
            val result = dataSource { respondJson(NOT_JSON) }.trending(WANTED)

            assertEquals(DataError.Server, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `an empty results array is an empty response`() =
        runTest {
            val result = dataSource { respondJson(EMPTY_RESULTS) }.trending(WANTED)

            assertEquals(DataError.EmptyResponse, assertIs<NetworkResult.Failure>(result).error)
        }

    @Test
    fun `a cancelled call is rethrown rather than returned as a failure`() =
        runTest {
            assertFailsWith<CancellationException> {
                dataSource { throw CancellationException("navigated away") }.trending(WANTED)
            }
        }

    @Test
    fun `every request carries the credential as a bearer header and never in the URL`() =
        runTest {
            dataSource { respondJson(TmdbFixture.Trending.PAGE_1) }.trending(TMDB_PAGE_SIZE)

            val request = requests.single()
            assertEquals("Bearer $TOKEN", request.headers[HttpHeaders.Authorization])
            assertFalse(request.url.toString().contains(TOKEN), "the credential is in the URL")
        }

    @Test
    fun `the credential is redacted in what the client logs`() =
        runTest {
            dataSource(
                logLevel = LogLevel.ALL,
            ) { respondJson(TmdbFixture.Trending.PAGE_1) }.trending(TMDB_PAGE_SIZE)

            val logged = logLines.joinToString("\n")
            assertTrue(logged.contains(HttpHeaders.Authorization), "no header was logged to redact")
            assertFalse(logged.contains(TOKEN), "the credential reached the log")
        }

    /** A source answering each successive request with the next recorded page, then with none. */
    private fun pagingSource() = sequenceSource(TmdbFixture.Trending.entries)

    /** A source answering with the two assembled pages that repeat rows across their boundary. */
    private fun overlapSource() = sequenceSource(TmdbFixture.Overlap.entries)

    /** A source answering every request with the same page, so the walk can only end on the cap. */
    private fun repeatingSource() = dataSource { respondJson(TmdbFixture.Trending.PAGE_1) }

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
                respondJson(TmdbFixture.Trending.PAGE_1)
            } else {
                throw IOException("dropped")
            }
        }
    }

    /**
     * A source whose every request is answered by [handler].
     *
     * @param logLevel raised only by the test that reads what was logged, because the production
     * level writes no header at all.
     */
    private fun dataSource(
        logLevel: LogLevel = LogLevel.NONE,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): TmdbRemoteDataSource {
        val engine =
            MockEngine { request ->
                requests += request
                handler(request)
            }
        val client =
            tmdbHttpClient(
                config = TmdbConfig(baseUrl = BASE_URL, readAccessToken = TOKEN),
                engine = engine,
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            logLines += message
                        }
                    },
                logLevel = logLevel,
            )
        return TmdbRemoteDataSource(client)
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
