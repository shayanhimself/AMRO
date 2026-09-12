package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult

/**
 * What one [FakeMovieRemoteDataSource.getMovies] call was asked for.
 *
 * @property selection which movies it wanted, and in what order.
 * @property count how many of them.
 * @property after where it resumed, or null where it asked for the head of the list.
 */
data class MoviesRequest(
    val selection: MovieSelection,
    val count: Int,
    val after: Cursor?,
)

/**
 * A source answering each call with the next result it was scripted, and recording what it was
 * asked for.
 */
class FakeMovieRemoteDataSource : MovieRemoteDataSource {
    // A queue read from the front: a call pops the head, so results come back in the order they
    // were scripted. ArrayDeque states that use in the type, where a list would leave it implied.
    private val movieResults = ArrayDeque<NetworkResult<MoviePage>>()
    private val detailResults = ArrayDeque<NetworkResult<MovieDetail>>()

    private var standingMoviesResult: NetworkResult<MoviePage>? = null
    private var standingDetailResult: NetworkResult<MovieDetail>? = null

    private val recordedRequests = mutableListOf<MoviesRequest>()
    private val recordedIds = mutableListOf<String>()

    /** What each [getMovies] call asked for, in call order. */
    val requests: List<MoviesRequest> get() = recordedRequests.toList()

    /** The id each [getMovieDetail] call asked for, in call order. */
    val requestedIds: List<String> get() = recordedIds.toList()

    /**
     * Scripts what the next [getMovies] calls answer.
     *
     * @param results one per call, in call order.
     */
    fun scriptMovies(vararg results: NetworkResult<MoviePage>) {
        movieResults += results
    }

    /**
     * Scripts what the next [getMovieDetail] calls answer.
     *
     * @param results one per call, in call order.
     */
    fun scriptMovieDetail(vararg results: NetworkResult<MovieDetail>) {
        detailResults += results
    }

    /** Answers every [getMovies] call [result] once the scripted ones run out. */
    fun alwaysAnswerMovies(result: NetworkResult<MoviePage>) {
        standingMoviesResult = result
    }

    /** Answers every [getMovieDetail] call [result] once the scripted ones run out. */
    fun alwaysAnswerMovieDetail(result: NetworkResult<MovieDetail>) {
        standingDetailResult = result
    }

    override suspend fun getMovies(
        selection: MovieSelection,
        count: Int,
        after: Cursor?,
    ): NetworkResult<MoviePage> {
        recordedRequests += MoviesRequest(selection = selection, count = count, after = after)
        return checkNotNull(movieResults.removeFirstOrNull() ?: standingMoviesResult) {
            "No movies result scripted for call ${recordedRequests.size}."
        }
    }

    override suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail> {
        recordedIds += movieId
        return checkNotNull(detailResults.removeFirstOrNull() ?: standingDetailResult) {
            "No movie detail result scripted for $movieId."
        }
    }
}
