package com.shayan.amro.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.testing.fake.FakeMovieLocalDataSource
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A narrowing and an ordering, so a request can be shown to carry the selection it was built on. */
private val SELECTION = MovieSelection(genres = setOf(Genre.COMEDY))

/** The second query, cached beside [SELECTION] so a write under one can be shown not to reach it. */
private val OTHER_SELECTION = MovieSelection(genres = setOf(Genre.HORROR))

private val FIRST_CURSOR = Cursor("2")
private val SECOND_CURSOR = Cursor("3")

/** More movies than one page holds, so the head and the append hold different ones. */
private val MOVIES = MovieFixture.rankedMovies(count = 4)

@OptIn(ExperimentalPagingApi::class)
class MoviesRemoteMediatorTest {
    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()

    @Test
    fun `a refresh writes the head of the list and asks the source for the selection`() =
        runTest {
            remote.scriptMovies(success(MOVIES.take(2), next = FIRST_CURSOR))

            val result = mediator().load(LoadType.REFRESH, emptyState())

            assertFalse(assertIs<MediatorResult.Success>(result).endOfPaginationReached)
            assertEquals(MOVIES.take(2), local.queryResults(SELECTION.queryKey()))
            assertEquals(SELECTION, remote.requests.single().selection)
            assertEquals(PAGE_SIZE, remote.requests.single().count)
        }

    @Test
    fun `an append resumes from the cursor the last write stored`() =
        runTest {
            remote.scriptMovies(
                success(MOVIES.take(2), next = FIRST_CURSOR),
                success(MOVIES.drop(2), next = SECOND_CURSOR),
            )
            val mediator = mediator()
            mediator.load(LoadType.REFRESH, emptyState())

            mediator.load(LoadType.APPEND, emptyState())

            assertEquals(FIRST_CURSOR, remote.requests.last().after)
            assertEquals(MOVIES, local.queryResults(SELECTION.queryKey()))
        }

    @Test
    fun `a null cursor ends pagination and an append past it asks for nothing`() =
        runTest {
            remote.scriptMovies(success(MOVIES.take(2), next = null))
            val mediator = mediator()

            val refresh = mediator.load(LoadType.REFRESH, emptyState())
            val append = mediator.load(LoadType.APPEND, emptyState())

            assertTrue(assertIs<MediatorResult.Success>(refresh).endOfPaginationReached)
            assertTrue(assertIs<MediatorResult.Success>(append).endOfPaginationReached)
            assertEquals(1, remote.requests.size)
        }

    @Test
    fun `a stretch shorter than the count asked for does not end pagination`() =
        runTest {
            remote.scriptMovies(success(MOVIES.take(1), next = FIRST_CURSOR))

            val result = mediator().load(LoadType.REFRESH, emptyState())

            assertFalse(assertIs<MediatorResult.Success>(result).endOfPaginationReached)
        }

    @Test
    fun `a failed refresh returns the cause and writes nothing`() =
        runTest {
            remote.scriptMovies(
                success(MOVIES.take(2), next = FIRST_CURSOR),
                NetworkResult.Failure(DataError.NoConnectivity),
            )
            val mediator = mediator()
            mediator.load(LoadType.REFRESH, emptyState())

            val result = mediator.load(LoadType.REFRESH, emptyState())

            val thrown = assertIs<MediatorResult.Error>(result).throwable
            assertEquals(DataError.NoConnectivity, assertIs<DataErrorException>(thrown).error)
            assertEquals(MOVIES.take(2), local.queryResults(SELECTION.queryKey()))
        }

    @Test
    fun `a refresh under one selection leaves another selection's list alone`() =
        runTest {
            remote.alwaysAnswerMovies(success(MOVIES.take(2), next = FIRST_CURSOR))
            mediator(OTHER_SELECTION).load(LoadType.REFRESH, emptyState())

            remote.scriptMovies(success(MOVIES.drop(2), next = FIRST_CURSOR))
            mediator().load(LoadType.REFRESH, emptyState())

            assertEquals(MOVIES.take(2), local.queryResults(OTHER_SELECTION.queryKey()))
            assertEquals(MOVIES.drop(2), local.queryResults(SELECTION.queryKey()))
        }

    @Test
    fun `a prepend ends pagination without asking the source for anything`() =
        runTest {
            val result = mediator().load(LoadType.PREPEND, emptyState())

            assertTrue(assertIs<MediatorResult.Success>(result).endOfPaginationReached)
            assertEquals(emptyList(), remote.requests)
        }

    @Test
    fun `an append leaves the movie rows a refresh no longer references`() =
        runTest {
            remote.scriptMovies(
                success(MOVIES.take(2), next = FIRST_CURSOR),
                success(MOVIES.drop(2), next = null),
            )
            val mediator = mediator()
            mediator.load(LoadType.REFRESH, emptyState())

            mediator.load(LoadType.REFRESH, emptyState())

            assertEquals(MOVIES.map { it.id }.toSet(), local.heldMovies.map { it.id }.toSet())
        }

    private fun mediator(selection: MovieSelection = SELECTION) =
        MoviesRemoteMediator(selection = selection, local = local, remote = remote)
}

/** One stretch of a list, as a source answers with one. */
private fun success(
    movies: List<Movie>,
    next: Cursor?,
): NetworkResult<MoviePage> = NetworkResult.Success(MoviePage(movies = movies, next = next))

/**
 * The state of a list nothing has been loaded into yet.
 *
 * The mediator reads where to resume from the database rather than from this, so no test here has
 * a reason to build a state with pages in it.
 */
private fun emptyState() =
    PagingState<Int, Movie>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        leadingPlaceholderCount = 0,
    )
