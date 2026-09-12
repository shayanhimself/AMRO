package com.shayan.amro.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult

/**
 * Fills one query's list from a source, so the database stays what the screen reads.
 *
 * @param selection which movies the list holds, and in what order.
 * @param local where the movies and their order are kept.
 * @param remote who serves them.
 */
@OptIn(ExperimentalPagingApi::class)
internal class MoviesRemoteMediator(
    private val selection: MovieSelection,
    private val local: MovieLocalDataSource,
    private val remote: MovieRemoteDataSource,
) : RemoteMediator<Int, Movie>() {
    private val queryKey = selection.queryKey()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Movie>,
    ): MediatorResult =
        when (loadType) {
            // The list only grows downward.
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)

            LoadType.REFRESH -> replace()

            LoadType.APPEND -> append()
        }

    /** Reads the head of the list again and writes it over what this query held. */
    private suspend fun replace(): MediatorResult =
        fetch(after = null) { page -> local.replaceQuery(queryKey, page) }

    /**
     * Reads the next stretch of the list, resuming where the last one stopped.
     *
     * A stored cursor that is null is the source having served everything it will, which is the
     * one end of pagination: a stretch shorter than the count asked for is not.
     */
    private suspend fun append(): MediatorResult {
        val cursor =
            local.queryCursor(queryKey)
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        return fetch(after = cursor) { page -> local.appendQuery(queryKey, page) }
    }

    /**
     * Fetches one stretch and writes it, leaving the cache untouched when it fails.
     *
     * @param after where to resume, or null to start at the head of the list.
     * @param write what to do with the stretch that came back.
     */
    private suspend fun fetch(
        after: Cursor?,
        write: suspend (page: MoviePage) -> Unit,
    ): MediatorResult =
        when (val result = remote.getMovies(selection, PAGE_SIZE, after)) {
            is NetworkResult.Failure -> {
                MediatorResult.Error(DataErrorException(result.error))
            }

            is NetworkResult.Success -> {
                write(result.value)
                MediatorResult.Success(endOfPaginationReached = result.value.next == null)
            }
        }
}
