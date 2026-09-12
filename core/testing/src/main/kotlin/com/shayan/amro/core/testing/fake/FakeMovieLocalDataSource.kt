package com.shayan.amro.core.testing.fake

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * The movies a device holds, as maps behind [MutableStateFlow]s.
 *
 * It stands in for the Room implementation, which is `internal` to its own module and so cannot be
 * reached from the modules that read this contract.
 */
class FakeMovieLocalDataSource : MovieLocalDataSource {
    private val movies = MutableStateFlow<Map<String, Movie>>(emptyMap())
    private val results = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private val cursors = mutableMapOf<String, Cursor?>()
    private val details = MutableStateFlow<Map<String, MovieDetail>>(emptyMap())
    private val openReads = mutableListOf<Pair<String, ListPagingSource>>()

    /** The movies one query has produced, in the order it produced them. */
    fun queryResults(queryKey: String): List<Movie> =
        results.value[queryKey].orEmpty().mapNotNull { movies.value[it] }

    /** Every movie this device holds, whichever query put it there. */
    val heldMovies: List<Movie> get() = movies.value.values.toList()

    override fun moviesPagingSource(queryKey: String): PagingSource<Int, Movie> =
        ListPagingSource { queryResults(queryKey) }.also { openReads += queryKey to it }

    override suspend fun replaceQuery(
        queryKey: String,
        page: MoviePage,
    ) {
        results.value += (queryKey to emptyList())
        write(queryKey, page)
    }

    override suspend fun appendQuery(
        queryKey: String,
        page: MoviePage,
    ) {
        write(queryKey, page)
    }

    override suspend fun queryCursor(queryKey: String): Cursor? = cursors[queryKey]

    override fun getMovieFlow(id: String): Flow<Movie?> = movies.map { it[id] }

    override fun getMovieDetailFlow(id: String): Flow<MovieDetail?> = details.map { it[id] }

    override suspend fun writeMovieDetail(detail: MovieDetail) {
        details.value += (detail.movie.id to detail)
    }

    /**
     * Adds one stretch of movies after everything the query holds, upserting the movies
     * themselves and keeping a movie it already holds at the place it first took.
     */
    private fun write(
        queryKey: String,
        page: MoviePage,
    ) {
        val positions = results.value[queryKey].orEmpty().toMutableList()
        page.movies.forEach { movie ->
            movies.value += (movie.id to movie)
            if (movie.id !in positions) positions += movie.id
        }
        results.value += (queryKey to positions)
        cursors[queryKey] = page.next
        openReads.filter { (key, _) -> key == queryKey }.forEach { (_, read) -> read.invalidate() }
    }
}

/**
 * A paged read over whatever the fake holds when a load runs.
 *
 * @param rows what the query produced, read afresh on every load so a write is visible to the
 * next one.
 */
private class ListPagingSource(
    private val rows: () -> List<Movie>,
) : PagingSource<Int, Movie>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val all = rows()
        val from = params.key ?: 0
        val page = all.drop(from).take(params.loadSize)
        return LoadResult.Page(
            data = page,
            prevKey = (from - params.loadSize).takeIf { it >= 0 },
            nextKey = (from + page.size).takeIf { it < all.size },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
}
