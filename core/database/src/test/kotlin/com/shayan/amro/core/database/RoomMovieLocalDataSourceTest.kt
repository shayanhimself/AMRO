package com.shayan.amro.core.database

import app.cash.turbine.test
import com.shayan.amro.core.database.testDatabase
import com.shayan.amro.core.model.SourceId
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
            source.getTrendingFlow().test {
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
                source.getTrendingFlow().first().sortedBy { it.title },
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

            assertEquals(listOf(SECOND_MOVIE), source.getTrendingFlow().first())
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
    fun `two sources issuing one id are two movies`() =
        runLocalDataSourceTest { source ->
            val other = FIRST_MOVIE.copy(id = FIRST_MOVIE.id.copy(source = SourceId("omdb")))

            source.replaceTrending(listOf(FIRST_MOVIE, other))
            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(2, source.getTrendingFlow().first().size)
            assertNull(source.getMovieDetailFlow(other.id).first())
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
        id = FIRST_MOVIE.id.copy(value = "755898"),
        title = "War of the Worlds",
    )

private val THIRD_MOVIE =
    movie(
        id = FIRST_MOVIE.id.copy(value = "1078605"),
        title = "Weapons",
    )

private val FIRST_DETAIL = detail(movie = FIRST_MOVIE)
