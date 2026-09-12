package com.shayan.amro.core.database

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import com.shayan.amro.core.testing.fixture.model.MovieFixture.movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the same movie's tagline reads as once TMDB has been asked again. */
private const val SECOND_TAGLINE = "It bites twice."

/** Two queries, so a write under one can be shown not to reach the other. */
private const val POPULAR_KEY = ":POPULARITY:DESCENDING"
private const val COMEDIES_KEY = "COMEDY:TITLE:ASCENDING"

private val FIRST_CURSOR = Cursor("2")
private val SECOND_CURSOR = Cursor("3")

@RunWith(RobolectricTestRunner::class)
class RoomMovieLocalDataSourceTest {
    @Test
    fun `a read joins and orders by the position the source put a movie at`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(THIRD_MOVIE, FIRST_MOVIE, SECOND_MOVIE))

            assertEquals(
                listOf(THIRD_MOVIE, FIRST_MOVIE, SECOND_MOVIE),
                source.moviesPagingSource(POPULAR_KEY).snapshot(),
            )
        }

    @Test
    fun `an append carries on after everything the query holds`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE))

            source.appendQuery(POPULAR_KEY, page(SECOND_MOVIE, THIRD_MOVIE))

            assertEquals(
                listOf(FIRST_MOVIE, SECOND_MOVIE, THIRD_MOVIE),
                source.moviesPagingSource(POPULAR_KEY).snapshot(),
            )
        }

    @Test
    fun `a movie an append repeats keeps the place it first took`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, SECOND_MOVIE))

            source.appendQuery(POPULAR_KEY, page(SECOND_MOVIE, THIRD_MOVIE))

            assertEquals(
                listOf(FIRST_MOVIE, SECOND_MOVIE, THIRD_MOVIE),
                source.moviesPagingSource(POPULAR_KEY).snapshot(),
            )
        }

    @Test
    fun `a movie one query repeats is still its own row under another`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE))

            source.replaceQuery(COMEDIES_KEY, page(FIRST_MOVIE))

            assertEquals(listOf(FIRST_MOVIE), source.moviesPagingSource(POPULAR_KEY).snapshot())
            assertEquals(listOf(FIRST_MOVIE), source.moviesPagingSource(COMEDIES_KEY).snapshot())
        }

    @Test
    fun `a replace clears one query and leaves another intact`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(COMEDIES_KEY, page(THIRD_MOVIE))
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, SECOND_MOVIE))

            source.replaceQuery(POPULAR_KEY, page(SECOND_MOVIE))

            assertEquals(listOf(SECOND_MOVIE), source.moviesPagingSource(POPULAR_KEY).snapshot())
            assertEquals(listOf(THIRD_MOVIE), source.moviesPagingSource(COMEDIES_KEY).snapshot())
        }

    @Test
    fun `a replace leaves the movie rows the query no longer references`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE))

            source.replaceQuery(POPULAR_KEY, page(SECOND_MOVIE))

            assertEquals(FIRST_MOVIE, source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a cursor is read back under the query it was written for`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, next = FIRST_CURSOR))
            source.replaceQuery(COMEDIES_KEY, page(SECOND_MOVIE, next = SECOND_CURSOR))

            assertEquals(FIRST_CURSOR, source.queryCursor(POPULAR_KEY))
            assertEquals(SECOND_CURSOR, source.queryCursor(COMEDIES_KEY))
        }

    @Test
    fun `an append overwrites the cursor with where the list now resumes`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, next = FIRST_CURSOR))

            source.appendQuery(POPULAR_KEY, page(SECOND_MOVIE, next = SECOND_CURSOR))

            assertEquals(SECOND_CURSOR, source.queryCursor(POPULAR_KEY))
        }

    @Test
    fun `a query the source has served everything for holds no cursor`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, next = null))

            assertNull(source.queryCursor(POPULAR_KEY))
        }

    @Test
    fun `a write invalidates a read that was already open over that query`() =
        runLocalDataSourceTest { source ->
            val paging = source.moviesPagingSource(POPULAR_KEY)
            paging.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = SNAPSHOT_PAGE_SIZE,
                    placeholdersEnabled = false,
                ),
            )

            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE))

            assertTrue(paging.invalid, "the rows a write added never reach a reader")
        }

    @Test
    fun `a query nothing has been written for reads as an empty list`() =
        runLocalDataSourceTest { source ->
            assertEquals(emptyList(), source.moviesPagingSource(POPULAR_KEY).snapshot())
        }

    @Test
    fun `a detail written for a movie no query references still reads back`() =
        runLocalDataSourceTest { source ->
            source.writeMovieDetail(FIRST_DETAIL)

            source.replaceQuery(POPULAR_KEY, page(SECOND_MOVIE))

            assertEquals(FIRST_DETAIL, source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `writing a detail leaves the movie rows untouched`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(SECOND_MOVIE))

            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(listOf(SECOND_MOVIE), source.moviesPagingSource(POPULAR_KEY).snapshot())
        }

    @Test
    fun `a detail read with nothing cached emits null`() =
        runLocalDataSourceTest { source ->
            assertNull(source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one nothing has written emits null`() =
        runLocalDataSourceTest { source ->
            assertNull(source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one a query put there emits it`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(FIRST_MOVIE, SECOND_MOVIE))

            assertEquals(FIRST_MOVIE, source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a detail write reaches a subscriber already collecting`() =
        runLocalDataSourceTest { source ->
            source.getMovieDetailFlow(FIRST_MOVIE.id).test {
                assertNull(awaitItem())

                source.writeMovieDetail(FIRST_DETAIL)

                assertEquals(FIRST_DETAIL, awaitItem())

                val refetched = FIRST_DETAIL.copy(tagline = SECOND_TAGLINE)
                source.writeMovieDetail(refetched)

                assertEquals(refetched, awaitItem())
            }
        }

    @Test
    fun `writing a detail twice leaves one row, carrying the second`() =
        runLocalDataSourceTest { source ->
            val refetched = FIRST_DETAIL.copy(tagline = SECOND_TAGLINE)
            source.writeMovieDetail(FIRST_DETAIL)

            source.writeMovieDetail(refetched)

            assertEquals(refetched, source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie the source knows nothing optional about reads back knowing nothing`() =
        runLocalDataSourceTest { source ->
            source.replaceQuery(POPULAR_KEY, page(BARE_MOVIE))

            assertEquals(BARE_MOVIE, source.getMovieFlow(BARE_MOVIE.id).first())
        }

    @Test
    fun `a detail the source knows nothing optional about reads back knowing nothing`() =
        runLocalDataSourceTest { source ->
            source.writeMovieDetail(BARE_DETAIL)

            assertEquals(BARE_DETAIL, source.getMovieDetailFlow(BARE_MOVIE.id).first())
        }

    @Test
    fun `a rating nobody has voted on reads back with a score and no count`() =
        runLocalDataSourceTest { source ->
            source.writeMovieDetail(DETAIL_WITH_UNCOUNTED_RATING)

            assertEquals(
                DETAIL_WITH_UNCOUNTED_RATING,
                source.getMovieDetailFlow(FIRST_MOVIE.id).first(),
            )
        }
}

/**
 * Runs [body] against a data source over a database of its own.
 */
private fun runLocalDataSourceTest(
    body: suspend TestScope.(MovieLocalDataSource) -> Unit,
): TestResult =
    runTest {
        val database = testDatabase()
        body(
            RoomMovieLocalDataSource(
                moviesDao = database.movieDao(),
                queriesDao = database.movieQueryDao(),
                detailsDao = database.movieDetailDao(),
            ),
        )
    }

/**
 * One stretch of a list, as a source answers with one.
 *
 * @param next where the list resumes, which most cases here do not turn on.
 */
private fun page(
    vararg movies: Movie,
    next: Cursor? = FIRST_CURSOR,
): MoviePage = MoviePage(movies = movies.toList(), next = next)

/** More rows than any fixture here holds, so one page is the whole list. */
private const val SNAPSHOT_PAGE_SIZE = 50

/** Everything a paged read holds, in the order it holds it. */
private suspend fun PagingSource<Int, Movie>.snapshot(): List<Movie> =
    Pager(
        config = PagingConfig(pageSize = SNAPSHOT_PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { this },
    ).flow.asSnapshot()

/**
 * Three movies one query produced.
 */
private val FIRST_MOVIE = movie(title = "The Mongoose")

private val SECOND_MOVIE =
    movie(
        id = "755898",
        title = "War of the Worlds",
    )

private val THIRD_MOVIE =
    movie(
        id = "1078605",
        title = "Weapons",
    )

private val FIRST_DETAIL = detail(movie = FIRST_MOVIE)

/**
 * A movie every optional column of which is empty, so writing it binds a null to each and reading
 * it back tells a column the source never filled from one holding a zero.
 */
private val BARE_MOVIE =
    movie(
        id = "634492",
        title = "Madame Web",
        releaseDate = null,
        poster = null,
    )

/** The record behind [BARE_MOVIE], as empty as the row it is stored in allows. */
private val BARE_DETAIL =
    detail(
        movie = BARE_MOVIE,
        overview = null,
        tagline = null,
        backdrop = null,
        runtime = null,
        budget = null,
        revenue = null,
        rating = null,
        imdbId = null,
    )

/** A score standing on its own, which is the one rating that stores a null beside a value. */
private val DETAIL_WITH_UNCOUNTED_RATING =
    detail(
        movie = FIRST_MOVIE,
        rating =
            Rating(
                average = 6.1,
                count = null,
            ),
    )
