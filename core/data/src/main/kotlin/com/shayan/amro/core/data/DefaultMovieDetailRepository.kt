package com.shayan.amro.core.data

import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Serves one movie's records from the database and fills the fuller one from a source.
 */
internal class DefaultMovieDetailRepository
    @Inject
    constructor(
        private val local: MovieLocalDataSource,
        private val remote: MovieRemoteDataSource,
    ) : MovieDetailRepository {
        override fun getMovieFlow(id: String): Flow<Movie?> = local.getMovieFlow(id)

        override fun getMovieDetailFlow(id: String): Flow<MovieDetail?> =
            local.getMovieDetailFlow(id)

        override suspend fun refresh(id: String): DataError? =
            when (val result = remote.getMovieDetail(id)) {
                is NetworkResult.Success -> {
                    local.writeMovieDetail(result.value)
                    null
                }

                is NetworkResult.Failure -> {
                    result.error
                }
            }
    }
