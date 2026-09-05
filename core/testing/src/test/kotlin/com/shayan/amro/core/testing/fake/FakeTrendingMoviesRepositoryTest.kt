package com.shayan.amro.core.testing.fake

import app.cash.turbine.test
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeTrendingMoviesRepositoryTest {
    private val repository = FakeTrendingMoviesRepository()

    @Test
    fun `a successful refresh replaces what the flow emits`() =
        runTest {
            repository.seedTrending(listOf(MOVIE))
            repository.scriptRefresh(listOf(OTHER_MOVIE))

            repository.getTrendingMoviesFlow().test {
                assertEquals(listOf(MOVIE), awaitItem())

                assertNull(repository.refresh())

                assertEquals(listOf(OTHER_MOVIE), awaitItem())
            }
        }

    @Test
    fun `a failed refresh returns the cause and changes no emission`() =
        runTest {
            repository.seedTrending(listOf(MOVIE))
            repository.scriptRefreshFailure(DataError.NoConnectivity)

            repository.getTrendingMoviesFlow().test {
                assertEquals(listOf(MOVIE), awaitItem())

                assertEquals(DataError.NoConnectivity, repository.refresh())

                expectNoEvents()
            }
        }

    @Test
    fun `the read emits nothing until a set is seeded`() =
        runTest {
            assertEquals(emptyList(), repository.getTrendingMoviesFlow().first())
        }

    @Test
    fun `every refresh is counted`() =
        runTest {
            repository.scriptRefresh(listOf(OTHER_MOVIE))
            repository.scriptRefreshFailure(DataError.Server)

            repository.refresh()
            repository.refresh()

            assertEquals(2, repository.refreshCount)
        }
}
