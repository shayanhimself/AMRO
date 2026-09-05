package com.shayan.amro.core.testing.fake

import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * The movies a device holds, as two maps behind [MutableStateFlow]s.
 *
 * It stands in for the Room implementation, which is `internal` to its own module and so cannot be
 * reached from the modules that read this contract.
 */
class FakeMovieLocalDataSource : MovieLocalDataSource {
    private val trending = MutableStateFlow<List<Movie>>(emptyList())
    private val details = MutableStateFlow<Map<MovieId, MovieDetail>>(emptyMap())

    override fun getTrendingMoviesFlow(): Flow<List<Movie>> = trending.asStateFlow()

    override suspend fun replaceTrending(movies: List<Movie>) {
        trending.value = movies
    }

    override fun getMovieFlow(id: MovieId): Flow<Movie?> =
        trending.map { movies -> movies.firstOrNull { it.id == id } }

    override fun getMovieDetailFlow(id: MovieId): Flow<MovieDetail?> = details.map { it[id] }

    override suspend fun writeMovieDetail(detail: MovieDetail) {
        details.value += (detail.movie.id to detail)
    }
}
