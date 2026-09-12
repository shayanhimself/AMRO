package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The count a caller asks for, which the fake has no opinion about. */
private const val REQUESTED_COUNT = 7

private val SELECTION = MovieSelection(genres = setOf(Genre.COMEDY))

private val CURSOR = Cursor("2")

/** One stretch of a list, as a source answers with one. */
private fun page(movie: Movie) =
    NetworkResult.Success(MoviePage(movies = listOf(movie), next = null))

class FakeMovieRemoteDataSourceTest {
    private val source = FakeMovieRemoteDataSource()

    @Test
    fun `results are returned in the order they were scripted`() =
        runTest {
            source.scriptMovies(
                page(MOVIE),
                NetworkResult.Failure(DataError.NoConnectivity),
                page(OTHER_MOVIE),
            )

            assertEquals(
                listOf(
                    page(MOVIE),
                    NetworkResult.Failure(DataError.NoConnectivity),
                    page(OTHER_MOVIE),
                ),
                List(3) { source.getMovies(SELECTION, REQUESTED_COUNT, after = null) },
            )
        }

    @Test
    fun `what each call asked for is recorded`() =
        runTest {
            source.scriptMovies(page(MOVIE), page(OTHER_MOVIE))

            source.getMovies(SELECTION, REQUESTED_COUNT, after = null)
            source.getMovies(MovieSelection(), 1, after = CURSOR)

            assertEquals(
                listOf(
                    MoviesRequest(SELECTION, REQUESTED_COUNT, after = null),
                    MoviesRequest(MovieSelection(), 1, after = CURSOR),
                ),
                source.requests,
            )
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
            assertFailsWith<IllegalStateException> {
                source.getMovies(SELECTION, REQUESTED_COUNT, after = null)
            }
        }

    @Test
    fun `a standing answer serves every call past the scripted ones`() =
        runTest {
            source.scriptMovies(page(MOVIE))
            source.alwaysAnswerMovies(page(OTHER_MOVIE))

            assertEquals(
                listOf(page(MOVIE), page(OTHER_MOVIE), page(OTHER_MOVIE)),
                List(3) { source.getMovies(SELECTION, REQUESTED_COUNT, after = null) },
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
