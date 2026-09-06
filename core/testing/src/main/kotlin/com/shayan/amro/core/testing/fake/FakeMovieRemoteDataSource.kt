package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult

/**
 * A source answering each call with the next result it was scripted, and recording what it was
 * asked for.
 *
 * An unscripted call fails the test rather than inventing an answer.
 */
class FakeMovieRemoteDataSource : MovieRemoteDataSource {
    // A queue read from the front: a call pops the head, so results come back in the order they
    // were scripted. ArrayDeque states that use in the type, where a list would leave it implied.
    private val trendingResults = ArrayDeque<NetworkResult<List<Movie>>>()
    private val detailResults = ArrayDeque<NetworkResult<MovieDetail>>()

    private val recordedCounts = mutableListOf<Int>()
    private val recordedIds = mutableListOf<String>()

    /** The count each [getTrendingMovies] call asked for, in call order. */
    val requestedCounts: List<Int> get() = recordedCounts.toList()

    /** The id each [getMovieDetail] call asked for, in call order. */
    val requestedIds: List<String> get() = recordedIds.toList()

    /**
     * Scripts what the next [getTrendingMovies] calls answer.
     *
     * @param results one per call, in call order.
     */
    fun scriptTrending(vararg results: NetworkResult<List<Movie>>) {
        trendingResults += results
    }

    /**
     * Scripts what the next [getMovieDetail] calls answer.
     *
     * @param results one per call, in call order.
     */
    fun scriptMovieDetail(vararg results: NetworkResult<MovieDetail>) {
        detailResults += results
    }

    override suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>> {
        recordedCounts += count
        return checkNotNull(trendingResults.removeFirstOrNull()) {
            "No trending result scripted for call ${recordedCounts.size}."
        }
    }

    override suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail> {
        recordedIds += movieId
        return checkNotNull(detailResults.removeFirstOrNull()) {
            "No movie detail result scripted for $movieId."
        }
    }
}
