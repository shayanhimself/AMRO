package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The pipe TMDB reads as a union. A comma in its place is an intersection. */
private const val COMEDY_OR_HORROR = "35|27"

class TmdbDiscoverMapperTest {
    @Test
    fun `an unfiltered selection omits the genre parameter`() {
        assertNull(MovieSelection().toDiscoverQuery().withGenres)
    }

    @Test
    fun `one genre maps to the id TMDB knows it by`() {
        val query = MovieSelection(genres = setOf(Genre.COMEDY)).toDiscoverQuery()

        assertEquals("35", query.withGenres)
    }

    @Test
    fun `two genres join on the pipe that makes the filter a union`() {
        val query =
            MovieSelection(genres = setOf(Genre.COMEDY, Genre.HORROR)).toDiscoverQuery()

        assertEquals(COMEDY_OR_HORROR.split("|").sorted(), query.withGenres?.split("|")?.sorted())
    }

    @Test
    fun `the same genres picked in either order write the same parameter`() {
        val one = MovieSelection(genres = setOf(Genre.COMEDY, Genre.HORROR)).toDiscoverQuery()
        val other = MovieSelection(genres = setOf(Genre.HORROR, Genre.COMEDY)).toDiscoverQuery()

        assertEquals(one.withGenres, other.withGenres)
    }

    @Test
    fun `each key and direction maps to the token TMDB orders on`() {
        val expected =
            mapOf(
                MovieSort(SortKey.POPULARITY, SortDirection.DESCENDING) to "popularity.desc",
                MovieSort(SortKey.POPULARITY, SortDirection.ASCENDING) to "popularity.asc",
                MovieSort(SortKey.TITLE, SortDirection.ASCENDING) to "original_title.asc",
                MovieSort(SortKey.TITLE, SortDirection.DESCENDING) to "original_title.desc",
                MovieSort(SortKey.RELEASE_DATE, SortDirection.DESCENDING) to
                    "primary_release_date.desc",
                MovieSort(SortKey.RELEASE_DATE, SortDirection.ASCENDING) to
                    "primary_release_date.asc",
            )

        val actual =
            expected.keys.associateWith { MovieSelection(sort = it).toDiscoverQuery().sortBy }

        assertEquals(expected, actual)
    }

    @Test
    fun `every sort a screen offers has a token of its own`() {
        val tokens =
            SortKey.entries
                .flatMap { key -> SortDirection.entries.map { MovieSort(key, it) } }
                .map { MovieSelection(sort = it).toDiscoverQuery().sortBy }

        assertEquals(tokens.distinct(), tokens)
    }
}
