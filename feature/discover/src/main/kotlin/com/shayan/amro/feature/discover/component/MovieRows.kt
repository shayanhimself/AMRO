package com.shayan.amro.feature.discover.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import com.shayan.amro.feature.discover.viewmodel.MovieRowUiState

/**
 * The rows a list holds, however they are paged.
 *
 * The list reads its rows through this rather than from a paged loader directly, so a preview and
 * a golden render the rows they were handed instead of waiting on a load that a rendering pass has
 * no way to run.
 */
@Stable
internal interface MovieRows {
    /** How many rows are loaded. */
    val count: Int

    /**
     * The row at [index]. Reading one is also what asks for the page it sits on.
     *
     * @return null while that page is still arriving.
     */
    operator fun get(index: Int): MovieRowUiState?

    /** What identifies the row at [index], without asking for the page it sits on. */
    fun keyAt(index: Int): Any
}

/**
 * A paged loader's rows.
 *
 * @param items what the loader has, and what asks it for more.
 */
@Composable
internal fun movieRows(items: LazyPagingItems<MovieRowUiState>): MovieRows =
    remember(items) { PagedMovieRows(items) }

/**
 * A list of rows already in hand, which is what a preview and a golden render.
 *
 * @param rows the rows, in the order the list holds them.
 */
internal fun movieRows(rows: List<MovieRowUiState>): MovieRows = HeldMovieRows(rows)

private class PagedMovieRows(
    private val items: LazyPagingItems<MovieRowUiState>,
) : MovieRows {
    override val count: Int get() = items.itemCount

    override fun get(index: Int): MovieRowUiState? = items[index]

    override fun keyAt(index: Int): Any = items.peek(index)?.movieId ?: index
}

private class HeldMovieRows(
    private val rows: List<MovieRowUiState>,
) : MovieRows {
    override val count: Int get() = rows.size

    override fun get(index: Int): MovieRowUiState = rows[index]

    override fun keyAt(index: Int): Any = rows[index].movieId
}
