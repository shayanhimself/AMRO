package com.shayan.amro.core.database.entity

import androidx.room.Entity

/**
 * One movie of a list.
 *
 * The key is the combination of [source] and [movieId].
 *
 * @property source the provider that issued this movie.
 * @property genres the app's genre constants, joined on a comma.
 * @property releaseDateEpochDay days since 1970-01-01, null when the movie has no date.
 * @property posterSmall null together with [posterLarge] when the movie has no poster.
 */
@Entity(tableName = "movies", primaryKeys = ["source", "movieId"])
internal data class MovieEntity(
    val source: String,
    val movieId: String,
    val title: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
)
