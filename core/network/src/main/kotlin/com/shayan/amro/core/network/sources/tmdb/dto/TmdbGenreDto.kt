package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * One TMDB genre.
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbGenreDto(
    val id: Int,
    val name: String,
)
