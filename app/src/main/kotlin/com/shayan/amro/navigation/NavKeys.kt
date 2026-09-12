package com.shayan.amro.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The movie list the app opens on. */
@Serializable
data object DiscoverKey : NavKey

/**
 * One movie's detail.
 *
 * @property movieId the movie the screen shows.
 */
@Serializable
data class MovieDetailKey(
    val movieId: String,
) : NavKey
