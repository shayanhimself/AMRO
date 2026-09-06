package com.shayan.amro.core.data

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import kotlinx.coroutines.flow.Flow

/**
 * Provides one movie's records.
 */
interface MovieDetailRepository {
    /**
     * The minimum data of one movie.
     *
     * @param id of the movie.
     * @return the flow of movie, or null when none exists.
     */
    fun getMovieFlow(id: String): Flow<Movie?>

    /**
     * One movie with all details.
     *
     * @param id of the movie.
     * @return the flow of movie detail, or null when none exists.
     */
    fun getMovieDetailFlow(id: String): Flow<MovieDetail?>

    /**
     * Triggers a fetch of one movie's details.
     *
     * @param id of the movie.
     * @return error if it failed, null when successful.
     */
    suspend fun refresh(id: String): DataError?
}
