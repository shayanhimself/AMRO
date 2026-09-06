package com.shayan.amro.feature.trending.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.SourceId
import com.shayan.amro.feature.trending.component.FilterSheetContent
import com.shayan.amro.feature.trending.viewmodel.MovieRowUiState
import com.shayan.amro.feature.trending.viewmodel.MovieSort
import com.shayan.amro.feature.trending.viewmodel.NoticeUiState
import com.shayan.amro.feature.trending.viewmodel.RefreshState
import com.shayan.amro.feature.trending.viewmodel.SortDirection
import com.shayan.amro.feature.trending.viewmodel.SortKey
import com.shayan.amro.feature.trending.viewmodel.TrendingContent
import com.shayan.amro.feature.trending.viewmodel.TrendingUiState
import com.shayan.amro.feature.trending.viewmodel.toTrendingUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

/** What every preview poster is painted as, so a golden captures layout and never a download. */
private val PREVIEW_POSTER_COLOR = Color(0xFF55617A)

/** The popularity the first preview row carries, which every later row steps down from. */
private const val PREVIEW_TOP_POPULARITY = 100.0

/** The source every preview movie is attributed to. */
private val PREVIEW_SOURCE = SourceId("preview")

/** The cache every preview state is read out of. */
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

/** A selection two preview movies carry. */
private val PREVIEW_NARROWING = listOf(Genre.COMEDY.name, Genre.HORROR.name)

/** A selection nothing in [PREVIEW_MOVIES] carries. */
private val PREVIEW_EXCLUDING = listOf(Genre.DOCUMENTARY.name, Genre.WESTERN.name)

/**
 * The states the trending screen is previewed and captured in.
 */
internal object TrendingPreviewData {
    /** The first load, with nothing cached and nothing failed. */
    val SKELETON = previewState(cache = emptyList())

    /** The list, in the order it opens on. */
    val LOADED = previewState()

    /** The list, narrowed by two genres and ordered away from the default. */
    val NARROWED =
        previewState(
            genreNames = PREVIEW_NARROWING,
            sort = MovieSort(key = SortKey.TITLE, direction = SortDirection.ASCENDING),
        )

    /** The list a refresh failed behind, with the bar naming why. */
    val WITH_NOTICE = previewState(refresh = RefreshState(error = DataError.NoConnectivity))

    /** A selection nothing in the cache matches. */
    val EMPTY_FROM_FILTER = previewState(genreNames = PREVIEW_EXCLUDING)

    /** Nothing cached, and a refresh that failed. */
    val ERROR =
        previewState(
            cache = emptyList(),
            refresh = RefreshState(error = DataError.NoConnectivity),
        )

    /** The rows of [LOADED], which a row previews one of. */
    val ROWS: ImmutableList<MovieRowUiState> = (LOADED.content as TrendingContent.Movies).rows

    /** The one row a source served without a poster. */
    val ROW_WITHOUT_POSTER: MovieRowUiState = ROWS.first { it.posterUrl == null }

    /** The bar a failed refresh raises. */
    val NOTICE: NoticeUiState = checkNotNull(WITH_NOTICE.notice)
}

/**
 * Reads one state the way the screen is fed it.
 *
 * @param cache what the device holds.
 * @param genreNames the genres narrowing the list, as the enum's own names.
 * @param sort the order the list is in.
 * @param refresh the state of the last refresh.
 * @return a [TrendingUiState].
 */
private fun previewState(
    cache: List<Movie> = PREVIEW_MOVIES,
    genreNames: List<String> = emptyList(),
    sort: MovieSort = MovieSort.DEFAULT,
    refresh: RefreshState = RefreshState(),
): TrendingUiState =
    cache.toTrendingUiState(selectedGenreNames = genreNames, sort = sort, refresh = refresh)

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
        id = MovieId(PREVIEW_SOURCE, id),
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
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun TrendingScreenPreviewHost(state: TrendingUiState) {
    val previewHandler = AsyncImagePreviewHandler { ColorImage(PREVIEW_POSTER_COLOR.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        TrendingScreen(
            uiState = state,
            onMovieClick = { _, _ -> },
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
            filter = TrendingPreviewData.NARROWED.filter,
            onToggleGenre = {},
            onSelectSortKey = {},
            onSelectSortDirection = {},
            onReset = {},
        )
    }
}
