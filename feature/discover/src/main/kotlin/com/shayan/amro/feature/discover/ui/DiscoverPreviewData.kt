package com.shayan.amro.feature.discover.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.shayan.amro.core.data.DataErrorException
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.feature.discover.component.FilterSheetContent
import com.shayan.amro.feature.discover.component.movieRows
import com.shayan.amro.feature.discover.viewmodel.DiscoverBody
import com.shayan.amro.feature.discover.viewmodel.DiscoverUiState
import com.shayan.amro.feature.discover.viewmodel.MovieRowUiState
import com.shayan.amro.feature.discover.viewmodel.NoticeUiState
import com.shayan.amro.feature.discover.viewmodel.discoverBody
import com.shayan.amro.feature.discover.viewmodel.toDiscoverUiState
import com.shayan.amro.feature.discover.viewmodel.toRow
import kotlinx.datetime.LocalDate

/** What every preview poster is painted as, so a golden captures layout and never a download. */
private val PREVIEW_POSTER_COLOR = Color(0xFF55617A)

/** The popularity the first preview row carries, which every later row steps down from. */
private const val PREVIEW_TOP_POPULARITY = 100.0

/** A list with nothing further to serve, which is where a read of its head settles. */
private val SETTLED = LoadState.NotLoading(endOfPaginationReached = true)

/** The movies every preview list is read out of, in the order a source ranked them. */
private val PREVIEW_MOVIES: List<Movie> =
    listOf(
        previewMovie("1", "Spider-Man: Brand New Day", Genre.SCIENCE_FICTION, Genre.ACTION),
        previewMovie("2", "The Odyssey", Genre.ADVENTURE, Genre.FANTASY),
        previewMovie("3", "Mutiny", Genre.ACTION, Genre.THRILLER),
        previewMovie("4", "The Last Sunrise", Genre.ROMANCE, Genre.DRAMA),
        previewMovie("5", "Facing El Chapo", Genre.CRIME, Genre.THRILLER),
        previewMovie("6", "Obsession", Genre.HORROR, Genre.THRILLER),
        previewMovie("7", "Toy Story 5", Genre.ANIMATION, Genre.FAMILY, Genre.COMEDY),
        previewMovie("8", "The Mongoose", Genre.ACTION, Genre.THRILLER, poster = null),
        previewMovie("9", "The Whisper Man", Genre.CRIME, Genre.DRAMA),
    )

/** A selection the narrowed preview is taken under. */
private val PREVIEW_NARROWING = setOf(Genre.COMEDY, Genre.HORROR)

/** The cause the failing previews are taken under. */
private val PREVIEW_CAUSE = DataError.NoConnectivity

/**
 * What the screen renders in one preview.
 *
 * @property uiState what the app bar and the sheet draw.
 * @property body what the list puts on and above the screen.
 * @property rows the rows the list holds.
 */
internal data class DiscoverPreviewState(
    val uiState: DiscoverUiState,
    val body: DiscoverBody,
    val rows: List<MovieRowUiState>,
)

/**
 * The states the discover screen is previewed and captured in.
 */
internal object DiscoverPreviewData {
    /** The first read of a selection nothing is cached for. */
    val SKELETON = previewState(refresh = LoadState.Loading, rowCount = 0)

    /** The list, in the order it opens on. */
    val LOADED = previewState()

    /** The list, narrowed by two genres and ordered away from the default. */
    val NARROWED =
        previewState(
            selection =
                MovieSelection(
                    genres = PREVIEW_NARROWING,
                    sort = MovieSort(key = SortKey.TITLE, direction = SortDirection.ASCENDING),
                ),
        )

    /** The list a fetch failed behind, with the bar naming why. */
    val WITH_NOTICE = previewState(refresh = failure(PREVIEW_CAUSE))

    /** A selection the catalogue answered nothing for. */
    val EMPTY_FROM_FILTER =
        previewState(selection = MovieSelection(genres = PREVIEW_NARROWING), rowCount = 0)

    /** Nothing cached for this selection, and a fetch that failed. */
    val ERROR = previewState(refresh = failure(PREVIEW_CAUSE), rowCount = 0)

    /** The rows of [LOADED], which a row previews one of. */
    val ROWS: List<MovieRowUiState> = PREVIEW_MOVIES.map { it.toRow() }

    /** The one row a source served without a poster. */
    val ROW_WITHOUT_POSTER: MovieRowUiState = ROWS.first { it.posterUrl == null }

    /** The bar a failed fetch raises. */
    val NOTICE: NoticeUiState = checkNotNull(WITH_NOTICE.body.notice)
}

/**
 * Reads one state the way the screen is fed it.
 *
 * @param selection what the list is narrowed and ordered by.
 * @param refresh what the last read of the head of the list did.
 * @param rowCount how many rows that read produced.
 */
private fun previewState(
    selection: MovieSelection = MovieSelection(),
    refresh: LoadState = SETTLED,
    rowCount: Int = PREVIEW_MOVIES.size,
): DiscoverPreviewState {
    val rows = PREVIEW_MOVIES.take(rowCount).map { it.toRow() }
    return DiscoverPreviewState(
        uiState = selection.toDiscoverUiState(),
        body = discoverBody(loadState = previewLoadStates(refresh), itemCount = rows.size),
        rows = rows,
    )
}

/**
 * What a preview's list reports about its reads.
 *
 * A preview is a still of a settled list, so everything but the read of its head has finished.
 *
 * @param refresh what the read of the head of the list is doing.
 */
private fun previewLoadStates(refresh: LoadState): CombinedLoadStates {
    val states = LoadStates(refresh = refresh, prepend = SETTLED, append = SETTLED)
    return CombinedLoadStates(
        refresh = refresh,
        prepend = SETTLED,
        append = SETTLED,
        source = states,
        mediator = states,
    )
}

/** A read of the head of the list that failed for [cause]. */
private fun failure(cause: DataError) = LoadState.Error(DataErrorException(cause))

/**
 * Builds one movie a row renders.
 *
 * @param id what tells this movie from the others in the list.
 * @param title the words the row leads with.
 * @param genres what the row lists under the title.
 * @param poster null for the one movie a source served without one.
 */
private fun previewMovie(
    id: String,
    title: String,
    vararg genres: Genre,
    poster: ImageRef? = ImageRef(small = "preview://poster/$id", large = "preview://poster/$id"),
): Movie =
    Movie(
        id = id,
        title = title,
        genres = genres.toList(),
        popularity = PREVIEW_TOP_POPULARITY - id.toDouble(),
        releaseDate = LocalDate.parse("2026-01-01"),
        poster = poster,
    )

/**
 * Renders the screen the way a preview and a golden need it: every poster painted locally, so
 * nothing on screen waits on a network.
 *
 * @param state what the screen renders.
 * @param selectedMovieId the selected movie when adaptive layout has 2 panes.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun DiscoverScreenPreviewHost(
    state: DiscoverPreviewState,
    selectedMovieId: String? = null,
) {
    val previewHandler = AsyncImagePreviewHandler { ColorImage(PREVIEW_POSTER_COLOR.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        DiscoverScreen(
            uiState = state.uiState,
            body = state.body,
            rows = movieRows(state.rows),
            selectedMovieId = selectedMovieId,
            onMovieClick = {},
            onRefresh = {},
            onToggleGenre = {},
            onSelectSortKey = {},
            onSelectSortDirection = {},
            onClearGenres = {},
            onReset = {},
        )
    }
}

/**
 * Renders what the sheet holds, which is the state a golden cannot reach through the screen: the
 * sheet's own visibility is remembered inside it.
 */
@Composable
internal fun FilterSheetPreviewHost() {
    Surface {
        FilterSheetContent(
            filter = DiscoverPreviewData.NARROWED.uiState.filter,
            onToggleGenre = {},
            onSelectSortKey = {},
            onSelectSortDirection = {},
            onReset = {},
        )
    }
}
