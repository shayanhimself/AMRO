package com.shayan.amro.feature.trending.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.testing.assertion.assertTouchTargetMeetsMinimum
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.quantityString
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.trending.R
import com.shayan.amro.feature.trending.viewmodel.MovieRowUiState
import com.shayan.amro.feature.trending.viewmodel.MovieSort
import com.shayan.amro.feature.trending.viewmodel.RefreshState
import com.shayan.amro.feature.trending.viewmodel.SortDirection
import com.shayan.amro.feature.trending.viewmodel.SortKey
import com.shayan.amro.feature.trending.viewmodel.TrendingContent
import com.shayan.amro.feature.trending.viewmodel.TrendingUiState
import com.shayan.amro.feature.trending.viewmodel.toTrendingUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.shayan.amro.core.ui.R as CoreUiR

@RunWith(AndroidJUnit4::class)
class TrendingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `selecting a row reports the movie it stands for`() {
        var selected: String? = null
        setContent(loaded(), onMovieClick = { movieId -> selected = movieId })

        composeRule.onNodeWithText(TOP_TITLE).performClick()

        assertEquals(CACHE.first { it.title == TOP_TITLE }.id, selected)
    }

    @Test
    fun `nothing narrowing the list leaves the filter action unbadged`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription(filterLabel()).assertExists()
    }

    @Test
    fun `the badge counts the genres and the sort together`() {
        setContent(
            loaded(
                genreNames = listOf(Genre.COMEDY.name, Genre.HORROR.name),
                sort = MovieSort(SortKey.TITLE, SortDirection.ASCENDING),
            ),
        )

        composeRule.onNodeWithContentDescription(filterLabel(active = 3)).assertExists()
        composeRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `retry fires from the notice bar`() {
        var refreshes = 0
        setContent(
            loaded(refresh = RefreshState(error = DataError.NoConnectivity)),
            onRefresh = { refreshes++ },
        )

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `retry fires from the error state`() {
        var refreshes = 0
        setContent(failed(DataError.Server), onRefresh = { refreshes++ })

        composeRule.onNodeWithText(string(R.string.feature_trending_error_server)).assertExists()
        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `a first load draws placeholder rows, and says so`() {
        setContent(TrendingUiState())

        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.core_ui_loading))
            .assertExists()
        composeRule.onNodeWithText(TOP_TITLE).assertDoesNotExist()
    }

    @Test
    fun `the notice bar is absent while a refresh runs`() {
        setContent(loaded(refresh = RefreshState(isRefreshing = true)))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).assertDoesNotExist()
    }

    @Test
    fun `each poster is described, and the placeholder is not an image`() {
        setContent(loaded())

        composeRule
            .onNodeWithContentDescription(posterLabel(TOP_TITLE), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `the filter action is at least the minimum tappable size`() {
        setContent(loaded())

        composeRule
            .onNodeWithContentDescription(filterLabel())
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `the empty state's action is at least the minimum tappable size`() {
        setContent(loaded(genreNames = listOf(UNMATCHED_GENRE.name)))

        composeRule
            .onNodeWithText(string(R.string.feature_trending_clear_filters))
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `a changed filter returns the list to the top`() {
        val uiState = mutableStateOf(loaded(cache = LONG_CACHE))
        setContent(uiState)
        scrollToBottom()

        composeRule.runOnIdle {
            uiState.value = loaded(cache = LONG_CACHE, genreNames = listOf(SHARED_GENRE.name))
        }

        composeRule.onNodeWithText(TOP_OF_LONG_CACHE).assertIsDisplayed()
    }

    @Test
    fun `a changed sort returns the list to the top`() {
        val uiState = mutableStateOf(loaded(cache = LONG_CACHE))
        setContent(uiState)
        scrollToBottom()

        composeRule.runOnIdle {
            uiState.value =
                loaded(
                    cache = LONG_CACHE,
                    sort = MovieSort(SortKey.RELEASE_DATE, SortDirection.DESCENDING),
                )
        }

        composeRule.onNodeWithText(TOP_OF_LONG_CACHE).assertIsDisplayed()
    }

    @Test
    fun `rows appended under the same selection leave the list where it is`() {
        val uiState = mutableStateOf(loaded(cache = LONG_CACHE))
        setContent(uiState)
        scrollToBottom()

        composeRule.runOnIdle { uiState.value = loaded(cache = LONG_CACHE + NEXT_PAGE) }

        composeRule.onNodeWithText(BOTTOM_OF_LONG_CACHE).assertIsDisplayed()
    }

    /** Scrolls the list until its last row is on screen. */
    private fun scrollToBottom() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(BOTTOM_OF_LONG_CACHE))
    }

    /** Renders the stateless screen over one state. */
    private fun setContent(
        uiState: TrendingUiState,
        onMovieClick: (movieId: String) -> Unit = {},
        onRefresh: () -> Unit = {},
    ) = setContent(mutableStateOf(uiState), onMovieClick, onRefresh)

    /** Renders the stateless screen over a state the test replaces while it is showing. */
    private fun setContent(
        uiState: State<TrendingUiState>,
        onMovieClick: (movieId: String) -> Unit = {},
        onRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                TrendingScreen(
                    uiState = uiState.value,
                    onMovieClick = onMovieClick,
                    onRefresh = onRefresh,
                    onToggleGenre = {},
                    onSelectSortKey = {},
                    onSelectSortDirection = {},
                    onClearGenres = {},
                    onReset = {},
                )
            }
        }
    }
}

/** The set every stateful test narrows and reorders. */
private val CACHE = MovieFixture.MOVIES_IN_NO_ORDER

/** A genre nothing in [CACHE] carries. */
private val UNMATCHED_GENRE = Genre.WESTERN

/** The title the loaded list leads with, which is the row a test can reach without scrolling. */
private val TOP_TITLE = loaded().rows.first().title

/**
 * The state a loaded screen renders, read out of the whole fixture the way the app reads it.
 */
private fun loaded(
    cache: List<Movie> = CACHE,
    genreNames: List<String> = emptyList(),
    sort: MovieSort = MovieSort.DEFAULT,
    refresh: RefreshState = RefreshState(),
) = cache.toTrendingUiState(selectedGenreNames = genreNames, sort = sort, refresh = refresh)

/** How many rows it takes for a list not to fit on one screen. */
private const val PAGE_SIZE = 30

/** The popularity the first movie of the first page carries, each one after it carrying less. */
private const val TOP_POPULARITY = 1000.0

/** The genre every movie a page holds carries, so narrowing by it keeps the whole page. */
private val SHARED_GENRE = Genre.HORROR

/**
 * A page of movies, each less popular than the one before, starting at [from]. Ids and titles are
 * padded, so every ordering the sheet offers reads them in the order the page is built in.
 */
private fun page(from: Int) =
    List(PAGE_SIZE) { index ->
        val position = "%02d".format(from + index)
        MovieFixture.movie(
            id = position,
            title = "Movie $position",
            genres = listOf(SHARED_GENRE),
            popularity = TOP_POPULARITY - (from + index),
        )
    }

/** A cache too long to fit on one screen, already in the order the default sort puts it in. */
private val LONG_CACHE = page(from = 0)

/** What a second page appends, which the default sort puts after every row of [LONG_CACHE]. */
private val NEXT_PAGE = page(from = PAGE_SIZE)

/** The title of the row every ordering of [LONG_CACHE] leads with. */
private val TOP_OF_LONG_CACHE = LONG_CACHE.first().title

/** The title of the row [LONG_CACHE] ends on, which a test scrolls to. */
private val BOTTOM_OF_LONG_CACHE = LONG_CACHE.last().title

/** The state a screen with nothing cached and a failed refresh renders. */
private fun failed(cause: DataError) =
    emptyList<Movie>().toTrendingUiState(
        selectedGenreNames = emptyList(),
        sort = MovieSort.DEFAULT,
        refresh = RefreshState(error = cause),
    )

/** The rows the state is showing, which only a state carrying movies has. */
private val TrendingUiState.rows: List<MovieRowUiState>
    get() = (content as TrendingContent.Movies).rows

private fun posterLabel(title: String) = string(CoreUiR.string.core_ui_poster_description, title)

/**
 * What the filter action is called.
 *
 * @param active how many genres are selected, plus one for a sort away from the default. Zero
 * leaves the action unbadged.
 */
private fun filterLabel(active: Int = 0) =
    if (active == 0) {
        string(R.string.feature_trending_filter_action)
    } else {
        quantityString(R.plurals.feature_trending_filter_action_active, active, active)
    }
