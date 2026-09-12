package com.shayan.amro.feature.discover.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.testing.fake.FakeMoviesRepository
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

/** A narrowing and an ordering away from the default, so every field of a key is exercised. */
private val NARROWED =
    MovieSelection(
        genres = setOf(Genre.COMEDY),
        sort = MovieSort(SortKey.TITLE, SortDirection.ASCENDING),
    )

private val MOVIES = MovieFixture.rankedMovies(count = 3)

private val OTHER_MOVIES = MovieFixture.rankedMovies(count = 2)

class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMoviesRepository()

    @Test
    fun `the list opens on everything, in the order the product opens on`() =
        runTest {
            repository.seed(MovieSelection(), MOVIES)

            val rows = viewModel().movieRows.asSnapshot()

            assertEquals(MOVIES.map { it.title }, rows.map { it.title })
            assertEquals(listOf(MovieSelection()), repository.requestedSelections)
        }

    @Test
    fun `selecting a genre asks for the movies that narrowing describes`() =
        runTest {
            repository.seed(MovieSelection(), MOVIES)
            repository.seed(MovieSelection(genres = setOf(Genre.COMEDY)), OTHER_MOVIES)
            val viewModel = viewModel()

            viewModel.onToggleGenre(Genre.COMEDY.name)

            assertEquals(
                OTHER_MOVIES.map { it.title },
                viewModel.movieRows.asSnapshot().map { it.title },
            )
        }

    @Test
    fun `selecting a genre twice puts the list back where it started`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onToggleGenre(Genre.COMEDY.name)
            viewModel.onToggleGenre(Genre.COMEDY.name)

            viewModel.uiState.test {
                assertEquals(0, awaitItem().activeSelectionCount)
            }
        }

    @Test
    fun `an ordering the user chose counts toward the badge`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onSelectSortKey(SortKey.TITLE)
            viewModel.onSelectSortDirection(SortDirection.ASCENDING)
            viewModel.onToggleGenre(Genre.COMEDY.name)

            viewModel.uiState.test {
                assertEquals(2, awaitItem().activeSelectionCount)
            }
        }

    @Test
    fun `clearing the genres leaves the order the user chose`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onSelectSortKey(SortKey.TITLE)
            viewModel.onToggleGenre(Genre.COMEDY.name)

            viewModel.onClearGenres()

            viewModel.uiState.test {
                val filter = awaitItem().filter
                assertEquals(emptyList(), filter.genres.filter { it.isSelected })
                assertEquals(
                    listOf(SortKey.TITLE),
                    filter.sortKeys.filter { it.isSelected }.map { it.value },
                )
            }
        }

    @Test
    fun `a reset drops the genres and returns the order to the default`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onSelectSortKey(SortKey.TITLE)
            viewModel.onSelectSortDirection(SortDirection.ASCENDING)
            viewModel.onToggleGenre(Genre.COMEDY.name)

            viewModel.onReset()

            viewModel.uiState.test {
                assertEquals(0, awaitItem().activeSelectionCount)
            }
        }

    @Test
    fun `a selection written to the handle is restored by a view model built on it`() =
        runTest {
            val handle = SavedStateHandle()
            val first = viewModel(handle)
            first.onToggleGenre(Genre.COMEDY.name)
            first.onSelectSortKey(SortKey.TITLE)
            first.onSelectSortDirection(SortDirection.ASCENDING)
            first.uiState.test { awaitItem() }

            repository.seed(NARROWED, OTHER_MOVIES)
            val restored = viewModel(handle)

            assertEquals(
                OTHER_MOVIES.map { it.title },
                restored.movieRows.asSnapshot().map { it.title },
            )
        }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        DiscoverViewModel(moviesRepository = repository, savedStateHandle = handle)
}
