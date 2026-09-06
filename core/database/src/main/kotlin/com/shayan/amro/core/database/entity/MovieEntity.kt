package com.shayan.amro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One movie of a list.
 *
 * @property movieId identifies the movie.
 * @property genres the app's genre constants, joined on a comma.
 * @property releaseDateEpochDay days since 1970-01-01, null when the movie has no date.
 * @property posterSmall null together with [posterLarge] when the movie has no poster.
 */
@Entity(tableName = "movies")
internal data class MovieEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val genres: String,
    val popularity: Double,
    val releaseDateEpochDay: Long?,
    val posterSmall: String?,
    val posterLarge: String?,
)
