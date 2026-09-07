package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The count a caller asks a trending call for, which the fake has no opinion about. */
private const val REQUESTED_COUNT = 7

class FakeMovieRemoteDataSourceTest {
    private val source = FakeMovieRemoteDataSource()

    @Test
    fun `trending results are returned in the order they were scripted`() =
        runTest {
            source.scriptTrending(
                NetworkResult.Success(listOf(MOVIE)),
                NetworkResult.Failure(DataError.NoConnectivity),
                NetworkResult.Success(listOf(OTHER_MOVIE)),
            )

            assertEquals(
                listOf(
                    NetworkResult.Success(listOf(MOVIE)),
                    NetworkResult.Failure(DataError.NoConnectivity),
                    NetworkResult.Success(listOf(OTHER_MOVIE)),
                ),
                listOf(
                    source.getTrendingMovies(REQUESTED_COUNT),
                    source.getTrendingMovies(REQUESTED_COUNT),
                    source.getTrendingMovies(REQUESTED_COUNT),
                ),
            )
        }

    @Test
    fun `the count each trending call asked for is recorded`() =
        runTest {
            source.scriptTrending(
                NetworkResult.Success(listOf(MOVIE)),
                NetworkResult.Success(listOf(OTHER_MOVIE)),
            )

            source.getTrendingMovies(REQUESTED_COUNT)
            source.getTrendingMovies(1)

            assertEquals(listOf(REQUESTED_COUNT, 1), source.requestedCounts)
        }

    @Test
    fun `the id each detail call asked for is recorded`() =
        runTest {
            source.scriptMovieDetail(NetworkResult.Success(detail(movie = OTHER_MOVIE)))

            source.getMovieDetail(OTHER_MOVIE.id)

            assertEquals(listOf(OTHER_MOVIE.id), source.requestedIds)
        }

    @Test
    fun `an unscripted call fails rather than inventing an answer`() =
        runTest {
            assertFailsWith<IllegalStateException> { source.getTrendingMovies(REQUESTED_COUNT) }
        }

    @Test
    fun `a standing trending answer serves every call past the scripted ones`() =
        runTest {
            source.scriptTrending(NetworkResult.Success(listOf(MOVIE)))
            source.alwaysAnswerTrending(NetworkResult.Success(listOf(OTHER_MOVIE)))

            assertEquals(
                listOf(
                    NetworkResult.Success(listOf(MOVIE)),
                    NetworkResult.Success(listOf(OTHER_MOVIE)),
                    NetworkResult.Success(listOf(OTHER_MOVIE)),
                ),
                listOf(
                    source.getTrendingMovies(REQUESTED_COUNT),
                    source.getTrendingMovies(REQUESTED_COUNT),
                    source.getTrendingMovies(REQUESTED_COUNT),
                ),
            )
        }

    @Test
    fun `a standing detail answer serves every call past the scripted ones`() =
        runTest {
            source.alwaysAnswerMovieDetail(NetworkResult.Success(detail(movie = OTHER_MOVIE)))

            assertEquals(
                listOf(
                    NetworkResult.Success(detail(movie = OTHER_MOVIE)),
                    NetworkResult.Success(detail(movie = OTHER_MOVIE)),
                ),
                listOf(
                    source.getMovieDetail(MOVIE.id),
                    source.getMovieDetail(OTHER_MOVIE.id),
                ),
            )
        }
}
