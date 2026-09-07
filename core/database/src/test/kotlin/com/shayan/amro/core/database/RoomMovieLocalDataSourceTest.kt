package com.shayan.amro.core.database

import app.cash.turbine.test
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

/** What the same movie's tagline reads as once TMDB has been asked again. */
private const val SECOND_TAGLINE = "It bites twice."

@RunWith(RobolectricTestRunner::class)
class RoomMovieLocalDataSourceTest {
    @Test
    fun `a replace emits once with the new set and never an empty list`() =
        runLocalDataSourceTest { source ->
            source.getTrendingMoviesFlow().test {
                assertEquals(emptyList(), awaitItem())

                source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

                assertEquals(listOf(FIRST_MOVIE, SECOND_MOVIE), awaitItem().sortedBy { it.title })
                expectNoEvents()
            }
        }

    @Test
    fun `a replace removes the rows the new set does not contain`() =
        runLocalDataSourceTest { source ->
            source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

            source.replaceTrending(listOf(SECOND_MOVIE, THIRD_MOVIE))

            assertEquals(
                listOf(SECOND_MOVIE, THIRD_MOVIE),
                source.getTrendingMoviesFlow().first().sortedBy { it.title },
            )
        }

    @Test
    fun `a detail written for a movie the trending table does not hold still reads back`() =
        runLocalDataSourceTest { source ->
            source.writeMovieDetail(FIRST_DETAIL)

            source.replaceTrending(listOf(SECOND_MOVIE))

            assertEquals(FIRST_DETAIL, source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `writing a detail leaves the trending table untouched`() =
        runLocalDataSourceTest { source ->
            source.replaceTrending(listOf(SECOND_MOVIE))

            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(listOf(SECOND_MOVIE), source.getTrendingMoviesFlow().first())
        }

    @Test
    fun `a detail read with nothing cached emits null`() =
        runLocalDataSourceTest { source ->
            assertNull(source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one a refresh dropped emits null`() =
        runLocalDataSourceTest { source ->
            source.replaceTrending(listOf(FIRST_MOVIE))

            source.replaceTrending(listOf(SECOND_MOVIE))

            assertNull(source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one the trending set holds emits it`() =
        runLocalDataSourceTest { source ->
            source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

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
            source.replaceTrending(listOf(BARE_MOVIE))

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
        body(RoomMovieLocalDataSource(database.movieDao(), database.movieDetailDao()))
    }

/**
 * Three movies of one trending set.
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
        rating = Rating(
            average = 6.1,
            count = null,
        ),
    )
