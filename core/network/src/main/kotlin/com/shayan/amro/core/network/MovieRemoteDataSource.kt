package com.shayan.amro.core.network

import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection

/**
 * One provider of movies, as the data layer sees it.
 *
 * A source narrows and orders its own catalogue, so it must filter by genre and sort by every key
 * in both directions. One that cannot do both can still serve movie details.
 */
interface MovieRemoteDataSource {
    /**
     * Reads one stretch of the movies a selection describes.
     *
     * @param selection which movies to serve, and in what order. It is the app's own vocabulary,
     * and each source maps it onto its own.
     * @param count how many movies are wanted. The remote data source does not guarantee that it
     * will return pages with this exact size, but it will not be less than this number.
     * @param after where a previous call left off, or null to start at the head of the list.
     * @return the movies and where to resume. A failure means the stretch could not be read, and
     * no movies come back with it: a source never presents a part of a list as the whole.
     */
    suspend fun getMovies(
        selection: MovieSelection,
        count: Int,
        after: Cursor?,
    ): NetworkResult<MoviePage>

    /**
     * Reads the fuller record behind one movie.
     *
     * @param movieId an id this source issued.
     * @return the detail, or the cause it could not be read.
     */
    suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail>
}
