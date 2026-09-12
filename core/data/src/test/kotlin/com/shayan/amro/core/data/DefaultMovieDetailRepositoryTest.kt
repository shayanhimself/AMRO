package com.shayan.amro.core.data

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fake.FakeMovieLocalDataSource
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What the cached record's tagline reads as before the source has been asked again. */
private const val CACHED_TAGLINE = "As it was cached."

/** The one query these tests put movies under, since none of them turns on which query it is. */
private const val QUERY_KEY = ":POPULARITY:DESCENDING"

/** One stretch of a list, as a source answers with one. */
private fun page(vararg movies: Movie) = MoviePage(movies = movies.toList(), next = null)

class DefaultMovieDetailRepositoryTest {
    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()
    private val repository = DefaultMovieDetailRepository(local = local, remote = remote)

    @Test
    fun `a refresh writes that detail and returns null`() =
        runTest {
            val fetched = detail(movie = MOVIE)
            remote.scriptMovieDetail(NetworkResult.Success(fetched))

            assertNull(repository.refresh(MOVIE.id))

            assertEquals(fetched, local.getMovieDetailFlow(MOVIE.id).first())
        }

    @Test
    fun `a refresh reads the movie it was asked for`() =
        runTest {
            remote.scriptMovieDetail(NetworkResult.Success(detail(movie = MOVIE)))

            repository.refresh(MOVIE.id)

            assertEquals(listOf(MOVIE.id), remote.requestedIds)
        }

    @Test
    fun `a failed refresh returns the cause and leaves the cached detail`() =
        runTest {
            val cached = detail(movie = MOVIE, tagline = CACHED_TAGLINE)
            local.writeMovieDetail(cached)
            remote.scriptMovieDetail(NetworkResult.Failure(DataError.Server))

            assertEquals(DataError.Server, repository.refresh(MOVIE.id))

            assertEquals(cached, repository.getMovieDetailFlow(MOVIE.id).first())
        }

    @Test
    fun `a refresh leaves the movies a query produced untouched`() =
        runTest {
            local.replaceQuery(QUERY_KEY, page(OTHER_MOVIE))
            remote.scriptMovieDetail(NetworkResult.Success(detail(movie = MOVIE)))

            repository.refresh(MOVIE.id)

            assertEquals(listOf(OTHER_MOVIE), local.queryResults(QUERY_KEY))
        }

    @Test
    fun `the movie read emits null for one no query produced`() =
        runTest {
            local.replaceQuery(QUERY_KEY, page(OTHER_MOVIE))

            assertNull(repository.getMovieFlow(MOVIE.id).first())
        }

    @Test
    fun `the movie read emits the one a query produced`() =
        runTest {
            local.replaceQuery(QUERY_KEY, page(MOVIE, OTHER_MOVIE))

            assertEquals(MOVIE, repository.getMovieFlow(MOVIE.id).first())
        }

    @Test
    fun `the detail read emits null with nothing cached`() =
        runTest {
            assertNull(repository.getMovieDetailFlow(MOVIE.id).first())
        }
}
