package com.shayan.amro.feature.discover.viewmodel

import androidx.annotation.StringRes
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.shayan.amro.core.data.DataErrorException
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.label.labelRes
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.discover.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** What sits between two genre names on a row. */
private const val GENRE_SEPARATOR = " · "

/**
 * The two directions, in the order the sheet offers them: the one the list opens on comes first.
 */
private val DIRECTIONS = listOf(SortDirection.DESCENDING, SortDirection.ASCENDING)

/**
 * Reads one selection as the chrome the screen draws around its list.
 *
 * @return what the app bar and the sheet render.
 */
internal fun MovieSelection.toDiscoverUiState(): DiscoverUiState =
    DiscoverUiState(
        filter =
            FilterUiState(
                genres = genreChips(selected = genres),
                sortKeys = sortKeyChips(selected = sort.key),
                directions = directionChips(selected = sort.direction),
            ),
        activeSelectionCount = genres.size + if (sort == MovieSort.DEFAULT) 0 else 1,
    )

/**
 * Reads what the list is doing as what the screen puts on and above the body.
 *
 * A failure surfaces as a bar while there are rows to keep and as the whole body when there are
 * none, which is the rule that a failure is stated when the content left on screen would otherwise
 * assert something false.
 *
 * @param loadState what the reads of the list are doing, both from the cache and from a source.
 * @param itemCount how many rows the list currently holds.
 */
internal fun discoverBody(
    loadState: CombinedLoadStates,
    itemCount: Int,
): DiscoverBody {
    val refresh = loadState.refresh
    val error = (refresh as? LoadState.Error)?.toDataError()
    // A read that has settled holding no rows is not an empty list yet: the movies a fetch wrote
    // reach the screen a read later, and the list holds none in between. It is empty only once
    // the source has said it has nothing further to serve.
    val servedEverything = refresh !is LoadState.Loading && loadState.append.endOfPaginationReached
    return DiscoverBody(
        content =
            when {
                itemCount > 0 -> DiscoverContent.Movies
                error != null -> error.toErrorContent()
                servedEverything -> DiscoverContent.EmptyFromFilter
                else -> DiscoverContent.Skeleton
            },
        notice = error?.takeIf { itemCount > 0 }?.toNotice(),
        isRefreshing = refresh is LoadState.Loading && itemCount > 0,
    )
}

/** One movie as the row that draws it. */
internal fun Movie.toRow(): MovieRowUiState =
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

/**
 * The cause behind a failed load.
 *
 * Anything the data layer did not raise is the app failing rather than the source, and the screen
 * has one sentence for that.
 */
private fun LoadState.Error.toDataError(): DataError =
    (error as? DataErrorException)?.error ?: DataError.Server

/** A failed fetch as the bar that states it. */
private fun DataError.toNotice(): NoticeUiState =
    NoticeUiState(glyph = glyph, message = AmroText.Resource(noticeRes))

/** A failed fetch as the body it becomes with nothing left to show. */
private fun DataError.toErrorContent(): DiscoverContent.Error =
    DiscoverContent.Error(glyph = glyph, title = AmroText.Resource(titleRes))

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
    DIRECTIONS
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
            DataError.NoConnectivity -> R.string.feature_discover_error_no_connectivity
            DataError.Server -> R.string.feature_discover_error_server
            DataError.EmptyResponse -> R.string.feature_discover_error_empty_response
        }

/** What the bar says about this cause, over rows that are still worth reading. */
@get:StringRes
private val DataError.noticeRes: Int
    get() =
        when (this) {
            DataError.NoConnectivity -> R.string.feature_discover_notice_no_connectivity
            DataError.Server -> R.string.feature_discover_notice_server
            DataError.EmptyResponse -> R.string.feature_discover_notice_empty_response
        }

/** What a sort key is called. */
@get:StringRes
private val SortKey.labelRes: Int
    get() =
        when (this) {
            SortKey.POPULARITY -> R.string.feature_discover_sort_popularity
            SortKey.TITLE -> R.string.feature_discover_sort_title
            SortKey.RELEASE_DATE -> R.string.feature_discover_sort_release_date
        }

/** What a direction is called. */
@get:StringRes
private val SortDirection.labelRes: Int
    get() =
        when (this) {
            SortDirection.DESCENDING -> R.string.feature_discover_direction_descending
            SortDirection.ASCENDING -> R.string.feature_discover_direction_ascending
        }

/** The arrow a direction runs in. */
private val SortDirection.glyph: String
    get() =
        when (this) {
            SortDirection.DESCENDING -> Glyphs.ARROW_DOWNWARD
            SortDirection.ASCENDING -> Glyphs.ARROW_UPWARD
        }
