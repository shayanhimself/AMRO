package com.shayan.amro.feature.discover.viewmodel

import androidx.compose.runtime.Immutable
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.ui.text.AmroText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * What the discover screen renders around its list.
 *
 * The list itself is paged, so what it holds and what a fetch did travel beside this rather than
 * inside it.
 *
 * @property filter what the sheet offers, and what it currently keeps.
 * @property activeSelectionCount how many of the user's choices the list is narrowed and ordered
 * by.
 */
@Immutable
internal data class DiscoverUiState(
    val filter: FilterUiState = FilterUiState(),
    val activeSelectionCount: Int = 0,
)

/**
 * What the paged list puts on the screen.
 *
 * @property content what the body of the screen shows.
 * @property notice a failed fetch, while there are still rows to keep. Null when nothing failed or
 * when the failure took the whole body instead.
 * @property isRefreshing whether a fetch is running over rows that are already on screen.
 */
@Immutable
internal data class DiscoverBody(
    val content: DiscoverContent = DiscoverContent.Skeleton,
    val notice: NoticeUiState? = null,
    val isRefreshing: Boolean = false,
)

/** The one region of the screen that is exactly one thing at a time. */
@Immutable
internal sealed interface DiscoverContent {
    /** Nothing is cached for this selection and nothing has failed. */
    data object Skeleton : DiscoverContent

    /** The rows the selection produced. */
    data object Movies : DiscoverContent

    /** The selection reached the whole catalogue and matched nothing in it. */
    data object EmptyFromFilter : DiscoverContent

    /**
     * Nothing is cached for this selection and a fetch failed.
     *
     * @property glyph what the screen draws over the message.
     * @property title what it says.
     */
    data class Error(
        val glyph: String,
        val title: AmroText,
    ) : DiscoverContent
}

/**
 * One movie, as a row draws it.
 *
 * @property movieId identifies the movie.
 * @property title the words the row leads with.
 * @property posterUrl null when the source offers no poster.
 * @property genres what the row lists under the title, in the order it lists them.
 */
@Immutable
internal data class MovieRowUiState(
    val movieId: String,
    val title: String,
    val posterUrl: String?,
    val genres: AmroText,
)

/**
 * A failed fetch, stated over rows that are still worth reading.
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
 */
@Immutable
internal data class FilterUiState(
    val genres: ImmutableList<ChipUiState<String>> = persistentListOf(),
    val sortKeys: ImmutableList<ChipUiState<SortKey>> = persistentListOf(),
    val directions: ImmutableList<ChipUiState<SortDirection>> = persistentListOf(),
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
