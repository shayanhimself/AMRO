package com.shayan.amro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shayan.amro.core.database.dao.MovieDao
import com.shayan.amro.core.database.dao.MovieDetailDao
import com.shayan.amro.core.database.dao.MovieQueryDao
import com.shayan.amro.core.database.entity.MovieDetailEntity
import com.shayan.amro.core.database.entity.MovieEntity
import com.shayan.amro.core.database.entity.QueryKeyEntity
import com.shayan.amro.core.database.entity.QueryResultEntity

/**
 * The tables this device holds.
 *
 * The schema is exported and checked in, which is what makes a real migration possible on the day
 * a table stops being a pure cache.
 */
@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        QueryResultEntity::class,
        QueryKeyEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
internal abstract class AmroDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    abstract fun movieQueryDao(): MovieQueryDao

    abstract fun movieDetailDao(): MovieDetailDao
}
