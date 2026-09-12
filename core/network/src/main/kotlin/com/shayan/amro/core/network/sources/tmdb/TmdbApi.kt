package com.shayan.amro.core.network.sources.tmdb

import androidx.annotation.VisibleForTesting
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.apiCall
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbGenreListDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDetailDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMoviePageDto
import com.shayan.amro.core.network.sources.tmdb.mapper.TmdbDiscoverQuery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import javax.inject.Inject

private const val DISCOVER_PATH = "discover/movie"

private const val MOVIE_PATH = "movie/"

private const val GENRE_LIST_PATH = "genre/movie/list"

private const val PAGE_PARAMETER = "page"

private const val SORT_BY_PARAMETER = "sort_by"

private const val WITH_GENRES_PARAMETER = "with_genres"

/**
 * Every TMDB endpoint the app calls.
 *
 * @param client Ktor client that carries the requests, already pointed at the API baseUrl.
 */
internal class TmdbApi
    @Inject
    constructor(
        private val client: HttpClient,
    ) {
        /**
         * Reads one page of the catalogue a query narrows and orders.
         *
         * `include_adult` defaults to false and is not sent.
         *
         * @param page which page to read, counted from one. TMDB fixes the page size, so a page
         * holds [TMDB_PAGE_SIZE] movies.
         * @param query how the catalogue is narrowed and ordered.
         */
        suspend fun discoverPage(
            page: Int,
            query: TmdbDiscoverQuery,
        ): NetworkResult<TmdbMoviePageDto> =
            get(DISCOVER_PATH) {
                parameter(PAGE_PARAMETER, page)
                parameter(SORT_BY_PARAMETER, query.sortBy)
                query.withGenres?.let { parameter(WITH_GENRES_PARAMETER, it) }
            }

        /**
         * Reads the fuller details behind one movie.
         *
         * @param movieId the movie id as TMDB wrote it.
         */
        suspend fun movieDetail(movieId: String): NetworkResult<TmdbMovieDetailDto> =
            get(MOVIE_PATH + movieId)

        /**
         * Reads the genres TMDB publishes right now.
         */
        @VisibleForTesting
        suspend fun genreList(): NetworkResult<TmdbGenreListDto> = get(GENRE_LIST_PATH)

        /**
         * Calls one endpoint and parses what came back into [T].
         *
         * @param path the endpoint, relative to the baseUrl the client carries.
         * @param configure carries whatever the request holds beyond its path.
         * @return the parsed body, or the cause it could not be read.
         */
        private suspend inline fun <reified T> get(
            path: String,
            crossinline configure: HttpRequestBuilder.() -> Unit = {},
        ): NetworkResult<T> =
            apiCall {
                val response = client.get(path) { configure() }
                // A non-2xx is a server failure whatever body came with it, so no error body is
                // ever parsed.
                if (!response.status.isSuccess()) {
                    return@apiCall NetworkResult.Failure(DataError.Server)
                }

                NetworkResult.Success(response.body<T>())
            }
    }
