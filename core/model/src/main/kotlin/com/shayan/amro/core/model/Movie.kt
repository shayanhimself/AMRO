package com.shayan.amro.core.model

import kotlinx.datetime.LocalDate

/**
 * The minimal data to show a movie in a list.
 *
 * @property id the source that issued this movie and its id there.
 * @property genres the app's own genres, already resolved.
 * @property popularity an ordering, not a measurement: greater is more popular.
 * @property releaseDate null when the source has no date for the movie.
 * @property poster null when the source offers no poster.
 */
data class Movie(
    val id: MovieId,
    val title: String,
    val genres: List<Genre>,
    val popularity: Double,
    val releaseDate: LocalDate?,
    val poster: ImageRef?,
)
