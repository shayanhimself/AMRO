package com.shayan.amro.core.testing.fake

import app.cash.turbine.test
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeMovieDetailRepositoryTest {
    private val repository = FakeMovieDetailRepository()

    @Test
    fun `a successful refresh replaces what the detail flow emits`() =
        runTest {
            val cached = detail(movie = MOVIE, tagline = "As it was cached.")
            val fetched = detail(movie = MOVIE)
            repository.seedDetail(cached)
            repository.scriptRefresh(fetched)

            repository.getMovieDetailFlow(MOVIE.id).test {
                assertEquals(cached, awaitItem())

                assertNull(repository.refresh(MOVIE.id))

                assertEquals(fetched, awaitItem())
            }
        }

    @Test
    fun `a failed refresh returns the cause and changes no emission`() =
        runTest {
            val cached = detail(movie = MOVIE)
            repository.seedDetail(cached)
            repository.scriptRefreshFailure(MOVIE.id, DataError.Server)

            repository.getMovieDetailFlow(MOVIE.id).test {
                assertEquals(cached, awaitItem())

                assertEquals(DataError.Server, repository.refresh(MOVIE.id))

                expectNoEvents()
            }
        }

    @Test
    fun `a movie with nothing seeded emits null from both reads`() =
        runTest {
            repository.seedMovie(OTHER_MOVIE)
            repository.seedDetail(detail(movie = OTHER_MOVIE))

            assertNull(repository.getMovieFlow(MOVIE.id).first())
            assertNull(repository.getMovieDetailFlow(MOVIE.id).first())
        }

    @Test
    fun `a seeded movie reads back`() =
        runTest {
            repository.seedMovie(MOVIE)

            assertEquals(MOVIE, repository.getMovieFlow(MOVIE.id).first())
        }

    @Test
    fun `the id each refresh asked for is recorded`() =
        runTest {
            repository.scriptRefresh(detail(movie = MOVIE))

            repository.refresh(MOVIE.id)

            assertEquals(listOf(MOVIE.id), repository.requestedIds)
        }
}
