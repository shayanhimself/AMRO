package com.shayan.amro.feature.trending.viewmodel

import androidx.compose.runtime.Immutable
import com.shayan.amro.core.ui.text.AmroText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Everything the trending screen needs to render.
 *
 * @property content what the body of the screen shows.
 * @property filter what the sheet offers, and what it currently keeps.
 * @property activeSelectionCount how many of the user's choices the list is showing.
 * @property notice a failed refresh, while there are still rows to keep. Null when nothing failed
 * or when the failure took the whole body instead.
 * @property isRefreshing whether a refresh is running.
 * @property selection holds the sort and filter the user has chosen.
 */
@Immutable
internal data class TrendingUiState(
    val content: TrendingContent = TrendingContent.Skeleton,
    val filter: FilterUiState = FilterUiState(),
    val activeSelectionCount: Int = 0,
    val notice: NoticeUiState? = null,
    val isRefreshing: Boolean = false,
    val selection: SelectionUiState = SelectionUiState(),
)

/**
 * The sort and filter the user has chosen.
 *
 * Nothing reads the fields. The screen uses this for pure comparison, to know when the selection
 * has changed.
 *
 * @property genreNames the selected genres, as the enum's own names.
 * @property sort the order the rows are in.
 */
@Immutable
internal data class SelectionUiState(
    val genreNames: ImmutableSet<String> = persistentSetOf(),
    val sort: MovieSort = MovieSort.DEFAULT,
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
     * @property title what it says.
     */
    data class Error(
        val glyph: String,
        val title: AmroText,
    ) : TrendingContent
}

/**
 * One movie, as a row draws it.
 *
 * @property movieId identifies the movie.
 * @property title the words the row leads with.
 * @property posterUrl null when the source offers no poster.
 * @property genreLabels what the row lists under the title, in the order it lists them.
 */
@Immutable
internal data class MovieRowUiState(
    val movieId: String,
    val title: String,
    val posterUrl: String?,
    val genres: AmroText,
)

/**
 * A failed refresh, stated over rows that are still worth reading.
 *
 * @property glyph what the bar draws beside the message.
 * @property message what it says.
 */
@Immutable
internal data class NoticeUiState(
    val glyph: String,
    val message: AmroText,
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
 * @property label what the chip is called.
 * @property isSelected whether the list is currently narrowed or ordered by this.
 * @property glyph drawn ahead of the label, for a choice a word alone states weakly.
 */
@Immutable
internal data class ChipUiState<out T>(
    val value: T,
    val label: AmroText,
    val isSelected: Boolean,
    val glyph: String? = null,
)
