package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.apiCall
import com.shayan.amro.core.network.sources.MovieId
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDetailDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbTrendingPageDto
import com.shayan.amro.core.network.sources.tmdb.mapper.toMovie
import com.shayan.amro.core.network.sources.tmdb.mapper.toMovieDetail
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import javax.inject.Inject

/** Stamps every id this source issues, so a second provider's ids cannot collide with these. */
internal const val TMDB_SOURCE = "tmdb"

/**
 * TMDB's trending list, relative to the configured base.
 */
private const val TRENDING_PATH = "trending/movie/week"

private const val MOVIE_DETAIL_PATH = "movie/"

private const val PAGE_PARAMETER = "page"

private const val FIRST_PAGE = 1

/**
 * TMDB as a [MovieRemoteDataSource].
 */
internal class TmdbRemoteDataSource
    @Inject
    constructor(
        private val client: HttpClient,
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
                when (val result = trendingPage(page)) {
                    is NetworkResult.Failure -> {
                        return result
                    }

                    is NetworkResult.Success -> {
                        if (result.value.isEmpty()) {
                            return if (page == FIRST_PAGE) {
                                NetworkResult.Failure(DataError.EmptyResponse)
                            } else {
                                held.upTo(count)
                            }
                        }
                        result.value.forEach { row ->
                            val movie = row.toMovie()
                            held.putIfAbsent(movie.id, movie)
                        }
                    }
                }
                page++
            }

            return held.upTo(count)
        }

        override suspend fun getMovieDetail(movieId: String): NetworkResult<MovieDetail> =
            apiCall {
                val response = client.get(MOVIE_DETAIL_PATH + MovieId.of(movieId).sourceMovieId)
                if (!response.status.isSuccess()) {
                    return@apiCall NetworkResult.Failure(DataError.Server)
                }

                NetworkResult.Success(response.body<TmdbMovieDetailDto>().toMovieDetail())
            }

        /**
         * Reads one page of the trending list.
         */
        private suspend fun trendingPage(page: Int): NetworkResult<List<TmdbMovieDto>> =
            apiCall {
                val response = client.get(TRENDING_PATH) { parameter(PAGE_PARAMETER, page) }
                // A non-2xx is a server failure whatever body came with it, so no error body is
                // ever parsed.
                if (!response.status.isSuccess()) {
                    return@apiCall NetworkResult.Failure(DataError.Server)
                }

                NetworkResult.Success(response.body<TmdbTrendingPageDto>().results)
            }
    }

/**
 * The movies in this map, at most [count] of them, as a success.
 */
private fun Map<String, Movie>.upTo(count: Int): NetworkResult<List<Movie>> =
    NetworkResult.Success(values.take(count))
