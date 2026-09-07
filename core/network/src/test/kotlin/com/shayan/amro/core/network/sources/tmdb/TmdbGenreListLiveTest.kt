package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.network.BuildConfig
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenre
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenreIds
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val SKIPPED = "no TMDB token in local.properties or the environment"

/**
 * Checks the app's genre table against the list TMDB publishes right now.
 *
 * It fails when TMDB has added a genre the table has no entry for, or retired one the table still
 * carries. Every other test reads a recording, which can notice neither.
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
     * Reads TMDB's genre list through the production client and endpoints.
     *
     * They are built here rather than injected, because this test runs on the JVM with no Hilt
     * component to read a binding from.
     */
    private fun publishedGenres(token: String) =
        runBlocking {
            val config = TmdbConfig(readAccessToken = token)
            tmdbHttpClient(config = config, engine = OkHttp.create()).use { client ->
                val result = TmdbApi(client).genreList()
                assertIs<NetworkResult.Success<TmdbGenreListDto>>(result).value.genres
            }
        }
}
