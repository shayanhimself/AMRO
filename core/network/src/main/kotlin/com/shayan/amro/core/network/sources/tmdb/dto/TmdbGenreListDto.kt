package com.shayan.amro.core.network.sources.tmdb.dto

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * TMDB's genre list response.
 *
 * Only used in tests.
 */
@VisibleForTesting
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbGenreListDto(
    val genres: List<TmdbGenreDto>,
)
