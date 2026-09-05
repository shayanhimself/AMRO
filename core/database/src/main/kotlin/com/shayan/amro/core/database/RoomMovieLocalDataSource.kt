package com.shayan.amro.core.database

import com.shayan.amro.core.database.dao.MovieDao
import com.shayan.amro.core.database.dao.MovieDetailDao
import com.shayan.amro.core.database.mapper.toEntity
import com.shayan.amro.core.database.mapper.toMovie
import com.shayan.amro.core.database.mapper.toMovieDetail
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [MovieLocalDataSource] over the two Room tables, and the only place the DAOs are held. */
internal class RoomMovieLocalDataSource
    @Inject
    constructor(
        private val moviesDao: MovieDao,
        private val detailsDao: MovieDetailDao,
    ) : MovieLocalDataSource {
        override fun getTrendingMoviesFlow(): Flow<List<Movie>> =
            moviesDao.getTrendingMoviesFlow().map { rows -> rows.map { it.toMovie() } }

        override suspend fun replaceTrending(movies: List<Movie>) {
            this.moviesDao.replaceAll(movies.map { it.toEntity() })
        }

        override fun getMovieFlow(id: MovieId): Flow<Movie?> =
            moviesDao
                .getMovieFlow(
                    source = id.source.value,
                    movieId = id.value,
                ).map { it?.toMovie() }

        override fun getMovieDetailFlow(id: MovieId): Flow<MovieDetail?> =
            detailsDao
                .getMovieDetailFlow(
                    source = id.source.value,
                    movieId = id.value,
                ).map { it?.toMovieDetail() }

        override suspend fun writeMovieDetail(detail: MovieDetail) {
            detailsDao.update(detail.toEntity())
        }
    }
