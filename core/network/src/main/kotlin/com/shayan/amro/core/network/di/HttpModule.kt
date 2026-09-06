package com.shayan.amro.core.network.di

import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/**
 * The HTTP client every request in the app is carried on.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object HttpModule {
    /**
     * The engine every client goes out on.
     */
    @Provides
    @Singleton
    fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create()

    /**
     * Fetches images over the engine.
     */
    @Provides
    @Singleton
    @OptIn(ExperimentalCoilApi::class)
    fun provideImageFetcherFactory(engine: HttpClientEngine): NetworkFetcher.Factory =
        KtorNetworkFetcherFactory(httpClient = HttpClient(engine))
}
