package com.shayan.amro.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shayan.amro.core.database.entity.MovieDetailEntity
import kotlinx.coroutines.flow.Flow

/**
 * The detail table's only reader and writer.
 */
@Dao
internal interface MovieDetailDao {
    @Query("SELECT * FROM movie_details WHERE source = :source AND movieId = :movieId")
    fun getMovieDetailFlow(
        source: String,
        movieId: String,
    ): Flow<MovieDetailEntity?>

    /**
     * Caches one movie's record. Upsert = Update + Insert.
     */
    @Upsert
    suspend fun update(detail: MovieDetailEntity)
}
