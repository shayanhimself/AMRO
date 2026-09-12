package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.BuildConfig
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenre
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenreIds
import com.shayan.amro.core.network.sources.tmdb.mapper.toDiscoverQuery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val SKIPPED = "no TMDB token in local.properties or the environment"

private const val DISCOVER_PATH = "discover/movie"
private const val PAGE_PARAMETER = "page"
private const val SORT_BY_PARAMETER = "sort_by"
private const val WITH_GENRES_PARAMETER = "with_genres"
private const val TOTAL_RESULTS_FIELD = "total_results"

/** The page past the deepest one TMDB serves, which is what fixes the ceiling as measured. */
private const val PAGE_PAST_THE_LAST = TMDB_LAST_PAGE + 1

/**
 * Checks what the app relies on TMDB doing, against TMDB right now.
 *
 * Every other test reads a recording, which cannot notice a provider changing under it.
 */
class TmdbLiveTest {
    @Test
    fun `the TMDB genre list is up to date`() =
        live { client ->
            val published =
                assertIs<NetworkResult.Success<TmdbGenreListDto>>(
                    TmdbApi(client).genreList(),
                ).value.genres

            assertEquals(
                emptyList(),
                published.filter { tmdbGenre(it.id) == null }.map { "${it.id} ${it.name}" },
                "TMDB publishes these and the table has no entry for them",
            )
            assertEquals(
                emptySet(),
                tmdbGenreIds - published.map { it.id }.toSet(),
                "the table carries these ids and TMDB no longer publishes them",
            )
        }

    @Test
    fun `the page past the last one TMDB serves is refused`() =
        live { client ->
            val response =
                client.get(DISCOVER_PATH) {
                    parameter(PAGE_PARAMETER, PAGE_PAST_THE_LAST)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `two genres joined on a pipe reach more movies than either alone`() =
        live { client ->
            val union =
                client.totalResults(setOf(Genre.COMEDY, Genre.HORROR))
            val comedy = client.totalResults(setOf(Genre.COMEDY))
            val horror = client.totalResults(setOf(Genre.HORROR))

            assertTrue(union > comedy, "the union reached $union against comedy's $comedy")
            assertTrue(union > horror, "the union reached $union against horror's $horror")
        }

    /**
     * How many movies TMDB says one narrowing reaches.
     *
     * The count is read off the raw body rather than a wire shape, because nothing in the app has
     * a consumer for it: it counts a catalogue the list cannot reach past ten thousand.
     */
    private suspend fun HttpClient.totalResults(genres: Set<Genre>): Long {
        val query = MovieSelection(genres = genres).toDiscoverQuery()
        val body =
            get(DISCOVER_PATH) {
                parameter(SORT_BY_PARAMETER, query.sortBy)
                query.withGenres?.let { parameter(WITH_GENRES_PARAMETER, it) }
            }.body<String>()
        return Json
            .parseToJsonElement(body)
            .jsonObject
            .getValue(TOTAL_RESULTS_FIELD)
            .jsonPrimitive.content
            .toLong()
    }

    /**
     * Runs one check against the live API, through the production client and its endpoints.
     *
     * The client is built here rather than injected, because this test runs on the JVM with no
     * Hilt component to read a binding from. With no credential there is nothing to check, so the
     * test skips rather than failing a clone that has none.
     */
    private fun live(check: suspend (client: HttpClient) -> Unit) {
        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
        assumeTrue(SKIPPED, token.isNotEmpty())
        runBlocking {
            tmdbHttpClient(
                config = TmdbConfig(readAccessToken = token),
                engine = OkHttp.create(),
            ).use { client -> check(client) }
        }
    }
}
