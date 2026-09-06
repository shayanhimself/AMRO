package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.network.sources.MovieId
import com.shayan.amro.core.network.sources.tmdb.TMDB_SOURCE
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDetailDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDto
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * If TMDB has no popularity for a movie, the app treats it as the least popular movie in the list.
 */
private const val UNPOPULAR = 0.0

/** TMDB's release statuses paired with the app status each one becomes. */
private val STATUSES_BY_TMDB_NAME =
    mapOf(
        "Rumored" to ReleaseStatus.RUMORED,
        "Planned" to ReleaseStatus.PLANNED,
        "In Production" to ReleaseStatus.IN_PRODUCTION,
        "Post Production" to ReleaseStatus.POST_PRODUCTION,
        "Released" to ReleaseStatus.RELEASED,
        "Canceled" to ReleaseStatus.CANCELED,
    )

/** The movie a list row describes. */
internal fun TmdbMovieDto.toMovie(): Movie =
    Movie(
        id = MovieId(TMDB_SOURCE, id.toString()).qualified,
        title = title,
        genres = genreIds.mapNotNull(::tmdbGenre),
        popularity = popularity ?: UNPOPULAR,
        releaseDate = releaseDate.toLocalDateOrNull(),
        poster = posterRef(posterPath),
    )

/** The fuller record a detail response describes. */
internal fun TmdbMovieDetailDto.toMovieDetail(): MovieDetail =
    MovieDetail(
        movie =
            Movie(
                id = MovieId(TMDB_SOURCE, id.toString()).qualified,
                title = title,
                genres = genres.mapNotNull { tmdbGenre(it.id) },
                popularity = popularity ?: UNPOPULAR,
                releaseDate = releaseDate.toLocalDateOrNull(),
                poster = posterRef(posterPath),
            ),
        overview = overview.orNullIfBlank(),
        tagline = tagline.orNullIfBlank(),
        backdrop = backdropRef(backdropPath),
        runtime = runtime.toRuntimeOrNull(),
        status = status.toReleaseStatus(),
        budget = budget.orNullIfZero(),
        revenue = revenue.orNullIfZero(),
        rating = toRatingOrNull(),
        imdbId = imdbId.orNullIfBlank(),
    )

/**
 * TMDB says "no value" three ways: a zero, an empty string and an absent field. Every one of them
 * becomes null here, so nothing above the data layer checks for a sentinel.
 */
private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

private fun Long.orNullIfZero(): Long? = takeIf { it != 0L }

private fun Int?.toRuntimeOrNull(): Duration? = this?.takeIf { it != 0 }?.minutes

/**
 * The rating TMDB reports, and the votes behind it.
 *
 * TMDB says "unrated" with a rating of 0 and 0 votes. And a vote count of 0 means no count.
 */
private fun TmdbMovieDetailDto.toRatingOrNull(): Rating? =
    if (voteAverage == 0.0 && voteCount == 0) {
        null
    } else {
        Rating(average = voteAverage, count = voteCount.takeIf { it != 0 })
    }

/** An unparseable or empty date is a movie with no date. */
private fun String?.toLocalDateOrNull(): LocalDate? {
    val date = this?.trim().orEmpty()
    if (date.isEmpty()) return null
    return try {
        LocalDate.parse(date)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun String?.toReleaseStatus(): ReleaseStatus =
    STATUSES_BY_TMDB_NAME[this?.trim()] ?: ReleaseStatus.UNKNOWN
