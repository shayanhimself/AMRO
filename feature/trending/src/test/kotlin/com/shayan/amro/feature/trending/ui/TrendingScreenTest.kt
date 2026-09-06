package com.shayan.amro.feature.trending.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        var selected: Pair<String, String>? = null
        setContent(loaded(), onMovieClick = { sourceId, movieId -> selected = sourceId to movieId })

        composeRule.onNodeWithText(TOP_TITLE).performClick()

        val id = CACHE.first { it.title == TOP_TITLE }.id
        assertEquals(id.source.value to id.value, selected)
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

    /** Renders the stateless screen over one state. */
    private fun setContent(
        uiState: TrendingUiState,
        onMovieClick: (sourceId: String, movieId: String) -> Unit = { _, _ -> },
        onRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                TrendingScreen(
                    uiState = uiState,
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
    genreNames: List<String> = emptyList(),
    sort: MovieSort = MovieSort.DEFAULT,
    refresh: RefreshState = RefreshState(),
) = CACHE.toTrendingUiState(selectedGenreNames = genreNames, sort = sort, refresh = refresh)

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

private fun posterLabel(title: String) = string(R.string.feature_trending_poster_description, title)

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
