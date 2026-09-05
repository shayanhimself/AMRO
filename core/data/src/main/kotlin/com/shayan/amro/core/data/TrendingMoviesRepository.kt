package com.shayan.amro.core.data

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Provides the trending movies set.
 */
interface TrendingMoviesRepository {
    /**
     * The trending set this device holds.
     *
     * @return the movies flow, in an unspecified order. Empty until a refresh has succeeded once.
     */
    fun getTrendingMoviesFlow(): Flow<List<Movie>>

    /**
     * Triggers a fetch of the trending set.
     *
     * @return error if it failed, null when successful.
     */
    suspend fun refresh(): DataError?
}
