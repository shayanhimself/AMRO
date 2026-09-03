package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One movie as TMDB writes it in a detail response.
 *
 * @property overview plot summary.
 * @property tagline marketing line, e.g. "A long time ago in a galaxy far, far away...".
 * @property genres TMDB's own genre list.
 * @property popularity TMDB's own popularity score. The higher the number, the more popular.
 * @property releaseDate ISO 8601, e.g. "1977-05-25".
 * @property posterPath partial path. Vertical photo. Prefix with TMDB's image base URL to load.
 * @property backdropPath partial path. Landscape photo. Prefix with TMDB's image base URL to load.
 * @property runtime minutes. 0 if unknown.
 * @property status release status as TMDB names it, e.g. "Released", "Post Production".
 * @property budget USD. 0 if unknown.
 * @property revenue USD. 0 if unknown.
 * @property voteAverage on a 0-10 scale.
 * @property imdbId IMDB id, e.g. "tt1234567".
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbMovieDetailDto(
    val id: Long,
    val title: String,
    val overview: String? = null,
    val tagline: String? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val runtime: Int? = null,
    val status: String? = null,
    val budget: Long = 0,
    val revenue: Long = 0,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("imdb_id") val imdbId: String? = null,
)
