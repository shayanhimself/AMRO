package com.shayan.amro.feature.trending.viewmodel

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Everything the trending screen renders.
 *
 * Nothing here is a type the data layer owns: the screen is handed resource ids, strings and
 * counts.
 *
 * @property content what the body of the screen shows.
 * @property filter what the sheet offers, and what it currently keeps.
 * @property activeSelectionCount how many of the user's choices the list is showing.
 * @property notice a failed refresh, while there are still rows to keep. Null when nothing failed
 * or when the failure took the whole body instead.
 * @property isRefreshing whether a refresh is running.
 */
@Immutable
internal data class TrendingUiState(
    val content: TrendingContent = TrendingContent.Skeleton,
    val filter: FilterUiState = FilterUiState(),
    val activeSelectionCount: Int = 0,
    val notice: NoticeUiState? = null,
    val isRefreshing: Boolean = false,
)

/** The one region of the screen that is exactly one thing at a time. */
@Immutable
internal sealed interface TrendingContent {
    /** Nothing is cached and nothing has failed. */
    data object Skeleton : TrendingContent

    /**
     * The rows the selection keeps.
     *
     * @property rows filtered and sorted, and never empty.
     */
    data class Movies(
        val rows: ImmutableList<MovieRowUiState>,
    ) : TrendingContent

    /** Movies are cached, and the selection keeps none of them. */
    data object EmptyFromFilter : TrendingContent

    /**
     * Nothing is cached and a refresh failed.
     *
     * @property glyph what the screen draws over the message.
     * @property titleRes what it says.
     */
    data class Error(
        val glyph: String,
        @param:StringRes val titleRes: Int,
    ) : TrendingContent
}

/**
 * One movie, as a row draws it.
 *
 * @property key what tells this row from the others across a refresh, so an unchanged row moves
 * instead of being rebuilt.
 * @property sourceId the provider that issued [movieId], which navigation carries to the detail
 * screen.
 * @property movieId the movie as that provider wrote it.
 * @property title the words the row leads with.
 * @property posterUrl null when the source offers no poster.
 * @property genreLabels what the row lists under the title, in the order it lists them.
 */
@Immutable
internal data class MovieRowUiState(
    val key: String,
    val sourceId: String,
    val movieId: String,
    val title: String,
    val posterUrl: String?,
    val genreLabels: ImmutableList<Int>,
)

/**
 * A failed refresh, stated over rows that are still worth reading.
 *
 * @property glyph what the bar draws beside the message.
 * @property messageRes what it says.
 */
@Immutable
internal data class NoticeUiState(
    val glyph: String,
    @param:StringRes val messageRes: Int,
)

/**
 * What the filter sheet offers.
 *
 * @property genres every genre, each carrying whether it is selected.
 * @property sortKeys every key the list can be ordered on.
 * @property directions every way that ordering can run.
 * @property shownCount how many movies the selection keeps.
 * @property totalCount how many movies are cached.
 */
@Immutable
internal data class FilterUiState(
    val genres: ImmutableList<ChipUiState<String>> = persistentListOf(),
    val sortKeys: ImmutableList<ChipUiState<SortKey>> = persistentListOf(),
    val directions: ImmutableList<ChipUiState<SortDirection>> = persistentListOf(),
    val shownCount: Int = 0,
    val totalCount: Int = 0,
)

/**
 * One selectable choice, whatever it selects.
 *
 * @param T what the chip sends back when it is chosen. The screen never reads it, so a choice the
 * app owns travels as its own type and a genre travels as the name the selection is saved under.
 * @property value what a click reports.
 * @property labelRes what the chip is called.
 * @property isSelected whether the list is currently narrowed or ordered by this.
 * @property glyph drawn ahead of the label, for a choice a word alone states weakly.
 */
@Immutable
internal data class ChipUiState<out T>(
    val value: T,
    @param:StringRes val labelRes: Int,
    val isSelected: Boolean,
    val glyph: String? = null,
)
