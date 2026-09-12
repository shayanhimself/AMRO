package com.shayan.amro.core.database

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shayan.amro.core.database.entity.MovieEntity
import com.shayan.amro.core.database.mapper.toMovie
import com.shayan.amro.core.model.Movie

/** A reload puts the row the reader was on in the middle, so it fills equally on each side. */
private const val SIDES_OF_THE_ANCHOR = 2

/**
 * Room's paged read of the movie rows, published in model types.
 *
 * Room builds a source over its own entity, and an entity never leaves this module, so the two are
 * bridged here rather than by mapping the pages in the layer above.
 *
 * @param rows what Room loads and invalidates.
 */
internal class MoviePagingSource(
    private val rows: PagingSource<Int, MovieEntity>,
) : PagingSource<Int, Movie>() {
    init {
        // A write invalidates Room's source, and Paging is holding this one, so the signal has to
        // be passed on or a refreshed table never reaches the screen.
        rows.registerInvalidatedCallback(::invalidate)
        if (rows.invalid) invalidate()
    }

    override val jumpingSupported: Boolean get() = rows.jumpingSupported

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> =
        when (val result = rows.load(params)) {
            is LoadResult.Page -> {
                LoadResult.Page(
                    data = result.data.map { it.toMovie() },
                    prevKey = result.prevKey,
                    nextKey = result.nextKey,
                    itemsBefore = result.itemsBefore,
                    itemsAfter = result.itemsAfter,
                )
            }

            is LoadResult.Error -> {
                LoadResult.Error(result.throwable)
            }

            is LoadResult.Invalid -> {
                LoadResult.Invalid()
            }
        }

    /**
     * The row a refresh reloads around, which keeps the reader where they were reading.
     *
     * It is worked out here rather than delegated, because the delegate reads a state over the
     * rows it loaded and this source has already replaced them with movies. A key is a row's
     * place in the query, and a reload takes as many rows before the reader as after, so the row
     * they were on ends up in the middle of what comes back.
     */
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { anchor ->
            val row = anchor + firstLoadedRow(state)
            maxOf(0, row - state.config.initialLoadSize / SIDES_OF_THE_ANCHOR)
        }

    /**
     * Where the rows now on screen begin in the query.
     *
     * An anchor counts from the first row loaded rather than from the first row of the query
     * whenever placeholders are off, and then a window that no longer starts at the head of the
     * query is that many rows further down than the anchor says.
     */
    private fun firstLoadedRow(state: PagingState<Int, Movie>): Int =
        if (state.config.enablePlaceholders) {
            0
        } else {
            state.pages
                .firstOrNull()
                ?.itemsBefore
                ?.coerceAtLeast(0) ?: 0
        }
}
