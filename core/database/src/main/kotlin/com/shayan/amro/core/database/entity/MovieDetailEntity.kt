package com.shayan.amro.core.database.entity

import androidx.room.Entity

/**
 * A full movie detail record.
 *
 * The key is the combination of [source] and [movieId].
 *
 * @property source the provider that issued this movie.
 * @property genres the app's genre constants, joined on a comma.
 * @property releaseDateEpochDay days since 1970-01-01, null when the movie has no date.
 * @property posterSmall null together with [posterLarge] when the movie has no poster.
 * @property backdropSmall null together with [backdropLarge] when the movie has no backdrop.
 */
@Entity(tableName = "movie_details", primaryKeys = ["source", "movieId"])
internal data class MovieDetailEntity(
    val source: String,
    val movieId: String,
    val title: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
    val overview: String?,
    val tagline: String?,
    val backdropSmall: String?,
    val backdropLarge: String?,
    val runtimeMinutes: Long?,
    val status: String,
    val budget: Long?,
    val revenue: Long?,
    val ratingAverage: Double?,
    val ratingCount: Int?,
    val imdbId: String?,
)
