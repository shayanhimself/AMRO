package com.shayan.amro.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The list of trending movies. */
@Serializable
data object TrendingKey : NavKey

/**
 * One movie's detail.
 *
 * @property id the movie the screen shows.
 */
@Serializable
data class MovieDetailKey(
    val id: Int,
) : NavKey
