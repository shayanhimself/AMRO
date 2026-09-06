package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.data.TrendingMoviesRepository
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What one scripted refresh does. */
private sealed interface TrendingRefresh {
    data class Succeeds(
        val movies: List<Movie>,
    ) : TrendingRefresh

    data class Fails(
        val error: DataError,
    ) : TrendingRefresh
}

/**
 * A trending repository over a seeded flow, answering each refresh with the next outcome it was
 * scripted.
 *
 * An unscripted refresh fails the test rather than inventing an outcome.
 */
class FakeTrendingMoviesRepository : TrendingMoviesRepository {
    private val trending = MutableStateFlow<List<Movie>>(emptyList())
    private val refreshes = ArrayDeque<TrendingRefresh>()

    private var gate: CompletableDeferred<Unit>? = null

    /** How many times [refresh] has been called. */
    var refreshCount: Int = 0
        private set

    /**
     * Holds every later refresh at its start, so a caller can be read while one is running.
     *
     * @return the gate to complete when the held refreshes should go on and finish.
     */
    fun holdRefreshes(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().also { gate = it }

    /**
     * Sets what the read emits, as a cache written before the test began.
     *
     * @param movies the trending set on this device.
     */
    fun seedTrending(movies: List<Movie>) {
        trending.value = movies
    }

    /**
     * Scripts a refresh that succeeds.
     *
     * @param movies what the read emits once it has run.
     */
    fun scriptRefresh(movies: List<Movie>) {
        refreshes += TrendingRefresh.Succeeds(movies)
    }

    /**
     * Scripts a refresh that fails, leaving what the read emits alone.
     *
     * @param error the cause it returns.
     */
    fun scriptRefreshFailure(error: DataError) {
        refreshes += TrendingRefresh.Fails(error)
    }

    override fun getTrendingMoviesFlow(): Flow<List<Movie>> = trending.asStateFlow()

    override suspend fun refresh(): DataError? {
        refreshCount++
        gate?.await()
        val next =
            checkNotNull(refreshes.removeFirstOrNull()) {
                "No refresh outcome scripted for call $refreshCount."
            }
        return when (next) {
            is TrendingRefresh.Succeeds -> {
                trending.value = next.movies
                null
            }

            is TrendingRefresh.Fails -> {
                next.error
            }
        }
    }
}
