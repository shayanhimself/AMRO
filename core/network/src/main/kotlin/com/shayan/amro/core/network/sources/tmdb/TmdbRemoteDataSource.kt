package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.sources.MovieId
import com.shayan.amro.core.network.sources.tmdb.mapper.toMovie
import com.shayan.amro.core.network.sources.tmdb.mapper.toMovieDetail
import javax.inject.Inject

/** Stamps every id this source issues, so a second provider's ids cannot collide with these. */
internal const val TMDB_SOURCE = "tmdb"

private const val FIRST_PAGE = 1

/**
 * TMDB as a [MovieRemoteDataSource].
 *
 * @param api the endpoints this source reads, which is the only way it reaches TMDB.
 */
internal class TmdbRemoteDataSource
    @Inject
    constructor(
        private val api: TmdbApi,
    ) : MovieRemoteDataSource {
        /**
         * Returns [count] trending movies.
         *
         * It walks TMDB's pages until it holds [count] distinct movies. TMDB has a fixed page size
         * and it re-ranks between requests, so a movie can arrive twice while another is missed. So
         * paging is the only way to reach a count here. A page that fails takes the walk down with
         * it.
         *
         * It's a workaround for a known issue:
         * https://www.themoviedb.org/talk/5ee3abd1590086001f50b3c1
         * https://www.themoviedb.org/talk/5bbabe890e0a2616d7005bd6
         *
         * @return up to [count] distinct movies. Fewer means the source ran out of them or the walk
         * reached [TMDB_PAGE_CAP], both of which are complete answers rather than failures.
         */
        override suspend fun getTrendingMovies(count: Int): NetworkResult<List<Movie>> {
            val held = LinkedHashMap<String, Movie>()
            var page = FIRST_PAGE

            while (held.size < count && page <= TMDB_PAGE_CAP) {
                when (val result = api.trendingPage(page)) {
                    is NetworkResult.Failure -> {
                        return result
                    }

                    is NetworkResult.Success -> {
                        val rows = result.value.results
                        if (rows.isEmpty()) {
                            return if (page == FIRST_PAGE) {
                                NetworkResult.Failure(DataError.EmptyResponse)
                            } else {
                                held.upTo(count)
                            }
                        }
                        rows.forEach { row ->
                            val movie = row.toMovie()
                            held.putIfAbsent(movie.id, movie)
                        }
                    }
                }
                page++
            }

            return held.upTo(count)
        }

        override suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail> {
            // Separate the TMDB prefix from the movieId.
            val tmdbMovieId = MovieId.of(movieId).sourceMovieId
            return when (val result = api.movieDetail(tmdbMovieId)) {
                is NetworkResult.Failure -> result
                is NetworkResult.Success -> NetworkResult.Success(result.value.toMovieDetail())
            }
        }
    }

/**
 * The movies in this map, at most [count] of them, as a success.
 */
private fun Map<String, Movie>.upTo(count: Int): NetworkResult<List<Movie>> =
    NetworkResult.Success(values.take(count))
