package com.shayan.amro.core.data

import app.cash.turbine.test
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fake.FakeMovieLocalDataSource
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultTrendingMoviesRepositoryTest {
    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()
    private val repository = DefaultTrendingMoviesRepository(local = local, remote = remote)

    @Test
    fun `a refresh writes the returned list and returns null`() =
        runTest {
            remote.scriptTrending(NetworkResult.Success(listOf(MOVIE, OTHER_MOVIE)))

            assertNull(repository.refresh())

            assertEquals(listOf(MOVIE, OTHER_MOVIE), local.getTrendingMoviesFlow().first())
        }

    @Test
    fun `a refresh asks for one hundred movies`() =
        runTest {
            remote.scriptTrending(NetworkResult.Success(listOf(MOVIE)))

            repository.refresh()

            assertEquals(listOf(100), remote.requestedCounts)
        }

    @Test
    fun `a failed refresh returns the cause and writes nothing`() =
        runTest {
            remote.scriptTrending(
                NetworkResult.Success(listOf(MOVIE)),
                NetworkResult.Failure(DataError.NoConnectivity),
            )
            repository.refresh()

            assertEquals(DataError.NoConnectivity, repository.refresh())

            assertEquals(listOf(MOVIE), local.getTrendingMoviesFlow().first())
        }

    @Test
    fun `the read emits the cache before any refresh, and again after one succeeds`() =
        runTest {
            local.replaceTrending(listOf(MOVIE))
            remote.scriptTrending(NetworkResult.Success(listOf(OTHER_MOVIE)))

            repository.getTrendingMoviesFlow().test {
                assertEquals(listOf(MOVIE), awaitItem())

                repository.refresh()

                assertEquals(listOf(OTHER_MOVIE), awaitItem())
            }
        }

    @Test
    fun `a refresh runs whatever the cache holds`() =
        runTest {
            local.replaceTrending(listOf(MOVIE))
            remote.scriptTrending(NetworkResult.Success(listOf(OTHER_MOVIE)))

            repository.refresh()

            assertEquals(1, remote.requestedCounts.size)
        }
}
