package com.shayan.amro.core.database

import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import kotlinx.coroutines.flow.Flow

/**
 * The movies this device holds, as the data layer sees it.
 *
 * Everything below it is `internal`, so a caller depends on model types alone and no storage type
 * reaches a layer above.
 */
interface MovieLocalDataSource {
    /**
     * The trending set this device holds.
     *
     * @return the movies, in an unspecified order.
     */
    fun getTrendingMoviesFlow(): Flow<List<Movie>>

    /**
     * Replaces the trending set.
     *
     * @param movies become the new trending set.
     */
    suspend fun replaceTrending(movies: List<Movie>)

    /**
     * One movie of the trending set.
     *
     * @param id of the movie.
     * @return the flow of movie, or null when it is not in the set this device holds.
     */
    fun getMovieFlow(id: String): Flow<Movie?>

    /**
     * The full movie detail this device holds for one movie.
     *
     * @param id of the movie.
     * @return flow of the movie detail, or null when none has been written for it.
     */
    fun getMovieDetailFlow(id: String): Flow<MovieDetail?>

    /**
     * Writes one movie detail record.
     *
     * @param detail movie detail to write.
     */
    suspend fun writeMovieDetail(detail: MovieDetail)
}
