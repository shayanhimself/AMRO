package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbTrendingPageDto
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenre
import com.shayan.amro.core.testing.fixture.tmdb.TmdbFixture
import com.shayan.amro.core.testing.fixture.tmdb.decode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** An id TMDB does not issue. */
private const val UNKNOWN_GENRE_ID = 999_999

class TmdbGenresTest {
    @Test
    fun `every genre TMDB publishes has an entry`() {
        val unmapped = publishedGenres().filter { tmdbGenre(it.id) == null }

        assertEquals(emptyList(), unmapped, "TMDB genres with no entry in the table")
    }

    @Test
    fun `every genre id in a recorded page has an entry`() {
        val unmapped =
            recordedGenreIds().distinct().filter { tmdbGenre(it) == null }

        assertEquals(
            emptyList(),
            unmapped,
            "genre ids sent on a trending page with no entry in the table",
        )
    }

    @Test
    fun `a known id maps and an unknown one does not`() {
        val known = publishedGenres().first()

        assertNotNull(tmdbGenre(known.id))
        assertNull(tmdbGenre(UNKNOWN_GENRE_ID))
    }
}

/** Every genre TMDB publishes, as its own genre list response names them. */
private fun publishedGenres() = TmdbFixture.Genres.decode<TmdbGenreListDto>().genres

/** Every genre id carried by the rows of a recorded trending page. */
private fun recordedGenreIds() =
    TmdbFixture.Trending.PAGE_1
        .decode<TmdbTrendingPageDto>()
        .results
        .flatMap { it.genreIds }
