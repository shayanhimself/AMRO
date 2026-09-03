package com.shayan.amro.core.model

import kotlin.time.Duration

/**
 * The fuller data behind one [Movie] to show in a detail screen.
 *
 * It embeds the movie rather than repeating its fields, so each field is declared once.
 *
 * @property movie the fields the trending list already renders.
 * @property runtime null when the source does not know the running time.
 * @property budget null when the money was never disclosed.
 * @property revenue null under the same rule as [budget].
 * @property rating null when the source reports no score at all.
 * @property imdbId the id of the movie in IMDB.
 */
data class MovieDetail(
    val movie: Movie,
    val overview: String?,
    val tagline: String?,
    val backdrop: ImageRef?,
    val runtime: Duration?,
    val status: ReleaseStatus,
    val budget: Long?,
    val revenue: Long?,
    val rating: Rating?,
    val imdbId: String?,
)
