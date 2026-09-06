package com.shayan.amro.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The list of trending movies. */
@Serializable
data object TrendingKey : NavKey

/**
 * One movie's detail.
 *
 * @property sourceId the provider that issued [movieId].
 * @property movieId the movie the screen shows.
 */
@Serializable
data class MovieDetailKey(
    val sourceId: String,
    val movieId: String,
) : NavKey
