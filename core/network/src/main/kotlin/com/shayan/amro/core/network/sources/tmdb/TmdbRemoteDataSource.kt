package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.sources.MovieId
import com.shayan.amro.core.network.sources.tmdb.mapper.toDiscoverQuery
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
         * Reads the catalogue TMDB's discover endpoint narrows and orders for [selection].
         *
         * Discover's paging is stable: page one repeated is identical and adjacent pages share no
         * movie. So the walk concatenates whole pages with nothing to deduplicate and nothing to
         * trim, and stops once it holds [count] of them.
         *
         * @return at least [count] movies while the catalogue lasts, and the page to resume on.
         * A null cursor means TMDB has served everything it will for this query, which is either
         * an empty page or the deepest page it answers.
         */
        override suspend fun getMovies(
            selection: MovieSelection,
            count: Int,
            after: Cursor?,
        ): NetworkResult<MoviePage> {
            val query = selection.toDiscoverQuery()
            var page = after?.toPage() ?: FIRST_PAGE
            // A cursor beyond the last page TMDB answers is the end of the list rather than a
            // request that would come back a 400.
            if (page > TMDB_LAST_PAGE) return NetworkResult.Success(MoviePage(emptyList(), null))

            val movies = mutableListOf<Movie>()
            while (true) {
                val rows =
                    when (val result = api.discoverPage(page = page, query = query)) {
                        // A page that fails fails the call, discarding what was read before it: a
                        // caller cannot tell a list cut short from one that is simply short.
                        is NetworkResult.Failure -> return result

                        is NetworkResult.Success -> result.value.results
                    }
                if (rows.isEmpty()) break
                movies += rows.map { it.toMovie() }
                if (page == TMDB_LAST_PAGE) break
                page++
                if (movies.size >= count) {
                    return NetworkResult.Success(MoviePage(movies, Cursor(page.toString())))
                }
            }
            return NetworkResult.Success(MoviePage(movies, null))
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
 * The page this cursor resumes on.
 *
 * TMDB pages by number, so this source writes one into every cursor it issues. Anything else was
 * not written here, and the head of the list is the only honest place to resume from.
 */
private fun Cursor.toPage(): Int = value.toIntOrNull() ?: FIRST_PAGE
