package com.shayan.amro.core.data

import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * How many distinct movies the trending list holds.
 */
internal const val TRENDING_COUNT = 100

/**
 * Serves the trending movies set from the database and fills it from a source.
 */
internal class DefaultTrendingMoviesRepository
    @Inject
    constructor(
        private val local: MovieLocalDataSource,
        private val remote: MovieRemoteDataSource,
    ) : TrendingMoviesRepository {
        override fun getTrendingMoviesFlow(): Flow<List<Movie>> = local.getTrendingMoviesFlow()

        override suspend fun refresh(): DataError? =
            when (val result = remote.getTrendingMovies(TRENDING_COUNT)) {
                is NetworkResult.Success -> {
                    local.replaceTrending(result.value)
                    null
                }

                is NetworkResult.Failure -> {
                    result.error
                }
            }
    }
