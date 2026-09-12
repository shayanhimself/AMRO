package com.shayan.amro.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.shayan.amro.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/** Reads one movie by key, which is what the detail screen renders before its own fetch returns. */
@Dao
internal interface MovieDao {
    @Query("SELECT * FROM movies WHERE movieId = :movieId")
    fun getMovieFlow(movieId: String): Flow<MovieEntity?>
}
