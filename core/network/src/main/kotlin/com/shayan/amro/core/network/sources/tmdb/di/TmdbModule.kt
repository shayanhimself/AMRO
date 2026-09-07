package com.shayan.amro.core.network.sources.tmdb.di

import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import com.shayan.amro.core.network.sources.tmdb.TmdbRemoteDataSource
import com.shayan.amro.core.network.sources.tmdb.tmdbHttpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/**
 * Dependency injection for the TMDB source.
 *
 * `TmdbConfig` is not provided here: it comes from `:app`, due to device tests.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class TmdbModule {
    @Binds
    abstract fun bindMovieRemoteDataSource(source: TmdbRemoteDataSource): MovieRemoteDataSource

    companion object {
        /**
         * The Ktor client, on OkHttp.
         *
         * The engine is chosen here rather than inside the client builder so a JVM test can build
         * the same client over `MockEngine` without going through Hilt.
         */
        @Provides
        @Singleton
        fun provideHttpClient(config: TmdbConfig): HttpClient =
            tmdbHttpClient(
                config = config,
                engine = OkHttp.create(),
            )
    }
}
