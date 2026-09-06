package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIES_IN_NO_ORDER
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.feature.trending.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrendingUiStateMapperTest {
    @Test
    fun `an answer with nothing in it over an empty cache is the error naming that`() {
        val state =
            emptyList<Movie>().uiState(
                refresh = RefreshState(error = DataError.EmptyResponse),
            )

        val content = state.content
        assertTrue(content is TrendingContent.Error, "expected an error, was $content")
        assertEquals(Glyphs.MOVIE_FILTER, content.glyph)
        assertEquals(R.string.feature_trending_error_empty_response, content.titleRes)
    }

    @Test
    fun `an answer with nothing in it over a cached set is a notice, and keeps the rows`() {
        val state =
            MOVIES_IN_NO_ORDER.uiState(
                refresh = RefreshState(error = DataError.EmptyResponse),
            )

        val notice = assertNotNull(state.notice)
        assertEquals(Glyphs.MOVIE_FILTER, notice.glyph)
        assertEquals(R.string.feature_trending_notice_empty_response, notice.messageRes)
        assertEquals(MOVIES_IN_NO_ORDER.size, state.rows().size)
    }

    @Test
    fun `a movie the source has no poster for is a row with no poster to load`() {
        val withPoster = MovieFixture.movie()
        val withoutPoster = MovieFixture.movie(poster = null)

        val carried = listOf(withPoster).uiState().rows().single()
        val missing = listOf(withoutPoster).uiState().rows().single()

        assertEquals(withPoster.poster?.small, carried.posterUrl)
        assertNull(missing.posterUrl)
    }
}

/** The state the screen renders over this set, with nothing selected unless a test says so. */
private fun List<Movie>.uiState(
    selectedGenreNames: List<String> = emptyList(),
    sort: MovieSort = MovieSort.DEFAULT,
    refresh: RefreshState = RefreshState(),
) = toTrendingUiState(selectedGenreNames = selectedGenreNames, sort = sort, refresh = refresh)

/**
 * The rows the state is showing.
 *
 * @return the rows, or fails the test when the body is anything else.
 */
private fun TrendingUiState.rows(): List<MovieRowUiState> {
    val content = content
    assertTrue(content is TrendingContent.Movies, "expected movies, was $content")
    return content.rows
}
