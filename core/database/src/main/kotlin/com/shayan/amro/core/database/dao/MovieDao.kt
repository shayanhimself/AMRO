package com.shayan.amro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.shayan.amro.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/** The trending table's only reader and writer. */
@Dao
internal interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getTrendingMoviesFlow(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE movieId = :movieId")
    fun getMovieFlow(movieId: String): Flow<MovieEntity?>

    @Transaction
    suspend fun replaceAll(movies: List<MovieEntity>) {
        deleteAll()
        insertAll(movies)
    }

    @Query("DELETE FROM movies")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(movies: List<MovieEntity>)
}
