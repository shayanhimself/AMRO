package com.shayan.amro.core.network.di

import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.sources.tmdb.TmdbRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Which source API(s) the data layer reads movies from.
 *
 * A second source is composed behind this binding rather than beside it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteSourceModule {
    @Binds
    internal abstract fun bindMovieRemoteDataSource(
        source: TmdbRemoteDataSource,
    ): MovieRemoteDataSource
}
