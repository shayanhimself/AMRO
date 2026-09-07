package com.shayan.amro.feature.detail.viewmodel

import androidx.compose.runtime.Immutable
import com.shayan.amro.core.ui.text.AmroText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Everything the movie detail screen needs to render.
 *
 * Nothing here is a type the data layer owns, and nothing here is a value the screen has to format:
 * every word arrives as an [AmroText] the screen only resolves.
 *
 * @property title the movie's name, in every state.
 * @property content what the body of the screen shows.
 */
@Immutable
internal data class MovieDetailUiState(
    val title: String? = null,
    val content: MovieDetailContent = MovieDetailContent.Skeleton(),
)

@Immutable
internal sealed interface MovieDetailContent {
    /**
     * Screen's loading state. It carries the minimal movie data it was handed over.
     *
     * @property posterUrl the image URL of the poster.
     * @property genreLabels the genre chips, in the order they are drawn.
     * @property genreDescription content description for the genres.
     */
    data class Skeleton(
        val posterUrl: String? = null,
        val genreLabels: ImmutableList<AmroText> = persistentListOf(),
        val genreDescription: AmroText = AmroText.Empty,
    ) : MovieDetailContent

    /**
     * The record, from the cache or from an answer.
     *
     * @property posterUrl the image URL of the poster.
     * @property backdropUrl the image URL of the backdrop.
     * @property badge the rating, or the status that explains why there is none. Null for a source
     * reporting neither.
     * @property tagline the movie's tagline, or null when the source leaves it empty.
     * @property overview the movie's overview, or null when the source has no description.
     * @property genreLabels the genre chips, in the order they are drawn.
     * @property genreDescription content description for the genres.
     * @property facts the grid under the overview, in the order it is read.
     * @property imdbUrl the title's address, or null where the source has no IMDB id.
     */
    data class Loaded(
        val posterUrl: String?,
        val backdropUrl: String?,
        val badge: HeaderBadgeUiState?,
        val tagline: String?,
        val overview: String?,
        val genreLabels: ImmutableList<AmroText>,
        val genreDescription: AmroText,
        val facts: ImmutableList<MovieFactUiState>,
        val imdbUrl: String?,
    ) : MovieDetailContent

    /**
     * Nothing is cached and a fetch failed.
     *
     * @property glyph what the screen draws over the message.
     * @property title what it says.
     */
    data class Error(
        val glyph: String,
        val title: AmroText,
    ) : MovieDetailContent
}

/**
 * The state of the pill under the header, which either carries a rating or a status.
 */
@Immutable
internal sealed interface HeaderBadgeUiState {
    /**
     * A rating and how many votes produced it.
     *
     * @property rating the rating, already read to one decimal.
     * @property voteCount how many votes, or null for a source that publishes no count.
     * @property description the rating, its scale and the sample size as one statement.
     */
    data class Rating(
        val rating: AmroText,
        val voteCount: AmroText?,
        val description: AmroText,
    ) : HeaderBadgeUiState

    /**
     * Where the movie sits in its release cycle.
     *
     * @property label what the pill reads.
     */
    data class Status(
        val label: AmroText,
    ) : HeaderBadgeUiState
}

/**
 * One cell of the fact grid.
 *
 * @property label what the fact is called.
 * @property value what the source has for it, or the words that state it has nothing.
 * @property disclosed false where the source has nothing. The cell reads in the muted role then,
 * which is what keeps "Not disclosed" from reading as a value the film has.
 */
@Immutable
internal data class MovieFactUiState(
    val label: AmroText,
    val value: AmroText,
    val disclosed: Boolean,
)
