package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.data.MovieDetailRepository
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A detail repository over seeded flows, answering a refresh with the outcome scripted for that
 * movie.
 *
 * A refresh for a movie nothing was scripted for fails the test rather than inventing an outcome.
 */
class FakeMovieDetailRepository : MovieDetailRepository {
    private val movies = MutableStateFlow<Map<MovieId, Movie>>(emptyMap())
    private val details = MutableStateFlow<Map<MovieId, MovieDetail>>(emptyMap())
    private val refreshes = mutableMapOf<MovieId, DetailRefresh>()
    private val recordedIds = mutableListOf<MovieId>()

    /** The id each [refresh] call asked for, in call order. */
    val requestedIds: List<MovieId> get() = recordedIds.toList()

    /**
     * Sets what [getMovieFlow] emits, as a trending set cached before the test began.
     *
     * @param movie the movie that set holds.
     */
    fun seedMovie(movie: Movie) {
        movies.value += (movie.id to movie)
    }

    /**
     * Sets what [getMovieDetailFlow] emits, as a record cached before the test began.
     *
     * @param detail the record this device holds.
     */
    fun seedDetail(detail: MovieDetail) {
        details.value += (detail.movie.id to detail)
    }

    /**
     * Scripts a refresh of that movie that succeeds.
     *
     * @param detail what [getMovieDetailFlow] emits once it has run.
     */
    fun scriptRefresh(detail: MovieDetail) {
        refreshes[detail.movie.id] = DetailRefresh.Succeeds(detail)
    }

    /**
     * Scripts a refresh of one movie that fails, leaving what the reads emit alone.
     *
     * @param id of the movie.
     * @param error the cause it returns.
     */
    fun scriptRefreshFailure(
        id: MovieId,
        error: DataError,
    ) {
        refreshes[id] = DetailRefresh.Fails(error)
    }

    override fun getMovieFlow(id: MovieId): Flow<Movie?> = movies.map { it[id] }

    override fun getMovieDetailFlow(id: MovieId): Flow<MovieDetail?> = details.map { it[id] }

    override suspend fun refresh(id: MovieId): DataError? {
        recordedIds += id
        val next =
            checkNotNull(refreshes[id]) {
                "No refresh outcome scripted for $id."
            }
        return when (next) {
            is DetailRefresh.Succeeds -> {
                seedDetail(next.detail)
                null
            }

            is DetailRefresh.Fails -> {
                next.error
            }
        }
    }
}

/** What one scripted refresh does. */
private sealed interface DetailRefresh {
    data class Succeeds(
        val detail: MovieDetail,
    ) : DetailRefresh

    data class Fails(
        val error: DataError,
    ) : DetailRefresh
}
