package com.shayan.amro.core.testing.fake

import app.cash.turbine.test
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeMovieLocalDataSourceTest {
    private val source = FakeMovieLocalDataSource()

    @Test
    fun `a replace emits once carrying the new set`() =
        runTest {
            source.getTrendingMoviesFlow().test {
                assertEquals(emptyList(), awaitItem())

                source.replaceTrending(listOf(MOVIE, OTHER_MOVIE))

                assertEquals(listOf(MOVIE, OTHER_MOVIE), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `a detail write leaves the trending flow untouched`() =
        runTest {
            source.replaceTrending(listOf(MOVIE))

            source.writeMovieDetail(detail(movie = OTHER_MOVIE))

            assertEquals(listOf(MOVIE), source.getTrendingMoviesFlow().first())
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

    @Test
    fun `a movie read emits the one the trending set holds`() =
        runTest {
            source.replaceTrending(listOf(MOVIE, OTHER_MOVIE))

            assertEquals(OTHER_MOVIE, source.getMovieFlow(OTHER_MOVIE.id).first())
        }
}
