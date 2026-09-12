package com.shayan.amro.core.database

import androidx.paging.PagingSource
import com.shayan.amro.core.database.dao.MovieDao
import com.shayan.amro.core.database.dao.MovieDetailDao
import com.shayan.amro.core.database.dao.MovieQueryDao
import com.shayan.amro.core.database.mapper.toEntity
import com.shayan.amro.core.database.mapper.toMovie
import com.shayan.amro.core.database.mapper.toMovieDetail
import com.shayan.amro.core.model.Cursor
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MoviePage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/** [MovieLocalDataSource] over the Room tables, and the only place the DAOs are held. */
internal class RoomMovieLocalDataSource
    @Inject
    constructor(
        private val moviesDao: MovieDao,
        private val queriesDao: MovieQueryDao,
        private val detailsDao: MovieDetailDao,
    ) : MovieLocalDataSource {
        /**
         * The paged reads open right now, each with the query it is over.
         *
         * A write tells them itself rather than leaving it to the database's own invalidation.
         * That invalidation begins observing a table while the first read of it is running, so a
         * write landing inside that window reaches a reader that is already showing an empty list
         * and never hears there is more.
         */
        private val openReads = CopyOnWriteArrayList<Pair<String, PagingSource<Int, Movie>>>()

        override fun moviesPagingSource(queryKey: String): PagingSource<Int, Movie> =
            MoviePagingSource(queriesDao.moviesPagingSource(queryKey)).also { read ->
                val open = queryKey to read as PagingSource<Int, Movie>
                openReads += open
                read.registerInvalidatedCallback { openReads -= open }
            }

        override suspend fun replaceQuery(
            queryKey: String,
            page: MoviePage,
        ) {
            queriesDao.replaceQuery(
                queryKey = queryKey,
                movies = page.movies.map { it.toEntity() },
                nextCursor = page.next?.value,
            )
            invalidateReadsOf(queryKey)
        }

        override suspend fun appendQuery(
            queryKey: String,
            page: MoviePage,
        ) {
            queriesDao.appendQuery(
                queryKey = queryKey,
                movies = page.movies.map { it.toEntity() },
                nextCursor = page.next?.value,
            )
            invalidateReadsOf(queryKey)
        }

        override suspend fun queryCursor(queryKey: String): Cursor? =
            queriesDao.nextCursor(queryKey)?.let(::Cursor)

        override fun getMovieFlow(id: String): Flow<Movie?> =
            moviesDao.getMovieFlow(id).map { it?.toMovie() }

        override fun getMovieDetailFlow(id: String): Flow<MovieDetail?> =
            detailsDao.getMovieDetailFlow(id).map { it?.toMovieDetail() }

        override suspend fun writeMovieDetail(detail: MovieDetail) {
            detailsDao.update(detail.toEntity())
        }

        /** Tells every read of [queryKey] that what it is showing is no longer what is stored. */
        private fun invalidateReadsOf(queryKey: String) {
            openReads
                .filter { (key, _) -> key == queryKey }
                .forEach { (_, read) -> read.invalidate() }
        }
    }
