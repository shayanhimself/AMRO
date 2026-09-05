package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIES_IN_NO_ORDER
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_ACCENTED_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_CAPITALISED_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_LOWERCASE_TITLE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE_WITH_UNACCENTED_TITLE
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieFilteringTest {
    @Test
    fun `an empty selection returns every movie in the order it was given`() {
        assertEquals(MOVIES_IN_NO_ORDER, MOVIES_IN_NO_ORDER.applyFilter(emptySet()))
    }

    @Test
    fun `one genre returns exactly the movies carrying it`() {
        val filtered = MOVIES_IN_NO_ORDER.applyFilter(setOf(Genre.ROMANCE))

        assertEquals(listOf(MOVIE_WITH_ACCENTED_TITLE, MOVIE_WITH_UNACCENTED_TITLE), filtered)
    }

    @Test
    fun `two genres return their union`() {
        val filtered =
            MOVIES_IN_NO_ORDER.applyFilter(setOf(Genre.ROMANCE, Genre.DRAMA))

        assertEquals(
            listOf(
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
                MOVIE_WITH_LOWERCASE_TITLE,
            ),
            filtered,
        )
    }

    @Test
    fun `a movie carrying two selected genres is returned once`() {
        val filtered =
            MOVIES_IN_NO_ORDER.applyFilter(setOf(Genre.ROMANCE, Genre.COMEDY))

        assertEquals(
            listOf(
                MOVIE_WITH_ACCENTED_TITLE,
                MOVIE_WITH_CAPITALISED_TITLE,
                MOVIE_WITH_UNACCENTED_TITLE,
            ),
            filtered,
        )
    }

    @Test
    fun `a genre no movie carries returns an empty list`() {
        assertEquals(
            emptyList(),
            MOVIES_IN_NO_ORDER.applyFilter(setOf(Genre.MUSIC)),
        )
    }
}
