package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIES_IN_NO_ORDER
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_ACCENTED_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_CAPITALISED_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_LOWERCASE_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_NEWEST_RELEASE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_NO_RELEASE_DATE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_UNACCENTED_TITLE
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieSortingTest {
    @Test
    fun `popularity ascending runs from the least popular`() {
        assertEquals(
            listOf(
                MOVIE_WITH_NO_RELEASE_DATE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_ACCENTED_TITLE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.POPULARITY, SortDirection.ASCENDING),
        )
    }

    @Test
    fun `popularity descending runs from the most popular`() {
        assertEquals(
            listOf(
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_NO_RELEASE_DATE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.POPULARITY, SortDirection.DESCENDING),
        )
    }

    @Test
    fun `title ascending is case insensitive`() {
        assertEquals(
            listOf(
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_NO_RELEASE_DATE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.TITLE, SortDirection.ASCENDING),
        )
    }

    @Test
    fun `title descending reverses that ordering and not the tiebreak`() {
        assertEquals(
            listOf(
                MOVIE_WITH_NO_RELEASE_DATE,
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.TITLE, SortDirection.DESCENDING),
        )
    }

    @Test
    fun `release date ascending runs from the oldest, with no date last`() {
        assertEquals(
            listOf(
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_NO_RELEASE_DATE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.RELEASE_DATE, SortDirection.ASCENDING),
        )
    }

    @Test
    fun `release date descending runs from the newest, with no date still last`() {
        assertEquals(
            listOf(
                MOVIE_WITH_NEWEST_RELEASE,
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_LOWERCASE_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_NO_RELEASE_DATE,
            ),
            MOVIES_IN_NO_ORDER.inOrder(SortKey.RELEASE_DATE, SortDirection.DESCENDING),
        )
    }

    @Ignore
    @Test
    fun `movies with an equal key hold one order whatever order they arrived in`() {
        SortKey.entries.forEach { key ->
            SortDirection.entries.forEach { direction ->
                assertEquals(
                    MOVIES_IN_NO_ORDER.inOrder(key, direction),
                    MOVIES_IN_NO_ORDER.reversed().inOrder(key, direction),
                )
            }
        }
    }

    @Test
    fun `filtering then sorting equals sorting then filtering`() {
        val genres = setOf(Genre.ROMANCE, Genre.DRAMA)
        val sort = MovieSort(key = SortKey.TITLE, direction = SortDirection.DESCENDING)

        assertEquals(
            MOVIES_IN_NO_ORDER.applyFilter(genres).applySort(sort),
            MOVIES_IN_NO_ORDER.applySort(sort).applyFilter(genres),
        )
    }
}

/** The fixture in one ordering. */
private fun List<Movie>.inOrder(
    key: SortKey,
    direction: SortDirection,
): List<Movie> = applySort(MovieSort(key = key, direction = direction))
