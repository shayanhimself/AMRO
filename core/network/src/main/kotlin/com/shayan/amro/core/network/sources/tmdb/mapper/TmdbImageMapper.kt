package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.ImageRef

/** Where TMDB serves images, with a width segment appended to it. */
private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

/**
 * The width a small and a large poster resolve to.
 */
private const val POSTER_SMALL_WIDTH = "w342"
private const val POSTER_LARGE_WIDTH = "w500"

/** The width a small and a large backdrop resolve to, under the same rule as the posters. */
private const val BACKDROP_SMALL_WIDTH = "w300"
private const val BACKDROP_LARGE_WIDTH = "w1280"

/**
 * The poster a path refers to, at both sizes. TMDB's posters are vertical photos.
 *
 * @param path TMDB's leading-slash image path.
 * @return null when there is no path.
 */
internal fun posterRef(path: String?): ImageRef? =
    imageRef(
        path = path,
        smallWidth = POSTER_SMALL_WIDTH,
        largeWidth = POSTER_LARGE_WIDTH,
    )

/**
 * The backdrop a path refers to, at both sizes. TMDB's backdrops are horizontal photos.
 *
 * @param path TMDB's leading-slash image path.
 * @return null when there is no path.
 */
internal fun backdropRef(path: String?): ImageRef? =
    imageRef(
        path = path,
        smallWidth = BACKDROP_SMALL_WIDTH,
        largeWidth = BACKDROP_LARGE_WIDTH,
    )

private fun imageRef(
    path: String?,
    smallWidth: String,
    largeWidth: String,
): ImageRef? {
    val imagePath = path?.trim().orEmpty()
    if (imagePath.isEmpty()) return null
    return ImageRef(
        small = TMDB_IMAGE_BASE_URL + smallWidth + imagePath,
        large = TMDB_IMAGE_BASE_URL + largeWidth + imagePath,
    )
}
