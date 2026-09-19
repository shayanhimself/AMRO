package com.shayan.amro.feature.trending.viewmodel

import androidx.annotation.StringRes
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.label.labelRes
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.trending.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** What sits between two genre names on a row. */
private const val GENRE_SEPARATOR = " · "

/**
 * Reads the cached trending movies, the selection over it and what the last refresh did.
 *
 * @param selection the filter and sort the user has chosen.
 * @param refresh the state of the last refresh.
 * @param listRevision a token that changes with the selection, carried onto the state so the
 * screen scrolls to the top on a new selection but not on rows arriving under the same one.
 * @return what the screen renders.
 */
internal fun List<Movie>.toTrendingUiState(
    selection: Selection,
    refresh: RefreshState,
    listRevision: Int = 0,
): TrendingUiState {
    val selectedGenres = selection.genreNames.mapTo(mutableSetOf()) { Genre.valueOf(it) }
    val shownMovies = applyFilter(selectedGenres).applySort(selection.sort)

    return TrendingUiState(
        content =
            contentFor(
                isCacheEmpty = isEmpty(),
                shownMovies = shownMovies,
                error = refresh.error,
            ),
        filter =
            FilterUiState(
                genres = genreChips(selected = selectedGenres),
                sortKeys = sortKeyChips(selected = selection.sort.key),
                directions = directionChips(selected = selection.sort.direction),
                shownCount = shownMovies.size,
                totalCount = size,
            ),
        activeSelectionCount =
            selectedGenres.size + if (selection.sort == MovieSort.DEFAULT) 0 else 1,
        notice = refresh.error?.takeIf { isNotEmpty() }?.toNotice(),
        isRefreshing = refresh.isRefreshing,
        listRevision = listRevision,
    )
}

/**
 * What to show in the body of the screen.
 *
 * @param isCacheEmpty whether the device holds no trending set at all.
 * @param shownMovies the movies the selection keeps.
 * @param error the cause the last refresh failed with, or null.
 * @return the content state.
 */
private fun contentFor(
    isCacheEmpty: Boolean,
    shownMovies: List<Movie>,
    error: DataError?,
): TrendingContent =
    when {
        isCacheEmpty && error != null -> error.toErrorContent()
        isCacheEmpty -> TrendingContent.Skeleton
        shownMovies.isEmpty() -> TrendingContent.EmptyFromFilter
        else -> TrendingContent.Movies(shownMovies.map { it.toRow() }.toImmutableList())
    }

/** One movie as the row that draws it. */
private fun Movie.toRow(): MovieRowUiState =
    MovieRowUiState(
        movieId = id,
        title = title,
        posterUrl = poster?.small,
        genres =
            AmroText.Joined(
                texts = genres.map { AmroText.Resource(it.labelRes) }.toImmutableList(),
                separator = GENRE_SEPARATOR,
            ),
    )

/** A failed refresh as the bar that states it. */
private fun DataError.toNotice(): NoticeUiState =
    NoticeUiState(glyph = glyph, message = AmroText.Resource(noticeRes))

/** A failed refresh as the body it becomes with nothing left to show. */
private fun DataError.toErrorContent(): TrendingContent.Error =
    TrendingContent.Error(glyph = glyph, title = AmroText.Resource(titleRes))

/** Every genre, both selected and unselected. */
private fun genreChips(selected: Set<Genre>): ImmutableList<ChipUiState<String>> =
    Genre.entries
        .map { genre ->
            ChipUiState(
                value = genre.name,
                label = AmroText.Resource(genre.labelRes),
                isSelected = genre in selected,
            )
        }.toImmutableList()

/** Every sorting key. */
private fun sortKeyChips(selected: SortKey): ImmutableList<ChipUiState<SortKey>> =
    SortKey.entries
        .map { key ->
            ChipUiState(
                value = key,
                label = AmroText.Resource(key.labelRes),
                isSelected = key == selected,
            )
        }.toImmutableList()

/** Every direction the ordering can run. */
private fun directionChips(selected: SortDirection): ImmutableList<ChipUiState<SortDirection>> =
    // The two directions, in the order they are rendered.
    listOf(SortDirection.DESCENDING, SortDirection.ASCENDING)
        .map { direction ->
            ChipUiState(
                value = direction,
                label = AmroText.Resource(direction.labelRes),
                isSelected = direction == selected,
                glyph = direction.glyph,
            )
        }.toImmutableList()

/** The glyph naming this cause. */
private val DataError.glyph: String
    get() =
        when (this) {
            DataError.NoConnectivity -> Glyphs.CLOUD_OFF
            DataError.Server -> Glyphs.ERROR
            DataError.EmptyResponse -> Glyphs.MOVIE_FILTER
        }

/** What the screen says about this cause, with nothing left to show. */
@get:StringRes
private val DataError.titleRes: Int
    get() =
        when (this) {
            DataError.NoConnectivity -> R.string.feature_trending_error_no_connectivity
            DataError.Server -> R.string.feature_trending_error_server
            DataError.EmptyResponse -> R.string.feature_trending_error_empty_response
        }

/** What the bar says about this cause, over rows that are still worth reading. */
@get:StringRes
private val DataError.noticeRes: Int
    get() =
        when (this) {
            DataError.NoConnectivity -> R.string.feature_trending_notice_no_connectivity
            DataError.Server -> R.string.feature_trending_notice_server
            DataError.EmptyResponse -> R.string.feature_trending_notice_empty_response
        }

/** What a sort key is called. */
@get:StringRes
private val SortKey.labelRes: Int
    get() =
        when (this) {
            SortKey.POPULARITY -> R.string.feature_trending_sort_popularity
            SortKey.TITLE -> R.string.feature_trending_sort_title
            SortKey.RELEASE_DATE -> R.string.feature_trending_sort_release_date
        }

/** What a direction is called. */
@get:StringRes
private val SortDirection.labelRes: Int
    get() =
        when (this) {
            SortDirection.DESCENDING -> R.string.feature_trending_direction_descending
            SortDirection.ASCENDING -> R.string.feature_trending_direction_ascending
        }

/** The arrow a direction runs in. */
private val SortDirection.glyph: String
    get() =
        when (this) {
            SortDirection.DESCENDING -> Glyphs.ARROW_DOWNWARD
            SortDirection.ASCENDING -> Glyphs.ARROW_UPWARD
        }
