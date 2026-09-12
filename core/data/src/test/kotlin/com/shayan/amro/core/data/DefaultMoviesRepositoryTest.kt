package com.shayan.amro.core.data

import androidx.paging.testing.asSnapshot
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fake.FakeMovieLocalDataSource
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fake.MoviesRequest
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val SELECTION = MovieSelection(genres = setOf(Genre.COMEDY))

/** Enough movies that a page of them is a list rather than a row. */
private val MOVIES = MovieFixture.rankedMovies(count = 3)

/** What a second stretch of the same list holds, which no movie of [MOVIES] repeats. */
private val MORE_MOVIES = MovieFixture.rankedMovies(count = 5).drop(3)

/** Where the first stretch says the list resumes. */
private val NEXT = Cursor("2")

class DefaultMoviesRepositoryTest {
    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()
    private val repository = DefaultMoviesRepository(local = local, remote = remote)

    @Test
    fun `the pages a selection reads are the ones a source filled for it`() =
        runTest {
            remote.alwaysAnswerMovies(
                NetworkResult.Success(MoviePage(movies = MOVIES, next = null)),
            )

            val rows = repository.moviesPagingFlow(SELECTION).asSnapshot()

            assertEquals(MOVIES, rows)
        }

    @Test
    fun `a fetch asks for the page size the policy owns, and the next resumes on the cursor`() =
        runTest {
            remote.scriptMovies(
                NetworkResult.Success(MoviePage(movies = MOVIES, next = NEXT)),
                NetworkResult.Success(MoviePage(movies = MORE_MOVIES, next = null)),
            )

            val rows = repository.moviesPagingFlow(SELECTION).asSnapshot()

            assertEquals(MOVIES + MORE_MOVIES, rows)
            assertEquals(
                listOf(
                    MoviesRequest(SELECTION, PAGE_SIZE, after = null),
                    MoviesRequest(SELECTION, PAGE_SIZE, after = NEXT),
                ),
                remote.requests,
            )
        }
}
