package com.shayan.amro.feature.trending.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.testing.fake.FakeTrendingMoviesRepository
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.rule.MainDispatcherRule
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.trending.R
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TrendingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTrendingMoviesRepository()

    @Test
    fun `an empty cache with no error is the skeleton`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(TrendingContent.Skeleton, awaitItem().content)
            }
        }

    @Test
    fun `a cached set renders in popularity descending order`() =
        runTest {
            repository.seedTrending(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                val state = awaitItem()

                // Asserted as an order rather than as a list, because the fixture ties on
                // popularity and restating how a tie breaks would restate the ordering itself.
                val popularities = state.rows().map { POPULARITY_BY_TITLE.getValue(it.title) }

                assertEquals(popularities.sortedDescending(), popularities)
                assertEquals(MovieSort.DEFAULT.key, state.selectedSortKey())
                assertEquals(MovieSort.DEFAULT.direction, state.selectedDirection())
            }
        }

    @Test
    fun `a failed refresh over an empty cache is the error, carrying the cause`() =
        runTest {
            repository.scriptRefreshFailure(DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onRefresh()
                val content = expectMostRecentItem().content

                assertTrue(content is TrendingContent.Error, "expected an error, was $content")
                assertEquals(
                    AmroText.Resource(R.string.feature_trending_error_no_connectivity),
                    content.title,
                )
            }
        }

    @Test
    fun `a failed refresh over a cached set keeps the rows and raises a notice`() =
        runTest {
            repository.seedTrending(CACHE)
            repository.scriptRefreshFailure(DataError.Server)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onRefresh()
                val state = expectMostRecentItem()

                assertEquals(CACHE.size, state.rows().size)
                assertEquals(
                    AmroText.Resource(R.string.feature_trending_notice_server),
                    state.notice?.message,
                )
            }
        }

    @Test
    fun `a successful refresh clears the notice`() =
        runTest {
            repository.seedTrending(CACHE)
            repository.scriptRefreshFailure(DataError.Server)
            repository.scriptRefresh(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onRefresh()
                viewModel.onRefresh()

                assertNull(expectMostRecentItem().notice)
            }
        }

    @Test
    fun `a running refresh clears the cause the last one failed with`() =
        runTest {
            repository.seedTrending(CACHE)
            repository.scriptRefreshFailure(DataError.Server)
            repository.scriptRefreshFailure(DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onRefresh()
                assertEquals(
                    AmroText.Resource(R.string.feature_trending_notice_server),
                    expectMostRecentItem().notice?.message,
                )

                val gate = repository.holdRefreshes()
                viewModel.onRefresh()
                val whileRunning = expectMostRecentItem()

                assertTrue(whileRunning.isRefreshing)
                assertNull(whileRunning.notice)
                gate.complete(Unit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the launch refresh runs once, however often the screen is shown`() =
        runTest {
            repository.scriptRefresh(CACHE)
            val viewModel = viewModel()

            viewModel.onLaunch()
            viewModel.onLaunch()

            assertEquals(1, repository.refreshCount)
        }

    @Test
    fun `a refresh the user asks for runs after the launch refresh has`() =
        runTest {
            repository.scriptRefresh(CACHE)
            repository.scriptRefresh(CACHE)
            val viewModel = viewModel()

            viewModel.onLaunch()
            viewModel.onRefresh()

            assertEquals(2, repository.refreshCount)
        }

    @Test
    fun `a selection that matches nothing is empty from filter, not an error`() =
        runTest {
            repository.seedTrending(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onToggleGenre(UNMATCHED_GENRE.name)

                assertEquals(TrendingContent.EmptyFromFilter, expectMostRecentItem().content)
            }
        }

    @Test
    fun `an empty selection over a cached set is never empty from filter`() =
        runTest {
            repository.seedTrending(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onToggleGenre(UNMATCHED_GENRE.name)
                viewModel.onToggleGenre(UNMATCHED_GENRE.name)
                val state = expectMostRecentItem()

                assertEquals(emptySet(), state.selectedGenreNames())
                assertEquals(CACHE.size, state.rows().size)
            }
        }

    @Test
    fun `the active count is the genres plus one for a sort away from the default`() =
        runTest {
            repository.seedTrending(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(0, awaitItem().activeSelectionCount)

                viewModel.onSelectSortKey(SortKey.TITLE)
                assertEquals(1, expectMostRecentItem().activeSelectionCount)

                viewModel.onReset()
                viewModel.onToggleGenre(Genre.COMEDY.name)
                viewModel.onToggleGenre(Genre.DRAMA.name)
                assertEquals(2, expectMostRecentItem().activeSelectionCount)

                viewModel.onSelectSortDirection(SortDirection.ASCENDING)
                assertEquals(3, expectMostRecentItem().activeSelectionCount)
            }
        }

    @Test
    fun `clearing genres empties the selection and leaves the sort`() =
        runTest {
            repository.seedTrending(CACHE)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                viewModel.onToggleGenre(MATCHED_GENRE.name)
                viewModel.onSelectSortKey(SortKey.TITLE)
                viewModel.onClearGenres()
                val state = expectMostRecentItem()

                assertEquals(emptySet(), state.selectedGenreNames())
                assertEquals(SortKey.TITLE, state.selectedSortKey())
            }
        }

    @Test
    fun `a selection written to the handle is restored by a view model built on it`() =
        runTest {
            repository.seedTrending(CACHE)
            val handle = SavedStateHandle()
            val before = viewModel(handle)

            before.uiState.test {
                awaitItem()
                before.onToggleGenre(MATCHED_GENRE.name)
                before.onSelectSortKey(SortKey.RELEASE_DATE)
                before.onSelectSortDirection(SortDirection.ASCENDING)
                expectMostRecentItem()
            }

            val after = viewModel(handle)

            after.uiState.test {
                val state = awaitItem()

                assertEquals(setOf(MATCHED_GENRE.name), state.selectedGenreNames())
                assertEquals(SortKey.RELEASE_DATE, state.selectedSortKey())
                assertEquals(SortDirection.ASCENDING, state.selectedDirection())
            }
        }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        TrendingViewModel(
            trendingMoviesRepository = repository,
            savedStateHandle = handle,
        )
}

/** Every movie the fixture holds, which is what a cache seeded with it renders. */
private val CACHE = MovieFixture.MOVIES_IN_NO_ORDER

/** What each movie in [CACHE] is as popular as, so a rendered row can be read back to one. */
private val POPULARITY_BY_TITLE = CACHE.associate { it.title to it.popularity }

/** A genre no movie in [CACHE] carries. */
private val UNMATCHED_GENRE = Genre.WESTERN

/** A genre two movies in [CACHE] carry. */
private val MATCHED_GENRE = Genre.DRAMA

/**
 * The rows the state is showing.
 *
 * @return the rows, or fails the test when the body is anything else.
 */
private fun TrendingUiState.rows(): List<MovieRowUiState> {
    val content = content
    assertTrue(content is TrendingContent.Movies, "expected movies, was $content")
    return content.rows
}

/** The genres the sheet is showing as selected, as the enum's own names. */
private fun TrendingUiState.selectedGenreNames(): Set<String> =
    filter.genres.filter { it.isSelected }.mapTo(mutableSetOf()) { it.value }

/** The key the sheet is showing as selected. */
private fun TrendingUiState.selectedSortKey(): SortKey =
    filter.sortKeys.first { it.isSelected }.value

/** The direction the sheet is showing as selected. */
private fun TrendingUiState.selectedDirection(): SortDirection =
    filter.directions.first { it.isSelected }.value
