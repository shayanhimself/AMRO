package com.shayan.amro.core.database

import androidx.paging.PagingSource
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import kotlinx.coroutines.flow.Flow

/**
 * The movies this device holds, as the data layer sees it.
 *
 * Everything below it is `internal`, so a caller depends on model types alone and no storage type
 * reaches a layer above.
 *
 * A query key names one narrowing and ordering. It is the app's own, so the same selection is one
 * cache whichever source answered it, and the layer that builds it owns its grammar.
 */
interface MovieLocalDataSource {
    /**
     * The movies one query produced, paged, in the order the source that answered it ranked them.
     *
     * @param queryKey which query to read.
     */
    fun moviesPagingSource(queryKey: String): PagingSource<Int, Movie>

    /**
     * Drops what one query held and writes the head of its list again.
     *
     * Other queries and every movie row are left alone.
     *
     * @param queryKey which query to replace.
     * @param page the head of its list, and where it resumes.
     */
    suspend fun replaceQuery(
        queryKey: String,
        page: MoviePage,
    )

    /**
     * Adds the next stretch of one query's list, after everything it already holds.
     *
     * A movie the query already holds keeps the place it first took, so a source that re-ranks
     * between two calls cannot put one movie in a list twice.
     *
     * @param queryKey which query to extend.
     * @param page the movies to add, and where the list resumes after them.
     */
    suspend fun appendQuery(
        queryKey: String,
        page: MoviePage,
    )

    /**
     * Where one query resumes.
     *
     * @param queryKey which query to read.
     * @return the cursor, or null when the source has served everything it will for that query.
     */
    suspend fun queryCursor(queryKey: String): Cursor?

    /**
     * One movie, by the id it was stored under.
     *
     * @param id of the movie.
     * @return the flow of movie, or null when this device holds no row for it.
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
