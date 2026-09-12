package com.shayan.amro.core.data.di

import com.shayan.amro.core.data.DefaultMovieDetailRepository
import com.shayan.amro.core.data.DefaultMoviesRepository
import com.shayan.amro.core.data.MovieDetailRepository
import com.shayan.amro.core.data.MoviesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dependency injection for the repositories.
 *
 * Neither is scoped: they hold no state between calls, and the database and the client they
 * delegate to are singletons already.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindMoviesRepository(repository: DefaultMoviesRepository): MoviesRepository

    @Binds
    abstract fun bindMovieDetailRepository(
        repository: DefaultMovieDetailRepository,
    ): MovieDetailRepository
}
