package com.shayan.amro.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.shayan.amro.core.database.AmroDatabase
import com.shayan.amro.core.database.MovieLocalDataSource
import com.shayan.amro.core.database.RoomMovieLocalDataSource
import com.shayan.amro.core.database.dao.MovieDao
import com.shayan.amro.core.database.dao.MovieDetailDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

private const val DATABASE_NAME = "amro.db"

/** Dependency injection for the database and everything that reads it. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {
    @Binds
    abstract fun bindMovieLocalDataSource(source: RoomMovieLocalDataSource): MovieLocalDataSource

    companion object {
        /**
         * The database, on the bundled SQLite driver.
         *
         * A schema change drops it. Everything stored here is re-fetchable, so a migration would
         * be code written to preserve what one request restores.
         */
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): AmroDatabase =
            Room
                .databaseBuilder(context, AmroDatabase::class.java, DATABASE_NAME)
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun provideMovieDao(database: AmroDatabase): MovieDao = database.movieDao()

        @Provides
        fun provideMovieDetailDao(database: AmroDatabase): MovieDetailDao =
            database.movieDetailDao()
    }
}
