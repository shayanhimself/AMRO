package com.shayan.amro.feature.detail.viewmodel

import androidx.annotation.StringRes
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.label.labelRes
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val IMDB_TITLE_URL = "https://www.imdb.com/title/"
private const val GENRE_SEPARATOR = ", "

/**
 * Maps the movie, the details behind it and the cause of the last fetch to UI state for the
 * movie detail screen.
 *
 * @param movie the summary the trending set holds, which names the screen before a record arrives.
 * @param detail the record, from the cache or from an answer.
 * @param error the cause the last fetch failed with, or null.
 * @return what the screen renders.
 */
internal fun toMovieDetailUiState(
    movie: Movie?,
    detail: MovieDetail?,
    error: DataError?,
): MovieDetailUiState {
    val summary = detail?.movie ?: movie
    return MovieDetailUiState(
        title = summary?.title,
        content =
            when {
                // A cached movie detail wins over a failure.
                detail != null -> detail.toLoaded()

                error != null -> error.toErrorContent()

                else -> summary.toSkeleton()
            },
    )
}

/**
 * A first load, carrying the movie summary.
 */
private fun Movie?.toSkeleton(): MovieDetailContent.Skeleton {
    val genreLabels = this?.genreLabels() ?: persistentListOf()
    return MovieDetailContent.Skeleton(
        posterUrl = this?.poster?.small,
        genreLabels = genreLabels,
        genreDescription = genreLabels.asOneStatement(),
    )
}

/** One record as the screen that draws it. */
private fun MovieDetail.toLoaded(): MovieDetailContent.Loaded {
    val statusRes = status.labelRes
    val genreLabels = movie.genreLabels()
    return MovieDetailContent.Loaded(
        posterUrl = movie.poster?.small,
        backdropUrl = backdrop?.large,
        badge = badgeFor(rating = rating, statusRes = statusRes),
        tagline = tagline?.takeIf { it.isNotBlank() },
        overview = overview?.takeIf { it.isNotBlank() },
        genreLabels = genreLabels,
        genreDescription = genreLabels.asOneStatement(),
        facts = facts(statusRes),
        imdbUrl = imdbId?.let { IMDB_TITLE_URL + it },
    )
}

/**
 * The facts about the movie, (runtime, budget, etc.) in the order they are shown.
 */
private fun MovieDetail.facts(statusRes: Int?): ImmutableList<MovieFactUiState> =
    buildList {
        add(fact(R.string.feature_detail_fact_runtime, runtime?.toRuntimeText()))
        add(fact(R.string.feature_detail_fact_release_date, movie.releaseDate?.toDateText()))
        add(fact(R.string.feature_detail_fact_budget, budget?.let(::moneyText)))
        add(fact(R.string.feature_detail_fact_revenue, revenue?.let(::moneyText)))
        // The status is only included where the header slot is not carrying it.
        if (statusRes != null && rating != null) {
            add(
                fact(
                    labelRes = R.string.feature_detail_fact_status,
                    value = AmroText.Resource(statusRes),
                ),
            )
        }
    }.toImmutableList()

/**
 * Builds one movie fact item.
 *
 * @param labelRes what the fact is called.
 * @param value what the source has for it, or null where it has nothing.
 */
internal fun fact(
    @StringRes labelRes: Int,
    value: AmroText?,
): MovieFactUiState =
    MovieFactUiState(
        label = AmroText.Resource(labelRes),
        value = value ?: AmroText.Resource(R.string.feature_detail_not_disclosed),
        disclosed = value != null,
    )

/** What the chips read, in the order they are drawn. */
private fun Movie.genreLabels(): ImmutableList<AmroText> =
    genres.map { AmroText.Resource(it.labelRes) }.toImmutableList()

/** Content description of a list: labels read as one statement. */
private fun ImmutableList<AmroText>.asOneStatement(): AmroText =
    AmroText.Joined(this, separator = GENRE_SEPARATOR)

/**
 * Rating or the status of the movie.
 *
 * @param rating the rating the source reports, or null.
 * @param statusRes what the status reads, or null where the app does not know the status.
 * @return the rating, the status that stands in for a missing one, or null where there is neither.
 */
private fun badgeFor(
    rating: Rating?,
    @StringRes statusRes: Int?,
): HeaderBadgeUiState? {
    if (rating != null) {
        return rating.toRatingBadge()
    }
    return statusRes?.let { HeaderBadgeUiState.Status(label = AmroText.Resource(it)) }
}

/** A rating and its sample size. */
private fun Rating.toRatingBadge(): HeaderBadgeUiState.Rating {
    val rating = ratingText(average)
    val votes = count?.let { voteCountText(it) }
    return HeaderBadgeUiState.Rating(
        rating = rating,
        voteCount = votes,
        description =
            if (votes == null) {
                AmroText.Resource(R.string.feature_detail_rating_description_without_count, rating)
            } else {
                AmroText.Resource(R.string.feature_detail_rating_description, rating, votes)
            },
    )
}

/** A failed fetch as the body it becomes with nothing to show. */
private fun DataError.toErrorContent(): MovieDetailContent.Error =
    MovieDetailContent.Error(glyph = glyph, title = AmroText.Resource(titleRes))

/** The glyph naming this cause. */
private val DataError.glyph: String
    get() =
        when (this) {
            DataError.NoConnectivity -> Glyphs.CLOUD_OFF
            DataError.Server -> Glyphs.ERROR
            DataError.EmptyResponse -> Glyphs.MOVIE_FILTER
        }

/** What the screen says about this cause. */
@get:StringRes
private val DataError.titleRes: Int
    get() =
        when (this) {
            DataError.NoConnectivity -> R.string.feature_detail_error_no_connectivity
            DataError.Server -> R.string.feature_detail_error_server
            DataError.EmptyResponse -> R.string.feature_detail_error_empty_response
        }

/** What a release status is called, and null where the app has no word for it. */
@get:StringRes
private val ReleaseStatus.labelRes: Int?
    get() =
        when (this) {
            ReleaseStatus.RUMORED -> R.string.feature_detail_status_rumored
            ReleaseStatus.PLANNED -> R.string.feature_detail_status_planned
            ReleaseStatus.IN_PRODUCTION -> R.string.feature_detail_status_in_production
            ReleaseStatus.POST_PRODUCTION -> R.string.feature_detail_status_post_production
            ReleaseStatus.RELEASED -> R.string.feature_detail_status_released
            ReleaseStatus.CANCELED -> R.string.feature_detail_status_canceled
            ReleaseStatus.UNKNOWN -> null
        }
