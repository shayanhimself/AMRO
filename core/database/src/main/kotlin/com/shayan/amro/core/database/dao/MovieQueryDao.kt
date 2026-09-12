package com.shayan.amro.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.shayan.amro.core.database.entity.MovieEntity
import com.shayan.amro.core.database.entity.QueryKeyEntity
import com.shayan.amro.core.database.entity.QueryResultEntity

/**
 * The lists this device holds, and the movies they are made of.
 *
 * The movie rows are written here rather than beside the read of one, because the rows and the
 * positions that order them have to land in the same transaction.
 */
@Dao
internal interface MovieQueryDao {
    /**
     * The movies one query produced, in the order the provider put them in.
     *
     * Nothing here branches on a genre or a sort: the selection was resolved into [queryKey]
     * before anything reached the database.
     */
    @Query(
        """
        SELECT movies.* FROM query_results
        JOIN movies ON movies.movieId = query_results.movieId
        WHERE query_results.queryKey = :queryKey
        ORDER BY query_results.position
        """,
    )
    fun moviesPagingSource(queryKey: String): PagingSource<Int, MovieEntity>

    /** The position the next movie of one query takes. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM query_results WHERE queryKey = :queryKey")
    suspend fun nextPosition(queryKey: String): Int

    /** Where one query resumes, or null when it has nothing further to serve. */
    @Query("SELECT nextCursor FROM query_keys WHERE queryKey = :queryKey")
    suspend fun nextCursor(queryKey: String): String?

    /**
     * Drops what one query held and writes the head of its list again.
     *
     * It clears one query's rows and touches no other query and no movie row: a refresh under one
     * selection must not discard a list cached under another.
     */
    @Transaction
    suspend fun replaceQuery(
        queryKey: String,
        movies: List<MovieEntity>,
        nextCursor: String?,
    ) {
        deleteResults(queryKey)
        writeResults(queryKey, movies, from = 0, nextCursor = nextCursor)
    }

    /**
     * Adds the next stretch of one query's list, after everything it already holds.
     *
     * The position it carries on from is read inside the transaction that writes, so nothing
     * outside can hand it a stale one.
     */
    @Transaction
    suspend fun appendQuery(
        queryKey: String,
        movies: List<MovieEntity>,
        nextCursor: String?,
    ) {
        writeResults(queryKey, movies, from = nextPosition(queryKey), nextCursor = nextCursor)
    }

    /**
     * Writes one stretch of movies, its positions and the resume point together.
     *
     * Movie rows are upserted and never deleted: a movie held under one query is the same movie
     * under another, and the detail screen reads that table by key.
     *
     * A movie the query already holds keeps the place it first took, which is what leaves a
     * re-ranked provider unable to put one movie in a list twice.
     */
    @Transaction
    suspend fun writeResults(
        queryKey: String,
        movies: List<MovieEntity>,
        from: Int,
        nextCursor: String?,
    ) {
        upsertMovies(movies)
        insertResults(
            movies.mapIndexed { index, movie ->
                QueryResultEntity(
                    queryKey = queryKey,
                    position = from + index,
                    movieId = movie.movieId,
                )
            },
        )
        upsertKey(QueryKeyEntity(queryKey = queryKey, nextCursor = nextCursor))
    }

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResults(results: List<QueryResultEntity>)

    @Upsert
    suspend fun upsertKey(key: QueryKeyEntity)

    @Query("DELETE FROM query_results WHERE queryKey = :queryKey")
    suspend fun deleteResults(queryKey: String)
}
