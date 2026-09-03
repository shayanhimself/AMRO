package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.mapper.tmdbGenre
import com.shayan.amro.core.testing.TmdbFixture
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** An id TMDB does not issue. */
private const val UNKNOWN_GENRE_ID = 999_999

class TmdbGenresTest {
    @Test
    fun `every genre TMDB publishes has an entry`() {
        val published = testJson.decodeFromString<TmdbGenreListDto>(TmdbFixture.GENRES.json).genres

        val unmapped = published.filter { tmdbGenre(it.id) == null }

        assertEquals(emptyList(), unmapped, "TMDB genres with no entry in the table")
    }

    @Test
    fun `every genre id in a recorded page has an entry`() {
        val page = testJson.decodeFromString<TmdbTrendingPageIds>(TmdbFixture.TRENDING_PAGE_1.json)

        val unmapped =
            page.results.flatMap { it.genreIds }.distinct().filter {
                tmdbGenre(it) ==
                    null
            }

        assertEquals(
            emptyList(),
            unmapped,
            "genre ids sent on a trending page with no entry in the table",
        )
    }

    @Test
    fun `a known id maps and an unknown one does not`() {
        val known =
            testJson
                .decodeFromString<TmdbGenreListDto>(
                    TmdbFixture.GENRES.json,
                ).genres
                .first()

        assertNotNull(tmdbGenre(known.id))
        assertNull(tmdbGenre(UNKNOWN_GENRE_ID))
    }
}

/**
 * Just the genre ids of a trending page.
 *
 * The production shape would do, but reading only what this test asserts on keeps the test from
 * failing on an unrelated mapping change.
 */
@Serializable
private data class TmdbTrendingPageIds(
    val results: List<Row>,
) {
    @Serializable
    data class Row(
        @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    )
}
