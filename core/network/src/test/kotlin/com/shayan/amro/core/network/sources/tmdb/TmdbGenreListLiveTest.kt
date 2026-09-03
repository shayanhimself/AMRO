package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.network.BuildConfig
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenre
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenreIds
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

/** TMDB's real address, which is the whole point of this test. */
private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

private const val GENRE_LIST_PATH = "genre/movie/list"

private const val SKIPPED = "no TMDB token in local.properties or the environment"

/**
 * Checks the app's genre table against the list TMDB publishes right now.
 *
 * It fails when TMDB has added a genre the table has no entry for, or retired one the table still
 * carries. Every other test reads a recording, which can notice neither.
 *
 * It needs a token and a network, so `check` leaves it out. `scripts/livecheck.sh` runs it, and
 * it skips there too when no token was supplied.
 */
class TmdbGenreListLiveTest {
    @Test
    fun `the TMDB genre list is up to date`() {
        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
        assumeTrue(SKIPPED, token.isNotEmpty())

        val published = publishedGenres(token)

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

    /**
     * Reads TMDB's genre list through the production client.
     *
     * Going through the real client rather than a bare request means a change to the base address,
     * the credential header or the JSON configuration fails here too.
     */
    private fun publishedGenres(token: String) =
        runBlocking {
            tmdbHttpClient(
                config = TmdbConfig(baseUrl = TMDB_BASE_URL, readAccessToken = token),
                engine = OkHttp.create(),
            ).use { client ->
                client.get(GENRE_LIST_PATH).body<TmdbGenreListDto>().genres
            }
        }
}
