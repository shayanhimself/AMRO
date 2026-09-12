package com.shayan.amro.core.database

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shayan.amro.core.database.entity.MovieEntity
import com.shayan.amro.core.database.mapper.toEntity
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PAGE_SIZE = 2

/** How far down the list a window sits when the head of it is no longer loaded. */
private const val ROWS_ABOVE_THE_WINDOW = 100

/** The rows the delegate serves, which are the movies the wrapper should publish. */
private val MOVIES = MovieFixture.rankedMovies(count = 4)

class MoviePagingSourceTest {
    @Test
    fun `a loaded page arrives as movies, keeping the keys the delegate set`() =
        runTest {
            val source = MoviePagingSource(FakeRowSource())

            val page =
                assertIs<PagingSource.LoadResult.Page<Int, Movie>>(
                    source.load(refreshParams()),
                )

            assertEquals(MOVIES.take(PAGE_SIZE), page.data)
            assertEquals(PAGE_SIZE, page.nextKey)
            assertNull(page.prevKey)
        }

    @Test
    fun `a delegate that fails fails the load, carrying what it threw`() =
        runTest {
            val thrown = IllegalStateException("the table went away")
            val source = MoviePagingSource(FakeRowSource(fails = thrown))

            val error =
                assertIs<PagingSource.LoadResult.Error<Int, Movie>>(source.load(refreshParams()))

            assertEquals(thrown, error.throwable)
        }

    @Test
    fun `a delegate that has been invalidated invalidates the read over it`() =
        runTest {
            val rows = FakeRowSource()
            val source = MoviePagingSource(rows)

            rows.invalidate()

            assertTrue(source.invalid)
            assertIs<PagingSource.LoadResult.Invalid<Int, Movie>>(source.load(refreshParams()))
        }

    @Test
    fun `a delegate invalidated before the read was built invalidates it at once`() {
        val rows = FakeRowSource().apply { invalidate() }

        assertTrue(MoviePagingSource(rows).invalid)
    }

    @Test
    fun `a refresh reloads around the row the reader was at`() {
        val source = MoviePagingSource(FakeRowSource())
        val loaded =
            PagingSource.LoadResult.Page(
                data = MOVIES.take(PAGE_SIZE),
                prevKey = null,
                nextKey = PAGE_SIZE,
            )

        val key =
            source.getRefreshKey(
                PagingState(
                    pages = listOf(loaded),
                    anchorPosition = 1,
                    config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                    leadingPlaceholderCount = 0,
                ),
            )

        assertEquals(0, key)
    }

    @Test
    fun `a refresh over a window further down the list reloads around the list's own row`() {
        val source = MoviePagingSource(FakeRowSource())
        val loaded =
            PagingSource.LoadResult.Page(
                data = MOVIES.take(PAGE_SIZE),
                prevKey = ROWS_ABOVE_THE_WINDOW,
                nextKey = ROWS_ABOVE_THE_WINDOW + PAGE_SIZE,
                itemsBefore = ROWS_ABOVE_THE_WINDOW,
                itemsAfter = 0,
            )
        val config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)

        val key =
            source.getRefreshKey(
                PagingState(
                    pages = listOf(loaded),
                    anchorPosition = 1,
                    config = config,
                    leadingPlaceholderCount = 0,
                ),
            )

        assertEquals(
            ROWS_ABOVE_THE_WINDOW + 1 - config.initialLoadSize / 2,
            key,
            "an anchor counts from the first row loaded, not from the head of the list",
        )
    }

    @Test
    fun `a refresh with nothing on screen reloads from the head of the list`() {
        val source = MoviePagingSource(FakeRowSource())

        assertNull(
            source.getRefreshKey(
                PagingState(
                    pages = emptyList(),
                    anchorPosition = null,
                    config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                    leadingPlaceholderCount = 0,
                ),
            ),
        )
    }

    @Test
    fun `jumping is whatever the delegate supports`() {
        assertFalse(MoviePagingSource(FakeRowSource()).jumpingSupported)
    }
}

/** A refresh of the head of the list. */
private fun refreshParams() =
    PagingSource.LoadParams.Refresh<Int>(
        key = null,
        loadSize = PAGE_SIZE,
        placeholdersEnabled = false,
    )

/**
 * A paged read of rows, standing in for the one Room builds.
 *
 * @param fails what every load throws instead of answering, or null where it answers.
 */
private class FakeRowSource(
    private val fails: Throwable? = null,
) : PagingSource<Int, MovieEntity>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieEntity> {
        if (invalid) return LoadResult.Invalid()
        fails?.let { return LoadResult.Error(it) }
        val from = params.key ?: 0
        val rows = MOVIES.drop(from).take(params.loadSize).map { it.toEntity() }
        return LoadResult.Page(
            data = rows,
            prevKey = (from - params.loadSize).takeIf { it >= 0 },
            nextKey = (from + rows.size).takeIf { it < MOVIES.size },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, MovieEntity>): Int? = null
}
