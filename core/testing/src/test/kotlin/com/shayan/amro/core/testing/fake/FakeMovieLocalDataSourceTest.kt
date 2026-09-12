package com.shayan.amro.core.testing.fake

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val QUERY_KEY = ":POPULARITY:DESCENDING"
private const val OTHER_QUERY_KEY = "COMEDY:TITLE:ASCENDING"

private val CURSOR = Cursor("2")

class FakeMovieLocalDataSourceTest {
    private val source = FakeMovieLocalDataSource()

    @Test
    fun `a replace makes the query read as the movies it was given, in that order`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(OTHER_MOVIE, MOVIE))

            assertEquals(
                listOf(OTHER_MOVIE, MOVIE),
                source.moviesPagingSource(QUERY_KEY).snapshot(),
            )
        }

    @Test
    fun `an append carries on after everything the query holds`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(MOVIE))

            source.appendQuery(QUERY_KEY, page(OTHER_MOVIE))

            assertEquals(
                listOf(MOVIE, OTHER_MOVIE),
                source.moviesPagingSource(QUERY_KEY).snapshot(),
            )
        }

    @Test
    fun `a movie an append repeats keeps the place it first took`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(MOVIE))

            source.appendQuery(QUERY_KEY, page(MOVIE, OTHER_MOVIE))

            assertEquals(
                listOf(MOVIE, OTHER_MOVIE),
                source.moviesPagingSource(QUERY_KEY).snapshot(),
            )
        }

    @Test
    fun `a replace under one query leaves another alone`() =
        runTest {
            source.replaceQuery(OTHER_QUERY_KEY, page(OTHER_MOVIE))

            source.replaceQuery(QUERY_KEY, page(MOVIE))

            assertEquals(
                listOf(OTHER_MOVIE),
                source.moviesPagingSource(OTHER_QUERY_KEY).snapshot(),
            )
        }

    @Test
    fun `a movie a query stopped referencing is still held`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(MOVIE))

            source.replaceQuery(QUERY_KEY, page(OTHER_MOVIE))

            assertEquals(MOVIE, source.getMovieFlow(MOVIE.id).first())
        }

    @Test
    fun `a cursor is read back under the query it was written for`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(MOVIE, next = CURSOR))

            assertEquals(CURSOR, source.queryCursor(QUERY_KEY))
            assertNull(source.queryCursor(OTHER_QUERY_KEY))
        }

    @Test
    fun `a detail write leaves the query results untouched`() =
        runTest {
            source.replaceQuery(QUERY_KEY, page(MOVIE))

            source.writeMovieDetail(detail(movie = OTHER_MOVIE))

            assertEquals(listOf(MOVIE), source.moviesPagingSource(QUERY_KEY).snapshot())
        }

    @Test
    fun `a detail write reads back under its own movie`() =
        runTest {
            val written = detail(movie = OTHER_MOVIE)

            source.writeMovieDetail(written)

            assertEquals(written, source.getMovieDetailFlow(OTHER_MOVIE.id).first())
        }

    @Test
    fun `an absent row emits null from both reads`() =
        runTest {
            assertNull(source.getMovieFlow(MOVIE.id).first())
            assertNull(source.getMovieDetailFlow(MOVIE.id).first())
        }
}

/** One stretch of a list, as a source answers with one. */
private fun page(
    vararg movies: Movie,
    next: Cursor? = CURSOR,
): MoviePage = MoviePage(movies = movies.toList(), next = next)

/** More rows than any fixture here holds, so one page is the whole list. */
private const val SNAPSHOT_PAGE_SIZE = 50

/** Everything a paged read holds, in the order it holds it. */
private suspend fun PagingSource<Int, Movie>.snapshot(): List<Movie> =
    Pager(
        config = PagingConfig(pageSize = SNAPSHOT_PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { this },
    ).flow.asSnapshot()
