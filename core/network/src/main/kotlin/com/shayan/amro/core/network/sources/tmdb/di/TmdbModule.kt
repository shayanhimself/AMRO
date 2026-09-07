package com.shayan.amro.core.network.sources.tmdb.di

import com.shayan.amro.core.network.BuildConfig
import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import com.shayan.amro.core.network.sources.tmdb.tmdbHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/** What the TMDB source is built from. */
@Module
@InstallIn(SingletonComponent::class)
internal object TmdbModule {
    @Provides
    @Singleton
    fun provideTmdbConfig(): TmdbConfig =
        TmdbConfig(readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN)

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
