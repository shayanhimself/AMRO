package com.shayan.amro.core.database.mapper

import com.shayan.amro.core.database.entity.MovieDetailEntity
import com.shayan.amro.core.database.entity.MovieEntity
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

private const val GENRE_SEPARATOR = ","

private val GENRES_BY_NAME = Genre.entries.associateBy { it.name }

private val STATUSES_BY_NAME = ReleaseStatus.entries.associateBy { it.name }

internal fun Movie.toEntity(): MovieEntity =
    MovieEntity(
        movieId = id,
        title = title,
        genres = genres.toColumn(),
        popularity = popularity,
        releaseDateEpochDay = releaseDate?.toEpochDays(),
        posterSmall = poster?.small,
        posterLarge = poster?.large,
    )

internal fun MovieEntity.toMovie(): Movie =
    Movie(
        id = movieId,
        title = title,
        genres = genres.toGenres(),
        popularity = popularity,
        releaseDate = releaseDateEpochDay?.let(LocalDate.Companion::fromEpochDays),
        poster = imageRef(posterSmall, posterLarge),
    )

/** The row one cached record becomes. */
internal fun MovieDetail.toEntity(): MovieDetailEntity =
    MovieDetailEntity(
        movieId = movie.id,
        title = movie.title,
        genres = movie.genres.toColumn(),
        popularity = movie.popularity,
        releaseDateEpochDay = movie.releaseDate?.toEpochDays(),
        posterSmall = movie.poster?.small,
        posterLarge = movie.poster?.large,
        overview = overview,
        tagline = tagline,
        backdropSmall = backdrop?.small,
        backdropLarge = backdrop?.large,
        runtimeMinutes = runtime?.inWholeMinutes,
        status = status.name,
        budget = budget,
        revenue = revenue,
        ratingAverage = rating?.average,
        ratingCount = rating?.count,
        imdbId = imdbId,
    )

/** The record one cached row describes. */
internal fun MovieDetailEntity.toMovieDetail(): MovieDetail =
    MovieDetail(
        movie =
            Movie(
                id = movieId,
                title = title,
                genres = genres.toGenres(),
                popularity = popularity,
                releaseDate = releaseDateEpochDay?.let(LocalDate.Companion::fromEpochDays),
                poster = imageRef(posterSmall, posterLarge),
            ),
        overview = overview,
        tagline = tagline,
        backdrop = imageRef(backdropSmall, backdropLarge),
        runtime = runtimeMinutes?.minutes,
        status = status.toReleaseStatus(),
        budget = budget,
        revenue = revenue,
        rating = ratingAverage?.let { Rating(average = it, count = ratingCount) },
        imdbId = imdbId,
    )

/**
 * A list of genres is stored as a single column.
 */
private fun List<Genre>.toColumn(): String = joinToString(GENRE_SEPARATOR) { it.name }

/**
 * A single column of genres is read as a genre list.
 *
 * mapNotNull drops a name the app does not know.
 */
private fun String.toGenres(): List<Genre> =
    split(GENRE_SEPARATOR).mapNotNull { name -> GENRES_BY_NAME[name] }

/**
 * A single column of release status is read as a release status enum.
 *
 * Mapped by hand because Room's own enum handling throws on a name it does not know.
 */
private fun String.toReleaseStatus(): ReleaseStatus =
    STATUSES_BY_NAME[this] ?: ReleaseStatus.UNKNOWN

private fun imageRef(
    small: String?,
    large: String?,
): ImageRef? = if (small == null || large == null) null else ImageRef(small, large)
