package com.shayan.amro.feature.detail.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.testing.fake.FakeMovieDetailRepository
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.rule.MainDispatcherRule
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import com.shayan.amro.feature.detail.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MovieDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMovieDetailRepository()

    @Test
    fun `a cached record is loaded before any fetch returns`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.seedDetail(CACHED)
            repository.holdRefreshes()
            val viewModel = viewModel()

            viewModel.uiState.test {
                val content = awaitItem().content

                assertTrue(content is MovieDetailContent.Loaded, "expected loaded, was $content")
                assertEquals(CACHED.tagline, content.tagline)
            }
        }

    @Test
    fun `nothing cached and nothing failed is the skeleton`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.holdRefreshes()
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertIs<MovieDetailContent.Skeleton>(awaitItem().content)
            }
        }

    @Test
    fun `a failed fetch with nothing cached is the error, carrying the cause`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.scriptRefreshFailure(MOVIE.id, DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                val content = awaitItem().content

                assertTrue(content is MovieDetailContent.Error, "expected an error, was $content")
                assertEquals(
                    AmroText.Resource(R.string.feature_detail_error_no_connectivity),
                    content.title,
                )
            }
        }

    @Test
    fun `a failed fetch over a cached record stays loaded`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.seedDetail(CACHED)
            repository.scriptRefreshFailure(MOVIE.id, DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                val content = awaitItem().content
                viewModel.onRetry()

                // A refresh the user never asked for has nothing to report over a record that is
                // still correct, so nothing on screen changes.
                expectNoEvents()
                assertTrue(content is MovieDetailContent.Loaded, "expected loaded, was $content")
            }
        }

    @Test
    fun `a successful fetch replaces the record on screen`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.seedDetail(CACHED)
            repository.scriptRefresh(RELEASED)
            val gate = repository.holdRefreshes()
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                gate.complete(Unit)
                val content = expectMostRecentItem().content

                assertTrue(content is MovieDetailContent.Loaded, "expected loaded, was $content")
                val badge = content.badge
                assertTrue(badge is HeaderBadgeUiState.Rating, "expected a rating, was $badge")
                assertEquals(ratingText(checkNotNull(RELEASED.rating).average), badge.rating)
            }
        }

    @Test
    fun `the title is the cached movie's before any record arrives`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.holdRefreshes()
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(MOVIE.title, awaitItem().title)
            }
        }

    @Test
    fun `the launch fetch runs once however often the screen is shown`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.scriptRefresh(CACHED)
            val viewModel = viewModel()

            viewModel.uiState.test { awaitItem() }
            // Past the subscription timeout, so the next collector starts the flow again.
            advanceTimeBy((SUBSCRIPTION_TIMEOUT_MILLIS + 1).milliseconds)
            viewModel.uiState.test { awaitItem() }

            assertEquals(listOf(MOVIE.id), repository.requestedIds)
        }

    @Test
    fun `retry fetches again, and the cause names the newest attempt`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.scriptRefreshFailure(MOVIE.id, DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                repository.scriptRefreshFailure(MOVIE.id, DataError.Server)
                viewModel.onRetry()
                val content = expectMostRecentItem().content

                assertTrue(content is MovieDetailContent.Error, "expected an error, was $content")
                assertEquals(AmroText.Resource(R.string.feature_detail_error_server), content.title)
            }

            assertEquals(listOf(MOVIE.id, MOVIE.id), repository.requestedIds)
        }

    @Test
    fun `a retry that succeeds leaves no cause behind`() =
        runTest {
            repository.seedMovie(MOVIE)
            repository.scriptRefreshFailure(MOVIE.id, DataError.NoConnectivity)
            val viewModel = viewModel()

            viewModel.uiState.test {
                awaitItem()
                repository.scriptRefresh(RELEASED)
                viewModel.onRetry()
                val content = expectMostRecentItem().content

                assertTrue(content is MovieDetailContent.Loaded, "expected loaded, was $content")
            }
        }

    private fun viewModel() =
        MovieDetailViewModel(
            movieId = MOVIE.id,
            movieDetailRepository = repository,
        )
}

/** The movie the trending set holds, which the screen was opened from. */
private val MOVIE = MovieFixture.MOVIE

/** The record cached before the test began: unreleased, so it carries no rating. */
private val CACHED: MovieDetail =
    MovieFixture.detail(
        movie = MOVIE,
        status = ReleaseStatus.IN_PRODUCTION,
        budget = null,
        revenue = null,
        rating = null,
    )

/** The same movie once it has been released, which a successful fetch replaces [CACHED] with. */
private val RELEASED: MovieDetail = MovieFixture.detail(movie = MOVIE)
