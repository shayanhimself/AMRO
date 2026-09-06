package com.shayan.amro

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.NetworkFetcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The Hilt root, and the image loader every poster is fetched through.
 */
@HiltAndroidApp
class AmroApplication :
    Application(),
    SingletonImageLoader.Factory {
    /**
     * How a poster is fetched, built by the network layer over the engine the API calls already
     * use, so images and requests share one HTTP stack and no client library is named here.
     */
    @Inject
    lateinit var imageFetcherFactory: NetworkFetcher.Factory

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(imageFetcherFactory) }
            .build()
}
