package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * TMDB's genre list response.
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbGenreListDto(
    val genres: List<TmdbGenreDto>,
)
