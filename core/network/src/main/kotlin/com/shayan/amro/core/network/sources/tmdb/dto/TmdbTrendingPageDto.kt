package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * TMDB's trending list response.
 *
 * `results` has no default: a response without it cannot become movies at all, so it fails to
 * parse and surfaces as a server failure.
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbTrendingPageDto(
    val results: List<TmdbMovieDto>,
)
