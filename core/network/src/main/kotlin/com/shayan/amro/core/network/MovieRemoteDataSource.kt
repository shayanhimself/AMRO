package com.shayan.amro.core.network

import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId

/**
 * One provider of movies, as the data layer sees it.
 */
interface MovieRemoteDataSource {
    /**
     * Reads the head of this source's trending list.
     *
     * @param count how many distinct movies are wanted.
     * @return up to [count] distinct movies, in the order the source ranks them. Fewer means the
     * source has no more to give, which is a complete answer. A failure means the list could not be
     * read, and no movies come back with it: a source never presents a part of a list as the whole.
     */
    suspend fun trending(count: Int): NetworkResult<List<Movie>>

    /**
     * Reads the fuller record behind one movie.
     *
     * @param id an id this source issued.
     * @return the detail, or the cause it could not be read.
     */
    suspend fun movieDetail(id: MovieId): NetworkResult<MovieDetail>
}
