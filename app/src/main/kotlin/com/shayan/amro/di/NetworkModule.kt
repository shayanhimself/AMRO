package com.shayan.amro.di

import com.shayan.amro.BuildConfig
import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** TMDB's v3 REST root. */
private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

/**
 * The TMDB config injection.
 *
 * Bound here rather than inside the network layer so a device test can point the app at a local
 * server.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTmdbConfig(): TmdbConfig =
        TmdbConfig(
            baseUrl = TMDB_BASE_URL,
            readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
        )
}
