package com.shayan.amro.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.MovieRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * How many movies one fetch asks a source for. The remote data source does not guarantee that it
 * will return pages with this exact size, but it will not be less than this number.
 *
 * How large a page is belongs to the policy that asks for it, and a provider's own page size is a
 * fact the source absorbs.
 */
internal const val PAGE_SIZE = 50

/** How many fetches the first read of a list is worth, so it opens on more than one screen. */
private const val FETCHES_ON_FIRST_LOAD = 2

/**
 * Serves one selection's movies from the database and fills them from a source.
 */
internal class DefaultMoviesRepository
    @Inject
    constructor(
        private val local: MovieLocalDataSource,
        private val remote: MovieRemoteDataSource,
    ) : MoviesRepository {
        @OptIn(ExperimentalPagingApi::class)
        override fun moviesPagingFlow(selection: MovieSelection): Flow<PagingData<Movie>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        initialLoadSize = PAGE_SIZE * FETCHES_ON_FIRST_LOAD,
                        prefetchDistance = PAGE_SIZE,
                        enablePlaceholders = false,
                    ),
                remoteMediator = MoviesRemoteMediator(selection, local, remote),
            ) {
                local.moviesPagingSource(selection.queryKey())
            }.flow
    }
