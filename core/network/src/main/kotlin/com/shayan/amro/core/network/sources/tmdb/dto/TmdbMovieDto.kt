package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One movie as TMDB writes it in a list response.
 *
 * Only fields with a consumer are declared.
 * Everything but `id` and `title` has a default, so a sparse record still parses and the mapper
 * decides what an absence means.
 *
 * @property popularity TMDB's own popularity score. The higher the number, the more popular.
 * @property releaseDate ISO 8601, e.g. "1977-05-25".
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbMovieDto(
    val id: Long,
    val title: String,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)
